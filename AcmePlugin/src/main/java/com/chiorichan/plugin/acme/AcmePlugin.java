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
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.event.EventBus;
import com.chiorichan.lang.PluginException;
import com.chiorichan.lang.ReportingLevel;
import com.chiorichan.lang.UncaughtException;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.plugin.acme.api.AcmeProtocol;
import com.chiorichan.plugin.acme.api.AcmeStorage;
import com.chiorichan.plugin.acme.certificate.CertificateMaintainer;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Ticks;
import com.chiorichan.util.FileFunc;

public class AcmePlugin extends Plugin
{
	protected static final String URL_TESTING = "https://acme-staging.api.letsencrypt.org";
	protected static final String URL_PRODUCTION = "https://acme-v01.api.letsencrypt.org";

	public static AcmePlugin instance()
	{
		return Plugin.getPlugin( AcmePlugin.class );
	}

	private boolean production;
	private String registrationUrl = null;

	private String[] contacts = null;
	private AcmeProtocol client;

	private int acmeTaskId;
	AcmeScheduledTask task = null;

	YamlConfiguration subYaml = null;

	public AcmeProtocol getClient()
	{
		return client;
	}

	public String[] getContacts()
	{
		return contacts;
	}

	@Override
	public File getDataFolder()
	{
		return new File( super.getDataFolder(), getConfig().getBoolean( "config.production" ) ? "production" : "testing" );
	}

	public String getRegistrationUrl()
	{
		return registrationUrl;
	}

	public YamlConfiguration getSubConfig()
	{
		return subYaml;
	}

	public boolean isDefaultCertificateAllowed()
	{
		return getConfig().getBoolean( "config.allowDefaultCertificate", true );
	}

	@Override
	public void onDisable() throws PluginException
	{
		if ( acmeTaskId > 0 )
			TaskManager.instance().cancelTask( acmeTaskId );
		saveConfig();
	}

	@Override
	public void onEnable() throws PluginException
	{
		if ( !getConfigFile().exists() )
			saveDefaultConfig();

		FileConfiguration yaml = getConfig();

		if ( !NetworkManager.isHttpsRunning() )
			throw new PluginException( getName() + " requires HTTPS to be enabled and running, see documentation to enable" );

		if ( !"letsencrypt".equals( yaml.getString( "config.ca", "letsencrypt" ) ) )
			throw new PluginException( getName() + " currently only supports the Let's Encrypt Certificate Authory but config option is set to '" + yaml.getString( "config.ca" ) + "'" );

		if ( !yaml.getBoolean( "config.accept-agreement" ) )
			throw new PluginException( "Let's Encrypt requires you to accept their agreement before they will issue certificates. Please read 'https://letsencrypt.org/documents/LE-SA-v1.0.1-July-27-2015.pdf', then change config value 'config.accept-agreement' to true" );

		if ( yaml.get( "config.email" ) == null || yaml.getString( "config.email" ).length() == 0 )
			throw new PluginException( "Let's Encrypt requires a valid e-mail address to issue certificates, see config value 'config.email'" );

		if ( !yaml.getBoolean( "config.production" ) )
			getLogger().warning( getName() + " is running in testing-mode, the issued certificates will have no real-world value, see config value 'config.production'." );

		subYaml = YamlConfiguration.loadConfiguration( new File( getDataFolder(), "subconfig.yaml" ) );

		File data = getDataFolder();
		if ( !FileFunc.setDirectoryAccess( data ) )
			throw new UncaughtException( ReportingLevel.E_ERROR, "Acme Plugin experienced a problem setting read and write access to directory \"" + FileFunc.relPath( data ) + "\"!" );

		try
		{
			production = yaml.getBoolean( "config.production", false );
			client = new AcmeProtocol( ( production ? URL_PRODUCTION : URL_TESTING ) + "/directory", yaml.getString( "config.agreement" ), new AcmeStorage( data ) );
			contacts = new String[] {"mailto:" + yaml.getString( "config.email" )};
			registrationUrl = subYaml.getString( "config.registrationUrl" );

			if ( !validateUrl( registrationUrl ) )
			{
				registrationUrl = client.newRegistration();
				subYaml.set( "config.registrationUrl", registrationUrl );
			}

			EventBus.instance().registerEvents( new AcmeEventListener( this ), this );

			task = new AcmeScheduledTask( this );
			acmeTaskId = TaskManager.instance().scheduleAsyncRepeatingTask( this, 0L, Ticks.DAY, task );
		}
		catch ( InvalidKeyException | KeyManagementException | UnrecoverableKeyException | SignatureException | NoSuchAlgorithmException | KeyStoreException | AcmeException | IOException e )
		{
			throw new PluginException( e );
		}
	}

	@Override
	public void onLoad() throws PluginException
	{

	}

	public void runTask()
	{
		TaskManager.instance().runTaskAsynchronously( this, task );
	}

	@Override
	public void saveConfig()
	{
		CertificateMaintainer.saveConfig();

		try
		{
			subYaml.save( new File( getDataFolder(), "subconfig.yaml" ) );
		}
		catch ( IOException e )
		{
			getLogger().severe( "Failed to save subconfiguration", e );
		}
		super.saveConfig();
	}

	public boolean validateUrl( String url )
	{
		return ! ( registrationUrl == null || url.startsWith( URL_PRODUCTION ) && !production || url.startsWith( URL_TESTING ) && production || !url.startsWith( URL_PRODUCTION ) && !url.startsWith( URL_TESTING ) );
	}
}
