package com.chiorichan.plugin.acme.api;

import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.types.yaml.YamlConfiguration;
import com.chiorichan.http.HttpCode;
import com.chiorichan.lang.PluginNotFoundException;
import com.chiorichan.logger.LogAPI;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.acme.AcmePlugin;
import com.chiorichan.plugin.acme.lang.AcmeDisabledDomainException;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.plugin.acme.lang.AcmeForbiddenError;
import com.chiorichan.plugin.acme.lang.AcmeState;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Ticks;
import com.chiorichan.tasks.Timings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AcmeChallenge
{
	private final Map<String, SingleChallengeHttp> challenges = new ConcurrentHashMap<>();
	private AcmeProtocol proto;

	private int callBackTask = 0;

	protected AcmeChallenge( AcmeProtocol proto )
	{
		Validate.notNull( proto );
		this.proto = proto;
	}

	public SingleChallengeHttp add( String domain, String subdomain ) throws AcmeForbiddenError
	{
		return add( domain, subdomain, false );
	}

	public SingleChallengeHttp add( String domain, String subdomain, boolean force ) throws AcmeForbiddenError
	{
		try
		{
			Validate.notNull( domain );
			domain = domain.trim().toLowerCase();

			if ( subdomain != null && ( subdomain.length() == 0 || "root".equalsIgnoreCase( subdomain ) ) )
				subdomain = null;

			YamlConfiguration config = AcmePlugin.instance().getSubConfig();
			String fullDomain = subdomain == null ? domain : subdomain + "." + domain;

			for ( String regex : AcmePlugin.instance().getDisabledDomains() )
				if ( fullDomain.matches( regex ) )
					throw new AcmeDisabledDomainException( String.format( "Domain [%s] is disabled per configuration regex [%s]", fullDomain, regex ) );

			if ( !challenges.containsKey( fullDomain ) || force )
			{
				if ( !force )
				{
					ConfigurationSection section = config.getConfigurationSection( "domains." + fullDomain.replace( '.', '_' ) );

					if ( section != null && domain.equals( section.getString( "domain" ) ) && ( section.getString( "subdomain" ) == null || section.getString( "subdomain" ).equalsIgnoreCase( subdomain ) ) )
						if ( "success".equalsIgnoreCase( section.getString( "challengeState" ) ) )
						{
							int lastChecked = section.getInt( "challengeLastChecked", 0 );

							if ( AcmePlugin.instance().validateUrl( section.getString( "challengeUri" ) ) )
							{
								SingleChallengeHttp sac = new SingleChallengeHttp( proto, AcmeChallengeType.get( section.getString( "challengeType" ) ), null, section.getString( "challengeUri" ), domain, subdomain );

								if ( lastChecked < Timings.epoch() - Timings.DAYS_14 )
								{
									sac.setState( AcmeState.PENDING );
									sac.verify();
								}
								else
									sac.setState( AcmeState.SUCCESS );

								challenges.put( fullDomain, sac );
								return sac;
							}
						}
						else if ( "failed".equalsIgnoreCase( section.getString( "challengeState" ) ) )
						{
							int lastChecked = section.getInt( "challengeLastChecked", 0 );
							if ( lastChecked > Timings.epoch() - Timings.DAYS_3 )
							{
								getLogger().severe( String.format( "%s: Permanent failure, no exact message currently available!", subdomain == null ? domain : subdomain + "." + domain ) );
								return null;
							}
						}
				}

				HttpResponse response = proto.newChallenge0( fullDomain );

				if ( response.getStatus() == HttpCode.HTTP_FORBIDDEN )
					throw new AcmeForbiddenError();

				if ( response.getStatus() != HttpCode.HTTP_CREATED )
					throw new AcmeException( String.format( "Http code '%s' '%s' was returned, we expected '201' 'CREATED'.", response.getStatus(), HttpCode.msg( response.getStatus() ) ) );

				JsonNode json = new ObjectMapper().readTree( response.getBodyString() );

				for ( JsonNode challenge : json.get( "challenges" ) )
				{
					String type = challenge.get( "type" ).asText();
					String challengeToken = challenge.get( "token" ).asText();
					String challengeUri = challenge.get( "uri" ).asText();

					switch ( type )
					{
						case "tls-sni-01":
							// Unsupported, so ignore!
							break;
						case "http-01":
							SingleChallengeHttp sac = new SingleChallengeHttp( proto, AcmeChallengeType.HTTP_01, challengeToken, challengeUri, domain, subdomain );
							challenges.put( fullDomain, sac );
							String prefix = "domains." + fullDomain.replace( '.', '_' ) + ".";
							config.set( prefix + "domain", domain );
							config.set( prefix + "subdomain", subdomain );
							config.set( prefix + "challengeType", type );
							config.set( prefix + "challengeUri", challengeUri );
							config.set( prefix + "challengeState", "created" );
							return sac;
						case "dns-01":
							// Unsupported, so ignore!
							break;
						default:
							getLogger().warning( "Unsupported challenge type received, '" + type + "'" );
					}
				}
			}
			else
			{
				SingleChallengeHttp sac = challenges.get( fullDomain );
				long lastChecked = sac.getLastChecked();

				if ( sac.getState() == AcmeState.SUCCESS )
				{
					if ( lastChecked < Timings.epoch() - Timings.DAYS_14 )
					{
						sac.setState( AcmeState.PENDING );
						sac.verify();

						if ( sac.getState() == AcmeState.FAILED || sac.getState() == AcmeState.INVALID )
							return add( fullDomain, subdomain, true );
					}
				}
				else if ( sac.getState() == AcmeState.FAILED )
					if ( lastChecked < Timings.epoch() - Timings.DAYS_3 )
						return add( fullDomain, subdomain, true );

				return sac;
			}
		}
		catch ( AcmeForbiddenError e )
		{
			throw e;
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}

		return null;

		/**
		 * <pre>
		 * {
		 *     "identifier":{
		 *         "type":"dns",
		 *         "value":"example.com"
		 *     },
		 *     "status":"pending",
		 *     "expires":"2015-12-18T03:25:53.827339252Z",
		 *     "challenges":[
		 *         {
		 *             "type":"com.chiorichan.http-01",
		 *             "status":"pending",
		 *             "uri":"https://acme-staging.api.letsencrypt.org/acme/challenge/Em0...udY/851773",
		 *             "token":"IR8...upo"
		 *         },
		 *         {
		 *             "type":"dns-01",
		 *             "status":"pending",
		 *             "uri":"https://acme-staging.api.letsencrypt.org/acme/challenge/Em0...udY/851774",
		 *             "token":"4x3...PsA"
		 *         },
		 *         {
		 *             "type":"tls-sni-01",
		 *             "status":"pending",
		 *             "uri":"https://acme-staging.api.letsencrypt.org/acme/challenge/Em0...udY/851775",
		 *             "token":"XS3...Bho"
		 *         }
		 *     ]
		 * }
		 *
		 *
		 * </pre>
		 */
	}

	public boolean challengesComplete()
	{
		for ( SingleChallengeHttp sac : challenges.values() )
			if ( sac.getState() != AcmeState.SUCCESS )
				return false;
		return true;
	}

	public void doCallBack( boolean force, boolean replace, Runnable runnable )
	{
		if ( hasCallBack() )
			if ( replace )
				TaskManager.instance().cancelTask( callBackTask );
			else
				throw new IllegalStateException( "Can't schedule a challenge callback because one is already active" );

		try
		{
			AcmePlugin plugin = ( AcmePlugin ) PluginManager.instance().getPluginByClass( AcmePlugin.class );

			callBackTask = TaskManager.instance().scheduleAsyncRepeatingTask( plugin, Ticks.SECOND_5, Ticks.SECOND, () ->
			{
				try
				{
					for ( SingleChallengeHttp sac : challenges.values() )
						if ( sac.getState() == AcmeState.CREATED || sac.getState() == AcmeState.PENDING )
						{
							if ( force )
								sac.verify();
							return;
						}

					TaskManager.instance().cancelTask( callBackTask );
					callBackTask = 0;
					runnable.run();
				}
				catch ( Throwable t )
				{
					t.printStackTrace();
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

	public SingleChallengeHttp get( String domain, String subdomain )
	{
		String fullDomain = subdomain == null || subdomain.length() == 0 || subdomain.equals( "root" ) ? domain : subdomain.trim().toLowerCase() + "." + domain;
		return challenges.get( fullDomain );
	}

	public List<SingleChallengeHttp> getChallenges()
	{
		return new ArrayList<SingleChallengeHttp>( challenges.values() );
	}

	public Set<String> getDomains()
	{
		return challenges.keySet();
	}

	private LogAPI getLogger()
	{
		return Plugin.getPlugin( AcmePlugin.class ).getLogger();
	}

	public boolean hasCallBack()
	{
		return callBackTask > 0;
	}

	public void remove( SingleChallengeHttp sac )
	{
		for ( Entry<String, SingleChallengeHttp> e : challenges.entrySet() )
			if ( e.getValue() == sac )
				challenges.remove( e.getKey() );
	}
}
