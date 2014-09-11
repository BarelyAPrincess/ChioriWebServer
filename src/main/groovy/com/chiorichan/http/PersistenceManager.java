/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http;

import java.net.ConnectException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.StartupException;
import com.chiorichan.account.bases.Account;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.util.Common;
import com.google.common.collect.Lists;

/**
 * Persistence manager handles sessions kept in memory. It also manages when to unload the session to free memory.
 * 
 * @author Chiori Greene
 * @copyright Greenetree LLC 2014
 */
public class PersistenceManager
{
	protected DatabaseEngine sql = new DatabaseEngine();
	
	static protected List<PersistentSession> sessionList = Lists.newCopyOnWriteArrayList();
	
	public void init() throws StartupException
	{
		YamlConfiguration config = Loader.getConfig();
		
		try
		{
			Class.forName( "com.mysql.jdbc.Driver" );
		}
		catch ( ClassNotFoundException e )
		{
			throw new StartupException( "We could not locate the 'com.mysql.jdbc.Driver' library regardless that its suppose to be included. If your running from source code be sure to have this library in your build path." );
		}
		
		switch ( config.getString( "server.database.type", "mysql" ) )
		{
			case "sqlite":
				String filename = config.getString( "server.database.dbfile", "chiori.db" );
				
				try
				{
					sql.init( filename );
				}
				catch ( SQLException e )
				{
					if ( e.getCause() instanceof ConnectException )
					{
						throw new StartupException( "We had a problem connecting to database '" + filename + "'. Reason: " + e.getCause().getMessage() );
					}
					else
					{
						throw new StartupException( e );
					}
				}
				
				break;
			case "mysql":
				String host = config.getString( "server.database.host", "localhost" );
				String port = config.getString( "server.database.port", "3306" );
				String database = config.getString( "server.database.database", "chiorifw" );
				String username = config.getString( "server.database.username", "fwuser" );
				String password = config.getString( "server.database.password", "fwpass" );
				
				try
				{
					sql.init( database, username, password, host, port );
				}
				catch ( SQLException e )
				{
					// e.printStackTrace();
					
					if ( e.getCause() instanceof ConnectException )
					{
						throw new StartupException( "We had a problem connecting to database '" + host + "'. Reason: " + e.getCause().getMessage() );
					}
					else
					{
						throw new StartupException( e );
					}
				}
				
				break;
			default:
				Loader.getLogger().panic( "The Framework Database can not support anything other then mySql or sqLite at the moment. Please change 'framework-database.type' to 'mysql' or 'sqLite' in 'chiori.yml'" );
		}
	}
	
	public void loadSessions()
	{
		synchronized ( sessionList )
		{
			sessionList.clear();
			
			try
			{
				ResultSet rs = sql.query( "SELECT * FROM `sessions`;" );
				
				if ( sql.getRowCount( rs ) > 0 )
					do
					{
						try
						{
							sessionList.add( new PersistentSession( rs ) );
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
		}
	}
	
	protected PersistentSession find( HttpRequest request )
	{
		PersistentSession sess = null;
		
		synchronized ( sessionList )
		{
			for ( PersistentSession s : sessionList )
			{
				if ( s.matchClient( request ) )
				{
					sess = s;
					sess.setRequest( request, true );
					break;
				}
			}
			
			if ( sess == null && Loader.getConfig().getBoolean( "sessions.reuseVacantSessions", true ) )
			{
				for ( PersistentSession s : sessionList )
				{
					if ( s.ipAddr.equals( request.getRemoteAddr() ) && !s.getUserState() )
					{
						sess = s;
						sess.setRequest( request, true );
						break;
					}
				}
			}
			
			if ( sess == null )
			{
				sess = new PersistentSession( request );
				sessionList.add( sess );
			}
		}
		
		return sess;
	}
	
	public static void mainThreadHeartbeat( long tick )
	{
		Iterator<PersistentSession> sessions = sessionList.iterator();
		
		while ( sessions.hasNext() )
		{
			PersistentSession var1 = sessions.next();
			
			if ( var1.getTimeout() > 0 && var1.getTimeout() < Common.getEpoch() )
			{
				try
				{
					destroySession( var1 );
				}
				catch ( SQLException e )
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public DatabaseEngine getDatabase()
	{
		return sql;
	}
	
	public List<PersistentSession> getSessions()
	{
		return sessionList;
	}
	
	public void shutdown()
	{
		Iterator<PersistentSession> sess = sessionList.iterator();
		
		while ( sess.hasNext() )
		{
			PersistentSession it = sess.next();
			it.saveSession( true );
		}
		
		sessionList.clear();
	}
	
	public List<PersistentSession> getSessionsByIp( String ipAddr )
	{
		List<PersistentSession> lst = Lists.newArrayList();
		
		for ( PersistentSession sess : sessionList )
		{
			if ( sess.ipAddr.equals( ipAddr ) )
				lst.add( sess );
		}
		
		return lst;
	}
	
	/**
	 * Remove said session from the server and sql database.
	 * 
	 * @param var1
	 * @throws SQLException
	 */
	public static void destroySession( PersistentSession var1 ) throws SQLException
	{
		Loader.getLogger().info( ChatColor.DARK_AQUA + "Session Destroyed `" + var1 + "`" );
		
		for ( Account u : Loader.getAccountsManager().getOnlineAccounts() )
			u.removeHandler( var1 );
		
		Loader.getPersistenceManager().sql.queryUpdate( "DELETE FROM `sessions` WHERE `sessionName` = '" + var1.candyName + "' AND `sessionId` = '" + var1.getId() + "';" );
		sessionList.remove( var1 );
	}
	
	public void reload()
	{
		// RELOAD ALL
	}
}
