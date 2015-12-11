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
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.Validate;

import com.chiorichan.APILogger;
import com.chiorichan.http.HttpCode;
import com.chiorichan.plugin.acme.AcmePlugin;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.util.NetworkFunc;
import com.chiorichan.util.StringFunc;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SuppressWarnings( "serial" )
public class AcmeProtocol
{
	// {"new-authz":"https://acme-staging.api.letsencrypt.org/acme/new-authz","new-cert":"https://acme-staging.api.letsencrypt.org/acme/new-cert","new-reg":"https://acme-staging.api.letsencrypt.org/acme/new-reg","revoke-cert":"https://acme-staging.api.letsencrypt.org/acme/revoke-cert"}
	// certificate X.509 and key PKCS#8

	private String nextNonce = null;
	private String agreementUrl;
	private KeyPair keyPair;

	protected final String urlNewAuthz;
	protected final String urlNewCert;
	protected final String urlNewReg;
	protected final String urlRevokeCert;

	public AcmeProtocol( String url, String agreementUrl, KeyPair keyPair )
	{
		Validate.notEmpty( url, "URL must be set!" );
		Validate.notEmpty( agreementUrl, "AgreementURL must be set!" );
		Validate.notNull( keyPair );

		this.agreementUrl = agreementUrl;
		this.keyPair = keyPair;

		byte[] result = NetworkFunc.readUrl( url );

		JsonElement element = new JsonParser().parse( new String( result ) );
		JsonObject obj = element.getAsJsonObject();

		urlNewAuthz = StringFunc.trimAll( obj.get( "new-authz" ).toString().trim(), '"' );
		urlNewCert = StringFunc.trimAll( obj.get( "new-cert" ).toString().trim(), '"' );
		urlNewReg = StringFunc.trimAll( obj.get( "new-reg" ).toString().trim(), '"' );
		urlRevokeCert = StringFunc.trimAll( obj.get( "revoke-cert" ).toString().trim(), '"' );
	}

	protected KeyPair getKeyPair()
	{
		return keyPair;
	}

	private APILogger getLogger()
	{
		return Plugin.getPlugin( AcmePlugin.class ).getLogger();
	}

	public AcmeChallenge newChallenge() throws AcmeException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException, IOException
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

		nextNonce = response.getHeaderString( "Replay-Nonce" );

		// {"id":102388,"key":{"kty":"RSA","n":"z1QxEAo9CzI14BHecj94rncGPyc0fCx315oO7kralJxU4G5EDCZ9May_WMrJvLYMXZCjMyvNrDx8inbpJRY4rw22EeSgunNEnR067Me8YFZwTEP3PX1SDgUWPmeX0mmyHiMvHRV9Z73M8hvFrf6QW4oycGwmb2DQo6fpn57eTJckNE0r0GBVgXcDrZMkGo_1uC80p7BU6lVQmUKbnk4FOOi4YU07zPqSlNzftHHDH-gurOHEYi03YgBlzj4BzMv7cEDoUQ0mAVs-KDCKUp4Xj96YwNEeviO8y2lfeimBJku7jr7RsikAdNnWjflyPWaIUkeUA0IgjU8o-jzPivZszVd8EzECqAClAze_XKo4nZ9T0qAyYXACv4A3Fe8h2SIxCoCAonEU0jCEnQr0kEAEDKwy-TDCI-wdjRwzg5Gfnx9JZRabfVrO2X7yZBfyzah3ToMK7O--oeJw0LBzFs8ww_-NJODTdySxdAw9F5q-EQOj3Hv-kJVzWAXmqvs9TyuXMyMEOS6r_qX_vWPfueD6RRSGLg4mXIws5eTOIaVmEk8Js3TkkYzqL4TmSSPa-E9p4bXsMTM0gYlHhShjMw2uVV56_gNGRBlZI1h8jiREAQdglGZKsHcaNk_FhyWIvzBUEcPZuadNi5TeuNzaL609oK3oTyE9byij-KX6GrdSkH8","e":"AQAB"},"contact":["mailto:chiorigreene@gmail.com"],"agreement":"https://letsencrypt.org/documents/LE-SA-v1.0.1-July-27-2015.pdf","initialIp":"99.198.198.240","createdAt":"2015-12-11T02:00:19Z"}

		return response.getStatus() == HttpCode.HTTP_ACCEPTED;
	}
}
