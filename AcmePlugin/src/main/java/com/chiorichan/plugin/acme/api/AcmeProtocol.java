package com.chiorichan.plugin.acme.api;

import com.chiorichan.helpers.TrustManagerFactory;
import com.chiorichan.http.HttpCode;
import com.chiorichan.lang.EnumColor;
import com.chiorichan.logger.Log;
import com.chiorichan.plugin.acme.AcmePlugin;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.plugin.acme.lang.AcmeForbiddenError;
import com.chiorichan.plugin.acme.lang.AcmeState;
import com.chiorichan.site.DomainMapping;
import com.chiorichan.site.SiteManager;
import com.chiorichan.zutils.ZHttp;
import com.chiorichan.zutils.ZIO;
import com.chiorichan.zutils.ZStrings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.x509.util.StreamParsingException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings( "serial" )
public class AcmeProtocol
{
	private String nextNonce = null;
	private final String agreementUrl;
	private final AcmeStorage storage;
	private final KeyPair keyPair;
	private AcmeChallenge challenge = newChallenge();

	private final String urlNewAuthz; // https://acme-staging.api.letsencrypt.org/acme/new-authz
	private final String urlNewCert; // https://acme-staging.api.letsencrypt.org/acme/new-cert
	private final String urlNewReg; // https://acme-staging.api.letsencrypt.org/acme/new-reg
	private final String urlRevokeCert; // https://acme-staging.api.letsencrypt.org/acme/revoke-cert

	// certificate X.509 and key PKCS#8

	public AcmeProtocol( String url, String agreementUrl, AcmeStorage storage ) throws AcmeException
	{
		Validate.notEmpty( url, "URL must be set!" );
		Validate.notEmpty( agreementUrl, "AgreementURL must be set!" );

		this.agreementUrl = agreementUrl;
		this.storage = storage;
		this.keyPair = storage.defaultPrivateKey();

		Validate.notNull( keyPair );

		byte[] result = ZHttp.readUrl( url, true );

		if ( result == null || result.length == 0 )
			throw new AcmeException( "AcmePlugin failed to get CA directory from url \"" + url + "\", result was null!" );

		JsonElement element = new JsonParser().parse( new String( result ) );
		JsonObject obj = element.getAsJsonObject();

		urlNewAuthz = ZStrings.trimAll( obj.get( "new-authz" ).toString().trim(), '"' );
		urlNewCert = ZStrings.trimAll( obj.get( "new-cert" ).toString().trim(), '"' );
		urlNewReg = ZStrings.trimAll( obj.get( "new-reg" ).toString().trim(), '"' );
		urlRevokeCert = ZStrings.trimAll( obj.get( "revoke-cert" ).toString().trim(), '"' );
	}

	public AcmeState checkDomainVerification( String domain, String subdomain, boolean force ) throws AcmeException
	{
		AcmePlugin plugin = AcmePlugin.instance();

		SingleChallengeHttp sac = validateDomain( challenge, domain, subdomain, force );

		if ( sac == null )
			return AcmeState.FAILED;

		if ( sac.isValid() )
		{
			Log.get().fine( EnumColor.DARK_AQUA + String.format( "%s: Domain verified as valid!", subdomain == null ? domain : subdomain + "." + domain ) );
			return AcmeState.SUCCESS;
		}
		else if ( sac.isPending() )
		{
			DomainMapping mapping = SiteManager.instance().getDomainMapping( sac.getFullDomain() );
			if ( mapping == null )
				challenge.remove( sac );
			else if ( !sac.hasCallBack() && sac.getChallengeToken() != null )
			{
				File acmeChallengeFile = new File( mapping.directory(), sac.getTokenPath() );

				try
				{
					if ( acmeChallengeFile.getParentFile().exists() || acmeChallengeFile.getParentFile().mkdirs() )
						ZIO.writeStringToFile( acmeChallengeFile, sac.getChallengeContent() );
					else
						throw new IOException( "There was problem creating the '.well-known' directory: " + acmeChallengeFile.getAbsolutePath() );
				}
				catch ( IOException e )
				{
					e.printStackTrace();
				}

				sac.doCallback( false, () ->
				{
					// // acmeChallengeFile.delete();
					File wellKnown = new File( mapping.directory(), ".well-known" );
					if ( wellKnown.exists() )
						wellKnown.delete();

					if ( !sac.isValid() )
						plugin.getLogger().info( EnumColor.RED + sac.getFullDomain() + ": Domain Challenge Failed for reason " + sac.getState() + " " + sac.lastMessage() );
					else
						plugin.getLogger().info( EnumColor.AQUA + sac.getFullDomain() + ": Domain Challenge Success" );
				} );
			}

			Log.get().info( EnumColor.AQUA + String.format( "%s: Verification Pending", subdomain == null ? domain : subdomain + "." + domain ) );
			return AcmeState.PENDING;
		}
		else
			return AcmeState.FAILED;
	}

	public AcmeStorage getAcmeStorage()
	{
		return storage;
	}

	public AcmeChallenge getChallenge()
	{
		return challenge;
	}

	protected KeyPair getDefaultKeyPair()
	{
		return keyPair;
	}

	public String getUrlNewAuthz()
	{
		return urlNewAuthz;
	}

	public String getUrlNewCert()
	{
		return urlNewCert;
	}

	public String getUrlNewRef()
	{
		return urlNewReg;
	}

	public String getUrlNewRevoke()
	{
		return urlRevokeCert;
	}

	public AcmeChallenge newChallenge()
	{
		return new AcmeChallenge( this );
	}

	protected HttpResponse newChallenge0( String domain ) throws AcmeException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException, IOException
	{
		String body = newJwt( new TreeMap<String, Object>()
		{
			{
				put( "resource", "new-authz" );
				put( "identifier", new TreeMap<String, Object>()
				{
					{
						put( "type", "dns" );
						put( "value", domain );
					}
				} );
			}
		} );

		HttpResponse response = AcmeUtils.post( "POST", urlNewAuthz, "application/json", body, "application/json" );

		nonce( response.getHeaderString( "Replay-Nonce" ) );

		return response;
	}

	public String newJwt( Map<String, Object> claims ) throws AcmeException
	{
		return Jwts.builder().setHeaderParam( "nonce", nonce() ).setHeaderParam( JwsHeader.JSON_WEB_KEY, AcmeUtils.getWebKey( keyPair.getPublic() ) ).setClaims( claims ).signWith( SignatureAlgorithm.RS256, keyPair.getPrivate() ).compact();
	}

	public String newRegistration() throws AcmeException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException, IOException
	{
		String body = newJwt( new TreeMap<String, Object>()
		{
			{
				put( "resource", "new-reg" );
			}
		} );

		HttpResponse response = AcmeUtils.post( "POST", urlNewReg, "application/json", body, "application/json" );

		nonce( response.getHeaderString( "Replay-Nonce" ) );

		if ( response.getStatus() != HttpCode.HTTP_CREATED && response.getStatus() != HttpCode.HTTP_CONFLICT )
			throw new AcmeException( "Registration Failed!" );

		return response.getHeaderString( "Location" );
	}

	public AcmeCertificateRequest newSigningRequest( Collection<String> domains ) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, OperatorCreationException, AcmeException, IOException, StreamParsingException
	{
		return new AcmeCertificateRequest( this, domains, null );
	}

	public AcmeCertificateRequest newSigningRequest( Collection<String> domains, File keyFile ) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, OperatorCreationException, AcmeException, IOException, StreamParsingException
	{
		return new AcmeCertificateRequest( this, domains, keyFile );
	}

	protected String nonce() throws AcmeException
	{
		if ( nextNonce == null )
			try
			{
				URL url = new URL( urlNewReg );

				HttpURLConnection.setFollowRedirects( false );
				HttpsURLConnection connection = ( HttpsURLConnection ) url.openConnection();

				try
				{
					SSLContext ctx = SSLContext.getInstance( "SSL" );
					ctx.init( null, TrustManagerFactory.getTrustManagers(), null );
					connection.setSSLSocketFactory( ctx.getSocketFactory() );
				}
				catch ( KeyManagementException | NoSuchAlgorithmException e )
				{
					Log.get().severe( "Failed to set the SSL Factory, so all certificates are accepted.", e );
				}

				connection.setRequestMethod( "HEAD" );
				connection.connect();

				nextNonce = connection.getHeaderField( "Replay-Nonce" );
			}
			catch ( IOException e )
			{
				throw new AcmeException( "Failure getting the first nonce", e );
			}

		return nextNonce;
	}

	public void nonce( String nonce )
	{
		nextNonce = nonce;
	}

	public boolean signAgreement( String registrationUrl, String[] contacts ) throws AcmeException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, IOException
	{
		String body = newJwt( new TreeMap<String, Object>()
		{
			{
				put( "resource", "reg" );
				if ( contacts != null && contacts.length > 0 )
					put( "contact", contacts );
				put( "agreement", agreementUrl );
			}
		} );

		HttpResponse response = AcmeUtils.post( "POST", registrationUrl, "application/json", body, "application/json" );

		nonce( response.getHeaderString( "Replay-Nonce" ) );

		// {"id":102388,"key":{"kty":"RSA","n":"z1QxEA...GrdSkH","e":"AZAB"},"contact":["mailto:me@chiorichan.com"],"agreement":"https://letsencrypt.org/documents/LE-SA-v1.0.1-July-27-2015.pdf","initialIp":"0.0.0.0","createdAt":"2015-12-11T02:00:19Z"}

		return response.getStatus() == HttpCode.HTTP_ACCEPTED;
	}

	private SingleChallengeHttp validateDomain( AcmeChallenge challenge, String domain, String subdomain, boolean force ) throws AcmeException
	{
		AcmePlugin plugin = AcmePlugin.instance();

		try
		{
			return challenge.add( domain, subdomain, force );
		}
		catch ( AcmeForbiddenError e )
		{
			try
			{
				if ( signAgreement( plugin.getRegistrationUrl(), plugin.getContacts() ) )
					return challenge.add( domain, subdomain, force );
				else
					throw new AcmeException( "Failed to sign agreement on users behalf, fatal error!" );
			}
			catch ( KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | IOException e1 )
			{
				e1.printStackTrace();
			}
		}
		return null;
	}
}
