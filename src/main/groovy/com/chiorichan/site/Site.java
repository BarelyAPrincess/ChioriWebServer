/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 * <p>
 * All Rights Reserved.
 */
package com.chiorichan.site;

import com.chiorichan.AppConfig;
import com.chiorichan.Versioning;
import com.chiorichan.account.AccountLocation;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.apache.ApacheConfiguration;
import com.chiorichan.configuration.types.yaml.YamlConfiguration;
import com.chiorichan.datastore.DatastoreManager;
import com.chiorichan.datastore.sql.bases.H2SQLDatastore;
import com.chiorichan.datastore.sql.bases.MySQLDatastore;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.datastore.sql.bases.SQLiteDatastore;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventException;
import com.chiorichan.event.site.SiteLoadEvent;
import com.chiorichan.factory.ScriptBinding;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.factory.ScriptingFactory;
import com.chiorichan.factory.ScriptingResult;
import com.chiorichan.factory.env.Env;
import com.chiorichan.factory.localization.Localization;
import com.chiorichan.http.Routes;
import com.chiorichan.http.ssl.CertificateWrapper;
import com.chiorichan.lang.ApplicationException;
import com.chiorichan.lang.EnumColor;
import com.chiorichan.lang.ExceptionReport;
import com.chiorichan.lang.SiteConfigurationException;
import com.chiorichan.lang.SiteException;
import com.chiorichan.logger.Log;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.session.SessionManager;
import com.chiorichan.session.SessionPersistenceMethod;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Timings;
import com.chiorichan.utils.UtilEncryption;
import com.chiorichan.utils.UtilHttp;
import com.chiorichan.utils.UtilIO;
import com.chiorichan.utils.UtilObjects;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import io.netty.handler.ssl.SslContext;
import org.apache.commons.lang3.text.WordUtils;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements loading sites from file
 */
public class Site implements AccountLocation
{
	/* Site Database */
	private SQLDatastore datastore;
	/* SiteManager instance */
	private final SiteManager mgr;
	/* Configuration file */
	private final File file;
	/* Root directory */
	private final File directory;
	/* Loaded site configuration */
	final YamlConfiguration yaml;
	/* Security encryption key -- WIP */
	private final String encryptionKey;
	/* URL routes */
	private final Routes routes;
	/* Environment variables */
	final Env env;
	/* Language strings */
	final Localization localization;
	/* Id */
	private final String siteId;
	/* Title */
	private String siteTitle;
	/* Listening IP addresses */
	private final List<String> ips;
	/* Default site SSL context */
	private SslContext defaultSslContext = null;
	/* Session persistence methods */
	private SessionPersistenceMethod sessionPersistence = SessionPersistenceMethod.COOKIE;
	private final List<String> cachePatterns = new ArrayList<>();

	/* Scripting variables binding */
	private final ScriptBinding binding = new ScriptBinding();
	/* ScriptingFactory instance, for interpreting script files */
	private final ScriptingFactory factory = ScriptingFactory.create( binding );
	/* Domain Mappings */
	protected final List<DomainMapping> mappings = new ArrayList<>();

	Site( SiteManager mgr, File file, YamlConfiguration yaml, Env env ) throws ApplicationException
	{
		UtilObjects.notNull( mgr );
		UtilObjects.notNull( file );
		UtilObjects.notNull( yaml );
		UtilObjects.notNull( env );

		// yaml.setEnvironmentVariables( env.getProperties() );

		this.mgr = mgr;
		this.file = file;
		this.yaml = yaml;
		this.env = env;

		if ( !yaml.has( "site.id" ) )
			throw new SiteException( "Site id is missing!" );

		siteId = yaml.getString( "site.id" ).toLowerCase();
		siteTitle = yaml.getString( "site.title", AppConfig.get().getString( "framework.sites.defaultTitle", "Unnamed Site" ) );

		ips = yaml.getAsList( "site.listen", new ArrayList<>() );

		for ( String ip : ips )
			if ( !UtilHttp.isValidIPv4( ip ) && !UtilHttp.isValidIPv6( ip ) )
				SiteManager.getLogger().warning( String.format( "The site '%s' is set to listen on ip '%s', but the ip does not match the valid IPv4 or IPv6 regex formula.", siteId, ip ) );

		List<String> listeningIps = NetworkManager.getListeningIps();

		if ( !listeningIps.containsAll( ips ) )
			SiteManager.getLogger().warning( String.format( "The site '%s' is set to listen on ips '%s', but the server is currently not on one or more of those ips. '%s'", siteId, Joiner.on( "," ).useForNull( "null" ).join( ips ), Joiner.on( "," ).useForNull( "null" ).join( listeningIps ) ) );

		if ( ips.contains( "localhost" ) )
			throw new SiteException( "Sites are not permitted to listen on hostname 'localhost', this hostname is reserved for the default site." );

		if ( yaml.has( "site.encryptionKey" ) )
			encryptionKey = yaml.getString( "site.encryptionKey" );
		else
		{
			encryptionKey = UtilEncryption.randomize( "0x0000X" );
			yaml.set( "site.encryptionKey", encryptionKey );
		}

		if ( SiteManager.instance().getSiteById( siteId ) != null )
			throw new SiteException( String.format( "There already exists a site by the provided site id '%s'", siteId ) );

		DatastoreManager.getLogger().info( String.format( "Loading site '%s' with title '%s' from YAML file.", siteId, siteTitle ) );

		directory = SiteManager.checkSiteRoot( siteId );

		this.localization = new Localization( directoryLang() );

		if ( !yaml.has( "site.web-allowed-origin" ) )
			yaml.set( "site.web-allowed-origin", "*" );

		mapDomain( yaml.getConfigurationSection( "site.domains", true ) );

		File ssl = directory( "ssl" );
		UtilIO.setDirectoryAccessWithException( ssl );

		String sslCertFile = yaml.getString( "site.sslCert" );
		String sslKeyFile = yaml.getString( "site.sslKey" );
		String sslSecret = yaml.getString( "site.sslSecret" );

		if ( sslCertFile != null && sslKeyFile != null )
		{
			File sslCert = new File( ssl.getAbsolutePath(), sslCertFile );
			File sslKey = new File( ssl.getAbsolutePath(), sslKeyFile );

			try
			{
				defaultSslContext = new CertificateWrapper( sslCert, sslKey, sslSecret ).context();
			}
			catch ( SSLException | FileNotFoundException | CertificateException e )
			{
				SiteManager.getLogger().severe( String.format( "Failed to load SslContext for site '%s' using cert '%s', key '%s', and hasSecret? %s", siteId, UtilIO.relPath( sslCert ), UtilIO.relPath( sslKey ), sslSecret != null && !sslSecret.isEmpty() ), e );
			}
		}

		try
		{
			if ( EventBus.instance().callEventWithException( new SiteLoadEvent( this ) ).isCancelled() )
				throw new SiteException( String.format( "Loading of site '%s' was cancelled by an internal event.", siteId ) );
		}
		catch ( EventException e )
		{
			throw new SiteException( e );
		}

		if ( yaml.has( "database" ) && yaml.isConfigurationSection( "database" ) )
			switch ( yaml.getString( "database.type", "sqlite" ).toLowerCase() )
			{
				case "sqlite":
				{
					datastore = new SQLiteDatastore( yaml.getString( "database.dbfile", yaml.getString( "database.filename", "server.db" ) ) );
					break;
				}
				case "mysql":
				{
					String host = yaml.getString( "database.host", "localhost" );
					String port = yaml.getString( "database.port", "3306" );
					String database = yaml.getString( "database.database", "chiorifw" );
					String username = yaml.getString( "database.username", "fwuser" );
					String password = yaml.getString( "database.password", "fwpass" );

					datastore = new MySQLDatastore( database, username, password, host, port );
					break;
				}
				case "h2":
				{
					datastore = new H2SQLDatastore( yaml.getString( "database.dbfile", yaml.getString( "database.filename", "server.db" ) ) );
					break;
				}
				case "none":
				case "":
					DatastoreManager.getLogger().warning( String.format( "The Database for site '%s' is unconfigured, some features maybe not function as expected. See config option 'database.type' in the site config and set the connection params.", siteId ) );
					break;
				default:
					DatastoreManager.getLogger().severe( String.format( "We are sorry, the datastore subsystem currently only supports mysql, sqlite, and h2 databases but we found '%s', please change 'database.type' to 'mysql', 'sqlite', or 'h2' in the site config and set the connection params", yaml.getString( "server.database.type", "sqlite" ).toLowerCase() ) );
			}

		routes = new Routes( this );

		if ( yaml.has( "sessions.persistenceMethod" ) )
			for ( SessionPersistenceMethod method : SessionPersistenceMethod.values() )
				if ( method.name().equalsIgnoreCase( yaml.getString( "sessions.persistenceMethod" ) ) )
					sessionPersistence = method;

		List<String> onLoadScripts = yaml.getStringList( "scripts.on-load" );

		if ( onLoadScripts != null )
			for ( String script : onLoadScripts )
			{
				ScriptingResult result = factory.eval( ScriptingContext.fromFile( this, script ).shell( "groovy" ).site( this ) );

				if ( result.hasExceptions() )
				{
					if ( result.hasException( FileNotFoundException.class ) )
						SiteManager.getLogger().severe( String.format( "Failed to eval onLoadScript '%s' for site '%s' because the file was not found.", script, siteId ) );
					else
					{
						SiteManager.getLogger().severe( String.format( "Exception caught while evaluate onLoadScript '%s' for site '%s'", script, siteId ) );
						ExceptionReport.printExceptions( result.getExceptions() );
					}
				}
				else
					SiteManager.getLogger().info( String.format( "Finished evaluate onLoadScript '%s' for site '%s' with result: %s", script, siteId, result.getString( true ) ) );
			}

		ConfigurationSection archive = yaml.getConfigurationSection( "archive", true );

		if ( !archive.has( "enable" ) )
			archive.set( "enable", false );

		if ( !archive.has( "interval" ) )
			archive.set( "interval", "24h" );

		if ( !archive.has( "keep" ) )
			archive.set( "keep", "3" );

		if ( !archive.has( "lastRun" ) )
			archive.set( "lastRun", "0" );

		if ( archive.getBoolean( "enable" ) )
		{
			String interval = archive.getString( "interval", "24h" ).trim();
			if ( interval.matches( "[0-9]+[dhmsDHMS]?" ) )
			{
				interval = interval.toLowerCase();
				int multiply = 1;

				if ( interval.endsWith( "d" ) || interval.endsWith( "h" ) || interval.endsWith( "m" ) || interval.endsWith( "s" ) )
				{
					switch ( interval.substring( interval.length() - 1 ) )
					{
						case "d":
							multiply = 1728000;
							break;
						case "h":
							multiply = 72000;
							break;
						case "m":
							multiply = 1200;
							break;
						case "s":
							multiply = 20;
							break;
					}
					interval = interval.substring( 0, interval.length() - 1 );
				}

				long timer = Long.parseLong( interval ) * multiply;
				long lastRun = Timings.epoch() - archive.getLong( "lastRun" );
				long nextRun = archive.getLong( "lastRun" ) < 1L ? 600L : lastRun > timer ? 600L : timer - lastRun;
				final Site site = this;

				SiteManager.getLogger().info( String.format( "%s%sScheduled site archive for %s {nextRun: %s, interval: %s}", EnumColor.AQUA, EnumColor.NEGATIVE, siteId, nextRun, timer ) );

				TaskManager.instance().scheduleSyncRepeatingTask( SiteManager.instance(), nextRun, timer, () ->
				{
					Log l = SiteManager.getLogger();
					l.info( String.format( "%s%sRunning archive for site %s...", EnumColor.AQUA, EnumColor.NEGATIVE, siteId ) );

					SiteManager.cleanupBackups( siteId, ".zip", archive.getInt( "keep", 3 ) );
					archive.set( "lastRun", Timings.epoch() );

					File dir = AppConfig.get().getDirectory( "archive", "archive" );
					dir = new File( dir, siteId );
					dir.mkdirs();

					File zip = new File( dir, new SimpleDateFormat( "yyyy-MM-dd_HH-mm-ss" ).format( new Date() ) + "-" + siteId + ".zip" );

					try
					{
						UtilIO.zipDir( site.directory(), zip );
					}
					catch ( IOException e )
					{
						l.severe( String.format( "%s%sFailed archiving site %s to %s", EnumColor.RED, EnumColor.NEGATIVE, siteId, zip.getAbsolutePath() ), e );
						return;
					}

					l.info( String.format( "%s%sFinished archiving site %s to %s", EnumColor.AQUA, EnumColor.NEGATIVE, siteId, zip.getAbsolutePath() ) );
				} );
			}
			else
				SiteManager.getLogger().warning( String.format( "Failed to initialize site backup for site %s, interval did not match regex '[0-9]+[dhmsDHMS]?'.", siteId ) );
		}
	}

	Site( SiteManager mgr, String siteId )
	{
		this.mgr = mgr;
		this.siteId = siteId;

		file = null;
		yaml = new YamlConfiguration();
		encryptionKey = UtilEncryption.randomize( "0x0000X" );
		ips = new ArrayList<>();
		siteTitle = Versioning.getProduct();
		datastore = AppConfig.get().getDatabase();

		directory = SiteManager.checkSiteRoot( siteId );
		localization = new Localization( directoryLang() );
		routes = new Routes( this );
		env = new Env();
	}

	private void mapDomain( ConfigurationSection domains ) throws SiteConfigurationException
	{
		mapDomain( domains, null, 0 );
	}

	private String defDomain = null;

	private void mapDomain( final ConfigurationSection domains, DomainMapping mapping, int depth ) throws SiteConfigurationException
	{
		UtilObjects.notNull( domains );
		UtilObjects.isTrue( depth >= 0 );

		for ( String key : domains.getKeys() )
		{
			/* Replace underscore with dot, ignore escaped underscore. */
			String domainKey = key.replaceAll( "(?<!\\\\)_", "." ).replace( "\\_", "_" );

			if ( key.startsWith( "__" ) ) // Configuration Directive
			{
				if ( depth == 0 || mapping == null )
					throw new SiteConfigurationException( String.format( "Domain configuration directive [%s.%s] is not allowed here.", domains.getCurrentPath(), key ) );
				mapping.putConfig( key.substring( 2 ), domains.getString( key ) );

				if ( "__default".equals( key ) && domains.getBoolean( key ) )
				{
					if ( defDomain != null )
						throw new SiteConfigurationException( String.format( "Domain configuration at [%s] is invalid, the DEFAULT domain was previously set to [%s]", domains.getCurrentPath(), defDomain ) );
					defDomain = mapping.getFullDomain();
				}
			}
			else if ( domains.isConfigurationSection( key ) ) // Child Domain
				try
				{
					DomainMapping mappingNew = mapping == null ? getMappings( domainKey ).findFirst().get() : mapping.getChildMapping( domainKey );
					mappingNew.map();
					mapDomain( domains.getConfigurationSection( key ), mappingNew, depth + 1 );
				}
				catch ( IllegalStateException e )
				{
					/* Convert the IllegalStateException to a proper SiteConfigurationException */
					throw new SiteConfigurationException( e );
				}
			else /* Invalid Directive */
				SiteManager.getLogger().warning( String.format( "Site configuration path [%s.%s] is invalid, domain directives MUST start with a double underscore (e.g., __key) and child domains must be a (empty) YAML section (e.g., {}).", domains.getCurrentPath(), key ) );
		}
	}

	public Stream<DomainMapping> getMappings()
	{
		return mappings.stream();
	}

	public DomainMapping getDefaultMapping()
	{
		// Prevent error if no mappings are mapped.
		Stream<DomainMapping> stream = mappings.stream().filter( DomainMapping::isDefault );
		return ( stream.count() == 0 ? getMappings() : stream ).findFirst().get();
	}

	public Stream<DomainMapping> getMappings( String fullDomain )
	{
		UtilObjects.notEmpty( fullDomain );
		Supplier<Stream<DomainMapping>> stream = () -> mappings.stream().filter( d -> d.matches( fullDomain ) );
		return stream.get().count() == 0 ? Stream.of( new DomainMapping( this, fullDomain ) ) : stream.get();
	}

	public void addToCachePatterns( String pattern )
	{
		if ( !cachePatterns.contains( pattern.toLowerCase() ) )
			cachePatterns.add( pattern.toLowerCase() );
	}

	/**
	 * @return The site main directory
	 */
	public File directory()
	{
		UtilObjects.notNull( directory );
		return directory;
	}

	/**
	 * @param sub The subdirectory name
	 * @return The subdirectory of the site main directory
	 */
	public File directory( String sub )
	{
		return new File( directory, sub );
	}

	public File directoryPublic()
	{
		return directory( "public" );
	}

	public File directoryResource()
	{
		return directory( "resource" );
	}

	public File directoryLang()
	{
		return directory( "lang" );
	}

	public File directoryTemp()
	{
		return AppConfig.get().getDirectoryCache( getId() );
	}

	public File directoryTemp( String append )
	{
		return AppConfig.get().getDirectoryCache( getId() + File.pathSeparator + append );
	}

	public ApacheConfiguration getApacheConfig()
	{
		return new ApacheConfiguration();
	}

	protected ScriptBinding getBinding()
	{
		return binding;
	}

	public List<String> getCachePatterns()
	{
		return cachePatterns;
	}

	public YamlConfiguration getConfig()
	{
		return yaml;
	}

	public SQLDatastore getDatastore()
	{
		return datastore;
	}

	public SslContext getDefaultSslContext()
	{
		return defaultSslContext;
	}

	public String getEncryptionKey()
	{
		return encryptionKey;
	}

	public ScriptingFactory getEvalFactory()
	{
		return factory;
	}

	public File getFile()
	{
		return file == null ? yaml.loadedFrom() == null ? null : new File( yaml.loadedFrom() ) : file;
	}

	public Object getGlobal( String key )
	{
		return binding.getVariable( key );
	}

	public Map<String, Object> getGlobals()
	{
		return binding.getVariables();
	}

	@Override
	public String getId()
	{
		return siteId;
	}

	public List<String> getIps()
	{
		return ips;
	}

	public String getLoginForm()
	{
		return getConfig().getString( "accounts.loginForm", "/~wisp/login" );
	}

	public String getLoginPost()
	{
		return getConfig().getString( "accounts.loginPost", "/" );
	}

	/**
	 * Same as calling {@code SiteManager.instance().getDomain( fullDomain ) } but instead checks the returned node belongs to this site.
	 *
	 * @param fullDomain The request domain
	 * @return The DomainNode
	 */
	public DomainNode getDomain( String fullDomain )
	{
		DomainNode node = mgr.getDomain( fullDomain );
		return node != null && node.getSite() == this ? node : null;
	}

	public Stream<DomainNode> getDomains()
	{
		return mgr.getDomainsBySite( this );
	}

	public Routes getRoutes()
	{
		return routes;
	}

	/**
	 * Gets the site configured Session Key from configuration.
	 *
	 * @return The Session Key
	 */
	public String getSessionKey()
	{
		String key = yaml.getString( "sessions.keyName" );
		if ( key == null )
			return SessionManager.getDefaultSessionName();
		return "_ws" + WordUtils.capitalize( key );
	}

	public SessionPersistenceMethod getSessionPersistenceMethod()
	{
		return sessionPersistence;
	}

	public String getDefaultDomain()
	{
		if ( defDomain != null )
			return defDomain;
		Stream<DomainNode> domains = getDomains();
		if ( domains.count() > 0 )
			return domains.findFirst().get().getFullDomain();
		return null;
	}

	public String getTitle()
	{
		return siteTitle;
	}

	public boolean hasDefaultSslContext()
	{
		return defaultSslContext != null;
	}

	public File resourcePackage( String pack ) throws FileNotFoundException
	{
		UtilObjects.notNull( pack, "Package can't be null" );

		if ( pack.length() == 0 )
			throw new FileNotFoundException( "Package can't be empty!" );

		return resourceFile( pack.replace( ".", UtilIO.PATH_SEPERATOR ) );
	}

	public File resourceFile( String file ) throws FileNotFoundException
	{
		UtilObjects.notNull( file, "File can't be null" );

		if ( file.length() == 0 )
			throw new FileNotFoundException( "File can't be empty!" );

		File root = directoryResource();

		File packFile = new File( root, file );

		if ( packFile.exists() )
			return packFile;

		root = packFile.getParentFile();

		if ( root.exists() && root.isDirectory() )
		{
			File[] files = root.listFiles();
			Map<String, File> found = Maps.newLinkedHashMap();
			List<String> preferred = ScriptingContext.getPreferredExtensions();

			for ( File child : files )
				if ( child.getName().startsWith( packFile.getName() + "." ) )
					found.put( child.getName().substring( packFile.getName().length() + 1 ).toLowerCase(), child );

			if ( found.size() > 0 )
			{
				if ( preferred.size() > 0 )
					for ( String ext : preferred )
						if ( found.containsKey( ext.toLowerCase() ) )
							return found.get( ext.toLowerCase() );

				return found.values().toArray( new File[0] )[0];
			}
		}

		throw new FileNotFoundException( String.format( "Could not find the file '%s' file in site '%s' resource directory '%s'.", file, getId(), root.getAbsolutePath() ) );
	}

	public void save() throws IOException
	{
		save( false );
	}

	public void save( boolean force ) throws IOException
	{
		File file = getFile();
		if ( file != null && ( file.exists() || force ) )
			yaml.save( file );
	}

	public void setGlobal( String key, Object val )
	{
		binding.setVariable( key, val );
	}

	public void setTitle( String title )
	{
		siteTitle = title;
		yaml.set( "site.title", title );
	}

	@Override
	public String toString()
	{
		return "Site{id=" + getId() + ",title=" + getTitle() + ",domains=" + getDomains().map( n -> n.getFullDomain() ).collect( Collectors.joining( "," ) ) + ",ips=" + ips.stream().collect( Collectors.joining( "," ) ) + ",siteDir=" + directory.getAbsolutePath() + "}";
	}

	public void unload()
	{
		// Do Nothing
	}

	public Localization getLocalization()
	{
		return localization;
	}
}
