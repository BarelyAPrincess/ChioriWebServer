/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.http.session.SessionProvider;

public class ConfigurationManagerWrapper
{
	protected SessionProvider sess;
	
	public ConfigurationManagerWrapper(SessionProvider _sess)
	{
		sess = _sess;
	}
	
	public YamlConfiguration getServerConfiguration()
	{
		return Loader.getConfig();
	}
	
	public DatabaseEngine getServerDatabase()
	{
		return Loader.getDatabase();
	}
	
	public YamlConfiguration getSiteConfiguration()
	{
		return sess.getSite().getYaml();
	}
	
	public DatabaseEngine getSiteDatabase()
	{
		return sess.getSite().getDatabase();
	}
	
	public boolean settingCompare( String settings )
	{
		return compareSetting( Arrays.asList( settings ) );
	}
	
	/**
	 * Does a setting compare based on a string if No expected mean is interped as a boolean of true. ex.
	 * USER_BETA_TESTER&USER_RANK=USER|USER_RANK=ADMIN
	 * 
	 * @param String
	 *             settingString
	 * @throws JSONException
	 */
	public boolean compareSetting( List<String> settings )
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
					if ( getSetting( key.substring( 1 ) ).equals( value ) )
						granted = true;
					else
					{
						granted = false;
						break;
					}
				}
				else
				{
					if ( getSetting( key ).equals( value ) )
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
	
	public boolean getSettingBool( String key )
	{
		Object result = getSetting( key );
		
		if ( result instanceof Boolean )
			return ( (Boolean) result );
		
		if ( result instanceof Integer && ( (Integer) result ) == 1 )
			return true;
		
		if ( result instanceof Long && ( (Long) result ) == 1 )
			return true;
		
		if ( result instanceof String && ( (String) result ).equals( "1" ) )
			return true;
		
		return false;
	}
	
	public Object getSetting( String key )
	{
		return getSetting( key, null, null, false );
	}
	
	public Object getSetting( String key, String idenifier )
	{
		return getSetting( key, idenifier, null, false );
	}
	
	public Object getSetting( String key, String idenifier, boolean defaultValue )
	{
		return getSetting( key, idenifier, defaultValue, false );
	}
	
	public Object getSetting( String key, String idenifier, Object defaultValue )
	{
		return getSetting( key, idenifier, defaultValue, false );
	}
	
	public Object getSetting( String key, String idenifier, boolean defaultValue, boolean returnRow )
	{
		return getSetting( key, idenifier, defaultValue, returnRow );
	}
	
	public Object getSetting( String key, String idenifier, Object defaultValue, boolean returnRow )
	{
		if ( defaultValue == null )
			defaultValue = "";
		
		try
		{
			DatabaseEngine sql = sess.getSite().getDatabase();
			
			if ( idenifier == null || idenifier == "-1" )
			{
				idenifier = ( sess.getUserState() ) ? sess.getAccount().getAccountId() : "";
			}
			
			ResultSet defaultRs = sql.query( "SELECT * FROM `settings_default` WHERE `key` = '" + key + "';" );
			
			if ( defaultRs == null || sql.getRowCount( defaultRs ) < 1 )
				return defaultValue;
			
			ResultSet customRs = sql.query( "SELECT * FROM `settings_custom` WHERE `key` = '" + key + "' AND `owner` = '" + idenifier + "';" );
			
			Map<String, Object> defop = DatabaseEngine.convertRow( defaultRs );
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
				
				Map<String, Object> op = DatabaseEngine.convertRow( customRs );
				
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
	public boolean setSetting( String key, String value, String idenifier )
	{
		try
		{
			DatabaseEngine sql = sess.getSite().getDatabase();
			
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
	
	/*
	 * public Object get( String key )
	 * {
	 * try
	 * {
	 * return sess.getRequest().getSite().getYaml().get( key );
	 * }
	 * catch ( Exception e )
	 * {
	 * return null;
	 * }
	 * }
	 * public boolean keyExists( String key )
	 * {
	 * return ( sess.getRequest().getSite().getYaml().get( key ) != null );
	 * }
	 * public String getString( String key )
	 * {
	 * return sess.getRequest().getSite().getYaml().getString( key );
	 * }
	 * public List<?> getArray( String key )
	 * {
	 * return sess.getRequest().getSite().getYaml().getList( key );
	 * }
	 * public boolean getBoolean( String key )
	 * {
	 * return sess.getRequest().getSite().getYaml().getBoolean( key );
	 * }
	 * public int getInt( String key )
	 * {
	 * return sess.getRequest().getSite().getYaml().getInt( key );
	 * }
	 */
}
