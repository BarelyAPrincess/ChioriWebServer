package com.chiorichan.framework;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONObject;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.event.EventException;
import com.chiorichan.event.server.SiteLoadEvent;
import com.chiorichan.file.YamlConfiguration;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class Site
{
	public String siteId, title, domain;
	Map<String, String> subdomains, aliases;
	Set<String> metatags, protectedFiles;
	YamlConfiguration config;
	SqlConnector sql;
	
	@SuppressWarnings( "unchecked" )
	public Site(ResultSet rs) throws SiteException
	{
		try
		{
			Type mapType = new TypeToken<HashMap<String, String>>() {}.getType();
			
			siteId = rs.getString( "siteID" );
			title = rs.getString( "title" );
			domain = rs.getString( "domain" );
			
			Loader.getLogger().info( "Loading site '" + siteId + "' with title '" + title + "' from Framework Database." );
			
			Gson gson = new GsonBuilder().create();
			try
			{
				if ( !rs.getString( "protected" ).isEmpty() )
					protectedFiles = gson.fromJson( new JSONObject( rs.getString( "protected" ) ).toString(), HashSet.class );
			}
			catch ( Exception e )
			{
				Loader.getLogger().warning( "MALFORMED JSON EXPRESSION for 'protected' field for site '" + siteId + "'" );
			}
			
			try
			{
				if ( !rs.getString( "metatags" ).isEmpty() )
					metatags = gson.fromJson( new JSONObject( rs.getString( "metatags" ) ).toString(), HashSet.class );
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
			
			if ( config != null && config.getConfigurationSection( "database" ) != null )
			{
				// String type = config.getString("database.type");
				String host = config.getString( "database.host" );
				String port = config.getString( "database.port" );
				String database = config.getString( "database.database" );
				String username = config.getString( "database.username" );
				String password = config.getString( "database.password" );
				
				sql = new SqlConnector();
				
				try
				{
					sql.init( database, username, password, host, port );
				}
				catch ( SQLException e )
				{
					if ( e.getCause() instanceof ConnectException )
						Loader.getLogger().severe( "We had a problem connecting to database '" + database + "'. Reason: " + e.getCause().getMessage() );
					else
						Loader.getLogger().severe( e.getMessage() );
					
					return;
				}
				finally
				{
					Loader.getLogger().info( "Successfully connected to site database for site `" + siteId + "`" );
				}
			}
			
			SiteLoadEvent event = new SiteLoadEvent( this );
			
			Loader.getPluginManager().callEventWithException( event );
			
			if ( event.isCancelled() )
				throw new SiteException( "Site loading was cancelled by an internal event." );
		}
		catch ( SQLException | EventException e )
		{
			throw new SiteException( e );
		}
	}
	
	public YamlConfiguration getYaml()
	{
		if ( config == null )
			config = new YamlConfiguration();
		
		return config;
	}
	
	public Set<String> getMetatags()
	{
		if ( metatags == null )
			return new HashSet<String>();
		
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
		protectedFiles = new HashSet<String>();
		metatags = Sets.newHashSet();
		aliases = Maps.newLinkedHashMap();
		subdomains = Maps.newLinkedHashMap();
	}
	
	public boolean protectCheck( String file )
	{
		if ( protectedFiles == null )
			return false;
		
		return protectedFiles.contains( file );
	}
	
	public File getAbsoluteRoot( String subdomain )
	{
		File target = new File( Loader.webroot, getWebRoot( subdomain ) );
		
		if ( target.isFile() )
			target.delete();
		
		if ( !target.exists() )
			target.mkdirs();
		
		return target;
	}
	
	public File getAbsoluteWebRoot( String subdomain )
	{
		File root = new File( Loader.webroot, getWebRoot( subdomain ) );
		
		if ( !root.exists() )
			root.mkdirs();
		
		return root;
	}
	
	public String getWebRoot( String subdomain )
	{
		String target = "/" + siteId;
		
		if ( subdomains != null && subdomain != null && !subdomain.isEmpty() )
		{
			String sub = subdomains.get( subdomain );
			
			if ( sub != null )
				target = "/" + siteId + "/" + sub;
		}
		
		return target;
	}
	
	public SqlConnector getDatabase()
	{
		return sql;
	}
	
	public File getResourceRoot()
	{
		File root = getAbsoluteRoot( null );
		root = new File( root.getAbsolutePath() + ".template" );
		
		if ( root.isFile() || !root.exists() )
		{
			root = new File( root.getAbsolutePath() + ".resource" );
			
			if ( root.isFile() )
				root.delete();
			
			if ( !root.exists() )
				root.mkdirs();
		}
		
		return root;
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
	
	public void setAutoSave( boolean b )
	{
		// TODO Auto-generated method stub
	}
	
	// TODO: Add methods to add protected files, metatags and aliases to site and save
}
