package com.chiorichan.framework;

import groovy.lang.Binding;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;

import com.chiorichan.Loader;
import com.chiorichan.StartupException;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.event.EventException;
import com.chiorichan.event.server.SiteLoadEvent;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.user.LookupAdapterException;
import com.chiorichan.user.builtin.FileAdapter;
import com.chiorichan.user.builtin.SqlAdapter;
import com.chiorichan.user.builtin.UserLookupAdapter;
import com.chiorichan.util.FileUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class Site
{
	public String siteId = null, title = null, domain = null;
	File source, resource;
	Map<String, String> subdomains = Maps.newConcurrentMap(),
			aliases = Maps.newConcurrentMap();
	List<String> metatags = Lists.newCopyOnWriteArrayList(),
			protectedFiles = Lists.newCopyOnWriteArrayList();
	YamlConfiguration config;
	SqlConnector sql;
	
	// Binding and evaling for use inside each site for executing site scripts outside of web requests.
	Binding binding = new Binding();
	Evaling eval = new Evaling( binding );
	
	UserLookupAdapter userLookupAdapter = null;
	
	public Site(File f) throws SiteException, StartupException
	{
		config = YamlConfiguration.loadConfiguration( f );
		
		if ( config == null )
			throw new SiteException( "Could not load site from YAML FileBase '" + f.getAbsolutePath() + "'" );
		
		siteId = config.getString( "site.siteId", null );
		title = config.getString( "site.title", Loader.getConfig().getString( "framework.sites.defaultTitle", "Unnamed Chiori-chan's Web Server Site" ) );
		domain = config.getString( "site.domain", null );
		
		String reason = null;
		
		if ( siteId == null )
			reason = "the provided Site Id is NULL. Check configs";
		else
			siteId = siteId.toLowerCase();
		
		if ( domain == null )
			reason = "the provided domain is NULL. Check configs";
		else
			domain = domain.toLowerCase();
		
		if ( Loader.getSiteManager().getSiteById( siteId ) != null )
			reason = "there already exists a site by the provided Site Id '" + siteId + "'";
		
		if ( reason != null )
			throw new SiteException( "Could not load site from YAML FileBase '" + f.getAbsolutePath() + "' because " + reason + "." );
		
		Loader.getLogger().info( "Loading site '" + siteId + "' with title '" + title + "' from YAML FileBase '" + f.getAbsolutePath() + "'." );
		
		// Load protected files list
		List<?> protectedFilesPre = config.getList( "protected", new CopyOnWriteArrayList<String>() );
		
		for ( Object o : protectedFilesPre )
		{
			if ( o instanceof String )
				protectedFiles.add( (String) o );
			else
				Loader.getLogger().warning( "Site '" + siteId + "' had an incorrect data object type under the YAML config for option 'protected', found type '" + o.getClass() + "'." );
		}
		
		// Load sources location
		String sources = config.getString( "site.source", "pages" );
		
		if ( sources == null || sources.isEmpty() )
		{
			source = getAbsoluteRoot();
		}
		else if ( sources.startsWith( "." ) )
		{
			source = new File( getAbsoluteRoot() + sources );
		}
		else
		{
			source = new File( getAbsoluteRoot(), sources );
			protectedFiles.add( "/" + sources );
		}
		
		FileUtil.directoryHealthCheck( source );
		
		// Load resources location
		String resources = config.getString( "site.resource", "resource" );
		
		if ( resources == null || resources.isEmpty() )
		{
			resource = getAbsoluteRoot();
		}
		else if ( resources.startsWith( "." ) )
		{
			resource = new File( getAbsoluteRoot() + resources );
		}
		else
		{
			resource = new File( getAbsoluteRoot(), resources );
			protectedFiles.add( "/" + resources );
		}
		
		FileUtil.directoryHealthCheck( resource );
		
		// Load metatags
		List<?> metatagsPre = config.getList( "metatags", new CopyOnWriteArrayList<String>() );
		
		for ( Object o : metatagsPre )
		{
			if ( o instanceof String )
				metatags.add( (String) o );
			else
				Loader.getLogger().warning( "Site '" + siteId + "' had an incorrect data object type under the YAML config for option 'metatags', found type '" + o.getClass() + "'." );
		}
		
		// Load aliases map
		ConfigurationSection aliasesPre = config.getConfigurationSection( "aliases" );
		if ( aliasesPre != null )
		{
			Set<String> akeys = aliasesPre.getKeys( false );
			
			if ( akeys != null )
				for ( String k : akeys )
				{
					if ( aliasesPre.getString( k, null ) != null )
						aliases.put( k, aliasesPre.getString( k ) );
				}
		}
		
		// Loader subdomains map
		ConfigurationSection subdomainsPre = config.getConfigurationSection( "subdomains" );
		if ( subdomainsPre != null )
		{
			Set<String> skeys = subdomainsPre.getKeys( false );
			
			if ( skeys != null )
				for ( String k : skeys )
				{
					if ( subdomainsPre.getString( k, null ) != null )
						subdomains.put( k, subdomainsPre.getString( k ) );
				}
		}
		
		finishLoad();
	}
	
	@SuppressWarnings( "unchecked" )
	public Site(ResultSet rs) throws SiteException, StartupException
	{
		try
		{
			Type mapType = new TypeToken<HashMap<String, String>>()
			{
			}.getType();
			
			siteId = rs.getString( "siteID" );
			title = rs.getString( "title" );
			domain = rs.getString( "domain" );
			
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
			
			if ( Loader.getSiteManager().getSiteById( siteId ) != null )
				reason = "there already exists a site by the provided Site Id '" + siteId + "'";
			
			if ( reason != null )
				throw new SiteException( "Could not load site from Database because " + reason + "." );
			
			Loader.getLogger().info( "Loading site '" + siteId + "' with title '" + title + "' from Database." );
			
			Gson gson = new GsonBuilder().create();
			try
			{
				if ( !rs.getString( "protected" ).isEmpty() )
					protectedFiles.addAll( gson.fromJson( new JSONObject( rs.getString( "protected" ) ).toString(), ArrayList.class ) );
			}
			catch ( Exception e )
			{
				Loader.getLogger().warning( "MALFORMED JSON EXPRESSION for 'protected' field for site '" + siteId + "'" );
			}
			
			String sources = rs.getString( "source" );
			
			if ( sources == null || sources.isEmpty() )
			{
				source = getAbsoluteRoot();
			}
			else if ( sources.startsWith( "." ) )
			{
				source = new File( getAbsoluteRoot() + sources );
			}
			else
			{
				source = new File( getAbsoluteRoot(), sources );
				protectedFiles.add( "/" + sources );
			}
			
			if ( source.isFile() )
				source.delete();
			
			if ( !source.exists() )
				source.mkdirs();
			
			String resources = rs.getString( "resource" );
			
			if ( resources == null || resources.isEmpty() )
			{
				resource = getAbsoluteRoot();
			}
			else if ( resources.startsWith( "." ) )
			{
				resource = new File( getAbsoluteRoot() + resources );
			}
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
				if ( !rs.getString( "metatags" ).isEmpty() )
					metatags.addAll( gson.fromJson( new JSONObject( rs.getString( "metatags" ) ).toString(), ArrayList.class ) );
			}
			catch ( Exception e )
			{
				Loader.getLogger().warning( "MALFORMED JSON EXPRESSION for 'metatags' field for site '" + siteId + "'" );
			}
			
			try
			{
				if ( !rs.getString( "aliases" ).isEmpty() )
					aliases = gson.fromJson( new JSONObject( rs.getString( "aliases" ) ).toString(), mapType );
			}
			catch ( Exception e )
			{
				Loader.getLogger().warning( "MALFORMED JSON EXPRESSION for 'aliases' field for site '" + siteId + "'" );
			}
			
			try
			{
				if ( !rs.getString( "subdomains" ).isEmpty() )
					subdomains = gson.fromJson( new JSONObject( rs.getString( "subdomains" ) ).toString(), mapType );
			}
			catch ( Exception e )
			{
				Loader.getLogger().warning( "MALFORMED JSON EXPRESSION for 'subdomains' field for site '" + siteId + "'" );
			}
			
			try
			{
				String yaml = rs.getString( "configYaml" );
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
		catch ( SQLException e )
		{
			throw new SiteException( e );
		}
	}
	
	private void finishLoad() throws SiteException, StartupException
	{
		// Framework site always uses the Builtin SQL Connector. Ignore YAML FileBase on this one.
		if ( siteId.equalsIgnoreCase( "framework" ) )
		{
			sql = Loader.getPersistenceManager().getSql();
		}
		else if ( config != null && config.getConfigurationSection( "database" ) != null )
		{
			String type = config.getString( "database.type" );
			
			String host = config.getString( "database.host" );
			String port = config.getString( "database.port" );
			String database = config.getString( "database.database" );
			String username = config.getString( "database.username" );
			String password = config.getString( "database.password" );
			
			String filename = config.getString( "database.filename" );
			
			sql = new SqlConnector();
			
			try
			{
				if ( type.equalsIgnoreCase( "mysql" ) )
					sql.init( database, username, password, host, port );
				else if ( type.equalsIgnoreCase( "sqlite" ) )
					sql.init( filename );
				else
					throw new SiteException( "The SqlConnector for site '" + siteId + "' can not support anything other then mySql or sqLite at the moment. Please change 'database.type' in the site config to 'mysql' or 'sqLite' and set the connection params." );
			}
			catch ( SQLException e )
			{
				if ( e.getCause() instanceof ConnectException )
					throw new SiteException( "We had a problem connecting to database '" + database + "'. Reason: " + e.getCause().getMessage() );
				else
					throw new SiteException( e.getMessage() );
			}
		}
		
		if ( config != null )
		{
			List<String> onLoadScripts = config.getStringList( "scripts.on-load" );
			
			if ( onLoadScripts != null )
			{
				for ( String script : onLoadScripts )
				{
					try
					{
						String result = WebUtils.evalPackage( eval, script, this );
						
						if ( result == null || result.isEmpty() )
							Loader.getLogger().info( "Finsihed evaling onLoadScript '" + script + "' for site '" + siteId + "'" );
						else
							Loader.getLogger().info( "Finsihed evaling onLoadScript '" + script + "' for site '" + siteId + "' with result: " + result );
					}
					catch ( IOException | CodeParsingException e )
					{
						Loader.getLogger().warning( "There was an exception encountered while evaling onLoadScript '" + script + "' for site '" + siteId + "'.", e );
					}
				}
			}
			
			if ( config.getConfigurationSection( "users" ) != null )
			{
				try
				{
					switch ( config.getString( "users.adapter", null ) )
					{
						case "sql":
							if ( sql == null )
								throw new SiteException( "Site '" + siteId + "' is configured with a SQL UserLookupAdapter but the site is missing a valid SQL Database, which is required for this adapter." );
							
							userLookupAdapter = new SqlAdapter( sql, config.getString( "users.table", "users" ), config.getStringList( "users.fields", new ArrayList<String>() ) );
							Loader.getLogger().info( "Initiated Sql UserLookupAdapter `" + userLookupAdapter + "` with sql '" + sql + "' for site '" + siteId + "'" );
							break;
						case "file":
							userLookupAdapter = new FileAdapter( config.getString( "users.filebase", "[site].users" ), this );
							Loader.getLogger().info( "Initiated FileBase UserLookupAdapter `" + userLookupAdapter + "` for site '" + siteId + "'" );
							break;
						case "builtin":
							if ( siteId.equalsIgnoreCase( "framework" ) )
								throw new SiteException( "Site 'framework' can not be configuration to use builtin UserLookupAdapter since it's the provider of the buildin UserLookupAdapter." );
							
							userLookupAdapter = Loader.getUserManager().getBuiltinUserLookupAdapter();
							Loader.getLogger().info( "Initiated Default UserLookupAdapter `" + userLookupAdapter + "` for site '" + siteId + "'" );
							break;
						case "shared":
							if ( config.getString( "users.shareWith", null ) == null )
								throw new SiteException( "Site '" + siteId + "' is configured to share the UserLookupAdapter with another site, but the config section 'users.shareWith' is missing." );
							
							Site shared = Loader.getSiteManager().getSiteById( config.getString( "users.shareWith" ) );
							
							if ( shared == null )
								throw new SiteException( "Site '" + siteId + "' is configured to share the UserLookupAdapter with site '" + config.getString( "users.shareWith" ) + "', but there was no sites found by that id." );
							
							if ( shared.getUserLookupAdapter() == null )
								throw new SiteException( "Site '" + siteId + "' is configured to share the UserLookupAdapter with site '" + config.getString( "users.shareWith" ) + "', but the found site has no Adapter configured." );
							
							userLookupAdapter = shared.getUserLookupAdapter();
							Loader.getLogger().info( "Initiated Shared UserLookupAdapter `" + userLookupAdapter + "` with site '" + config.getString( "users.shareWith" ) + "' for site '" + siteId + "'" );
							break;
						default: // TODO Create custom UserLookupAdapters.
							if ( siteId.equalsIgnoreCase( "framework" ) )
								throw new StartupException( "Site 'framework' must have a UserLookupAdapter configured. The simplest one to use would be 'file', set this in the '000-default.yaml' YAML FileBase." );
							
							Loader.getLogger().warning( "Site '" + siteId + "' is not configured with a UserLookupAdapter. This site will be unable to login any users." );
					}
				}
				catch ( LookupAdapterException e )
				{
					throw new SiteException( "There was an exception encoutered when attempting to create the UserLookupAdapter for site '" + siteId + "'.", e );
				}
			}
		}
		
		SiteLoadEvent event = new SiteLoadEvent( this );
		
		try
		{
			Loader.getEventBus().callEventWithException( event );
		}
		catch ( EventException e )
		{
			throw new SiteException( e );
		}
		
		// Plugins are not permitted to cancel the loading of the framework site
		if ( event.isCancelled() && !siteId.equalsIgnoreCase( "framework" ) )
			throw new SiteException( "Loading of site '" + siteId + "' was cancelled by an internal event." );
	}
	
	protected Site setDatabase( SqlConnector sql )
	{
		this.sql = sql;
		
		return this;
	}
	
	public YamlConfiguration getYaml()
	{
		if ( config == null )
			config = new YamlConfiguration();
		
		return config;
	}
	
	public List<String> getMetatags()
	{
		if ( metatags == null )
			return new CopyOnWriteArrayList<String>();
		
		return metatags;
	}
	
	public Map<String, String> getAliases()
	{
		return aliases;
	}
	
	public Site(String id, String title0, String domain0)
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
	
	public boolean protectCheck( String file )
	{
		if ( protectedFiles == null )
			return false;
		
		// Is the file being checked belong to our webroot
		if ( file.startsWith( getRoot() ) )
		{
			// Strip our webroot from file
			file = file.substring( getRoot().length() );
			
			for ( String n : protectedFiles )
			{
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
		}
		
		return false;
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
			// SiteException WILL NEVER THROW ON AN EMPTY SUBDOMAIN ARGUMENT. At least for now.
		}
	}
	
	public File getAbsoluteRoot( String subdomain ) throws SiteException
	{
		File target = new File( Loader.webroot, getRoot( subdomain ) );
		
		if ( target.isFile() )
			target.delete();
		
		if ( !target.exists() )
			target.mkdirs();
		
		return target;
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
			// SiteException WILL NEVER THROW ON AN EMPTY SUBDOMAIN ARGUMENT. At least for now.
		}
	}
	
	public String getRoot( String subdomain ) throws SiteException
	{
		String target = siteId;
		
		if ( subdomains != null && subdomain != null && !subdomain.isEmpty() )
		{
			String sub = subdomains.get( subdomain );
			
			if ( sub != null )
				target = siteId + "/" + sub;
			else if ( Loader.getConfig().getBoolean( "framework.sites.autoCreateSubdomains", true ) )
				target = siteId + "/" + subdomain;
			else if ( !Loader.getConfig().getBoolean( "framework.sites.subdomainsDefaultToRoot" ) )
				throw new SiteException( "This subdomain was not found on this server. If your the website owner, please check documentation." );
		}
		
		return target;
	}
	
	public SqlConnector getDatabase()
	{
		return sql;
	}
	
	public String applyAlias( String source )
	{
		if ( aliases == null || aliases.size() < 1 )
			return source;
		
		for ( Entry<String, String> entry : aliases.entrySet() )
		{
			source = source.replace( "%" + entry.getKey() + "%", entry.getValue() );
		}
		
		return source;
	}
	
	public String getName()
	{
		return siteId;
	}
	
	public File getResourceDirectory()
	{
		if ( resource == null )
			resource = new File( getAbsoluteRoot(), "resource" );
		
		return resource;
	}
	
	public File getSourceDirectory()
	{
		if ( source == null )
			source = getAbsoluteRoot();
		
		return source;
	}
	
	public void setAutoSave( boolean b )
	{
		// TODO Auto-generated method stub
	}
	
	public UserLookupAdapter getUserLookupAdapter()
	{
		return userLookupAdapter;
	}
	
	// TODO: Add methods to add protected files, metatags and aliases to site and save
	
	public void setGlobal( String key, Object val )
	{
		binding.setVariable( key, val );
	}
	
	public Object getGlobal( String key )
	{
		return binding.getVariable( key );
	}
	
	@SuppressWarnings( "unchecked" )
	public Map<String, Object> getGlobals()
	{
		return binding.getVariables();
	}
	
	protected Binding getBinding()
	{
		return binding;
	}
}
