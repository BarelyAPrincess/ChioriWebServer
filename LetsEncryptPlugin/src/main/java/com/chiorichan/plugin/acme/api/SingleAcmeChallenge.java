package com.chiorichan.plugin.acme.api;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.TreeMap;

import com.chiorichan.Loader;
import com.chiorichan.http.HttpCode;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.acme.AcmePlugin;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.plugin.acme.lang.AcmeState;
import com.chiorichan.plugin.lang.PluginNotFoundException;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Ticks;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;

@SuppressWarnings( "serial" )
public class SingleAcmeChallenge
{
	private AcmeState state = AcmeState.CREATED;

	private final AcmeChallengeType challengeType;
	private final String challengeToken;
	private final String challengeUri;
	private final String rootDomain;
	private final String subdomain;
	private String lastMessage;

	private final AcmeProtocol proto;

	protected SingleAcmeChallenge( AcmeProtocol proto, AcmeChallengeType challengeType, String challengeToken, String challengeUri, String rootDomain, String subdomain )
	{
		this.challengeType = challengeType;
		this.challengeToken = challengeToken;
		this.challengeUri = challengeUri;
		this.rootDomain = rootDomain;
		this.subdomain = subdomain;
		this.proto = proto;
	}

	public void doCallback( Runnable runnable )
	{
		if ( state == AcmeState.CREATED )
			verify();

		try
		{
			AcmePlugin plugin = ( AcmePlugin ) PluginManager.INSTANCE.getPluginByClass( AcmePlugin.class );

			TaskManager.INSTANCE.scheduleAsyncRepeatingTask( plugin, Ticks.SECOND_5, Ticks.SECOND, new Runnable()
			{
				@Override
				public void run()
				{
					verify();

					if ( getState() != AcmeState.PENDING )
					{
						TaskManager.INSTANCE.cancelTask( this );
						runnable.run();
					}
				}
			} );
		}
		catch ( PluginNotFoundException e )
		{
			Loader.getLogger().severe( "There was a severe internal plugin error", e );
		}
	}

	public String getChallengeContent()
	{
		return challengeToken + "." + AcmeUtils.getWebKeyThumbprintSHA256( proto.getDefaultKeyPair().getPublic() );
	}

	public String getChallengeToken()
	{
		return challengeToken;
	}

	public AcmeChallengeType getChallengeType()
	{
		return challengeType;
	}

	public String getFullDomain()
	{
		return subdomain + "." + rootDomain;
	}

	public String getRootDomain()
	{
		return rootDomain;
	}

	public AcmeState getState()
	{
		return state;
	}

	public String getSubDomain()
	{
		return subdomain;
	}

	private boolean handleVerify( HttpResponse response ) throws AcmeException
	{
		try
		{
			if ( response.getStatus() == HttpCode.HTTP_ACCEPTED )
			{
				JsonNode json = new ObjectMapper().readTree( response.getBody() );

				if ( "invalid".equals( json.get( "status" ).asText() ) )
				{
					/*
					 * {
					 * "error": {
					 * "detail": "Error parsing key authorization file: Invalid key authorization: 8 parts",
					 * "type": "urn:acme:error:unauthorized"
					 * },
					 * "keyAuthorization": "jg1pOr5gOM6elcxRctIxpEHzrYdOLgWTyyiZvxRJrGU.uY1fKhx5_30V4mwo1tw9C9hhUHLYb6pj3IUiCU9l304",
					 * "status": "invalid",
					 * "token": "jg1pOr5gOM6elcxRctIxpEHzrYdOLgWTyyiZvxRJrGU",
					 * "type": "http-01",
					 * "uri": "https://acme-staging.api.letsencrypt.org/acme/challenge/yndE7QefJALXVqnK9MYs7cT6O2-NSeSf7TsJzX3Hipw/851889",
					 * "validationRecord": [
					 * {
					 * "addressUsed": "104.28.2.100",
					 * "addressesResolved": [
					 * "104.28.2.100",
					 * "104.28.3.100"
					 * ],
					 * "hostname": "penoaks.com",
					 * "port": "80",
					 * "url": "http://penoaks.com/.well-known/acme-challenge/jg1pOr5gOM6elcxRctIxpEHzrYdOLgWTyyiZvxRJrGU"
					 * }
					 * ]
					 * }
					 */

					lastMessage = json.get( "error" ).get( "detail" ).asText();

					String usedIpAddr = json.get( "validationRecord" ).get( "addressUsed" ).asText();

					if ( !NetworkManager.getListeningIps().contains( usedIpAddr ) )
						lastMessage += String.format( " Additionally, the IP address tried does not match the IP address the server is listening on, '%s', '%s'", usedIpAddr, Joiner.on( "," ).join( NetworkManager.getListeningIps() ) );

					state = AcmeState.INVALID;
					return true;
				}
				else if ( "valid".equals( json.get( "status" ).asText() ) )
				{
					lastMessage = "Domain verification has been marked as valid";
					state = AcmeState.SUCCESS;
					return true;
				}
				else if ( "pending".equals( json.get( "status" ).asText() ) )
				{
					lastMessage = "Domain verification is still pending, please try again shortly";
					state = AcmeState.PENDING;
					return true;
				}
				else
					throw new AcmeException( "Unknown challenge status returned " + json.get( "status" ).asText() );
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		state = AcmeState.FAILED;
		return false;
	}

	public boolean hasFailed()
	{
		return state == AcmeState.INVALID || state == AcmeState.FAILED;
	}

	public String lastMessage()
	{
		if ( lastMessage == null )
			lastMessage = "No Message Available";
		return lastMessage;
	}

	public boolean verify()
	{
		try
		{
			return verifyWithException();
		}
		catch ( KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | AcmeException e )
		{
			return false;
		}
	}

	public boolean verifyWithException() throws AcmeException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException
	{
		try
		{
			if ( state == AcmeState.SUCCESS )
				return true;
			else if ( hasFailed() )
				return false;
			else if ( state == AcmeState.CREATED )
			{
				String body = proto.newJwt( new TreeMap<String, Object>()
				{
					{
						put( "resource", "challenge" );
						put( "type", "http-01" );
						put( "tls", true );
						put( "keyAuthorization", getChallengeContent() );
						put( "token", challengeToken );
					}
				} );

				HttpResponse response = AcmeUtils.post( "POST", challengeUri, "application/json", body, "application/json" );
				proto.nonce( response.getHeaderString( "Replay-Nonce" ) );

				return handleVerify( response );
			}
			else if ( state == AcmeState.PENDING )
			{
				HttpResponse response = AcmeUtils.get( challengeUri, "application/json" );
				return handleVerify( response );
			}
		}
		catch ( AcmeException | KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e )
		{
			lastMessage = e.getMessage();
			state = AcmeState.FAILED;
			throw e;
		}

		return false;
	}
}
