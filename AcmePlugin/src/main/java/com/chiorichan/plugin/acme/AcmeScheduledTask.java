package com.chiorichan.plugin.acme;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.x509.util.StreamParsingException;

import com.chiorichan.Loader;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.acme.api.AcmeProtocol;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.site.SiteManager;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Ticks;
import com.chiorichan.util.FileFunc;

public class AcmeScheduledTask implements Runnable
{
	private AcmePlugin plugin;
	AcmeProtocol client;

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
				TaskManager.INSTANCE.cancelTask( this );
				throw new IllegalStateException( "The Acme Plugin is disabled, can't manage certificates without it. Most likely a programming bug." );
			}

			if ( !NetworkManager.isHttpsRunning() )
			{
				PluginManager.INSTANCE.disablePlugin( plugin );
				TaskManager.INSTANCE.cancelTask( this );
				throw new IllegalStateException( "The HTTPS server is disabled, Acme Plugin can't manage certificates without it enabled." );
			}


			Set<String> domains = new HashSet<>();
			boolean domainsNeededChecking = false;

			for ( Entry<String, Set<String>> e : SiteManager.INSTANCE.getDomains().entrySet() )
			{
				switch ( client.checkDomainVerification( e.getKey(), null, false ) )
				{
					case SUCCESS:
						domains.add( e.getKey() );
						break;
					case PENDING:
						domainsNeededChecking = true;
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
								domains.add( s + "." + e.getKey() );
								break;
							case PENDING:
								domainsNeededChecking = true;
								break;
							case FAILED:
							default:
								// Ignore, it won't get included in the new signed server certificate
								break;
						}
			}

			// Loader.getLogger().debug( "Domains awaiting signing: " + Joiner.on( "," ).join( domains ) );

			if ( domainsNeededChecking )
			{
				plugin.saveConfig();

				Loader.getLogger().warning( "Cert signing was delayed by one minute" );
				// Return in a minute to check if all domains are either verified or failed.
				TaskManager.INSTANCE.scheduleAsyncDelayedTask( plugin, Ticks.MINUTE, this );
				return;
			}

			if ( plugin.getConfig().getBoolean( "config.enableServerWide" ) )
			{
				File sslCertFile = FileFunc.buildFile( plugin.getDataFolder(), "default", "fullchain.pem" );
				File sslKeyFile = CertificateMaintainer.getPrivateKey( "domain" );

				if ( CertificateMaintainer.getDefaultCertificate() == null || !sslCertFile.exists() || !sslKeyFile.exists() )
					CertificateMaintainer.signNewDefaultCertificate( domains );
			}

			plugin.saveConfig();
		}
		catch ( KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | OperatorCreationException | StreamParsingException | AcmeException | IOException e )
		{
			e.printStackTrace();
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
		}
	}
}
