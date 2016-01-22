package com.chiorichan.plugin.acme;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.SSLException;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.x509.util.StreamParsingException;

import com.chiorichan.Loader;
import com.chiorichan.http.HttpsManager;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.acme.api.AcmeCertificateRequest;
import com.chiorichan.plugin.acme.api.AcmeProtocol;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.plugin.acme.lang.AcmeState;
import com.chiorichan.site.SiteManager;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Ticks;
import com.chiorichan.util.FileFunc;
import com.chiorichan.util.SecureFunc;
import com.google.common.base.Joiner;

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

			Loader.getLogger().debug( "Domains awaiting signing: " + Joiner.on( "," ).join( domains ) );

			if ( domainsNeededChecking )
			{
				plugin.saveConfig();

				Loader.getLogger().warning( "Cert signing was delayed one minute" );
				// Return in a minute to check if all domains are now verified.
				TaskManager.INSTANCE.scheduleAsyncDelayedTask( plugin, Ticks.MINUTE, this );
				return;
			}

			if ( plugin.getConfig().getBoolean( "config.enableServerWide" ) )
			{
				String md5 = plugin.getConfig().getString( "config.serverCertificateMD5" );
				String certMD5 = SecureFunc.md5( HttpsManager.INSTANCE.getServerCertificateFile() );

				// TODO Check expiration and check if all domains are contained

				if ( true )//md5 == null || !md5.equals( certMD5 ) )
				{
					AcmeCertificateRequest signingRequest = client.newSigningRequest( new ArrayList<String>( domains ) );

					signingRequest.doCallback( true, new Runnable()
					{
						@Override
						public void run()
						{
							if ( signingRequest.getState() == AcmeState.SUCCESS )
								try
								{
									signingRequest.save( FileFunc.buildFile( plugin.getDataFolder(), "server" ) );

									File sslCert = FileFunc.buildFile( plugin.getDataFolder(), "server", "cert.crt" );
									File sslKey = FileFunc.buildFile( plugin.getDataFolder(), "domain.key" );

									HttpsManager.INSTANCE.updateCertificate( sslCert, sslKey, null, true );

									plugin.getConfig().set( "config.serverCertificateMD5", SecureFunc.md5( sslCert ) );
								}
								catch ( AcmeException | FileNotFoundException | SSLException e )
								{
									e.printStackTrace();
								}
							else
								plugin.getLogger().severe( "Failed certificate signing for reason " + signingRequest.lastMessage() );
						}
					} );
				}
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
