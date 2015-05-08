/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.session;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.util.TimingFunc;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

public class SqlDatastore extends SessionDatastore
{
	@Override
	List<SessionData> getSessions() throws SessionException
	{
		List<SessionData> data = Lists.newArrayList();
		DatabaseEngine sql = Loader.getDatabase();
		
		if ( sql == null )
			throw new SessionException( "Sessions can't be stored in a SQL Database without a properly configured server database." );
		
		TimingFunc.start( this );
		
		try
		{
			ResultSet rs = sql.query( "SELECT * FROM `sessions`;" );
			
			if ( sql.getRowCount( rs ) > 0 )
				do
				{
					try
					{
						data.add( new SqlSessionData( rs ) );
					}
					catch ( SessionException e )
					{
						e.printStackTrace();
					}
				}
				while ( rs.next() );
		}
		catch ( SQLException e )
		{
			Loader.getLogger().warning( "There was a problem reloading saved sessions.", e );
		}
		
		PermissionManager.getLogger().info( "SqlSession loaded " + data.size() + " sessions from the data store in " + TimingFunc.finish( this ) + "ms!" );
		
		return data;
	}
	
	@Override
	public SessionData createSession() throws SessionException
	{
		return new SqlSessionData();
	}
	
	class SqlSessionData extends SessionData
	{
		SqlSessionData( ResultSet rs ) throws SessionException
		{
			this();
			
			try
			{
				readSession( rs );
			}
			catch ( SQLException e )
			{
				throw new SessionException( e );
			}
		}
		
		SqlSessionData()
		{
			super( SqlDatastore.this );
		}
		
		@Override
		void reload() throws SessionException
		{
			ResultSet rs = null;
			try
			{
				rs = Loader.getDatabase().query( "SELECT * FROM `sessions` WHERE `sessionId` = '" + sessionId + "'" );
				if ( rs == null || Loader.getDatabase().getRowCount( rs ) < 1 )
					return;
				readSession( rs );
			}
			catch ( SQLException e )
			{
				throw new SessionException( e );
			}
		}
		
		private void readSession( ResultSet rs ) throws SQLException
		{
			timeout = rs.getInt( "timeout" );
			ipAddr = rs.getString( "ipAddr" );
			
			if ( rs.getString( "sessionName" ) != null && !rs.getString( "sessionName" ).isEmpty() )
				sessionName = rs.getString( "sessionName" );
			sessionId = rs.getString( "sessionId" );
			
			site = Loader.getSiteManager().getSiteById( rs.getString( "sessionSite" ) );
			
			if ( !rs.getString( "data" ).isEmpty() )
			{
				data = new Gson().fromJson( rs.getString( "data" ), new TypeToken<Map<String, String>>()
				{
					private static final long serialVersionUID = -1734352198651744570L;
				}.getType() );
			}
		}
		
		@Override
		void save() throws SessionException
		{
			try
			{
				String dataJson = new Gson().toJson( data );
				DatabaseEngine sql = Loader.getDatabase();
				
				if ( sql == null )
					throw new SessionException( "Sessions can't be stored in a SQL Database without a properly configured server database." );
				
				ResultSet rs = sql.query( "SELECT * FROM `sessions` WHERE `sessionId` = '" + sessionId + "';" );
				
				if ( rs == null || sql.getRowCount( rs ) < 1 )
					sql.queryUpdate( "INSERT INTO `sessions` (`sessionId`, `timeout`, `ipAddr`, `sessionName`, `sessionSite`, `data`) VALUES ('" + sessionId + "', '" + timeout + "', '" + ipAddr + "', '" + sessionName + "', '" + site.getSiteId() + "', '" + dataJson + "');" );
				else
					sql.queryUpdate( "UPDATE `sessions` SET `data` = '" + dataJson + "', `timeout` = '" + timeout + "', `sessionName` = '" + sessionName + "', `ipAddr` = '" + ipAddr + "', `sessionSite` = '" + site.getSiteId() + "' WHERE `sessionId` = '" + sessionId + "';" );
			}
			catch ( SQLException e )
			{
				throw new SessionException( "There was an exception thrown while trying to save the session.", e );
			}
		}
		
		@Override
		void destroy() throws SessionException
		{
			try
			{
				Loader.getDatabase().queryUpdate( "DELETE FROM `sessions` WHERE `sessionId` = '" + sessionId + "';" );
			}
			catch ( SQLException e )
			{
				throw new SessionException( "There was an exception thrown while trying to save the session.", e );
			}
		}
	}
}