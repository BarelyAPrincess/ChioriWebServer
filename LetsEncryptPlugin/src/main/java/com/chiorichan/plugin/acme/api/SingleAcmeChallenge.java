package com.chiorichan.plugin.acme.api;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.TreeMap;

import com.chiorichan.http.HttpCode;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;

@SuppressWarnings( "serial" )
public class SingleAcmeChallenge
{
	private enum AcmeChallengeState
	{
		INVALID, FAILED, PENDING, SUCCESS, CREATED;
	}

	private AcmeChallengeState state = AcmeChallengeState.CREATED;

	private final AcmeChallengeType challengeType;
	private final String challengeToken;
	private final String challengeUri;
	private final String domain;
	private String lastMessage;

	private final AcmeProtocol proto;

	protected SingleAcmeChallenge( AcmeProtocol proto, AcmeChallengeType challengeType, String challengeToken, String challengeUri, String domain )
	{
		this.challengeType = challengeType;
		this.challengeToken = challengeToken;
		this.challengeUri = challengeUri;
		this.domain = domain;
		this.proto = proto;
	}

	public String getChallengeContent()
	{
		return challengeToken + "." + AcmeUtils.getWebKeyThumbprintSHA256( proto.getKeyPair().getPublic() );
	}

	public String getChallengeToken()
	{
		return challengeToken;
	}

	public AcmeChallengeType getChallengeType()
	{
		return challengeType;
	}

	public String getDomain()
	{
		return domain;
	}

	public AcmeChallengeState getState()
	{
		return state;
	}

	private boolean handleVerify( HttpResponse response ) throws AcmeException, JsonProcessingException, IOException
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

				lastMessage += String.format( " Additionally, the IP address tried does not match the IP address the server is listening on, '%s', '%s'", usedIpAddr, Joiner.on( "," ).join( NetworkManager.getListeningIps() ) );

				state = AcmeChallengeState.INVALID;
				return true;
			}
			else if ( "valid".equals( json.get( "status" ).asText() ) )
			{
				lastMessage = "Domain verification has been marked as valid";
				state = AcmeChallengeState.SUCCESS;
				return true;
			}
			else if ( "pending".equals( json.get( "status" ).asText() ) )
			{
				lastMessage = "Domain verification is still pending, please try again shortly";
				state = AcmeChallengeState.PENDING;
				return true;
			}
			else
				throw new AcmeException( "Unknown challenge status returned " + json.get( "status" ).asText() );
		}

		state = AcmeChallengeState.FAILED;
		return false;
	}

	public boolean hasFailed()
	{
		return state == AcmeChallengeState.INVALID || state == AcmeChallengeState.FAILED;
	}

	public String lastMessage()
	{
		if ( lastMessage == null )
			lastMessage = "No Message Available";
		return lastMessage;
	}

	public boolean verify() throws AcmeException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, IOException
	{
		if ( state == AcmeChallengeState.SUCCESS )
		{
			return true;
		}
		else if ( hasFailed() )
		{
			return false;
		}
		else if ( state == AcmeChallengeState.CREATED )
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
		else if ( state == AcmeChallengeState.PENDING )
		{
			HttpResponse response = AcmeUtils.get( challengeUri, "application/json" );
			return handleVerify( response );
		}

		return false;
	}
}
