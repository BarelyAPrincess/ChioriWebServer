/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.session;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.LogColor;
import com.chiorichan.account.AccountInstance;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.Kickable;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.server.MessageEvent;
import com.chiorichan.http.CSRFToken;
import com.chiorichan.http.HttpCookie;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.StringFunc;
import com.chiorichan.util.WeakReferenceList;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class is used to carry data that is to be persistent from request to request.
 * If you need to sync data across requests then we recommend using Session Vars for Security.
 */
public final class Session extends AccountPermissible implements Kickable
{
	boolean newSession = false;
	
	// Indicates if the Session has been unloaded or destroyed!
	boolean isInvalidated = false;
	
	/**
	 * The underlying data for this session<br>
	 * Preserves access to the datastore and it's methods {@link SessionData#save()}, {@link SessionData#reload()}, {@link SessionData#destroy()}
	 */
	final SessionData data;
	
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
	
	private CSRFToken token = null;
	
	Session( SessionData data ) throws SessionException
	{
		Validate.notNull( data );
		this.data = data;
		
		sessionId = data.sessionId;
		
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
			SessionManager.getLogger().info( LogColor.DARK_AQUA + "Session " + ( data.stale ? "Loaded" : "Created" ) + " `" + this + "`" );
		
		initialized();
	}
	
	public AccountInstance account()
	{
		return account;
	}
	
	public boolean changesMade()
	{
		return !isInvalidated && dataChangeHistory.size() > 0;
	}
	
	public void destroy() throws SessionException
	{
		if ( SessionManager.isDebug() )
			Loader.getLogger().info( LogColor.DARK_AQUA + "Session Destroyed `" + this + "`" );
		
		SessionManager.sessions.remove( this );
		
		for ( SessionWrapper wrap : wrappers )
		{
			wrap.finish();
			unregisterAttachment( wrap );
		}
		wrappers.clear();
		
		timeout = Timings.epoch();
		data.timeout = Timings.epoch();
		
		if ( sessionCookie != null )
			sessionCookie.setMaxAge( 0 );
		
		data.destroy();
		isInvalidated = true;
	}
	
	@Override
	protected void failedLogin( AccountResult result )
	{
		// Do Nothing
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
	
	public Map<String, HttpCookie> getCookies()
	{
		return Collections.unmodifiableMap( sessionCookies );
	}
	
	public CSRFToken getCSRFToken()
	{
		if ( token == null )
			regenCSRFToken();
		return token;
	}
	
	public Map<String, String> getDataMap()
	{
		return data.data;
	}
	
	@Override
	public String getDisplayName()
	{
		return account.getDisplayName();
	}
	
	@Override
	public PermissibleEntity getEntity()
	{
		return account.getEntity();
	}
	
	public Object getGlobal( String key )
	{
		return globals.get( key );
	}
	
	public Map<String, Object> getGlobals()
	{
		return Collections.unmodifiableMap( globals );
	}
	
	@Override
	public String getId()
	{
		return account == null ? null : account.getId();
	}
	
	@Override
	public Collection<String> getIpAddresses()
	{
		Set<String> ips = Sets.newHashSet();
		for ( SessionWrapper sp : wrappers )
			if ( sp.hasSession() && sp.getSession() == this )
			{
				String ipAddr = sp.getIpAddr();
				if ( ipAddr != null && !ipAddr.isEmpty() && !ips.contains( ipAddr ) )
					ips.add( ipAddr );
			}
		return ips;
	}
	
	public String getName()
	{
		return sessionKey;
	}
	
	public String getSessId()
	{
		return sessionId;
	}
	
	public HttpCookie getSessionCookie()
	{
		return sessionCookie;
	}
	
	/**
	 * @return A set of active SessionProviders for this session.
	 */
	public Set<SessionWrapper> getSessionWrappers()
	{
		return wrappers.toSet();
	}
	
	@Override
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
	
	public long getTimeout()
	{
		return timeout;
	}
	
	@Override
	public String getVariable( String key )
	{
		return getVariable( key, null );
	}
	
	@Override
	public String getVariable( String key, String def )
	{
		if ( !data.data.containsKey( key ) || data.data.get( key ) == null )
		{
			if ( SessionManager.isDebug )
				SessionManager.getLogger().info( String.format( "%sGetting variable key `%s` which resulted in default value '%s'", LogColor.GRAY, key, def ) );
			
			return def;
		}
		
		if ( SessionManager.isDebug )
			SessionManager.getLogger().info( String.format( "%sGetting variable key `%s` with value '%s' and default value '%s'", LogColor.GRAY, key, data.data.get( key ), def ) );
		
		return data.data.get( key );
	}
	
	@Override
	public AccountInstance instance()
	{
		return account;
	}
	
	public boolean isInvalidated()
	{
		return isInvalidated;
	}
	
	public boolean isNew()
	{
		return newSession;
	}
	
	public boolean isSet( String key )
	{
		return data.data.containsKey( key );
	}
	
	@Override
	public AccountResult kick( String reason )
	{
		if ( isInvalidated )
			throw new IllegalStateException( "This session has been invalidated" );
		
		return logout();
	}
	
	@Override
	public AccountMeta meta()
	{
		return account.meta();
	}
	
	/**
	 * Removes the session expiration and prevents the Session Manager from unloading or destroying sessions
	 */
	public void noTimeout()
	{
		if ( isInvalidated )
			throw new IllegalStateException( "This session has been invalidated" );
		
		timeout = 0;
		data.timeout = 0;
	}
	
	@EventHandler( priority = EventPriority.NORMAL )
	public void onAccountMessageEvent( MessageEvent event )
	{
		
	}
	
	public void processSessionCookie()
	{
		if ( isInvalidated )
			throw new IllegalStateException( "This session has been invalidated" );
		
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
	
	// TODO Sessions can outlive a login.
	// TODO Sessions can have an expiration in 7 days and a login can have an expiration of 24 hours.
	// TODO Remember should probably make it so logins last as long as the session does. Hmmmmmm
	
	void putSessionCookie( String key, HttpCookie cookie )
	{
		if ( isInvalidated )
			throw new IllegalStateException( "This session has been invalidated" );
		
		sessionCookies.put( key, cookie );
	}
	
	public void rearmTimeout()
	{
		if ( isInvalidated )
			throw new IllegalStateException( "This session has been invalidated" );
		
		int defaultTimeout = SessionManager.getDefaultTimeout();
		
		// Grant the timeout an additional 10 minutes per request, capped at one hour or 6 requests.
		requestCnt++;
		
		// Grant the timeout an additional 2 hours for having a user logged in.
		if ( isLoginPresent() )
		{
			defaultTimeout = SessionManager.getDefaultTimeoutWithLogin();
			
			if ( StringFunc.isTrue( getVariable( "remember", "false" ) ) )
				defaultTimeout = SessionManager.getDefaultTimeoutWithRememberMe();
			
			if ( Loader.getConfig().getBoolean( "allowNoTimeoutPermission" ) && checkPermission( "com.chiorichan.noTimeout" ).isTrue() )
				defaultTimeout = Integer.MAX_VALUE;
		}
		
		timeout = Timings.epoch() + defaultTimeout + ( Math.min( requestCnt, 6 ) * 600 );
		
		data.timeout = timeout;
		
		if ( sessionCookie != null )
			sessionCookie.setExpiration( timeout );
	}
	
	public void regenCSRFToken()
	{
		token = new CSRFToken( this );
	}
	
	/**
	 * Registers a newly created wrapper with our session
	 * 
	 * @param wrapper
	 *            The newly created wrapper
	 */
	public void registerWrapper( SessionWrapper wrapper )
	{
		if ( isInvalidated )
			throw new IllegalStateException( "This session has been invalidated" );
		
		assert wrapper.getSession() == this : "SessionWrapper does not contain proper reference to this Session";
		
		registerAttachment( wrapper );
		wrappers.add( wrapper );
		knownIps.add( wrapper.getIpAddr() );
	}
	
	public void reload() throws SessionException
	{
		if ( isInvalidated )
			throw new IllegalStateException( "This session has been invalidated" );
		
		data.reload();
	}
	
	/**
	 * Sets if the user login should be remembered for a longer amount of time
	 * 
	 * @param remember
	 *            Should we?
	 */
	public void remember( boolean remember )
	{
		if ( isInvalidated )
			throw new IllegalStateException( "This session has been invalidated" );
		
		setVariable( "remember", remember ? "true" : "false" );
		rearmTimeout();
	}
	
	public void removeWrapper( SessionWrapper wrapper )
	{
		if ( isInvalidated )
			throw new IllegalStateException( "This session has been invalidated" );
		
		wrappers.remove( wrapper );
		unregisterAttachment( wrapper );
	}
	
	public void save() throws SessionException
	{
		save( false );
	}
	
	public void save( boolean force ) throws SessionException
	{
		if ( isInvalidated )
			throw new IllegalStateException( "This session has been invalidated" );
		
		if ( force || changesMade() )
		{
			data.sessionName = sessionKey;
			data.sessionId = sessionId;
			
			data.ipAddr = Joiner.on( "|" ).join( knownIps );
			
			data.save();
			dataChangeHistory.clear();
		}
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
	
	public void setGlobal( String key, Object val )
	{
		if ( isInvalidated )
			throw new IllegalStateException( "This session has been invalidated" );
		
		globals.put( key, val );
	}
	
	public void setSite( Site site )
	{
		if ( isInvalidated )
			throw new IllegalStateException( "This session has been invalidated" );
		
		Validate.notNull( site );
		this.site = site;
		data.site = site.getSiteId();
	}
	
	@Override
	public void setVariable( String key, String value )
	{
		if ( isInvalidated )
			throw new IllegalStateException( "This session has been invalidated" );
		
		SessionManager.getLogger().info( String.format( "Setting session variable `%s` with value '%s'", key, value ) );
		
		if ( value == null )
			data.data.remove( key );
		
		data.data.put( key, value );
		dataChangeHistory.add( key );
	}
	
	@Override
	public void successfulLogin() throws AccountException
	{
		if ( isInvalidated )
			throw new IllegalStateException( "This session has been invalidated" );
		
		for ( SessionWrapper wrapper : wrappers )
			registerAttachment( wrapper );
		
		try
		{
			account().meta().context().credentials().makeResumable( this );
		}
		catch ( AccountException e )
		{
			SessionManager.getLogger().severe( "We had a problem making the current login resumable!", e );
		}
		
		rearmTimeout();
		saveWithoutException();
	}
	
	@Override
	public String toString()
	{
		return "Session{key=" + sessionKey + ",id=" + sessionId + ",ipAddr=" + getIpAddresses() + ",timeout=" + timeout + ",isInvalidated=" + isInvalidated + ",data=" + data + ",requestCount=" + requestCnt + ",site=" + site + "}";
	}
	
	public void upload()
	{
		if ( SessionManager.isDebug() )
			Loader.getLogger().info( LogColor.DARK_AQUA + "Session Unloaded `" + this + "`" );
		
		SessionManager.sessions.remove( this );
		
		for ( SessionWrapper wrap : wrappers )
		{
			wrap.finish();
			unregisterAttachment( wrap );
		}
		wrappers.clear();
		
		isInvalidated = true;
	}
}
