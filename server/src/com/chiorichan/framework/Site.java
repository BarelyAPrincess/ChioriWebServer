package com.chiorichan.framework;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.ConnectException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONObject;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.user.UserList;
import com.google.gson.Gson;

public class Site
{
	public String siteId, title, domain;
	Map<String, String> subdomains, aliases;
	Set<String> metatags, protectedFiles;
	UserList userList = null;
	YamlConfiguration config;
	SqlConnector sql;
	
	//NOTE TO SELF: Sets do not contain duplicates while Lists can
	
	public Site(ResultSet rs) throws SQLException
	{
		siteId = rs.getString( "siteID" );
		title = rs.getString( "title" );
		domain = rs.getString( "domain" );
		
		Loader.getLogger().info( "Loading site '" + siteId + "' with title '" + title + "' from Framework Database." );
		
		// Convert from hashmap to JSON
		// new JSONObject( LinkedHashMap );
		
		Gson gson = new Gson();
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
				aliases = gson.fromJson( new JSONObject( rs.getString( "aliases" ) ).toString(), LinkedHashMap.class );
		}
		catch ( Exception e )
		{
			Loader.getLogger().warning( "MALFORMED JSON EXPRESSION for 'aliases' field for site '" + siteId + "'" );
		}
		
		try
		{
			if ( !rs.getString( "subdomains" ).isEmpty() )
				subdomains = gson.fromJson( new JSONObject( rs.getString( "subdomains" ) ).toString(), LinkedHashMap.class );
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
			//String type = config.getString("database.type");
			String host = config.getString("database.host");
			String port = config.getString("database.port");
			String database = config.getString("database.database");
			String username = config.getString("database.username");
			String password = config.getString("database.password");
			
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
				Loader.getLogger().info( "Successfully connected to site database for site " + siteId );
				
				initalizeUserList();
			}
		}
	}
	
	protected Site()
	{
		
	}
	
	private void initalizeUserList()
	{
		userList = new UserList( this );
		
		
		
	}
	
	public UserList getUserList()
	{
		return userList;
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
		metatags = new HashSet<String>();
		aliases = new LinkedHashMap<String, String>();
		subdomains = new LinkedHashMap<String, String>();
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
	
	public String subDomain = "";

	public void setSubDomain( String var1 )
	{
		if ( var1 == null )
			var1 = "";
		
		subDomain = var1;
	}

	public String getSubDomain()
	{
		return subDomain;
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
			source = source.replace( "%" + entry.getKey() + "%", entry.getValue() );
		
		return source;
	}

	public String getName()
	{
		return siteId;
	}
	
	// TODO: Add methods to add protected files, metatags and aliases to site and save
}
