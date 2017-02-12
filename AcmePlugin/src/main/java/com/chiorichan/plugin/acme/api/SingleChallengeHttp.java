package com.chiorichan.plugin.acme.api;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.TreeMap;

import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.http.HttpCode;
import com.chiorichan.lang.PluginNotFoundException;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.acme.AcmePlugin;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.plugin.acme.lang.AcmeState;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Ticks;
import com.chiorichan.tasks.Timings;
import com.chiorichan.zutils.ZIO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings( "serial" )
public final class SingleChallengeHttp
{
	// TODO For future new challenges, this class will extend a common parent

	private AcmeState state = AcmeState.CREATED;

	private final AcmeChallengeType challengeType;
	private final String challengeToken;
	private final String challengeUri;
	private final String domain;
	private final String subdomain;
	private String lastMessage;

	private final AcmeProtocol proto;

	private int callBackTask = 0;

	protected SingleChallengeHttp( AcmeProtocol proto, AcmeChallengeType challengeType, String challengeToken, String challengeUri, String domain, String subdomain )
	{
		this.challengeType = challengeType;
		this.challengeToken = challengeToken;
		this.challengeUri = challengeUri;
		this.domain = domain;
		this.subdomain = subdomain;
		this.proto = proto;
	}

	public void doCallback( boolean replace, Runnable runnable )
	{
		if ( hasCallBack() )
			if ( replace )
				TaskManager.instance().cancelTask( callBackTask );
			else
				throw new IllegalStateException( "Can't schedule a challenge callback because one is already active" );

		if ( state == AcmeState.CREATED )
			verify();

		try
		{
			AcmePlugin plugin = ( AcmePlugin ) PluginManager.instance().getPluginByClass( AcmePlugin.class );

			callBackTask = TaskManager.instance().scheduleAsyncRepeatingTask( plugin, Ticks.SECOND_5, Ticks.SECOND, new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						verify();

						if ( getState() != AcmeState.PENDING && getState() != AcmeState.CREATED )
						{
							TaskManager.instance().cancelTask( callBackTask );
							callBackTask = 0;
							runnable.run();
						}
					}
					catch ( Throwable t )
					{
						t.printStackTrace();
					}
				}
			} );

			if ( callBackTask == -1 )
			{
				callBackTask = 0;
				throw new IllegalStateException( "Failed to schedule the callback task" );
			}
		}
		catch ( PluginNotFoundException e )
		{
			throw new IllegalStateException( "There was a severe internal plugin error", e );
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

	public String getDomain()
	{
		return domain;
	}

	public String getFullDomain()
	{
		return subdomain == null || subdomain.length() == 0 || subdomain.equals( "root" ) ? domain : subdomain + "." + domain;
	}

	public int getLastChecked()
	{
		return AcmePlugin.instance().getSubConfig().getInt( "domains." + getFullDomain().replace( '.', '_' ) + ".challengeLastChecked", Timings.epoch() );
	}

	public AcmeState getState()
	{
		return state;
	}

	public String getSubDomain()
	{
		return subdomain;
	}

	public String getTokenPath()
	{
		if ( getChallengeToken() == null )
			return null;
		return ZIO.buildPath( ".well-known", "acme-challenge", getChallengeToken() );
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
					 * <pre>
					 * {
					 *     "error":{
					 *         "detail":"Error parsing key authorization file: Invalid key authorization: 8 parts",
					 *         "type":"urn:acme:error:unauthorized"
					 *     },
					 *     "keyAuthorization":"jg1...304",
					 *     "status":"invalid",
					 *     "token":"jg1...rGU",
					 *     "type":"com.chiorichan.http-01",
					 *     "uri":"https://acme-staging.api.letsencrypt.org/acme/challenge/ynd...889",
					 *     "validationRecord":[
					 *         {
					 *             "addressUsed":"104.28.2.100",
					 *             "addressesResolved":[
					 *                 "***.***.***.***"
					 *             ],
					 *             "hostname":"example.com",
					 *             "port":"80",
					 *             "url":"http://penoaks.com/.well-known/acme-challenge/jg1...rGU"
					 *         }
					 *     ]
					 * }
					 * </pre>
					 */

					lastMessage = json.get( "error" ).get( "detail" ).asText();
					setState( AcmeState.FAILED );

					/*
					 * if ( json.get( "validationRecord" ).isArray() )
					 * for ( JsonNode jn : json.get( "validationRecord" ) )
					 * {
					 * String usedIpAddress = jn.get( "addressUsed" ).asText();
					 *
					 * if ( !NetworkManager.getListeningIps().contains( usedIpAddress ) )
					 * lastMessage += String.format( " Additionally, the IP address tried does not match the IP address the server is listening on, '%s', '%s'", usedIpAddress, Joiner.on( "," ).join( NetworkManager.getListeningIps() ) );
					 * }
					 */
				}
				else if ( "valid".equals( json.get( "status" ).asText() ) )
				{
					/*
					 * <pre>
					 * {
					 *     "type":"com.chiorichan.http-01",
					 *     "status":"valid",
					 *     "uri":"https://acme-staging.api.letsencrypt.org/acme/challenge/nwe...099",
					 *     "token":"xZK...TNU",
					 *     "keyAuthorization":"xZK...uwM",
					 *     "validationRecord":[
					 *         {
					 *             "Authorities":null,
					 *             "url":"http://api.penoaks.com/.well-known/acme-challenge/xZK...TNU",
					 *             "hostname":"example.com",
					 *             "port":"80",
					 *             "addressesResolved":[
					 *                 "***.***.***.***"
					 *             ],
					 *             "addressUsed":"***.***.***.***"
					 *         }
					 *     ]
					 * }
					 * </pre>
					 */

					lastMessage = "Domain verification has been marked as valid";
					setState( AcmeState.SUCCESS );
				}
				else if ( "pending".equals( json.get( "status" ).asText() ) )
				{
					/*
					 * <pre>
					 * {
					 *     "type":"com.chiorichan.http-01",
					 *     "status":"pending",
					 *     "uri":"https://acme-staging.api.letsencrypt.org/acme/challenge/nwe...099",
					 *     "token":"xZK...TNU",
					 *     "keyAuthorization":"xZK0p...eLuwM"
					 * }
					 * </pre>
					 */

					lastMessage = "Domain verification is still pending, please try again shortly";
					setState( AcmeState.PENDING );
				}
				else if ( json.has( "detail" ) )
				{
					/*
					 * <pre>
					 * {
					 *     "type":"urn:acme:error:malformed",
					 *     "detail":"Expired authorization",
					 *     "status":404
					 * }
					 * </pre>
					 */

					lastMessage = "Domain verification has FAILED for reason: " + json.get( "detail" ).asText();
					setState( AcmeState.INVALID );
				}
				else
					throw new AcmeException( "Unknown verification response returned " + json.toString() );

				return true;
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		setState( AcmeState.INVALID );
		return false;
	}

	public boolean hasCallBack()
	{
		return callBackTask > 0;
	}

	public boolean hasFailed()
	{
		return state == AcmeState.INVALID || state == AcmeState.FAILED;
	}

	public boolean isPending()
	{
		return getState() == AcmeState.PENDING || getState() == AcmeState.CREATED;
	}

	public boolean isValid()
	{
		return state == AcmeState.SUCCESS;
	}

	public String lastMessage()
	{
		if ( lastMessage == null )
			lastMessage = "No Message Available";
		return lastMessage;
	}

	ConfigurationSection getDomainConfig()
	{
		return AcmePlugin.instance().getSubConfig().getConfigurationSection( "domains." + getFullDomain().replace( '.', '_' ), true );
	}

	void setState( AcmeState state )
	{
		getDomainConfig().set( "challengeState", state.name().toLowerCase() );
		getDomainConfig().set( "challengeLastChecked", Timings.epoch() );
		this.state = state;
	}

	public boolean verify()
	{
		try
		{
			return verifyWithException();
		}
		catch ( KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | AcmeException | IOException e )
		{
			return false;
		}
	}

	public boolean verifyWithException() throws AcmeException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, IOException
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
						put( "type", "com.chiorichan.http-01" );
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
		catch ( AcmeException | KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | IOException e )
		{
			lastMessage = e.getMessage();
			setState( AcmeState.FAILED );
			throw e;
		}

		return false;
	}
}
