package com.chiorichan.plugin.acme;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.chiorichan.lang.EnumColor;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.acme.api.AcmeProtocol;
import com.chiorichan.plugin.acme.certificate.CertificateMaintainer;
import com.chiorichan.site.SiteManager;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Ticks;

public class AcmeScheduledTask implements Runnable
{
	private static boolean domainsPending = false;
	private static final Set<String> verifedDomains = new HashSet<>();

	public static boolean domainsPending()
	{
		return domainsPending;
	}

	public static Set<String> getVerifiedDomains()
	{
		return verifedDomains;
	}

	private AcmePlugin plugin;

	private AcmeProtocol client;

	public AcmeScheduledTask( AcmePlugin plugin )
	{
		this.plugin = plugin;
		client = plugin.getClient();
	}

	@Override
	public void run()
	{
		try
		{
			if ( !plugin.isEnabled() )
			{
				TaskManager.instance().cancelTask( this );
				throw new IllegalStateException( "The Acme Plugin is disabled, can't manage certificates without it. Most likely a programming bug." );
			}

			if ( !NetworkManager.isHttpsRunning() )
			{
				PluginManager.instance().disablePlugin( plugin );
				TaskManager.instance().cancelTask( this );
				throw new IllegalStateException( "The HTTPS server is disabled, Acme Plugin can't manage certificates without it enabled." );
			}

			verifedDomains.clear();
			domainsPending = false;

			for ( Entry<String, Set<String>> e : SiteManager.instance().getDomains().entrySet() )
			{
				switch ( client.checkDomainVerification( e.getKey(), null, false ) )
				{
					case SUCCESS:
						verifedDomains.add( e.getKey() );
						break;
					case PENDING:
						domainsPending = true;
						break;
					case FAILED:
					default:
						// Ignore, it won't get included in the new signed server certificate
						break;
				}

				for ( String s : e.getValue() )
					if ( s != null && !s.equals( "root" ) && s.length() > 0 )
						switch ( client.checkDomainVerification( e.getKey(), s, false ) )
						{
							case SUCCESS:
								verifedDomains.add( s + "." + e.getKey() );
								break;
							case PENDING:
								domainsPending = true;
								break;
							case FAILED:
							default:
								// Ignore, it won't get included in the new signed server certificate
								break;
						}
			}

			if ( domainsPending )
			{
				plugin.getLogger().info( EnumColor.YELLOW + "Domains are currently pending verification. Certificates can not be signed or renewed until the vertification finishes." );
				TaskManager.instance().scheduleAsyncDelayedTask( plugin, Ticks.SECOND_30, this );
			}
			else
				CertificateMaintainer.checkAndSignCertificates();

			plugin.saveConfig();
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
		}
	}
}
