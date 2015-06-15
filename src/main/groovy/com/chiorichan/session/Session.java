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
import com.chiorichan.site.SiteManager;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.StringFunc;
import com.chiorichan.util.WeakReferenceList;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class is used to carry data that is to be persistent from request to request.
 * If you need to sync data across requests then we recommend using Session Vars for Security.
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public final class Session extends AccountPermissible implements Listener
{
	boolean newSession = false;
	
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
	 * History of changes made to the variables since last {@link #save()}
	 */
	private final Set<String> dataChangeHistory = Sets.newHashSet();
	
	/**
	 * Holds a set of known IP Addresses
	 */
	private final Set<String> knownIps = Sets.newHashSet();
	
	/**
	 * Reference to each wrapper that is utilizing this session<br>
	 * We use a WeakReference so they can still be reclaimed by the GC
	 */
	private final WeakReferenceList<SessionWrapper> wrappers = new WeakReferenceList<SessionWrapper>();
	
	/**
	 * The epoch for when this session is to be destroyed
	 */
	private int timeout = 0;
	
	/**
	 * Number of times this session has been requested<br>
	 * More requests mean longer TTL
	 */
	private int requestCnt = 0;
	
	/**
	 * The sessionKey of this session
	 */
	private String sessionKey = SessionManager.getDefaultSessionName();
	
	/**
	 * The sessionId of this session
	 */
	private final String sessionId;
	
	/**
	 * The Session Cookie
	 */
	private HttpCookie sessionCookie;
	
	/**
	 * Tracks session sessionCookies
	 */
	private Map<String, HttpCookie> sessionCookies = Maps.newLinkedHashMap();
	
	/**
	 * The site this session is bound to
	 */
	private Site site = SiteManager.INSTANCE.getDefaultSite();
	
	Session( SessionData data ) throws SessionException
	{
		this.data = data;
		
		this.sessionId = data.sessionId;
		
		sessionKey = data.sessionName;
		timeout = data.timeout;
		knownIps.addAll( Splitter.on( "|" ).splitToList( data.ipAddr ) );
		site = SiteManager.INSTANCE.getSiteById( data.site );
		
		if ( site == null )
		{
			site = SiteManager.INSTANCE.getDefaultSite();
			data.site = site.getSiteId();
		}
		
		timeout = data.timeout;
		
		/*
		 * if ( timeout > 0 && timeout < CommonFunc.getEpoch() )
		 * {
		 * SessionManager.getLogger().warning( "The session '" + sessionId + "' expired at epoch '" + timeout + "', might have expired while offline or this is a bug!" );
		 * data.destroy();
		 * return;
		 * }
		 */
		
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
		
		// XXX New Session, Requested Session, Loaded Session
		
		if ( SessionManager.isDebug() )
			SessionManager.getLogger().info( ConsoleColor.DARK_AQUA + "Session " + ( data.stale ? "Loaded" : "Created" ) + " `" + this + "`" );
		
		initialized();
	}
	
	@Override
	public void successfulLogin()
	{
		account.metadata().context().credentials().makeResumable( this );
		rearmTimeout();
		saveWithoutException();
	}
	
	@Override
	public void failedLogin( AccountResult result )
	{
		
	}
	
	public void processSessionCookie()
	{
		// TODO Session Cookies and Session expire at the same time. - Basically, as long as a Session might become called, we keep the session in existence.
		// TODO Unload a session once it has but been used for a while but might still be called upon at anytime.
		
		/**
		 * Has a Session Cookie been produced yet?
		 * If not we try and create a new one from scratch
		 */
		if ( sessionCookie == null )
		{
			assert sessionId != null && !sessionId.isEmpty();
			
			sessionKey = getSite().getSessionKey();
			sessionCookie = getSite().createSessionCookie( sessionId );
			rearmTimeout();
		}
		
		/**
		 * Check if our current session cookie key does not match the key used by the Site.
		 * If so, we move the old session to the general cookie array and set it as expired.
		 * This usually forces the browser to delete the old session cookie.
		 */
		if ( !sessionCookie.getKey().equals( getSite().getSessionKey() ) )
		{
			String oldKey = sessionCookie.getKey();
			sessionCookie.setKey( getSite().getSessionKey() );
			sessionCookies.put( oldKey, new HttpCookie( oldKey, "" ).setExpiration( 0 ) );
		}
	}
	
	public AccountInstance account()
	{
		return account;
	}
	
	public void setSite( Site site )
	{
		this.site = site;
		data.site = site.getSiteId();
	}
	
	/**
	 * Get the present data change history
	 * 
	 * @return
	 *         A unmodifiable copy of dataChangeHistory.
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
		return "Session{key=" + sessionKey + ",id=" + sessionId + ",ipAddr=" + getIpAddresses() + ",timeout=" + timeout + ",data=" + data + ",requestCount=" + requestCnt + ",site=" + site + "}";
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
		int defaultTimeout = SessionManager.getDefaultTimeout();
		
		// Grant the timeout an additional 10 minutes per request, capped at one hour or 6 requests.
		requestCnt++;
		
		// Grant the timeout an additional 2 hours for having a user logged in.
		if ( isLoginPresent() )
		{
			defaultTimeout = SessionManager.getDefaultTimeoutWithLogin();
			
			if ( StringFunc.isTrue( getVariable( "remember" ) ) )
				defaultTimeout = SessionManager.getDefaultTimeoutWithRememberMe();
			
			if ( Loader.getConfig().getBoolean( "allowNoTimeoutPermission" ) && checkPermission( "com.chiorichan.noTimeout" ).isTrue() )
				defaultTimeout = Integer.MAX_VALUE;
		}
		
		timeout = Timings.epoch() + defaultTimeout + ( Math.min( requestCnt, 6 ) * 600 );
		
		data.timeout = timeout;
		
		if ( sessionCookie != null )
			sessionCookie.setExpiration( timeout );
	}
	
	public long getTimeout()
	{
		return timeout;
	}
	
	/**
	 * Removes the session expiration and prevents the Session Manager from unloading or destroying sessions
	 */
	public void noTimeout()
	{
		timeout = 0;
		data.timeout = 0;
	}
	
	/**
	 * Reports if there is an Account logged in
	 * 
	 * @return True is there is
	 */
	public boolean isLoginPresent()
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
			return SiteManager.INSTANCE.getDefaultSite();
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
		return data.data;
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
	
	public void saveWithoutException()
	{
		try
		{
			save();
		}
		catch ( SessionException e )
		{
			SessionManager.getLogger().severe( "We had a problem saving the current session, changes were not saved to the datastore!", e );
		}
	}
	
	public void save() throws SessionException
	{
		save( false );
	}
	
	public void save( boolean force ) throws SessionException
	{
		if ( force || changesMade() )
		{
			data.sessionName = sessionKey;
			data.sessionId = sessionId;
			
			data.ipAddr = Joiner.on( "|" ).join( knownIps );
			
			data.save();
			dataChangeHistory.clear();
		}
	}
	
	public void destroy() throws SessionException
	{
		if ( SessionManager.isDebug() )
			Loader.getLogger().info( ConsoleColor.DARK_AQUA + "Session Destroyed `" + this + "`" );
		
		SessionManager.sessions.remove( this );
		
		for ( SessionWrapper wrap : wrappers )
			wrap.finish();
		wrappers.clear();
		
		timeout = Timings.epoch();
		data.timeout = Timings.epoch();
		
		if ( sessionCookie != null )
			sessionCookie.setMaxAge( 0 );
		
		data.destroy();
		data = null;
	}
	
	@Override
	public Set<String> getIpAddresses()
	{
		Set<String> ips = Sets.newHashSet();
		for ( SessionWrapper sp : wrappers )
			if ( sp.getSessionWithoutException() != null && sp.getSessionWithoutException() == this )
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
		
		knownIps.add( wrapper.getIpAddr() );
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
	public String getDisplayName()
	{
		return account.getDisplayName();
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
	
	// TODO Sessions can outlive a login.
	// TODO Sessions can have an expiration in 7 days and a login can have an expiration of 24 hours.
	// TODO Remember should probably make it so logins last as long as the session does. Hmmmmmm
	
	/**
	 * Sets if the user login should be remembered for a longer amount of time
	 * 
	 * @param remember
	 *            Should we?
	 */
	public void remember( boolean remember )
	{
		setVariable( "remember", remember ? "true" : "false" );
		rearmTimeout();
	}
	
	public void removeWrapper( SessionWrapper wrapper )
	{
		wrappers.remove( wrapper );
	}
	
	void putSessionCookie( String key, HttpCookie cookie )
	{
		sessionCookies.put( key, cookie );
	}
	
	public boolean isNew()
	{
		return newSession;
	}
}
