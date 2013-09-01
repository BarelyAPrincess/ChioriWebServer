package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.google.gson.Gson;

public class FrameworkConfigurationManager
{
	protected Framework fw;
	
	public FrameworkConfigurationManager( Framework fw0 )
	{
		fw = fw0;
	}
	
	public Object getConfig( String key )
	{
		try
		{
			return fw.getCurrentSite().getYaml().get( key );
		}
		catch ( Exception e )
		{
			return null;
		}
	}
	
	public boolean keyExists( String key )
	{
		return ( fw.getCurrentSite().getYaml().get( key ) != null );
	}
	
	public String getString( String key )
	{
		return fw.getCurrentSite().getYaml().getString( key );
	}
	
	public List<?> getArray( String key )
	{
		return fw.getCurrentSite().getYaml().getList( key );
	}
	
	public boolean getBoolean( String key )
	{
		return fw.getCurrentSite().getYaml().getBoolean( key );
	}
	
	public int getInt( String key )
	{
		return fw.getCurrentSite().getYaml().getInt( key );
	}
	
	public SqlConnector getDatabase()
	{
		return fw.getCurrentSite().sql;
	}
	
	public boolean settingCompare( String setting ) throws JSONException
	{
		return settingCompare( Arrays.asList( setting ) );
	}
	
	/**
	 * Does a setting compare based on a string if No expected mean is interped as a boolean of true. ex.
	 * USER_BETA_TESTER&USER_RANK=USER|USER_RANK=ADMIN
	 * 
	 * @param String
	 *           settingString
	 * @throws JSONException 
	 */
	public boolean settingCompare( List<String> settings ) throws JSONException
	{
		for ( String setting : settings )
		{
			boolean granted = false;
			for ( String key : setting.split( "[&]" ) )
			{
				String value;
				
				if ( key.indexOf( "=" ) < 0 )
					value = "1";
				else
				{
					value = key.substring( key.indexOf( "=" ) );
					key = key.substring( 0, key.indexOf( "=" ) );
				}
				
				if ( key.startsWith( "!" ) )
				{
					if ( get( key.substring( 1 ) ).equals( value ) )
						granted = true;
					else
					{
						granted = false;
						break;
					}
				}
				else
				{
					if ( get( key ).equals( value ) )
						granted = true;
					else
					{
						granted = false;
						break;
					}
				}
			}
			
			if ( granted )
				return true;
		}
		
		return false;
	}
	
	public Object get( String key ) throws JSONException
	{
		return get( key, null, null, false );
	}
	
	public Object get( String key, String idenifier ) throws JSONException
	{
		return get( key, idenifier, null, false );
	}
	
	public Object get( String key, String idenifier, String defaultValue ) throws JSONException
	{
		return get( key, idenifier, defaultValue, false );
	}
	
	public Object get( String key, String idenifier, String defaultValue, boolean returnRow ) throws JSONException
	{
		if ( defaultValue == null )
			defaultValue = "";
		
		try
		{
			SqlConnector sql = fw.getCurrentSite().sql;
			
			if ( idenifier == null || idenifier == "-1" )
			{
				idenifier = ( fw.getUserService().getUserState() ) ? fw.getUserService().getCurrentUser().getUserId() : "";
			}
			
			ResultSet defaultRs = sql.query( "SELECT * FROM `settings_default` WHERE `key` = '" + key + "';" );
			
			if ( defaultRs == null || sql.getRowCount( defaultRs ) < 1 )
				return defaultValue;
			
			ResultSet customRs = sql.query( "SELECT * FROM `settings_custom` WHERE `key` = '" + key + "' AND `owner` = '" + idenifier + "';" );
			
			JSONObject json = SqlConnector.convert( defaultRs );
			Map<String, Object> defop = (Map<String, Object>) new Gson().fromJson( json.toString(), TreeMap.class ).get( "0" );
			
			if ( customRs == null || sql.getRowCount( customRs ) < 1 )
			{
				defaultRs.first();
				
				if ( !returnRow )
					return defaultRs.getString( "value" );
				
				return defop;
			}
			else
			{
				if ( !returnRow )
					return customRs.getString( "value" );
				
				json = SqlConnector.convert( customRs );
				Map<String, Object> op = (Map<String, Object>) new Gson().fromJson( json.toString(), TreeMap.class ).get( "0" );
				
				defop.putAll( op );
				
				return defop;
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
			SqlConnector sql = fw.getCurrentSite().sql;
			
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
