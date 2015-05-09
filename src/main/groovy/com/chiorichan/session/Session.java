/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.session;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountInstance;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.event.AccountMessageEvent;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.http.HttpCookie;
import com.chiorichan.site.Site;
import com.chiorichan.util.CommonFunc;
import com.chiorichan.util.RandomFunc;
import com.chiorichan.util.StringFunc;
import com.chiorichan.util.WeakReferenceList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class is used to carry data that is to be persistent from request to request.
 * If you need to sync data across requests then we recommend using Session Vars for Security.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public final class Session extends AccountPermissible implements Listener
{
	/**
	 * The session manager, DUH!
	 */
	SessionManager manager;
	
	/**
	 * The underlying data for this session<br>
	 * Preserves access to the datastore and it's methods {@link SessionData#save()}, {@link SessionData#reload()}, {@link SessionData#destroy()}
	 */
	SessionData data;
	
	/**
	 * Global session variables<br>
	 * Globals will not live outside of the session's life
	 */
	final Map<String, Object> globals = Maps.newLinkedHashMap();
	
	/**
	 * Persistent session variables<br>
	 * Session variables will live outside of the sessions's life
	 */
	final Map<String, String> variables = Maps.newLinkedHashMap();
	
	/**
	 * History of changes made to the variables since last {@link #save()}
	 */
	Set<String> dataChangeHistory = Sets.newHashSet();
	
	
	/**
	 * Reference to each wrapper that is utilizing this session<br>
	 * We use a WeakReference so they can still be reclaimed by the GC
	 */
	final WeakReferenceList<SessionWrapper> wrappers = new WeakReferenceList<SessionWrapper>();
	
	/**
	 * The epoch for when this session is to be destroyed
	 */
	int timeout = 0;
	
	/**
	 * Number of times this session has been requested<br>
	 * More requests mean longer TTL
	 */
	int requestCnt = 0;
	
	/**
	 * The sessionKey of this session
	 */
	String sessionKey = SessionManager.getDefaultCookieName();
	
	/**
	 * The sessionId of this session
	 */
	String sessionId = "";
	
	/**
	 * The Session Cookie
	 */
	HttpCookie sessionCookie;
	
	/**
	 * Limits the number of times a session is logged by tracking the last session
	 * XXX This might be obsolete once new Log Engine is implemented
	 */
	static String lastSession = "";
	
	/**
	 * Limits the number of times a session is logged by tracking the time since last logging
	 * XXX This might be obsolete once new Log Engine is implemented
	 */
	static long lastTime = CommonFunc.getEpoch();
	
	/**
	 * Tracks session sessionCookies
	 */
	Map<String, HttpCookie> sessionCookies = Maps.newLinkedHashMap();
	
	/**
	 * The site this session is bound to
	 */
	Site site;
	
	boolean isValid = true;
	
	Session( SessionManager manager, SessionData data ) throws SessionException
	{
		this.manager = manager;
		// Session Keys?
		this.sessionId = data.sessionId;
		this.data = data;
		this.variables.putAll( data.data );
		
		timeout = data.timeout;
		
		if ( timeout > 0 && timeout < CommonFunc.getEpoch() )
			throw new SessionException( "The session '" + sessionId + "' expired at epoch '" + timeout + "', might have expired while offline or this is a bug!" );
		
		sessionCookie = new HttpCookie( sessionKey, sessionId );
		sessionCookies.put( sessionKey, sessionCookie );
		
		/*
		 * TODO Figure out how to track if a particular wrapper's IP changes
		 * Maybe check the original IP the wrapper was authenticated with
		 * TCP IP: ?
		 * HTTP IP: ?
		 * 
		 * String origIpAddr = lastIpAddr;
		 * 
		 * // Possible Session Hijacking! nullify!!!
		 * if ( lastIpAddr != null && !lastIpAddr.equals( origIpAddr ) && !Loader.getConfig().getBoolean( "sessions.allowIPChange" ) )
		 * {
		 * sessionCookie = null;
		 * lastIpAddr = origIpAddr;
		 * }
		 */
		
		List<Session> sessions = Loader.getSessionManager().getSessionsByIp( data.ipAddr );
		if ( sessions.size() > Loader.getConfig().getInt( "sessions.maxSessionsPerIP" ) )
		{
			long oldestTime = CommonFunc.getEpoch();
			Session oldest = null;
			
			for ( Session s : sessions )
			{
				if ( s != this && s.getTimeout() < oldestTime )
				{
					oldest = s;
					oldestTime = s.getTimeout();
				}
			}
			
			if ( oldest != null )
				oldest.destroy();
		}
		
		if ( !lastSession.equals( getSessId() ) || CommonFunc.getEpoch() - lastTime > 5 )
		{
			lastSession = getSessId();
			lastTime = CommonFunc.getEpoch();
			
			// XXX New Session, Requested Session, Loaded Session
			
			if ( SessionManager.isDebug() )
				SessionManager.getLogger().info( ConsoleColor.DARK_AQUA + "Session Constructed `" + this + "`" );
		}
		
		initialized();
	}
	
	@Override
	public void successfulLogin()
	{
		
	}
	
	@Override
	public void failedLogin( AccountResult result )
	{
		
	}
	
	public void processSessionCookie()
	{
		if ( sessionCookie == null )
		{
			int defaultLife = ( getSite().getYaml() != null ) ? getSite().getYaml().getInt( "sessions.default-life", 604800 ) : 604800;
			timeout = CommonFunc.getEpoch() + Loader.getConfig().getInt( "sessions.defaultTimeout", 3600 );
			
			if ( sessionId == null || sessionId.isEmpty() )
				sessionId = StringFunc.md5( RandomFunc.randomize( "$e$$i0n_R%ND0Mne$$" ) + System.currentTimeMillis() );
			
			sessionCookie = new HttpCookie( sessionKey, sessionId );
			sessionCookie.setMaxAge( defaultLife );
			sessionCookie.setPath( "/" );
			
			sessionCookies.put( sessionKey, sessionCookie );
			
			try
			{
				save( true );
			}
			catch ( SessionException e )
			{
				SessionManager.getLogger().severe( "We had a problem saving the session `" + sessionId + "`", e );
			}
		}
		
		sessionKey = sessionCookie.getKey();
		sessionId = sessionCookie.getValue();
	}
	
	public SessionManager manager()
	{
		return manager;
	}
	
	public AccountInstance account()
	{
		return account;
	}
	
	public void setSite( Site site )
	{
		this.site = site;
	}
	
	/**
	 * Get the present data change history
	 * 
	 * @return
	 *         A clone of the dataChangeHistory set for comparison later
	 */
	Set<String> getChangeHistory()
	{
		return Collections.unmodifiableSet( new HashSet<String>( dataChangeHistory ) );
	}
	
	public boolean changesMade()
	{
		return dataChangeHistory.size() > 0;
	}
	
	@Override
	public String toString()
	{
		String extra = "";
		
		if ( site != null )
			extra += ",site=" + site.getName();
		
		return "Session{key=" + sessionKey + ",id=" + sessionId + ",ipAddr=" + getIpAddresses() + ",timeout=" + timeout + ",data=" + data + ",requestCount=" + requestCnt + extra + "}";
	}
	
	/**
	 * Returns a sessionCookie if existent in the session.
	 * 
	 * @param key
	 * @return Candy
	 */
	public HttpCookie getCookie( String key )
	{
		return ( sessionCookies.containsKey( key ) ) ? sessionCookies.get( key ) : new HttpCookie( key, null );
	}
	
	public HttpCookie getSessionCookie()
	{
		return sessionCookie;
	}
	
	public String getSessId()
	{
		return sessionId;
	}
	
	@Override
	public void setVariable( String key, String value )
	{
		if ( value == null )
			data.data.remove( key );
		
		data.data.put( key, value );
		dataChangeHistory.add( key );
	}
	
	@Override
	public String getVariable( String key )
	{
		if ( !data.data.containsKey( key ) )
			return "";
		
		if ( data.data.get( key ) == null )
			return "";
		
		return data.data.get( key );
	}
	
	public boolean isSet( String key )
	{
		return data.data.containsKey( key );
	}
	
	public void rearmTimeout()
	{
		int defaultTimeout = Loader.getConfig().getInt( "sessions.defaultTimeout", 3600 );
		
		// Grant the timeout an additional 10 minutes per request, capped at one hour or 6 requests.
		requestCnt++;
		
		// Grant the timeout an additional 2 hours for having a user logged in.
		if ( getAccountState() )
		{
			defaultTimeout = Loader.getConfig().getInt( "sessions.defaultTimeoutWithLogin", 86400 );
			
			if ( StringFunc.isTrue( getVariable( "remember" ) ) )
				defaultTimeout = Loader.getConfig().getInt( "sessions.defaultTimeoutRememberMe", 604800 );
			
			if ( Loader.getConfig().getBoolean( "allowNoTimeoutPermission" ) && checkPermission( "com.chiorichan.noTimeout" ).isTrue() )
				defaultTimeout = Integer.MAX_VALUE;
		}
		
		timeout = CommonFunc.getEpoch() + defaultTimeout + ( Math.min( requestCnt, 6 ) * 600 );
		sessionCookie.setExpiration( timeout );
	}
	
	public long getTimeout()
	{
		return timeout;
	}
	
	/**
	 * This method is only to be used to make this session unremovable from memory by the session garbage collector. Be
	 * sure that you rearm the timeout at some point to prevent build ups in memory.
	 */
	public void infiniTimeout()
	{
		timeout = 0;
	}
	
	/**
	 * Reports if the state of the Account login
	 * 
	 * @return Is there an Account logged in?
	 */
	public boolean getAccountState()
	{
		return ( account != null );
	}
	
	@EventHandler( priority = EventPriority.NORMAL )
	public void onAccountMessageEvent( AccountMessageEvent event )
	{
		
	}
	
	public Site getSite()
	{
		if ( site == null )
			return Loader.getSiteManager().getFrameworkSite();
		else
			return site;
	}
	
	@Override
	public String getSiteId()
	{
		return getSite().getName();
	}
	
	/**
	 * @return A set of active SessionProviders for this session.
	 */
	public Set<SessionWrapper> getSessionWrappers()
	{
		return wrappers.toSet();
	}
	
	public Map<String, HttpCookie> getCookies()
	{
		return Collections.unmodifiableMap( sessionCookies );
	}
	
	public void setGlobal( String key, Object val )
	{
		globals.put( key, val );
	}
	
	public Object getGlobal( String key )
	{
		return globals.get( key );
	}
	
	public Map<String, Object> getGlobals()
	{
		return Collections.unmodifiableMap( globals );
	}
	
	public Map<String, String> getDataMap()
	{
		return variables;
	}
	
	// TODO Make abstract
	protected static List<Session> getActiveSessions() throws SessionException
	{
		return Lists.newCopyOnWriteArrayList();
	}
	
	public void reload() throws SessionException
	{
		data.reload();
	}
	
	public void save() throws SessionException
	{
		save( false );
	}
	
	public void save( boolean force ) throws SessionException
	{
		if ( force || changesMade() )
		{
			data.save();
			dataChangeHistory.clear();
		}
	}
	
	public void destroy() throws SessionException
	{
		if ( SessionManager.isDebug() )
			Loader.getLogger().info( ConsoleColor.DARK_AQUA + "Session Destroyed `" + this + "`" );
		
		for ( SessionWrapper wrap : wrappers )
			wrap.finish();
		wrappers.clear();
		
		timeout = CommonFunc.getEpoch();
		sessionCookie.setMaxAge( 0 );
		
		data.destroy();
	}
	
	@Override
	public Set<String> getIpAddresses()
	{
		Set<String> ips = Sets.newHashSet();
		for ( SessionWrapper sp : wrappers )
			if ( sp.getSession() == this )
			{
				String ipAddr = sp.getIpAddr();
				if ( ipAddr != null && !ipAddr.isEmpty() && !ips.contains( ipAddr ) )
				{
					ips.add( ipAddr );
				}
			}
		return ips;
	}
	
	public String getName()
	{
		return sessionKey;
	}
	
	@Override
	public String getEntityId()
	{
		return account == null ? null : account.getAcctId();
	}
	
	/**
	 * Registers a newly created wrapper with our session
	 * 
	 * @param wrapper
	 *            The newly created wrapper
	 */
	public void registerWrapper( SessionWrapper wrapper )
	{
		assert wrapper.getSession() == this : "SessionWrapper does not contain proper reference to this Session";
		
		wrappers.add( wrapper );
	}
	
	@Override
	public AccountMeta metadata()
	{
		return instance().metadata();
	}
	
	@Override
	public AccountInstance instance()
	{
		return account;
	}
	
	@Override
	public String getHumanReadableName()
	{
		return account.getHumanReadableName();
	}
	
	@Override
	public void send( Object obj )
	{
		for ( SessionWrapper sw : wrappers )
			sw.send( obj );
	}
	
	@Override
	public void send( Account sender, Object obj )
	{
		for ( SessionWrapper sw : wrappers )
			sw.send( sender, obj );
	}
}
