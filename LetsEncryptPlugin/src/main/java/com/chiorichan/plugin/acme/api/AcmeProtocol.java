package com.chiorichan.plugin.acme.api;

import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.Validate;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.x509.util.StreamParsingException;

import com.chiorichan.http.HttpCode;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.util.NetworkFunc;
import com.chiorichan.util.StringFunc;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SuppressWarnings( "serial" )
public class AcmeProtocol
{
	private String nextNonce = null;
	private String agreementUrl;
	private AcmeStorage storage;
	private KeyPair keyPair;

	protected final String urlNewAuthz; // https://acme-staging.api.letsencrypt.org/acme/new-authz
	protected final String urlNewCert; // https://acme-staging.api.letsencrypt.org/acme/new-cert
	protected final String urlNewReg; // https://acme-staging.api.letsencrypt.org/acme/new-reg
	protected final String urlRevokeCert; // https://acme-staging.api.letsencrypt.org/acme/revoke-cert

	// certificate X.509 and key PKCS#8

	public AcmeProtocol( String url, String agreementUrl, AcmeStorage storage ) throws AcmeException
	{
		Validate.notEmpty( url, "URL must be set!" );
		Validate.notEmpty( agreementUrl, "AgreementURL must be set!" );

		this.agreementUrl = agreementUrl;
		this.storage = storage;
		this.keyPair = storage.defaultPrivateKey();

		Validate.notNull( keyPair );

		byte[] result = NetworkFunc.readUrl( url );

		JsonElement element = new JsonParser().parse( new String( result ) );
		JsonObject obj = element.getAsJsonObject();

		urlNewAuthz = StringFunc.trimAll( obj.get( "new-authz" ).toString().trim(), '"' );
		urlNewCert = StringFunc.trimAll( obj.get( "new-cert" ).toString().trim(), '"' );
		urlNewReg = StringFunc.trimAll( obj.get( "new-reg" ).toString().trim(), '"' );
		urlRevokeCert = StringFunc.trimAll( obj.get( "revoke-cert" ).toString().trim(), '"' );
	}

	public AcmeStorage getAcmeStorage()
	{
		return storage;
	}

	protected KeyPair getDefaultKeyPair()
	{
		return keyPair;
	}

	public AcmeChallenge newChallenge()
	{
		return new AcmeChallenge( this );
	}

	protected HttpResponse newChallenge0( String domain ) throws AcmeException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException
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

		nextNonce = response.getHeaderString( "Replay-Nonce" );

		return response;
	}

	protected String newJwt( Map<String, Object> claims ) throws AcmeException
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

		nextNonce = response.getHeaderString( "Replay-Nonce" );

		if ( response.getStatus() != HttpCode.HTTP_CREATED && response.getStatus() != HttpCode.HTTP_CONFLICT )
			throw new AcmeException( "Registration Failed!" );

		return response.getHeaderString( "Location" );
	}

	public AcmeCertificateRequest newSigningRequest( List<String> domains ) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, OperatorCreationException, AcmeException, IOException, StreamParsingException
	{
		return new AcmeCertificateRequest( this, domains );
	}

	protected String nonce() throws AcmeException
	{
		if ( nextNonce == null )
			try
			{
				URL url = new URL( urlNewReg );

				HttpURLConnection.setFollowRedirects( false );
				HttpURLConnection connection = ( HttpURLConnection ) url.openConnection();
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

	protected void nonce( String nonce )
	{
		nextNonce = nonce;
	}

	public boolean signAgreement( String registrationUrl, String[] contacts ) throws AcmeException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException
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

		nextNonce = response.getHeaderString( "Replay-Nonce" );

		// {"id":102388,"key":{"kty":"RSA","n":"z1QxEA...GrdSkH","e":"AZAB"},"contact":["mailto:me@chiorichan.com"],"agreement":"https://letsencrypt.org/documents/LE-SA-v1.0.1-July-27-2015.pdf","initialIp":"0.0.0.0","createdAt":"2015-12-11T02:00:19Z"}

		return response.getStatus() == HttpCode.HTTP_ACCEPTED;
	}
}
