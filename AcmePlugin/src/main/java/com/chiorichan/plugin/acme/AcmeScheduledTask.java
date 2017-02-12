package com.chiorichan.plugin.acme;

import com.chiorichan.lang.EnumColor;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.acme.api.AcmeProtocol;
import com.chiorichan.plugin.acme.certificate.CertificateMaintainer;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.site.SiteManager;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Ticks;

import java.util.HashSet;
import java.util.Set;

public class AcmeScheduledTask implements Runnable
{
	private static boolean domainsPending = false;
	private static final Set<String> verifiedDomains = new HashSet<>();

	public static boolean domainsPending()
	{
		return domainsPending;
	}

	public static Set<String> getVerifiedDomains()
	{
		return verifiedDomains;
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

			verifiedDomains.clear();
			domainsPending = false;

			SiteManager.instance().getDomains().filter( n -> n.getSite() != null ).forEach( node ->
			{
				try
				{
					switch ( client.checkDomainVerification( node.getFullDomain(), null, false ) )
					{
						case SUCCESS:
							verifiedDomains.add( node.getFullDomain() );
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
				catch ( AcmeException e )
				{
					// TODO Gather thrown exceptions
					e.printStackTrace();
				}
			} );

			if ( domainsPending )
			{
				plugin.getLogger().info( EnumColor.YELLOW + "Domains are currently pending verification. Certificates can not be signed or renewed until the verification finishes." );
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
