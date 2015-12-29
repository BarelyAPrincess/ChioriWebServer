package com.chiorichan.plugin.acme;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;

import com.chiorichan.configuration.file.FileConfiguration;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.plugin.acme.api.AcmeProtocol;
import com.chiorichan.plugin.acme.api.AcmeStorage;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.plugin.lang.PluginException;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Ticks;
import com.chiorichan.util.FileFunc;

public class AcmePlugin extends Plugin
{
	protected static final String URL_TESTING = "https://acme-staging.api.letsencrypt.org/directory";
	protected static final String URL_PRODUCTION = "https://acme-v01.api.letsencrypt.org/directory";

	private String registrationUrl = null;
	private String[] contacts = null;

	private AcmeProtocol client;
	private int acmeTaskId;

	public AcmeProtocol getClient()
	{
		return client;
	}

	protected String[] getContacts()
	{
		return contacts;
	}

	protected String getRegistrationUrl()
	{
		return registrationUrl;
	}

	@Override
	public void onDisable() throws PluginException
	{

	}

	@Override
	public void onEnable() throws PluginException
	{
		if ( !getConfigFile().exists() )
			saveDefaultConfig();

		FileConfiguration yaml = getConfig();

		if ( !NetworkManager.isHttpsRunning() )
			throw new PluginException( getName() + " requires HTTPS to be enabled and running, see documentation to enable. The server will generate a temporary self signed certificate if none exists." );

		if ( !"letsencrypt".equals( yaml.getString( "config.ca" ) ) )
			throw new PluginException( getName() + " currently only supports the Let's Encrypt Certificate Authory but config option is set to '" + yaml.getString( "config.ca" ) + "'" );

		if ( !yaml.getBoolean( "config.accept-agreement" ) )
			throw new PluginException( "Let's Encrypt requires you to accept their agreement before they will issue certificates. Read 'https://letsencrypt.org/documents/LE-SA-v1.0.1-July-27-2015.pdf', then change config value 'config.accept-agreement' to true." );

		if ( yaml.get( "config.email" ) == null || yaml.getString( "config.email" ).length() == 0 )
			throw new PluginException( "Let's Encrypt requires a valid e-mail address to issue certificates, see config value 'config.email'." );

		if ( !yaml.getBoolean( "config.production" ) )
			getLogger().warning( getName() + " is running in testing-mode, the issued certificates will have no real-world value, see config value 'config.production'." );

		File data = getDataFolder();
		FileFunc.patchDirectory( data );

		try
		{
			client = new AcmeProtocol( yaml.getBoolean( "config.production", false ) ? URL_PRODUCTION : URL_TESTING, yaml.getString( "config.agreement" ), new AcmeStorage( data ) );

			if ( yaml.getBoolean( "enabled.server" ) )
			{
				contacts = new String[] {"mailto:" + yaml.getString( "config.email" )};

				if ( yaml.get( "config.registrationUrl" ) == null )
				{
					registrationUrl = client.newRegistration();
					yaml.set( "config.registrationUrl", registrationUrl );
				}
				else
					registrationUrl = yaml.getString( "config.registrationUrl" );
				// TODO Verify registrationUrl

				acmeTaskId = TaskManager.INSTANCE.scheduleSyncRepeatingTask( this, 0L, Ticks.DAY, new AcmeScheduledTask( this ) );
			}
			else
				throw new PluginException( "HTTPS server is disabled, Acme Plugin can't manage certificates without it enabled" );
		}
		catch ( InvalidKeyException | KeyManagementException | UnrecoverableKeyException | SignatureException | NoSuchAlgorithmException | KeyStoreException | AcmeException | IOException e )
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onLoad() throws PluginException
	{
		if ( acmeTaskId > 0 )
			TaskManager.INSTANCE.cancelTask( acmeTaskId );
	}
}
