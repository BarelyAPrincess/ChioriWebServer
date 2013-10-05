package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import com.chiorichan.database.SqlConnector;

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
	
	public Object get( String key, String idenifier, boolean defaultValue ) throws JSONException
	{
		return get( key, idenifier, defaultValue, false );
	}
	
	public Object get( String key, String idenifier, Object defaultValue ) throws JSONException
	{
		return get( key, idenifier, defaultValue, false );
	}
	
	public Object get( String key, String idenifier, boolean defaultValue, boolean returnRow ) throws JSONException
	{
		return get( key, idenifier, defaultValue, returnRow );
	}
	
	public Object get( String key, String idenifier, Object defaultValue, boolean returnRow ) throws JSONException
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
			
			Map<String, Object> defop = SqlConnector.convertRow( defaultRs );
			defop.put( "default", defop.get( "value" ) );
			
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
				
				Map<String, Object> op = SqlConnector.convertRow( customRs );
				
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
