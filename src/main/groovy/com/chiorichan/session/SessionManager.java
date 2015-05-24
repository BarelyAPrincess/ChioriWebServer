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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.text.WordUtils;

import com.chiorichan.ConsoleColor;
import com.chiorichan.ConsoleLogger;
import com.chiorichan.Loader;
import com.chiorichan.ServerManager;
import com.chiorichan.http.HttpCookie;
import com.chiorichan.lang.StartupException;
import com.chiorichan.scheduler.ScheduleManager;
import com.chiorichan.scheduler.TaskCreator;
import com.chiorichan.util.CommonFunc;
import com.chiorichan.util.RandomFunc;
import com.chiorichan.util.StringFunc;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Persistence manager handles sessions kept in memory. It also manages when to unload the session to free memory.
 */
public class SessionManager implements TaskCreator, ServerManager
{
	static List<Session> sessions = Lists.newCopyOnWriteArrayList();
	static boolean isDebug = false;
	SessionDatastore datastore = null;
	
	public void init() throws StartupException
	{
		try
		{
			isDebug = Loader.getConfig().getBoolean( "sessions.debug" );
			
			String datastoreType = Loader.getConfig().getString( "sessions.datastore", "file" );
			
			if ( "db".equalsIgnoreCase( datastoreType ) || "database".equalsIgnoreCase( datastoreType ) || "sql".equalsIgnoreCase( datastoreType ) )
			{
				if ( Loader.getDatabase() == null )
					getLogger().severe( "Session Manager's datastore is configured to use database but the server's database is unconfigured. Falling back to the file datastore." );
				else
					datastore = new SqlDatastore();
			}
			
			if ( "file".equalsIgnoreCase( datastoreType ) || datastore == null )
			{
				if ( !FileDatastore.getSessionsDirectory().canWrite() )
					getLogger().severe( "Session Manager's datastore is configured to use the file system but we can't write to the directory `" + FileDatastore.getSessionsDirectory().getAbsolutePath() + "`. Falling back to the memory datastore, i.e., sessions will not be saved." );
				else
					datastore = new FileDatastore();
			}
			
			if ( datastore == null )
				datastore = new MemoryDatastore();
			
			for ( SessionData data : datastore.getSessions() )
			{
				try
				{
					sessions.add( new Session( data ) );
				}
				catch ( SessionException e )
				{
					// If there is a problem with the session, make warning and destroy
					getLogger().warning( e.getMessage() );
					data.destroy();
				}
				catch ( Throwable t )
				{
					t.printStackTrace();
					data.destroy();
				}
			}
		}
		catch ( Throwable t )
		{
			throw new StartupException( "There was a problem initalizing the Session Manager", t );
		}
		
		/*
		 * This schedules the Session Manager with the Scheduler to run every 5 minutes (by default) to cleanup sessions.
		 */
		Loader.getScheduleManager().scheduleAsyncRepeatingTask( this, new Runnable()
		{
			@Override
			public void run()
			{
				int cleanupCount = 0;
				
				Set<String> knownIps = Sets.newHashSet();
				
				for ( Session sess : sessions )
					if ( sess.getTimeout() > 0 && sess.getTimeout() < CommonFunc.getEpoch() )
						try
						{
							sess.destroy();
							cleanupCount++;
						}
						catch ( SessionException e )
						{
							getLogger().severe( "SessionException: " + e.getMessage() );
						}
					else
						knownIps.addAll( sess.getIpAddresses() );
				
				int maxPerIp = Loader.getConfig().getInt( "sessions.maxSessionsPerIP" );
				
				for ( String ip : knownIps )
				{
					List<Session> sessions = Loader.getSessionManager().getSessionsByIp( ip );
					if ( sessions.size() > maxPerIp )
					{
						Map<Long, Session> sorted = Maps.newTreeMap();
						
						for ( Session s : sessions )
						{
							long key = s.getTimeout();
							while ( sorted.containsKey( key ) )
								key++;
							sorted.put( key, s );
						}
						
						Session[] sortedArray = sorted.values().toArray( new Session[0] );
						
						for ( int i = 0; i < sortedArray.length - maxPerIp; i++ )
							try
							{
								sortedArray[i].destroy();
								cleanupCount++;
							}
							catch ( SessionException e )
							{
								getLogger().severe( "SessionException: " + e.getMessage() );
							}
					}
				}
				
				if ( cleanupCount > 0 && SessionManager.isDebug() )
					getLogger().info( ConsoleColor.DARK_AQUA + "The cleanup cycle destroyed " + cleanupCount + " sessions." );
			}
		}, 0L, ScheduleManager.DELAY_MINUTE * Loader.getConfig().getInt( "sessions.cleanupInterval", 5 ) );
	}
	
	public Session startSession( SessionWrapper wrapper ) throws SessionException
	{
		String sessionKey = wrapper.getSite().getSessionKey();
		Session session = null;
		
		HttpCookie cookie = wrapper.getServerCookie( sessionKey );
		
		if ( cookie == null )
			cookie = wrapper.getServerCookie( getDefaultSessionName() );
		
		if ( cookie != null )
			for ( Session sess : sessions )
				if ( sess != null && cookie.getValue().equals( sess.getSessId() ) )
				{
					session = sess;
					break;
				}
		
		/*
		 * XXX We need to evaluate the security risk behind doing this? Might just need removal.
		 * if ( Loader.getConfig().getBoolean( "sessions.reuseVacantSessions", true ) )
		 * for ( Session s : sessions )
		 * if ( s.getIpAddr() != null && s.getIpAddr().equals( wrapper.getIpAddr() ) && !s.getUserState() )
		 * return s;
		 */
		
		if ( session == null )
			session = createSession( wrapper );
		
		session.registerWrapper( wrapper );
		
		// getLogger().debug( "Debug: IpAddr " + wrapper.getIpAddr() + " | Loaded? " + session.data.stale + " | Expires " + ( session.getTimeout() - CommonFunc.getEpoch() ) );
		
		return session;
	}
	
	public static String getDefaultSessionName()
	{
		return "_ws" + WordUtils.capitalize( Loader.getConfig().getString( "sessions.defaultCookieName", "sessionId" ) );
	}
	
	public List<Session> getSessions()
	{
		return sessions;
	}
	
	public void reload() throws SessionException
	{
		synchronized ( sessions )
		{
			for ( Session session : sessions )
				session.reload();
		}
	}
	
	public void shutdown() throws SessionException
	{
		synchronized ( sessions )
		{
			for ( Session sess : sessions )
				sess.save();
		}
		
		sessions.clear();
	}
	
	public List<Session> getSessionsByIp( String ipAddr )
	{
		List<Session> lst = Lists.newArrayList();
		
		for ( Session sess : sessions )
			if ( sess != null && sess.getIpAddresses() != null && sess.getIpAddresses().contains( ipAddr ) )
				lst.add( sess );
		
		return lst;
	}
	
	public Session createSession( SessionWrapper wrapper ) throws SessionException
	{
		Session session = new Session( datastore.createSession( sessionIdBaker(), wrapper ) );
		sessions.add( session );
		return session;
	}
	
	public String sessionIdBaker()
	{
		// TODO Implement a solid session id generating method
		return StringFunc.md5( RandomFunc.randomize( "$e$$i0n_R%ND0Mne$$" ) + System.currentTimeMillis() );
	}
	
	public static boolean isDebug()
	{
		return isDebug;
	}
	
	public static ConsoleLogger getLogger()
	{
		return Loader.getLogger( "SessMgr" );
	}
	
	@Override
	public boolean isEnabled()
	{
		return true;
	}
	
	@Override
	public String getName()
	{
		return "SessionManager";
	}
	
	public static int getDefaultTimeout()
	{
		return Loader.getConfig().getInt( "sessions.defaultTimeout", 3600 );
	}
	
	public static int getDefaultTimeoutWithLogin()
	{
		return Loader.getConfig().getInt( "sessions.defaultTimeoutWithLogin", 86400 );
	}
	
	public static int getDefaultTimeoutWithRememberMe()
	{
		return Loader.getConfig().getInt( "sessions.defaultTimeoutRememberMe", 604800 );
	}
	
	// int defaultLife = ( getSite().getYaml() != null ) ? getSite().getYaml().getInt( "sessions.lifetimeDefault", 604800 ) : 604800;
	// timeout = CommonFunc.getEpoch() + Loader.getConfig().getInt( "sessions.defaultTimeout", 3600 );
}
