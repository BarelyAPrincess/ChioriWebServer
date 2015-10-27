/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.site;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.text.WordUtils;
import org.json.JSONObject;

import com.chiorichan.Loader;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.file.YamlConfiguration;
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
import com.chiorichan.lang.StartupException;
import com.chiorichan.session.SessionManager;
import com.chiorichan.util.FileFunc;
import com.chiorichan.util.RandomFunc;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Holds the Site Instance
 */
public class Site
{
	public enum SessionPersistenceMethod
	{
		COOKIE, PARAM
	}
	
	private String siteId = null, title = null, domain = null;
	private File source, resource;
	private Map<String, String> subdomains = Maps.newConcurrentMap(), aliases = Maps.newConcurrentMap();
	private List<String> metatags = Lists.newCopyOnWriteArrayList(), protectedFiles = Lists.newCopyOnWriteArrayList();
	private YamlConfiguration config;
	private SQLDatastore sql;
	private SiteType siteType = SiteType.NOTSET;
	private File file;
	private List<String> cachePatterns = Lists.newArrayList();
	private Routes routes = null;
	private String encryptionKey = null;
	private String sessionPersistence = "cookie";
	
	// Binding and evaling for use inside each site for executing site scripts outside of web requests.
	private final ScriptBinding binding = new ScriptBinding();
	private final ScriptingFactory factory = ScriptingFactory.create( binding );
	
	Site( File file ) throws SiteException, StartupException
	{
		siteType = SiteType.FILE;
		this.file = file;
		
		config = YamlConfiguration.loadConfiguration( file );
		
		if ( config == null )
			throw new SiteException( "Could not load site file from YAML FileBase '" + file.getAbsolutePath() + "'" );
		
		siteId = config.getString( "site.siteId", null );
		title = config.getString( "site.title", Loader.getConfig().getString( "framework.sites.defaultTitle", "Unnamed Site" ) );
		domain = config.getString( "site.domain", null );
		
		encryptionKey = config.getString( "site.encryptionKey" );
		
		String reason = null;
		
		if ( siteId == null )
			reason = "the provided Site Id is NULL. Check configs";
		else
			siteId = siteId.toLowerCase();
		
		// XXX Temp file or old default siteId
		if ( "framework".equals( siteId ) )
			siteId = "default";
		
		// Default site is universal and accepts any domain
		if ( "default".equals( siteId ) )
			domain = "";
		
		if ( domain == null )
			reason = "the provided domain is NULL. Check configs";
		else
			domain = domain.toLowerCase();
		
		if ( SiteManager.INSTANCE.getSiteById( siteId ) != null )
			reason = "there already exists a site by the provided Site Id '" + siteId + "'";
		
		if ( reason != null )
			throw new SiteException( "Could not load site file from YAML FileBase '" + file.getAbsolutePath() + "' because " + reason + "." );
		
		Loader.getLogger().info( "Loading site '" + siteId + "' with title '" + title + "' file from YAML FileBase '" + file.getAbsolutePath() + "'." );
		
		// Load protected files list
		List<?> protectedFilesPre = config.getList( "protected", new CopyOnWriteArrayList<String>() );
		
		for ( Object o : protectedFilesPre )
			if ( o instanceof String )
				protectedFiles.add( ( String ) o );
			else
				Loader.getLogger().warning( "Site '" + siteId + "' had an incorrect data object type under the YAML config fileor option 'protected', fileound type '" + o.getClass() + "'." );
		
		// Load sources location
		String sources = config.getString( "site.source", "" );
		
		if ( sources == null || sources.isEmpty() )
			source = getAbsoluteRoot();
		else if ( sources.startsWith( "." ) )
			source = new File( getAbsoluteRoot() + sources );
		else
		{
			source = new File( getAbsoluteRoot(), sources );
			protectedFiles.add( "/" + sources );
		}
		
		FileFunc.directoryHealthCheck( source );
		
		// Load resources location
		String resources = config.getString( "site.resource", "resource" );
		
		if ( resources == null || resources.isEmpty() )
			resource = getAbsoluteRoot();
		else if ( resources.startsWith( "." ) )
		{
			resource = new File( getAbsoluteRoot() + resources );
			protectedFiles.add( "/" + resources );
		}
		else
		{
			resource = new File( getAbsoluteRoot(), resources );
			protectedFiles.add( "/" + resources );
		}
		
		FileFunc.directoryHealthCheck( resource );
		
		// Load metatags
		List<?> metatagsPre = config.getList( "metatags", new CopyOnWriteArrayList<String>() );
		
		for ( Object o : metatagsPre )
			if ( o instanceof String )
				metatags.add( ( String ) o );
			else
				Loader.getLogger().warning( "Site '" + siteId + "' had an incorrect data object type under the YAML config fileor option 'metatags', fileound type '" + o.getClass() + "'." );
		
		// Load aliases map
		ConfigurationSection aliasesPre = config.getConfigurationSection( "aliases" );
		if ( aliasesPre != null )
		{
			Set<String> akeys = aliasesPre.getKeys( false );
			
			if ( akeys != null )
				for ( String k : akeys )
					if ( aliasesPre.getString( k, null ) != null )
						aliases.put( k, aliasesPre.getString( k ) );
		}
		
		// Loader subdomains map
		ConfigurationSection subdomainsPre = config.getConfigurationSection( "subdomains" );
		if ( subdomainsPre != null )
		{
			Set<String> skeys = subdomainsPre.getKeys( false );
			
			if ( skeys != null )
				for ( String k : skeys )
					if ( subdomainsPre.getString( k, null ) != null )
						subdomains.put( k, subdomainsPre.getString( k ) );
		}
		
		finishLoad();
	}
	
	@SuppressWarnings( "unchecked" )
	Site( Map<String, String> data ) throws SiteException, StartupException
	{
		siteType = SiteType.SQL;
		file = null;
		
		Type mapType = new TypeToken<HashMap<String, String>>()
		{
		}.getType();
		
		siteId = data.get( "siteId" );
		title = data.get( "title" );
		domain = data.get( "domain" );
		
		encryptionKey = data.get( "encryptionKey" );
		
		String reason = null;
		
		if ( siteId == null )
			reason = "the provided Site Id is NULL. Check configs";
		else
			siteId = siteId.toLowerCase();
		
		if ( title == null )
			title = Loader.getConfig().getString( "framework.sites.defaultTitle", "Unnamed Chiori-chan's Web Server Site" );
		
		if ( domain == null )
			reason = "the provided domain is NULL. Check configs";
		else
			domain = domain.toLowerCase();
		
		if ( SiteManager.INSTANCE.getSiteById( siteId ) != null )
			reason = "there already exists a site by the provided Site Id '" + siteId + "'";
		
		if ( reason != null )
			throw new SiteException( "Could not load site from Database because " + reason + "." );
		
		Loader.getLogger().info( "Loading site '" + siteId + "' with title '" + title + "' from Database." );
		
		Gson gson = new GsonBuilder().create();
		try
		{
			if ( !data.get( "protected" ).isEmpty() )
				protectedFiles.addAll( gson.fromJson( new JSONObject( data.get( "protected" ) ).toString(), ArrayList.class ) );
		}
		catch ( Exception e )
		{
			Loader.getLogger().warning( "MALFORMED JSON EXPRESSION for 'protected' field for site '" + siteId + "'" );
		}
		
		String sources = data.get( "source" );
		
		if ( sources == null || sources.isEmpty() )
			source = getAbsoluteRoot();
		else if ( sources.startsWith( "." ) )
			source = new File( getAbsoluteRoot() + sources );
		else
		{
			source = new File( getAbsoluteRoot(), sources );
			protectedFiles.add( "/" + sources );
		}
		
		if ( source.isFile() )
			source.delete();
		
		if ( !source.exists() )
			source.mkdirs();
		
		String resources = data.get( "resource" );
		
		if ( resources == null || resources.isEmpty() )
			resource = getAbsoluteRoot();
		else if ( resources.startsWith( "." ) )
			resource = new File( getAbsoluteRoot() + resources );
		else
		{
			resource = new File( getAbsoluteRoot(), resources );
			protectedFiles.add( "/" + resources );
		}
		
		if ( resource.isFile() )
			resource.delete();
		
		if ( !resource.exists() )
			resource.mkdirs();
		
		try
		{
			if ( !data.get( "metatags" ).isEmpty() )
				metatags.addAll( gson.fromJson( new JSONObject( data.get( "metatags" ) ).toString(), ArrayList.class ) );
		}
		catch ( Exception e )
		{
			Loader.getLogger().warning( "MALFORMED JSON EXPRESSION for 'metatags' field for site '" + siteId + "'" );
		}
		
		try
		{
			if ( !data.get( "aliases" ).isEmpty() )
				aliases = gson.fromJson( new JSONObject( data.get( "aliases" ) ).toString(), mapType );
		}
		catch ( Exception e )
		{
			Loader.getLogger().warning( "MALFORMED JSON EXPRESSION for 'aliases' field for site '" + siteId + "'" );
		}
		
		try
		{
			if ( !data.get( "subdomains" ).isEmpty() )
				subdomains = gson.fromJson( new JSONObject( data.get( "subdomains" ) ).toString(), mapType );
		}
		catch ( Exception e )
		{
			Loader.getLogger().warning( "MALFORMED JSON EXPRESSION for 'subdomains' field for site '" + siteId + "'" );
		}
		
		try
		{
			String yaml = data.get( "configYaml" );
			InputStream is = new ByteArrayInputStream( yaml.getBytes( "ISO-8859-1" ) );
			config = YamlConfiguration.loadConfiguration( is );
		}
		catch ( Exception e )
		{
			Loader.getLogger().warning( "MALFORMED YAML EXPRESSION for 'configYaml' field for site '" + siteId + "'" );
			config = new YamlConfiguration();
		}
		
		finishLoad();
	}
	
	Site( String siteId )
	{
		this.siteId = siteId;
	}
	
	public Site( String id, String title0, String domain0 )
	{
		siteId = id;
		title = title0;
		domain = domain0;
		protectedFiles = Lists.newCopyOnWriteArrayList();
		metatags = Lists.newCopyOnWriteArrayList();
		aliases = Maps.newLinkedHashMap();
		subdomains = Maps.newLinkedHashMap();
		
		source = getAbsoluteRoot();
		resource = new File( getAbsoluteRoot(), "resource" );
		
		if ( !source.exists() )
			source.mkdirs();
		
		if ( !resource.exists() )
			resource.mkdirs();
	}
	
	public void addToCachePatterns( String pattern )
	{
		if ( !cachePatterns.contains( pattern.toLowerCase() ) )
			cachePatterns.add( pattern.toLowerCase() );
	}
	
	public HttpCookie createSessionCookie( String sessionId )
	{
		return new HttpCookie( getSessionKey(), sessionId ).setDomain( "." + getDomain() ).setPath( "/" ).setHttpOnly( true );
	}
	
	private void finishLoad() throws SiteException, StartupException
	{
		if ( encryptionKey == null )
			encryptionKey = RandomFunc.randomize( "0x0000X" );
		
		/*
		 * Default site always uses the Builtin SQL Connector. Ignore YAML FileBase on this one.
		 */
		if ( siteId.equalsIgnoreCase( "default" ) )
			sql = Loader.getDatabase();
		else if ( config != null && config.getConfigurationSection( "database" ) != null )
			switch ( config.getString( "database.type", "sqlite" ).toLowerCase() )
			{
				case "sqlite":
				{
					sql = new SQLiteDatastore( config.getString( "database.dbfile", config.getString( "database.filename", "server.db" ) ) );
					break;
				}
				case "mysql":
				{
					String host = config.getString( "database.host", "localhost" );
					String port = config.getString( "database.port", "3306" );
					String database = config.getString( "database.database", "chiorifw" );
					String username = config.getString( "database.username", "fwuser" );
					String password = config.getString( "database.password", "fwpass" );
					
					sql = new MySQLDatastore( database, username, password, host, port );
					break;
				}
				case "h2":
				{
					sql = new H2SQLDatastore( config.getString( "database.dbfile", config.getString( "database.filename", "server.db" ) ) );
					break;
				}
				case "none":
				case "":
					DatastoreManager.getLogger().warning( "The Database for site " + siteId + " is unconfigured, some features maybe not function as expected. See config option 'database.type' in the site config and set the connection params." );
					break;
				default:
					DatastoreManager.getLogger().severe( "We are sorry, the Database Engine currently only supports mysql, sqlite, and h2 databases but we found '" + config.getString( "server.database.type", "sqlite" ).toLowerCase() + "', please change 'database.type' to 'mysql', 'sqlite', or 'h2' in the site config and set the connection params" );
			}
		
		sessionPersistence = config.getString( "sessions.persistenceMethod", sessionPersistence ).toLowerCase();
		
		if ( !"cookie".equals( sessionPersistence ) && !"param".equals( sessionPersistence ) )
			throw new SiteException( "Session Perssitence of either 'cookie' or 'param' are supported." );
		
		if ( config != null )
		{
			List<String> onLoadScripts = config.getStringList( "scripts.on-load" );
			
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
		}
		
		SiteLoadEvent event = new SiteLoadEvent( this );
		
		try
		{
			EventBus.INSTANCE.callEventWithException( event );
		}
		catch ( EventException e )
		{
			throw new SiteException( e );
		}
		
		// Plugins are not permitted to cancel the loading of the default site
		if ( event.isCancelled() && !siteId.equalsIgnoreCase( "default" ) )
			throw new SiteException( "Loading of site '" + siteId + "' was cancelled by an internal event." );
		
		/*
		 * Warn the user that files can not be served from the `wisp`, a.k.a. Web Interface and Server Point, folder since the server uses it for internal requests.
		 */
		if ( new File( getAbsoluteRoot(), "wisp" ).exists() && !siteId.equalsIgnoreCase( "default" ) )
			SiteManager.getLogger().warning( "It would appear that site '" + siteId + "' contains a subfolder by the name of 'wisp', since this server uses the uri '/wisp' for internal requests, you will be unable to serve files from this folder!" );
	}
	
	public File getAbsoluteRoot()
	{
		try
		{
			return getAbsoluteRoot( null );
		}
		catch ( SiteException e )
		{
			return null;
			// A SiteException will never be thrown when the subdomain is empty.
		}
	}
	
	public File getAbsoluteRoot( String subdomain ) throws SiteException
	{
		File target = new File( Loader.getWebRoot(), getRoot( subdomain ) );
		
		if ( target.isFile() )
			target.delete();
		
		if ( !target.exists() )
			target.mkdirs();
		
		return target;
	}
	
	public Map<String, String> getAliases()
	{
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
	
	public SQLDatastore getDatabase()
	{
		return sql;
	}
	
	public String getDomain()
	{
		return domain;
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
		return file;
	}
	
	public Object getGlobal( String key )
	{
		return binding.getVariable( key );
	}
	
	public Map<String, Object> getGlobals()
	{
		return binding.getVariables();
	}
	
	// TODO: Add methods to add protected files, metatags and aliases to site and save
	
	public List<String> getMetatags()
	{
		if ( metatags == null )
			return new CopyOnWriteArrayList<String>();
		
		return metatags;
	}
	
	public String getName()
	{
		return siteId;
	}
	
	public File getResource( String packageNode )
	{
		try
		{
			return getResourceWithException( packageNode );
		}
		catch ( FileNotFoundException e )
		{
			if ( !packageNode.contains( ".includes." ) )
				Loader.getLogger().warning( e.getMessage() );
			return null;
		}
	}
	
	public File getResourceDirectory()
	{
		if ( resource == null )
			resource = new File( getAbsoluteRoot(), "resource" );
		
		return resource;
	}
	
	public File getResourceWithException( String pack ) throws FileNotFoundException
	{
		if ( pack == null || pack.isEmpty() )
			throw new FileNotFoundException( "Package can't be empty!" );
		
		pack = pack.replace( ".", System.getProperty( "file.separator" ) );
		
		File root = getResourceDirectory();
		
		File packFile = new File( root, pack );
		
		if ( packFile.exists() )
			return packFile;
		
		root = packFile.getParentFile();
		
		if ( root.exists() && root.isDirectory() )
		{
			File[] files = root.listFiles();
			String[] exts = new String[] {"html", "htm", "groovy", "gsp", "jsp", "chi"};
			
			for ( File child : files )
				if ( child.getName().startsWith( packFile.getName() ) )
					for ( String ext : exts )
						if ( child.getName().toLowerCase().endsWith( "." + ext ) )
							return child;
		}
		
		throw new FileNotFoundException( "Could not find the package `" + pack + "` file in site `" + getName() + "`." );
	}
	
	public String getRoot()
	{
		try
		{
			return getRoot( null );
		}
		catch ( SiteException e )
		{
			return null;
			// A SiteException will never be thrown when the subdomain is null.
		}
	}
	
	/**
	 * Returns the site webroot
	 * 
	 * @param subdomain
	 *            The subdomain subdirectory
	 * @return
	 *         Directory absolute path
	 * @throws SiteException
	 */
	public String getRoot( String subdomain ) throws SiteException
	{
		String target = siteId.replaceAll( " ", "" );
		
		if ( subdomains != null && subdomain != null && !subdomain.isEmpty() )
		{
			String sub = subdomains.get( subdomain );
			
			if ( sub != null )
				target = siteId + "/" + sub;
			else if ( Loader.getConfig().getBoolean( "framework.sites.autoCreateSubdomains", true ) )
				target = siteId + "/" + FileFunc.nameSpaceToPath( subdomain, true );
			else if ( !Loader.getConfig().getBoolean( "framework.sites.subdomainsDefaultToRoot" ) )
				throw new SiteException( "This subdomain was not found on this server, please check documentation." );
		}
		
		return target;
	}
	
	public Routes getRoutes()
	{
		if ( routes == null )
			routes = new Routes( this );
		
		return routes;
	}
	
	/**
	 * Gets the site configured Session Key from configuration.
	 * 
	 * @return
	 *         The Session Key
	 */
	public String getSessionKey()
	{
		String key = config.getString( "sessions.keyName" );
		if ( key == null )
			return SessionManager.getDefaultSessionName();
		return "_ws" + WordUtils.capitalize( key );
	}
	
	public SessionPersistenceMethod getSessionPersistenceMethod()
	{
		switch ( sessionPersistence )
		{
			case "cookie":
				return SessionPersistenceMethod.COOKIE;
			case "param":
				return SessionPersistenceMethod.PARAM;
			default:
				return null;
		}
	}
	
	public String getSiteId()
	{
		return siteId;
	}
	
	public File getSourceDirectory()
	{
		if ( source == null )
			source = getAbsoluteRoot();
		
		return source;
	}
	
	/**
	 * TODO Make it so site configuration can change the location the the temp directory.
	 * 
	 * @return The temp directory for this site.
	 */
	public File getTempFileDirectory()
	{
		File tmpFileDirectory = new File( Loader.getTempFileDirectory(), getSiteId() );
		
		if ( !tmpFileDirectory.exists() )
			tmpFileDirectory.mkdirs();
		
		if ( !tmpFileDirectory.isDirectory() )
			SiteManager.getLogger().severe( "The temp directory specified in the server configs is not a directory, File Uploads will FAIL until this problem is resolved." );
		
		if ( !tmpFileDirectory.canWrite() )
			SiteManager.getLogger().severe( "The temp directory specified in the server configs is not writable, File Uploads will FAIL until this problem is resolved." );
		
		return tmpFileDirectory;
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public YamlConfiguration getYaml()
	{
		if ( config == null )
			config = new YamlConfiguration();
		return config;
	}
	
	public boolean protectCheck( String file )
	{
		if ( protectedFiles == null )
			return false;
		
		// Does this file belong to our webroot
		if ( file.startsWith( getRoot() ) )
		{
			// Strip our webroot from file
			file = file.substring( getRoot().length() );
			
			for ( String n : protectedFiles )
				if ( n != null && !n.isEmpty() )
				{
					// If the length is greater then 1 and file starts with this string.
					if ( n.length() > 1 && file.startsWith( n ) )
						return true;
					
					// Does our file end with this. ie: .php, .txt, .etc
					if ( file.endsWith( n ) )
						return true;
					
					// If the pattern does not start with a /, see if name contain this string.
					if ( !n.startsWith( "/" ) && file.contains( n ) )
						return true;
					
					// Lastly try the string as a RegEx pattern
					if ( file.matches( n ) )
						return true;
				}
		}
		
		return false;
	}
	
	protected void save()
	{
		switch ( siteType )
		{
			case FILE:
				
				break;
			case SQL:
				
				break;
			default: // DO NOTHING
		}
		
		// TODO SAVE SITES ASAP
		// EncryptionKey needs to be saved ASAP
	}
	
	public void setAutoSave( boolean b )
	{
		// TODO Auto-generated method stub
	}
	
	protected Site setDatabase( SQLDatastore sql )
	{
		this.sql = sql;
		return this;
	}
	
	public void setGlobal( String key, Object val )
	{
		binding.setVariable( key, val );
	}
	
	public String siteId()
	{
		return siteId();
	}
	
	public SiteType siteType()
	{
		return siteType();
	}
	
	@Override
	public String toString()
	{
		return "Site{id=" + getSiteId() + "name=" + getName() + ",title=" + title + ",domain=" + getDomain() + ",type=" + siteType + ",source=" + source + ",resource=" + resource + "}";
	}
}
