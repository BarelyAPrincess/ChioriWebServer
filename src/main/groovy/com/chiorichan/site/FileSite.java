/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.site;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.text.WordUtils;

import com.chiorichan.Loader;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.datastore.Datastore;
import com.chiorichan.datastore.DatastoreManager;
import com.chiorichan.datastore.sql.bases.H2SQLDatastore;
import com.chiorichan.datastore.sql.bases.MySQLDatastore;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.datastore.sql.bases.SQLiteDatastore;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventException;
import com.chiorichan.event.server.SiteLoadEvent;
import com.chiorichan.factory.ScriptBinding;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.factory.ScriptingFactory;
import com.chiorichan.factory.ScriptingResult;
import com.chiorichan.http.HttpCookie;
import com.chiorichan.http.Routes;
import com.chiorichan.lang.SiteException;
import com.chiorichan.session.SessionManager;
import com.chiorichan.session.SessionPersistenceMethod;
import com.chiorichan.util.SecureFunc;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Implements loading sites from file
 */
public class FileSite implements Site
{
	private final YamlConfiguration yaml;
	private SQLDatastore datastore;
	
	private final String siteId;
	private String siteTitle;
	private String siteDomain;
	
	private final List<String> cachePatterns = Lists.newArrayList();
	private SessionPersistenceMethod sessionPersistence = SessionPersistenceMethod.COOKIE;
	private final String encryptionKey;
	private final Routes routes = new Routes( this );
	
	private final List<String> enabledSubDomains;
	
	private File siteDir;
	private File publicDir;
	private File resourceDir;
	
	// Deprecated
	private final List<String> metatags = Lists.newCopyOnWriteArrayList();
	
	private final ScriptBinding binding = new ScriptBinding();
	private final ScriptingFactory factory = ScriptingFactory.create( binding );
	
	FileSite( YamlConfiguration yaml ) throws SiteException
	{
		Validate.notNull( yaml );
		
		this.yaml = yaml;
		
		if ( !yaml.has( "site.id" ) )
			throw new SiteException( "Site id is missing!" );
		if ( !yaml.has( "site.domain" ) )
			throw new SiteException( "Site domain is missing!" );
		
		siteId = yaml.getString( "site.id" ).toLowerCase();
		siteDomain = yaml.getString( "site.domain" );
		siteTitle = yaml.getString( "site.title", Loader.getConfig().getString( "framework.sites.defaultTitle", "Unnamed Site" ) );
		
		if ( yaml.has( "site.encryptionKey" ) )
			encryptionKey = yaml.getString( "site.encryptionKey" );
		else
		{
			encryptionKey = SecureFunc.randomize( "0x0000X" );
			yaml.set( "site.encryptionKey", encryptionKey );
		}
		
		if ( SiteManager.INSTANCE.getSiteById( siteId ) != null )
			throw new SiteException( String.format( "There already exists a site by the provided site id '%s'", siteId ) );
		
		Datastore.getLogger().info( String.format( "Loading site '%s' with title '%s' from YAML file.", siteId, siteTitle ) );
		
		// Stick to using conventions for the time being, might allows for customization later but not now.
		siteDir = SiteManager.checkSiteRoot( siteId );
		publicDir = new File( siteDir, "public" );
		resourceDir = new File( siteDir, "resource" );
		
		if ( yaml.has( "site.source" ) )
			yaml.set( "site.source", null ); // Deprecated
			
		if ( yaml.has( "site.resource" ) )
			yaml.set( "site.resource", null ); // Deprecated
			
		/*
		 * // Load protected files list
		 * List<?> protectedFilesPre = config.getList( "protected", new CopyOnWriteArrayList<String>() );
		 * 
		 * for ( Object o : protectedFilesPre )
		 * if ( o instanceof String )
		 * protectedFiles.add( ( String ) o );
		 * else
		 * Loader.getLogger().warning( "Site '" + siteId + "' had an incorrect data object type under the YAML config fileor option 'protected', fileound type '" + o.getClass() + "'." );
		 * 
		 * 
		 * 
		 * // Load metatags
		 * List<?> metatagsPre = config.getList( "metatags", new CopyOnWriteArrayList<String>() );
		 * 
		 * for ( Object o : metatagsPre )
		 * if ( o instanceof String )
		 * metatags.add( ( String ) o );
		 * else
		 * Loader.getLogger().warning( "Site '" + siteId + "' had an incorrect data object type under the YAML config fileor option 'metatags', fileound type '" + o.getClass() + "'." );
		 * 
		 * // Load aliases map
		 * ConfigurationSection aliasesPre = config.getConfigurationSection( "aliases" );
		 * if ( aliasesPre != null )
		 * {
		 * Set<String> akeys = aliasesPre.getKeys( false );
		 * 
		 * if ( akeys != null )
		 * for ( String k : akeys )
		 * if ( aliasesPre.getString( k, null ) != null )
		 * aliases.put( k, aliasesPre.getString( k ) );
		 * }
		 * 
		 * // Loader subdomains map
		 * ConfigurationSection subdomainsPre = config.getConfigurationSection( "subdomains" );
		 * if ( subdomainsPre != null )
		 * {
		 * Set<String> skeys = subdomainsPre.getKeys( false );
		 * 
		 * if ( skeys != null )
		 * for ( String k : skeys )
		 * if ( subdomainsPre.getString( k, null ) != null )
		 * subdomains.put( k, subdomainsPre.getString( k ) );
		 * }
		 */
		
		enabledSubDomains = yaml.getStringList( "subdomains" );
		
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
		
		if ( yaml.has( "sessions.persistenceMethod" ) )
			for ( SessionPersistenceMethod method : SessionPersistenceMethod.values() )
				if ( method.name().equalsIgnoreCase( yaml.getString( "sessions.persistenceMethod" ) ) )
					sessionPersistence = method;
		
		SiteLoadEvent event = new SiteLoadEvent( this );
		
		try
		{
			EventBus.INSTANCE.callEventWithException( event );
		}
		catch ( EventException e )
		{
			throw new SiteException( e );
		}
		
		if ( event.isCancelled() )
			throw new SiteException( String.format( "Loading of site '%s' was cancelled by an internal event.", siteId ) );
		
		List<String> onLoadScripts = yaml.getStringList( "scripts.on-load" );
		
		if ( onLoadScripts != null )
			for ( String script : onLoadScripts )
			{
				ScriptingResult result = factory.eval( ScriptingContext.fromFile( this, script ).shell( "groovy" ).site( this ) );
				
				if ( result.hasExceptions() )
				{
					SiteManager.getLogger().severe( String.format( "Exception caught while evaling onLoadScript '%s' for site '%s'", script, siteId ) );
					SiteManager.getLogger().exceptions( result.getExceptions() );
				}
				else
					Loader.getLogger().info( "Finsihed evaling onLoadScript '" + script + "' for site '" + siteId + "' with result: " + result.getString( true ) );
			}
		
		/**
		 * Warn the user that files can not be served from the `wisp`, a.k.a. Web Interface and Server Point, folder since the server uses it for internal requests.
		 */
		if ( subDir( "~wisp" ).exists() )
			SiteManager.getLogger().warning( String.format( "It would appear that site '%s' contains a subfolder by the name of '~wisp', since we use the uri '/~wisp' for internal access, you will be unable to serve files from this directory!", siteId ) );
	}
	
	public void addToCachePatterns( String pattern )
	{
		if ( !cachePatterns.contains( pattern.toLowerCase() ) )
			cachePatterns.add( pattern.toLowerCase() );
	}
	
	@Override
	public HttpCookie createSessionCookie( String sessionId )
	{
		return new HttpCookie( getSessionKey(), sessionId ).setDomain( "." + getDomain() ).setPath( "/" ).setHttpOnly( true );
	}
	
	@Deprecated
	@Override
	public Map<String, String> getAliases()
	{
		Map<String, String> aliases = Maps.newHashMap();
		
		for ( String subdomain : enabledSubDomains )
			aliases.put( subdomain, "http://" + subdomain + "." + getDomain() + "/" );
		
		return aliases;
	}
	
	protected ScriptBinding getBinding()
	{
		return binding;
	}
	
	public List<String> getCachePatterns()
	{
		return cachePatterns;
	}
	
	@Override
	public YamlConfiguration getConfig()
	{
		return yaml;
	}
	
	@Override
	public SQLDatastore getDatastore()
	{
		return datastore;
	}
	
	@Override
	public String getDomain()
	{
		return siteDomain;
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
		return new File( yaml.loadedFrom() );
	}
	
	public Object getGlobal( String key )
	{
		return binding.getVariable( key );
	}
	
	public Map<String, Object> getGlobals()
	{
		return binding.getVariables();
	}
	
	@Deprecated
	@Override
	public List<String> getMetatags()
	{
		if ( metatags == null )
			return new CopyOnWriteArrayList<String>();
		
		return metatags;
	}
	
	@Override
	public String getName()
	{
		return siteId;
	}
	
	@Override
	public Routes getRoutes()
	{
		return routes;
	}
	
	/**
	 * Gets the site configured Session Key from configuration.
	 * 
	 * @return
	 *         The Session Key
	 */
	@Override
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
	
	@Override
	public String getSiteId()
	{
		return siteId;
	}
	
	@Override
	public String getTitle()
	{
		return siteTitle;
	}
	
	@Override
	public File publicDirectory()
	{
		return subDomainDirectory( "root" );
	}
	
	@Override
	public File resourceDirectory()
	{
		return resourceDir;
	}
	
	@Override
	public File resourceFile( String file ) throws FileNotFoundException
	{
		Validate.notNull( file, "File can't be null" );
		
		if ( file.length() == 0 )
			throw new FileNotFoundException( "File can't be empty!" );
		
		File root = resourceDirectory();
		
		File packFile = new File( root, file );
		
		if ( packFile.exists() )
			return packFile;
		
		root = packFile.getParentFile();
		
		if ( root.exists() && root.isDirectory() )
		{
			File[] files = root.listFiles();
			Map<String, File> found = Maps.newLinkedHashMap();
			List<String> preferred = Loader.getConfig().getStringList( "packages.preferredExt" );
			
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
		
		throw new FileNotFoundException( String.format( "Could not find the file '%s' file in site public directory '%s'.", file, getName() ) );
	}
	
	@Override
	public File resourcePackage( String pack ) throws FileNotFoundException
	{
		Validate.notNull( pack, "Package can't be null" );
		
		if ( pack.length() == 0 )
			throw new FileNotFoundException( "Package can't be empty!" );
		
		pack = pack.replace( ".", System.getProperty( "file.separator" ) );
		
		File root = resourceDirectory();
		
		File packFile = new File( root, pack );
		
		if ( packFile.exists() )
			return packFile;
		
		root = packFile.getParentFile();
		
		if ( root.exists() && root.isDirectory() )
		{
			File[] files = root.listFiles();
			Map<String, File> found = Maps.newLinkedHashMap();
			List<String> preferred = Loader.getConfig().getStringList( "packages.preferredExt" );
			
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
		
		throw new FileNotFoundException( String.format( "Could not find the package '%s' file in site public directory '%s'.", pack, getName() ) );
	}
	
	@Override
	public File rootDirectory()
	{
		return siteDir;
	}
	
	protected void save()
	{
		// TODO Save Site
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
	
	protected File subDir( String pack )
	{
		pack = pack.replace( ".", System.getProperty( "file.separator" ) );
		return new File( siteDir, pack );
	}
	
	@Override
	public File subDomainDirectory( String subdomain )
	{
		Validate.notNull( subdomain );
		
		subdomain = subdomain.toLowerCase();
		if ( subdomain.isEmpty() || subdomain.equals( "root" ) )
			return new File( publicDir, "root" );
		
		if ( enabledSubDomains.contains( subdomain ) )
			return new File( publicDir, subdomain );
		else
			return null;
	}
	
	@Override
	public boolean subDomainExists( String subdomain )
	{
		if ( subdomain.length() == 0 || subdomain.equalsIgnoreCase( "root" ) )
			return true;
		
		return enabledSubDomains.contains( subdomain.toLowerCase() );
	}
	
	@Override
	public File tempDirectory()
	{
		return Loader.getTempFileDirectory( getSiteId() );
	}
	
	@Override
	public String toString()
	{
		return "Site{id=" + getSiteId() + "name=" + getName() + ",title=" + getTitle() + ",domain=" + getDomain() + ",siteDir=" + siteDir.getAbsolutePath() + "}";
	}
	
	@Override
	public void unload()
	{
		// Do Nothing
	}
	
	/*
	 * public boolean protectCheck( String file )
	 * {
	 * if ( protectedFiles == null )
	 * return false;
	 * 
	 * // Does this file belong to our webroot
	 * if ( file.startsWith( getRoot() ) )
	 * {
	 * // Strip our webroot from file
	 * file = file.substring( getRoot().length() );
	 * 
	 * for ( String n : protectedFiles )
	 * if ( n != null && !n.isEmpty() )
	 * {
	 * // If the length is greater then 1 and file starts with this string.
	 * if ( n.length() > 1 && file.startsWith( n ) )
	 * return true;
	 * 
	 * // Does our file end with this. ie: .php, .txt, .etc
	 * if ( file.endsWith( n ) )
	 * return true;
	 * 
	 * // If the pattern does not start with a /, see if name contain this string.
	 * if ( !n.startsWith( "/" ) && file.contains( n ) )
	 * return true;
	 * 
	 * // Lastly try the string as a RegEx pattern
	 * if ( file.matches( n ) )
	 * return true;
	 * }
	 * }
	 * 
	 * return false;
	 * }
	 */
}
