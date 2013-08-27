package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.chiorichan.database.SqlConnector;

public class FrameworkConfigurationManager
{
	protected HttpServletRequest request;
	protected HttpServletResponse response;
	protected FilterChain chain;
	protected String requestId;
	protected Site site;
	
	public FrameworkConfigurationManager(HttpServletRequest request0, HttpServletResponse response0, FilterChain chain0, String requestId0, Site site0)
	{
		request = request0;
		response = response0;
		chain = chain0;
		requestId = requestId0;
		site = site0;
	}
	
	public Object getConfig( String key )
	{
		try
		{
			return site.getYaml().get( key );
		}
		catch ( Exception e )
		{
			return null;
		}
	}
	
	public boolean keyExists( String key )
	{
		return ( site.getYaml().get( key ) != null );
	}
	
	public String getString( String key )
	{
		return site.getYaml().getString( key );
	}
	
	public List<?> getArray( String key )
	{
		return site.getYaml().getList( key );
	}
	
	public boolean getBoolean( String key )
	{
		return site.getYaml().getBoolean( key );
	}
	
	public int getInt( String key )
	{
		return site.getYaml().getInt( key );
	}
	
	public SqlConnector getDatabase()
	{
		return site.sql;
	}
	
	/**
	 * Does a setting compare based on a string if No expected mean is interped as a boolean of true. ex.
	 * USER_BETA_TESTER&USER_RANK=USER|USER_RANK=ADMIN
	 * 
	 * @param String
	 *           settingString
	 */
	public boolean settingCompare( String setting )
	{
		// TODO!!!
		return false;
	}
	
	public String get( String key, String idenifier )
	{
		return get( key, idenifier, null );
	}
	
	public String get( String key, String idenifier, String defaultValue )
	{
		try
		{
			SqlConnector sql = site.sql;
			
			if ( idenifier == null || idenifier == "-1" )
			{
				// TODO: Set idenifier to the logged in userId
			}
			
			ResultSet defaultRs = sql.query( "SELECT * FROM `settings_default` WHERE `key` = '" + key + "';" );
			
			if ( defaultRs == null || sql.getRowCount( defaultRs ) < 1 )
				return defaultValue;
			
			ResultSet customRs = sql.query( "SELECT * FROM `settings_custom` WHERE `key` = '" + key + "' AND `owner` = '" + idenifier + "';" );
			
			if ( customRs == null || sql.getRowCount( customRs ) < 1 )
			{
				return defaultRs.getString( "value" );
			}
			else
			{
				return customRs.getString( "value" );
			}
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
			return defaultValue;
		}
	}
	
	/*
	 * Empty value deletes resets setting to default.
	 */
	public boolean set( String key, String value, String idenifier )
	{
		try
		{
			SqlConnector sql = site.sql;
			
			if ( idenifier == null || idenifier == "-1" )
			{
				// TODO: Set idenifier to the logged in userId
				
				// if ( key.startsWith( "TEXT" ) || key.startsWith( "LOCATION" ) )
				// TODO: Set to the first location user is allowed to use
				// else if ( key.startsWith( "ACCOUNT" ) )
				
				// else
				idenifier = "";
			}
			
			ResultSet defaultRs = sql.query( "SELECT * FROM `settings_default` WHERE `key` = '" + key + "';" );
			
			if ( defaultRs == null || sql.getRowCount( defaultRs ) < 1 )
				return false;
			
			if ( value.isEmpty() || defaultRs.getString( "value" ).equals( value ) )
			{
				sql.queryUpdate( "DELETE FROM `settings_custom` WHERE `key` = '" + key + "' AND `owner` = '" + idenifier + "'" );
				return true;
			}
			
			ResultSet customRs = sql.query( "SELECT * FROM `settings_custom` WHERE `key` = '" + key + "' AND `owner` = '" + idenifier + "' LIMIT 1;" );
			
			if ( customRs == null || sql.getRowCount( customRs ) < 1 )
			{
				sql.queryUpdate( "UPDATE `settings_custom` SET `value` = '" + value + "' WHERE `key` = '" + key + "' AND `owner` = '" + idenifier + "';" );
			}
			else
			{
				sql.queryUpdate( "INSERT INTO `settings_custom` (`key`, `value`, `owner`)VALUES('" + key + "', '" + value + "', '" + idenifier + "');" );
			}
			
			return true;
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
			return false;
		}
	}
}
