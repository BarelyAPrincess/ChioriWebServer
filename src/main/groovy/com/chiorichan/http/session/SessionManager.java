/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http.session;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.StartupException;
import com.chiorichan.account.Account;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.util.Common;
import com.google.common.collect.Lists;

/**
 * Persistence manager handles sessions kept in memory. It also manages when to unload the session to free memory.
 * 
 * @author Chiori Greene
 * @copyright Greenetree LLC 2014
 */
public class SessionManager
{
	static private List<Session> sessionList = Lists.newCopyOnWriteArrayList();
	
	public void init() throws StartupException
	{
		switch ( Loader.getConfig().getString( "server.database.type", "file" ) )
		{
			case "db":
				if ( Loader.getDatabase() == null )
					throw new StartupException( "Session Manager is configured to use the Framework Database but the server's database is unconfigured, which is required for this configuration." );
				
				sessionList = SqlSession.getActiveSessions();
				break;
			default:
				sessionList = FileSession.getActiveSessions();
		}
	}
	
	public SessionProvider find( HttpRequestWrapper request )
	{
		SessionProvider sess = null;
		
		synchronized( sessionList )
		{
			for ( Session s : sessionList )
			{
				if ( s.matchClient( request ) )
				{
					sess = s.getSessionProvider( request );
					break;
				}
			}
			
			if ( sess == null && Loader.getConfig().getBoolean( "sessions.reuseVacantSessions", true ) )
			{
				for ( Session s : sessionList )
				{
					if ( s.getIpAddr() != null && s.getIpAddr().equals( request.getRemoteAddr() ) && !s.getUserState() )
					{
						sess = s.getSessionProvider( request );
						break;
					}
				}
			}
			
			if ( sess == null )
			{
				sess = new SessionProviderWeb( request );
				sessionList.add( sess.getParentSession() );
			}
		}
		
		return sess;
	}
	
	public static void mainThreadHeartbeat( long tick )
	{
		Iterator<Session> sessions = sessionList.iterator();
		
		while( sessions.hasNext() )
		{
			Session var1 = sessions.next();
			
			if ( var1.getTimeout() > 0 && var1.getTimeout() < Common.getEpoch() )
			{
				destroySession( var1 );
			}
		}
	}
	
	public List<Session> getSessions()
	{
		return sessionList;
	}
	
	public void shutdown()
	{
		Iterator<Session> sess = sessionList.iterator();
		
		while( sess.hasNext() )
		{
			Session it = sess.next();
			it.saveSession( true );
		}
		
		sessionList.clear();
	}
	
	public List<Session> getSessionsByIp( String ipAddr )
	{
		List<Session> lst = Lists.newArrayList();
		
		for ( Session sess : sessionList )
		{
			if ( sess.getIpAddr() != null && sess.getIpAddr().equals( ipAddr ) )
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
	public static void destroySession( Session sess )
	{
		Loader.getLogger().info( ChatColor.DARK_AQUA + "Session Destroyed `" + sess + "`" );
		
		for ( Account<?> u : Loader.getAccountManager().getOnlineAccounts() )
			u.removeHandler( sess );
		
		sess.destroySession();
		
		sessionList.remove( sess );
	}
	
	public static Session createSession()
	{
		Session newSession = null;
		
		switch ( Loader.getConfig().getString( "server.database.type", "file" ) )
		{
			case "db":
				newSession = new SqlSession();
				break;
			default:
				newSession = new FileSession();
		}
		
		return newSession;
	}
	
	public void reload()
	{
		// RELOAD ALL
	}
}
