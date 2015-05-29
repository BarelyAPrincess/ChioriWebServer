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
import com.chiorichan.scheduler.Timings;
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
		
		Timings.start( this );
		
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
		
		PermissionManager.getLogger().info( "SqlSession loaded " + data.size() + " sessions from the datastore in " + Timings.finish( this ) + "ms!" );
		
		return data;
	}
	
	@Override
	public SessionData createSession( String sessionId, SessionWrapper wrapper ) throws SessionException
	{
		return new SqlSessionData( sessionId, wrapper );
	}
	
	class SqlSessionData extends SessionData
	{
		SqlSessionData( ResultSet rs ) throws SessionException
		{
			super( SqlDatastore.this, true );
			readSession( rs );
		}
		
		SqlSessionData( String sessionId, SessionWrapper wrapper ) throws SessionException
		{
			super( SqlDatastore.this, false );
			this.sessionId = sessionId;
			
			ipAddr = wrapper.getIpAddr();
			site = wrapper.getSite().getSiteId();
			
			save();
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
		
		private void readSession( ResultSet rs ) throws SessionException
		{
			try
			{
				timeout = rs.getInt( "timeout" );
				ipAddr = rs.getString( "ipAddr" );
				
				if ( rs.getString( "sessionName" ) != null && !rs.getString( "sessionName" ).isEmpty() )
					sessionName = rs.getString( "sessionName" );
				sessionId = rs.getString( "sessionId" );
				
				site = rs.getString( "sessionSite" );
				
				if ( !rs.getString( "data" ).isEmpty() )
				{
					data = new Gson().fromJson( rs.getString( "data" ), new TypeToken<Map<String, String>>()
					{
						private static final long serialVersionUID = -1734352198651744570L;
					}.getType() );
				}
			}
			catch ( SQLException e )
			{
				throw new SessionException( e );
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
					sql.queryUpdate( "INSERT INTO `sessions` (`sessionId`, `timeout`, `ipAddr`, `sessionName`, `sessionSite`, `data`) VALUES ('" + sessionId + "', '" + timeout + "', '" + ipAddr + "', '" + sessionName + "', '" + site + "', '" + dataJson + "');" );
				else
					sql.queryUpdate( "UPDATE `sessions` SET `data` = '" + dataJson + "', `timeout` = '" + timeout + "', `sessionName` = '" + sessionName + "', `ipAddr` = '" + ipAddr + "', `sessionSite` = '" + site + "' WHERE `sessionId` = '" + sessionId + "';" );
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
				if ( Loader.getDatabase().queryUpdate( "DELETE FROM `sessions` WHERE `sessionId` = '" + sessionId + "';" ) < 1 )
					Loader.getLogger().severe( "We could not remove the session '" + sessionId + "' from the database." );
			}
			catch ( SQLException e )
			{
				throw new SessionException( "There was an exception thrown while trying to destroy the session.", e );
			}
		}
	}
}
