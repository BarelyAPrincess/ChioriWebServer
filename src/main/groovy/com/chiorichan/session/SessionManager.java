/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 * <p>
 * All Rights Reserved.
 */
package com.chiorichan.session;

import com.chiorichan.AppConfig;
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
import com.chiorichan.utils.UtilEncryption;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.text.WordUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
		return "_ws" + WordUtils.capitalize( AppConfig.get().getString( "sessions.defaultCookieName", "sessionId" ) );
	}

	/**
	 * Gets the Default Session Timeout in seconds.
	 *
	 * @return Session timeout in seconds
	 */
	public static int getDefaultTimeout()
	{
		return AppConfig.get().getInt( "sessions.defaultTimeout", 3600 );
	}

	/**
	 * Gets the Default Timeout in seconds with additional time added for a login being present
	 *
	 * @return Session timeout in seconds
	 */
	public static int getDefaultTimeoutWithLogin()
	{
		return AppConfig.get().getInt( "sessions.defaultTimeoutWithLogin", 86400 );
	}

	/**
	 * Gets the Default Timeout in second with additional time added for a login being present and the user checking the "Remember Me" checkbox
	 *
	 * @return Session timeout in seconds
	 */
	public static int getDefaultTimeoutWithRememberMe()
	{
		return AppConfig.get().getInt( "sessions.defaultTimeoutRememberMe", 604800 );
	}

	/**
	 * Get the {@link com.chiorichan.logger.LogAPI} instance for this SessionManager
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
	 * @return True if we are
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
	 * @param wrapper The {@link SessionWrapper} to reference
	 * @return The hot out of the oven Session
	 * @throws SessionException If there was a problem - seriously!
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
		return "SessionManager";
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
	 * @param ipAddress The Ip Address to check for
	 * @return A List of Sessions that matched
	 */
	public List<Session> getSessionsByIp( String ipAddress )
	{
		return sessions.stream().filter( s -> s.getIpAddresses() != null && s.getIpAddresses().contains( ipAddress ) ).collect( Collectors.toList() );
	}

	/**
	 * Initializes the Session Manager
	 *
	 * @throws StartupException If there was any problems
	 */
	@Override
	public void init() throws StartupException
	{
		try
		{
			isDebug = AppConfig.get().getBoolean( "sessions.debug" );

			String datastoreType = AppConfig.get().getString( "sessions.datastore", "file" );

			if ( "db".equalsIgnoreCase( datastoreType ) || "database".equalsIgnoreCase( datastoreType ) || "sql".equalsIgnoreCase( datastoreType ) )
				if ( AppConfig.get().getDatabase() == null )
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
			throw new StartupException( "There was a problem initializing the Session Manager", t );
		}

		/*
		 * This schedules the Session Manager with the Scheduler to run every 5 minutes (by default) to cleanup sessions.
		 */
		TaskManager.instance().scheduleAsyncRepeatingTask( this, 0L, Ticks.MINUTE * AppConfig.get().getInt( "sessions.cleanupInterval", 5 ), new Runnable()
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
	 * @throws SessionException If there was problems
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

		for ( Session session : sessions )
			if ( session.getTimeout() > 0 && session.getTimeout() < Timings.epoch() )
				try
				{
					cleanupCount++;
					session.destroy( SessionManager.EXPIRED );
				}
				catch ( SessionException e )
				{
					getLogger().severe( "SessionException: " + e.getMessage() );
				}
			else
				knownIps.addAll( session.getIpAddresses() );

		int maxPerIp = AppConfig.get().getInt( "sessions.maxSessionsPerIP", 6 );

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
		return UtilEncryption.md5( UtilEncryption.randomize( UtilEncryption.random(), "$e$$i0n_R%ND0Mne$$" ) + System.currentTimeMillis() );
	}

	/**
	 * Finalizes the Session Manager for Shutdown
	 */
	public void shutdown()
	{
		synchronized ( sessions )
		{
			for ( Session session : sessions )
				try
				{
					session.save();
					session.unload();
				}
				catch ( SessionException e )
				{
					// Ignore
				}

			sessions.clear();
		}
	}

	public Session startSession( SessionWrapper wrapper ) throws SessionException
	{
		HttpCookie cookie = wrapper.getServerCookie( wrapper.getLocation().getSessionKey(), getDefaultSessionName() );
		Session session = null;

		if ( cookie != null )
			session = sessions.stream().filter( s -> s != null && cookie.getValue().equals( s.getSessionId() ) ).findFirst().orElse( null );

		if ( session == null )
			session = createSession( wrapper );

		session.registerWrapper( wrapper );

		// getLogger().debug( "Debug: IpAddress " + wrapper.getIpAddress() + " | Loaded? " + session.data.stale + " | Expires " + ( session.getTimeout() - CommonFunc.getEpoch() ) );

		return session;
	}

	// int defaultLife = ( getSite().getYaml() != null ) ? getSite().getYaml().getInt( "sessions.lifetimeDefault", 604800 ) : 604800;
	// timeout = CommonFunc.getEpoch() + AppConfig.get().getInt( "sessions.defaultTimeout", 3600 );
}
