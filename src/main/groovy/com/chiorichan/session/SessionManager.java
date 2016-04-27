/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.session;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.text.WordUtils;

import com.chiorichan.AppController;
import com.chiorichan.http.HttpCookie;
import com.chiorichan.lang.EnumColor;
import com.chiorichan.lang.StartupException;
import com.chiorichan.logger.Log;
import com.chiorichan.logger.LogSource;
import com.chiorichan.services.AppManager;
import com.chiorichan.services.ServiceManager;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.TaskRegistrar;
import com.chiorichan.tasks.Ticks;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.SecureFunc;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Persistence manager handles sessions kept in memory. It also manages when to unload the session to free memory.
 */
public class SessionManager implements TaskRegistrar, ServiceManager, LogSource
{
	public static final int MANUAL = 0;
	public static final int EXPIRED = 1;
	public static final int MAXPERIP = 2;

	static List<Session> sessions = Lists.newCopyOnWriteArrayList();
	static boolean isDebug = false;

	/**
	 * Gets the Default Session Name
	 *
	 * @return Session Name as string
	 */
	public static String getDefaultSessionName()
	{
		return "_ws" + WordUtils.capitalize( AppController.config().getString( "sessions.defaultCookieName", "sessionId" ) );
	}

	/**
	 * Gets the Default Session Timeout in seconds.
	 *
	 * @return Session timeout in seconds
	 */
	public static int getDefaultTimeout()
	{
		return AppController.config().getInt( "sessions.defaultTimeout", 3600 );
	}

	/**
	 * Gets the Default Timeout in seconds with additional time added for a login being present
	 *
	 * @return Session timeout in seconds
	 */
	public static int getDefaultTimeoutWithLogin()
	{
		return AppController.config().getInt( "sessions.defaultTimeoutWithLogin", 86400 );
	}

	/**
	 * Gets the Default Timeout in second with additional time added for a login being present and the user checking the "Remember Me" checkbox
	 *
	 * @return Session timeout in seconds
	 */
	public static int getDefaultTimeoutWithRememberMe()
	{
		return AppController.config().getInt( "sessions.defaultTimeoutRememberMe", 604800 );
	}

	/**
	 * Get the {@link com.chiorichan.LogAPI} instance for this SessionManager
	 *
	 * @return ConsoleLogger instance
	 */
	public static Log getLogger()
	{
		return AppManager.manager( SessionManager.class ).getLogger();
	}

	public static SessionManager instance()
	{
		return AppManager.manager( SessionManager.class ).instance();
	}

	/**
	 * Is the Session Manager is debug mode, i.e., mean more debug will output to the console
	 *
	 * @return
	 *         True if we are
	 */
	public static boolean isDebug()
	{
		return isDebug;// || Versioning.isDevelopment();
	}

	SessionDatastore datastore = null;

	private boolean isCleanupRunning = false;

	private SessionManager()
	{

	}

	/**
	 * Creates a fresh {@link Session} and saves it's reference.
	 *
	 * @param wrapper
	 *             The {@link SessionWrapper} to reference
	 * @return The hot out of the oven Session
	 * @throws SessionException
	 *              If there was a problem - seriously!
	 */
	public Session createSession( SessionWrapper wrapper ) throws SessionException
	{
		Session session = new Session( datastore.createSession( sessionIdBaker(), wrapper ) );
		session.newSession = true;
		sessions.add( session );
		return session;
	}

	@Override
	public String getLoggerId()
	{
		return "SessMgr";
	}

	@Override
	public String getName()
	{
		return "SessionManager";
	}

	/**
	 * Gets an unmodifiable list of currently loaded {@link Session}s
	 *
	 * @return A unmodifiable list of sessions
	 */
	public List<Session> getSessions()
	{
		return Collections.unmodifiableList( sessions );
	}

	/**
	 * Retrieves a list of {@link Session}s based on the Ip Address provided.
	 *
	 * @param ipAddr
	 *             The Ip Address to check for
	 * @return A List of Sessions that matched
	 */
	public List<Session> getSessionsByIp( String ipAddr )
	{
		List<Session> lst = Lists.newArrayList();

		for ( Session sess : sessions )
			if ( sess != null && sess.getIpAddresses() != null && sess.getIpAddresses().contains( ipAddr ) )
				lst.add( sess );

		return lst;
	}

	/**
	 * Initializes the Session Manager
	 *
	 * @throws StartupException
	 *              If there was any problems
	 */
	@Override
	public void init() throws StartupException
	{
		try
		{
			isDebug = AppController.config().getBoolean( "sessions.debug" );

			String datastoreType = AppController.config().getString( "sessions.datastore", "file" );

			if ( "db".equalsIgnoreCase( datastoreType ) || "database".equalsIgnoreCase( datastoreType ) || "sql".equalsIgnoreCase( datastoreType ) )
				if ( AppController.config().getDatabase() == null )
					getLogger().severe( "Session Manager's datastore is configured to use database but the server's database is unconfigured. Falling back to the file datastore." );
				else
					datastore = new SqlDatastore();

			if ( "file".equalsIgnoreCase( datastoreType ) || datastore == null )
				if ( !FileDatastore.getSessionsDirectory().canWrite() )
					getLogger().severe( "Session Manager's datastore is configured to use the file system but we can't write to the directory `" + FileDatastore.getSessionsDirectory().getAbsolutePath() + "`. Falling back to the memory datastore, i.e., sessions will not be saved." );
				else
					datastore = new FileDatastore();

			if ( datastore == null )
				datastore = new MemoryDatastore();

			for ( SessionData data : datastore.getSessions() )
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
		catch ( Throwable t )
		{
			throw new StartupException( "There was a problem initalizing the Session Manager", t );
		}

		/*
		 * This schedules the Session Manager with the Scheduler to run every 5 minutes (by default) to cleanup sessions.
		 */
		TaskManager.instance().scheduleAsyncRepeatingTask( this, 0L, Ticks.MINUTE * AppController.config().getInt( "sessions.cleanupInterval", 5 ), new Runnable()
		{
			@Override
			public void run()
			{
				sessionCleanup();
			}
		} );
	}

	@Override
	public boolean isEnabled()
	{
		return true;
	}

	/**
	 * Reloads the currently loaded sessions from their Datastore
	 *
	 * @throws SessionException
	 *              If there was problems
	 */
	public void reload() throws SessionException
	{
		synchronized ( sessions )
		{
			// Run session cleanup before saving sessions
			sessionCleanup();

			// XXX Are we sure we want to override existing sessions without saving?
			for ( Session session : sessions )
				session.reload();
		}
	}

	public void sessionCleanup()
	{
		if ( isCleanupRunning )
			return;
		isCleanupRunning = true;

		int cleanupCount = 0;

		Set<String> knownIps = Sets.newHashSet();

		for ( Session sess : sessions )
			if ( sess.getTimeout() > 0 && sess.getTimeout() < Timings.epoch() )
				try
				{
					cleanupCount++;
					sess.destroy( SessionManager.EXPIRED );
				}
				catch ( SessionException e )
				{
					getLogger().severe( "SessionException: " + e.getMessage() );
				}
			else
				knownIps.addAll( sess.getIpAddresses() );

		int maxPerIp = AppController.config().getInt( "sessions.maxSessionsPerIP", 6 );

		for ( String ip : knownIps )
		{
			List<Session> sessions = getSessionsByIp( ip );
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
						cleanupCount++;
						sortedArray[i].destroy( SessionManager.MAXPERIP );
					}
					catch ( SessionException e )
					{
						getLogger().severe( "SessionException: " + e.getMessage() );
					}
			}
		}

		if ( cleanupCount > 0 )
			getLogger().info( EnumColor.DARK_AQUA + "The cleanup task recycled " + cleanupCount + " session(s)." );

		isCleanupRunning = false;
	}

	/**
	 * Generates a random Session Id based on randomness.
	 *
	 * @return Random Session Id as a string
	 */
	public String sessionIdBaker()
	{
		return SecureFunc.md5( SecureFunc.randomize( SecureFunc.random(), "$e$$i0n_R%ND0Mne$$" ) + System.currentTimeMillis() );
	}

	/**
	 * Finalizes the Session Manager for Shutdown
	 */
	public void shutdown()
	{
		synchronized ( sessions )
		{
			for ( Session sess : sessions )
				try
				{
					sess.save();
					sess.unload();
				}
				catch ( SessionException e )
				{

				}

			sessions.clear();
		}
	}

	public Session startSession( SessionWrapper wrapper ) throws SessionException
	{
		String sessionKey = wrapper.getLocation().getSessionKey();
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
		 * if ( AppController.config().getBoolean( "sessions.reuseVacantSessions", true ) )
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

	// int defaultLife = ( getSite().getYaml() != null ) ? getSite().getYaml().getInt( "sessions.lifetimeDefault", 604800 ) : 604800;
	// timeout = CommonFunc.getEpoch() + AppController.config().getInt( "sessions.defaultTimeout", 3600 );
}
