/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.session;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountHandler;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.LoginException;
import com.chiorichan.http.Candy;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.site.Site;
import com.chiorichan.util.Common;
import com.chiorichan.util.StringUtil;
import com.chiorichan.util.WebUtils;
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
public abstract class Session extends AccountHandler
{
	protected Map<String, String> data = Maps.newLinkedHashMap();
	protected int timeout = 0;
	protected int requestCnt = 0;
	protected String candyId = "", candyName = "sessionId", lastIpAddr = null;
	protected Candy sessionCandy;
	protected List<String> pendingMessages = Lists.newArrayList();
	protected static String lastSession = "";
	protected static long lastTime = Common.getEpoch();
	
	protected Map<String, Candy> candies = Maps.newLinkedHashMap();
	protected Site site;
	protected boolean stale = false;
	protected boolean isValid = true;
	protected boolean changesMade = false;
	
	protected final Map<String, Object> bindingMap = Maps.newLinkedHashMap();
	protected final Set<SessionProvider> sessionProviders = Sets.newHashSet();
	
	protected Session()
	{
		
	}
	
	public void setSite( Site site )
	{
		this.site = site;
	}
	
	protected void loginSessionUser()
	{
		String username = getVariable( "user" );
		String password = getVariable( "pass" );
		
		try
		{
			Account user = Loader.getAccountManager().attemptLogin( this, username, password );
			currentAccount = user;
			
			if ( AccountManager.isDebug() )
				SessionManager.getLogger().info( ConsoleColor.GREEN + "Login Restored `Username \"" + username + "\", Password \"" + password + "\", UserId \"" + user.getAcctId() + "\", Display Name \"" + user.getDisplayName() + "\"`" );
		}
		catch ( LoginException l )
		{
			if ( AccountManager.isDebug() )
				SessionManager.getLogger().info( ConsoleColor.YELLOW + "No Valid Login Present" );
		}
	}
	
	protected void initSession( String parentDomain ) throws SessionException
	{
		if ( sessionCandy != null )
		{
			candyName = sessionCandy.getKey();
			candyId = sessionCandy.getValue();
			
			reloadSession();
		}
		
		if ( sessionCandy == null )
		{
			int defaultLife = ( getSite().getYaml() != null ) ? getSite().getYaml().getInt( "sessions.default-life", 604800 ) : 604800;
			
			if ( candyId == null || candyId.isEmpty() )
				candyId = StringUtil.md5( WebUtils.createGUID( "sessionGen" ) + System.currentTimeMillis() );
			
			sessionCandy = new Candy( candyName, candyId );
			
			sessionCandy.setMaxAge( defaultLife );
			
			if ( parentDomain != null && !parentDomain.isEmpty() )
				sessionCandy.setDomain( "." + parentDomain );
			
			sessionCandy.setPath( "/" );
			
			candies.put( candyName, sessionCandy );
			
			timeout = Common.getEpoch() + Loader.getConfig().getInt( "sessions.defaultTimeout", 3600 );
			
			saveSession( true );
		}
		
		List<Session> sessions = Loader.getSessionManager().getSessionsByIp( getIpAddr() );
		int maxPerIp = Loader.getConfig().getInt( "sessions.maxSessionsPerIP" );
		if ( sessions.size() > maxPerIp )
		{
			Map<Long, Session> sortedSessionMap = Maps.newTreeMap();
			
			for ( Session s : sessions )
				sortedSessionMap.put( s.getTimeout(), s );
			
			if ( sortedSessionMap.size() > maxPerIp )
			{
				int stopIndex = sortedSessionMap.size() - maxPerIp;
				int curIndex = 0;
				
				for ( Entry<Long, Session> e : sortedSessionMap.entrySet() )
				{
					curIndex++;
					
					if ( curIndex > stopIndex )
						break;
					
					SessionManager.destroySession( e.getValue() );
				}
			}
		}
		
		if ( !lastSession.equals( getSessId() ) || Common.getEpoch() - lastTime > 5 )
		{
			lastSession = getSessId();
			lastTime = Common.getEpoch();
			
			if ( SessionManager.isDebug() )
				if ( stale )
					SessionManager.getLogger().info( ConsoleColor.DARK_AQUA + "Session Requested `" + this + "`" );
				else
					SessionManager.getLogger().info( ConsoleColor.DARK_AQUA + "Session Created `" + this + "`" );
		}
	}
	
	public void saveSession( boolean force )
	{
		if ( force || changesMade )
		{
			saveSession();
			changesMade = false;
		}
	}
	
	@Override
	public String toString()
	{
		String extra = "";
		
		if ( site != null )
			extra += ",site=" + site.getName();
		
		return candyName + "{id=" + candyId + ",ipAddr=" + getIpAddr() + ",timeout=" + timeout + ",data=" + data + ",stale=" + stale + ",requestCount=" + requestCnt + extra + "}";
	}
	
	/**
	 * Determines if this session belongs to the supplied HttpRequest based on the SessionId cookie.
	 * 
	 * @param request
	 * @return boolean
	 */
	protected boolean matchClient( HttpRequestWrapper request )
	{
		String candyName = request.getSite().getYaml().getString( "sessions.cookie-name", Loader.getConfig().getString( "sessions.defaultSessionName", "sessionId" ) );
		Map<String, Candy> requestCandies = SessionUtils.poleCandies( request );
		
		return ( requestCandies.containsKey( candyName ) && getCandy( this.candyName ).compareTo( requestCandies.get( candyName ) ) );
	}
	
	/**
	 * Returns a cookie if existent in the session.
	 * 
	 * @param key
	 * @return Candy
	 */
	public Candy getCandy( String key )
	{
		return ( candies.containsKey( key ) ) ? candies.get( key ) : new Candy( key, null );
	}
	
	public Candy getSessionCandy()
	{
		return sessionCandy;
	}
	
	/**
	 * Indicates if this session was previously used in a prior request
	 * 
	 * @return boolean
	 */
	public boolean isStale()
	{
		return stale;
	}
	
	public String getSessId()
	{
		return candyId;
	}
	
	public void setVariable( String key, String value )
	{
		if ( value == null )
			data.remove( key );
		
		data.put( key, value );
		changesMade = true;
	}
	
	public String getVariable( String key )
	{
		if ( !data.containsKey( key ) )
			return "";
		
		if ( data.get( key ) == null )
			return "";
		
		return data.get( key );
	}
	
	public boolean isSet( String key )
	{
		return data.containsKey( key );
	}
	
	public void setCookieExpiry( int valid )
	{
		sessionCandy.setMaxAge( valid );
	}
	
	public void destroy() throws SessionException
	{
		timeout = Common.getEpoch();
		setCookieExpiry( 0 );
		
		SessionManager.destroySession( this );
	}
	
	public void rearmTimeout()
	{
		int defaultTimeout = Loader.getConfig().getInt( "sessions.defaultTimeout", 3600 );
		
		// Grant the timeout an additional 10 minutes per request, capped at one hour or 6 requests.
		requestCnt++;
		
		// Grant the timeout an additional 2 hours for having a user logged in.
		if ( getUserState() )
		{
			defaultTimeout = Loader.getConfig().getInt( "sessions.defaultTimeoutWithLogin", 86400 );
			
			if ( StringUtil.isTrue( getVariable( "remember" ) ) )
				defaultTimeout = Loader.getConfig().getInt( "sessions.defaultTimeoutRememberMe", 604800 );
			
			if ( Loader.getConfig().getBoolean( "allowNoTimeoutPermission" ) && checkPermission( "com.chiorichan.noTimeout" ).isTrue() )
				defaultTimeout = Integer.MAX_VALUE;
		}
		
		timeout = Common.getEpoch() + defaultTimeout + ( Math.min( requestCnt, 6 ) * 600 );
		sessionCandy.setExpiration( timeout );
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
	
	public boolean getUserState()
	{
		return ( currentAccount != null );
	}
	
	/**
	 * Logout the current logged in user.
	 */
	public void logoutAccount()
	{
		if ( currentAccount != null )
			Loader.getLogger().info( ConsoleColor.GREEN + "User Logout `" + currentAccount + "`" );
		
		// setArgument( "remember", null );
		setVariable( "user", null );
		setVariable( "pass", null );
		currentAccount = null;
		
		for ( Account u : Loader.getAccountManager().getOnlineAccounts() )
			u.removeHandler( this );
	}
	
	@Override
	public boolean kick( String kickMessage )
	{
		logoutAccount();
		pendingMessages.add( kickMessage );
		
		return true;
	}
	
	@Override
	public void sendMessage( String... msgs )
	{
		for ( String msg : msgs )
			pendingMessages.add( msg );
		notifyProviders();
	}
	
	public void notifyProviders()
	{
		for ( SessionProvider p : sessionProviders )
			p.onNotify();
	}
	
	public Site getSite()
	{
		if ( site == null )
			return Loader.getSiteManager().getFrameworkSite();
		else
			return site;
	}
	
	/**
	 * Creates a new SessionProvider for the provided HttpRequest instance.
	 * 
	 * @param request
	 *            instance
	 * @return a new SessionProviderWeb
	 */
	public SessionProvider getSessionProvider( HttpRequestWrapper request )
	{
		return new SessionProviderWeb( this, request );
	}
	
	/**
	 * @return A set of active SessionProviders for this session.
	 */
	public Set<SessionProvider> getSessionProviders()
	{
		return sessionProviders;
	}
	
	public Map<String, Candy> getCandies()
	{
		return candies;
	}
	
	public void setGlobal( String key, Object val )
	{
		bindingMap.put( key, val );
	}
	
	public Object getGlobal( String key )
	{
		return bindingMap.get( key );
	}
	
	public Map<String, Object> getGlobals()
	{
		return bindingMap;
	}
	
	public Map<String, String> getDataMap()
	{
		return data;
	}
	
	// TODO Make abstract
	protected static List<Session> getActiveSessions() throws SessionException
	{
		return Lists.newCopyOnWriteArrayList();
	}
	
	public abstract void reloadSession();
	
	public abstract void saveSession();
	
	protected abstract void destroySession();
	
	public Set<String> getIpAddrs()
	{
		Set<String> ips = Sets.newHashSet();
		
		synchronized ( sessionProviders )
		{
			for ( SessionProvider sp : sessionProviders )
				if ( sp.getParentSession() == this && sp.getRequest() != null )
				{
					String ipAddr = sp.getRequest().getRemoteAddr();
					if ( ipAddr != null && !ipAddr.isEmpty() && !ips.contains( ipAddr ) )
					{
						ips.add( ipAddr );
					}
				}
		}
		
		return ips;
	}
	
	@Override
	public String getIpAddr()
	{
		return lastIpAddr;
	}
	
	@Override
	public boolean isRemote()
	{
		return true;
	}
	
	public String getName()
	{
		return candyName;
	}
}
