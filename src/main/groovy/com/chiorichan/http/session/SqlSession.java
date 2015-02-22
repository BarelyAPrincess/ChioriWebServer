/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http.session;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.http.Candy;
import com.chiorichan.util.Common;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class SqlSession extends Session
{
	public SqlSession( ResultSet rs ) throws SessionException
	{
		try
		{
			stale = true;
			
			timeout = rs.getInt( "timeout" );
			lastIpAddr = rs.getString( "ipAddr" );
			
			if ( !rs.getString( "data" ).isEmpty() )
				data = new Gson().fromJson( rs.getString( "data" ), new TypeToken<Map<String, String>>()
				{
					private static final long serialVersionUID = 2808406085740098578L;
				}.getType() );
			
			if ( rs.getString( "sessionName" ) != null && !rs.getString( "sessionName" ).isEmpty() )
				candyName = rs.getString( "sessionName" );
			candyId = rs.getString( "sessionId" );
			
			if ( timeout < Common.getEpoch() )
				throw new SessionException( "This session expired at " + timeout + " epoch!" );
			
			if ( rs.getString( "sessionSite" ) == null || rs.getString( "sessionSite" ).isEmpty() )
				setSite( Loader.getSiteManager().getFrameworkSite() );
			else
				setSite( Loader.getSiteManager().getSiteById( rs.getString( "sessionSite" ) ) );
			
			sessionCandy = new Candy( candyName, rs.getString( "sessionId" ) );
			candies.put( candyName, sessionCandy );
			
			loginSessionUser();
			
			Loader.getLogger().info( ConsoleColor.DARK_AQUA + "Session Restored `" + this + "`" );
		}
		catch ( SQLException e )
		{
			throw new SessionException( e );
		}
	}
	
	protected SqlSession()
	{
		
	}
	
	@Override
	public void reloadSession()
	{
		ResultSet rs = null;
		try
		{
			rs = Loader.getDatabase().query( "SELECT * FROM `sessions` WHERE `sessionId` = '" + sessionCandy.getValue() + "'" );
		}
		catch ( SQLException e1 )
		{
			e1.printStackTrace();
		}
		
		if ( rs == null || Loader.getDatabase().getRowCount( rs ) < 1 )
			sessionCandy = null;
		else
		{
			try
			{
				if ( rs.getLong( "timeout" ) > timeout )
					timeout = rs.getLong( "timeout" );
				
				if ( !rs.getString( "data" ).isEmpty() )
				{
					Map<String, String> tmpData = new Gson().fromJson( rs.getString( "data" ), new TypeToken<Map<String, String>>()
					{
						private static final long serialVersionUID = -1734352198651744570L;
					}.getType() );
					
					if ( changesMade )
					{
						tmpData.putAll( data );
						data = tmpData;
					}
					else
						data.putAll( tmpData );
				}
				
				String ipAddr = rs.getString( "ipAddr" );
				if ( ipAddr != null && !ipAddr.isEmpty() )
				{
					// Possible Session Hijacking! nullify!!!
					if ( !ipAddr.equals( ipAddr ) && !Loader.getConfig().getBoolean( "sessions.allowIPChange" ) )
					{
						sessionCandy = null;
					}
					
					this.lastIpAddr = ipAddr;
				}
			}
			catch ( JsonSyntaxException | SQLException e )
			{
				e.printStackTrace();
				sessionCandy = null;
			}
		}
	}
	
	@Override
	public void saveSession()
	{
		String dataJson = new Gson().toJson( data );
		
		DatabaseEngine sql = Loader.getDatabase();
		
		if ( sql == null )
		{
			Loader.getLogger().severe( "There was a problem saving a session because the Framework Database was NULL!" );
			return;
		}
		
		try
		{
			ResultSet rs = sql.query( "SELECT * FROM `sessions` WHERE `sessionId` = '" + getSessId() + "';" );
			
			if ( rs == null || sql.getRowCount( rs ) < 1 )
				sql.queryUpdate( "INSERT INTO `sessions` (`sessionId`, `timeout`, `ipAddr`, `sessionName`, `sessionSite`, `data`)VALUES('" + sessionCandy.getValue() + "', '" + getTimeout() + "', '" + getIpAddr() + "', '" + sessionCandy.getKey() + "', '" + getSite().getName() + "', '" + dataJson + "');" );
			else
				sql.queryUpdate( "UPDATE `sessions` SET `data` = '" + dataJson + "', `timeout` = '" + getTimeout() + "', `sessionName` = '" + sessionCandy.getKey() + "', `ipAddr` = '" + getIpAddr() + "', `sessionSite` = '" + getSite().getName() + "' WHERE `sessionId` = '" + sessionCandy.getValue() + "';" );
		}
		catch ( SQLException e )
		{
			Loader.getLogger().severe( "There was an exception thorwn while trying to save the session.", e );
		}
	}
	
	@Override
	protected void destroySession()
	{
		try
		{
			Loader.getDatabase().queryUpdate( "DELETE FROM `sessions` WHERE `sessionName` = '" + getName() + "' AND `sessionId` = '" + getSessId() + "';" );
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
	}
	
	protected static List<Session> getActiveSessions()
	{
		List<Session> sessionList = Lists.newCopyOnWriteArrayList();
		DatabaseEngine sql = Loader.getDatabase();
		
		try
		{
			ResultSet rs = sql.query( "SELECT * FROM `sessions`;" );
			
			if ( sql.getRowCount( rs ) > 0 )
				do
				{
					try
					{
						sessionList.add( new SqlSession( rs ) );
					}
					catch ( SessionException e )
					{
						if ( e.getMessage().contains( "expired" ) )
							sql.queryUpdate( "DELETE FROM `sessions` WHERE `sessionId` = '" + rs.getString( "sessionId" ) + "' && `sessionName` = '" + rs.getString( "sessionName" ) + "';" );
						else
							e.printStackTrace();
					}
				}
				while ( rs.next() );
		}
		catch ( SQLException e )
		{
			Loader.getLogger().warning( "There was a problem reloading saved sessions.", e );
		}
		
		return sessionList;
	}
}
