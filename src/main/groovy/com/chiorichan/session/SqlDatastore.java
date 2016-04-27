/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.session;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.chiorichan.AppController;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.datastore.sql.query.SQLQuerySelect;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.tasks.Timings;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

public class SqlDatastore extends SessionDatastore
{
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
			site = wrapper.getLocation().getId();

			save();
		}

		@Override
		void destroy() throws SessionException
		{
			try
			{
				if ( AppController.config().getDatabase().table( "sessions" ).delete().where( "sessionId" ).matches( sessionId ).execute().rowCount() < 1 )
					SessionManager.getLogger().severe( "Failed to remove the session '" + sessionId + "' from the database, no results." );
			}
			catch ( SQLException e )
			{
				throw new SessionException( "There was an exception thrown while trying to destroy the session.", e );
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
					data = new Gson().fromJson( rs.getString( "data" ), new TypeToken<Map<String, String>>()
					{
						private static final long serialVersionUID = -1734352198651744570L;
					}.getType() );
			}
			catch ( SQLException e )
			{
				throw new SessionException( e );
			}
		}

		@Override
		void reload() throws SessionException
		{
			try
			{
				SQLQuerySelect select = AppController.config().getDatabase().table( "sessions" ).select().where( "sessionId" ).matches( sessionId ).execute();
				// rs = Loader.getDatabase().query( "SELECT * FROM `sessions` WHERE `sessionId` = '" + sessionId + "'" );
				if ( select.rowCount() < 1 )
					return;
				readSession( select.result() );
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
				SQLDatastore sql = AppController.config().getDatabase();

				if ( sql == null )
					throw new SessionException( "Sessions can't be stored in a SQL Database without a properly configured server database." );

				SQLQuerySelect select = sql.table( "sessions" ).select().where( "sessionId" ).matches( sessionId ).execute();
				// query( "SELECT * FROM `sessions` WHERE `sessionId` = '" + sessionId + "';" );

				if ( select.rowCount() < 1 )
					sql.table( "sessions" ).insert().value( "sessionId", sessionId ).value( "timeout", timeout ).value( "ipAddr", ipAddr ).value( "sessionName", sessionName ).value( "sessionSite", site ).value( "data", dataJson ).execute();
				// sql.queryUpdate( "INSERT INTO `sessions` (`sessionId`, `timeout`, `ipAddr`, `sessionName`, `sessionSite`, `data`) VALUES ('" + sessionId + "', '" + timeout + "', '" + ipAddr + "', '" + sessionName + "', '" + site + "', '"
				// + dataJson + "');" );
				else
					sql.table( "sessions" ).update().value( "timeout", timeout ).value( "ipAddr", ipAddr ).value( "sessionName", sessionName ).value( "sessionSite", site ).value( "data", dataJson ).where( "sessionId" ).matches( sessionId ).execute();
				// sql.queryUpdate( "UPDATE `sessions` SET `data` = '" + dataJson + "', `timeout` = '" + timeout + "', `sessionName` = '" + sessionName + "', `ipAddr` = '" + ipAddr + "', `sessionSite` = '" + site + "' WHERE `sessionId` = '"
				// + sessionId + "';" );
			}
			catch ( SQLException e )
			{
				throw new SessionException( "There was an exception thrown while trying to save the session.", e );
			}
		}
	}

	@Override
	public SessionData createSession( String sessionId, SessionWrapper wrapper ) throws SessionException
	{
		return new SqlSessionData( sessionId, wrapper );
	}

	@Override
	List<SessionData> getSessions() throws SessionException
	{
		List<SessionData> data = Lists.newArrayList();
		SQLDatastore sql = AppController.config().getDatabase();

		if ( sql == null )
			throw new SessionException( "Sessions can't be stored in a SQL Database without a properly configured server database." );

		Timings.start( this );

		try
		{
			// Attempt to delete all expired sessions before we try and load them.
			int expired = sql.table( "sessions" ).delete().where( "timeout" ).moreThan( 0 ).where( "timeout" ).lessThan( Timings.epoch() ).execute().rowCount();
			PermissionManager.getLogger().info( String.format( "SqlSession removed %s expired sessions from the datastore!", expired ) );

			SQLQuerySelect select = sql.table( "sessions" ).select().execute();

			if ( select.rowCount() > 0 )
			{
				ResultSet result = select.result();
				do
					try
					{
						data.add( new SqlSessionData( result ) );
					}
					catch ( SessionException e )
					{
						e.printStackTrace();
					}
				while ( result.next() );
			}
		}
		catch ( SQLException e )
		{
			SessionManager.getLogger().warning( "There was a problem reloading saved sessions.", e );
		}

		PermissionManager.getLogger().info( "SqlSession loaded " + data.size() + " sessions from the datastore in " + Timings.finish( this ) + "ms!" );

		return data;
	}
}
