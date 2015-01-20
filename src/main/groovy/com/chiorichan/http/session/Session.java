/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http.session;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountHandler;
import com.chiorichan.account.LoginException;
import com.chiorichan.framework.Site;
import com.chiorichan.framework.WebUtils;
import com.chiorichan.http.Candy;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.permission.Permissible;
import com.chiorichan.permission.PermissibleType;
import com.chiorichan.util.Common;
import com.chiorichan.util.StringUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class is used to carry data that is to be persistent from request to request.
 * If you need to sync data across requests then we recommend using Session Vars for Security.
 */
public abstract class Session extends AccountHandler
{
	protected Map<String, String> data = new LinkedHashMap<String, String>();
	protected long timeout = 0;
	protected int requestCnt = 0;
	protected String candyId = "", candyName = "sessionId", ipAddr = null;
	protected Candy sessionCandy;
	protected Account currentAccount = null;
	protected List<String> pendingMessages = Lists.newArrayList();
	
	protected Map<String, Candy> candies = new LinkedHashMap<String, Candy>();
	protected Site site;
	protected boolean stale = false;
	protected boolean isValid = true;
	protected boolean changesMade = false;
	
	protected final Map<String, Object> bindingMap = Maps.newConcurrentMap();
	protected final Set<SessionProvider> sessionProviders = Sets.newHashSet();
	
	protected Session()
	{
		
	}
	
	protected void setSite( Site _site )
	{
		site = _site;
	}
	
	protected void loginSessionUser()
	{
		String username = getVariable( "user" );
		String password = getVariable( "pass" );
		
		try
		{
			Account user = Loader.getAccountsManager().attemptLogin( this, username, password );
			currentAccount = user;
			Loader.getLogger().info( ChatColor.GREEN + "Login Restored `Username \"" + username + "\", Password \"" + password + "\", UserId \"" + user.getAcctId() + "\", Display Name \"" + user.getDisplayName() + "\"`" );
		}
		catch ( LoginException l )
		{
			// Loader.getLogger().warning( ChatColor.GREEN + "Login Status: No Valid Login Present" );
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
		
		if ( stale )
			Loader.getLogger().info( ChatColor.DARK_AQUA + "Session Requested `" + this + "`" );
		else
			Loader.getLogger().info( ChatColor.DARK_AQUA + "Session Created `" + this + "`" );
	}
	
	public void saveSession( boolean force )
	{
		if ( force || changesMade )
		{
			saveSession();
			changesMade = false;
		}
	}
	
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
		String _candyName = request.getSite().getYaml().getString( "sessions.cookie-name", Loader.getConfig().getString( "sessions.defaultSessionName", "sessionId" ) );
		Map<String, Candy> requestCandies = SessionUtils.poleCandies( request );
		
		return ( requestCandies.containsKey( _candyName ) && getCandy( candyName ).compareTo( requestCandies.get( _candyName ) ) );
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
	
	public String getId()
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
		long defaultTimeout = Loader.getConfig().getInt( "sessions.defaultTimeout", 3600 );
		
		// Grant the timeout an additional 10 minutes per request, capped at one hour or 6 requests.
		requestCnt++;
		
		// Grant the timeout an additional 2 hours for having a user logged in.
		if ( getUserState() )
		{
			defaultTimeout = Loader.getConfig().getInt( "sessions.defaultTimeoutWithLogin", 86400 );
			
			if ( StringUtil.isTrue( getVariable( "remember" ) ) )
				defaultTimeout = Loader.getConfig().getInt( "sessions.defaultTimeoutRememberMe", 604800 );
			
			if ( Loader.getConfig().getBoolean( "allowNoTimeoutPermission" ) )// XXX && currentAccount.hasPermission( "chiori.noTimeout" ) )
				defaultTimeout = 31096821392L;
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
	
	public Account getAccount()
	{
		return currentAccount;
	}
	
	/**
	 * Logout the current logged in user.
	 */
	public void logoutAccount()
	{
		if ( currentAccount != null )
			Loader.getLogger().info( ChatColor.GREEN + "User Logout `" + currentAccount + "`" );
		
		// setArgument( "remember", null );
		setVariable( "user", null );
		setVariable( "pass", null );
		currentAccount = null;
		
		for ( Account u : Loader.getAccountsManager().getOnlineAccounts() )
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
	public void sendMessage( String... messages )
	{
		for ( String m : messages )
			pendingMessages.add( m );
	}
	
	@Override
	public void sendMessage( String msg )
	{
		pendingMessages.add( msg );
	}
	
	// A session can't handle just any permissible object.
	// At least for the time being.
	@Override
	public void attachPermissible( Permissible permissible )
	{
		if ( permissible instanceof Account )
			currentAccount = (Account) permissible;
		else
			isValid = false;
	}
	
	@Override
	public boolean isValid()
	{
		return isValid;
	}
	
	@Override
	public Permissible getPermissible()
	{
		return currentAccount;
	}
	
	@Override
	public void removePermissible()
	{
		currentAccount = null;
	}
	
	@Override
	public String getName()
	{
		if ( currentAccount == null )
			return "(NULL)";
		
		return currentAccount.getName();
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
	 * @param HttpRequestWrapper instance
	 * @return a new SessionProviderWeb
	 */
	public SessionProvider getSessionProvider( HttpRequestWrapper request )
	{
		return new SessionProviderWeb( this, request );
	}
	
	/*
	 * TODO! FOR TCP CONNECTIONS.
	 * public SessionProvider getSessionProvider( NetConnection net )
	 * {
	 * return new SessionProviderNet( this, net );
	 * }
	 */
	
	/**
	 * 
	 * @return A set of active SessionProviders for this session. Sessions are given to the Java TrashCollector when a request finishes.
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
	
	protected static List<Session> getActiveSessions() throws SessionException
	{
		return Lists.newCopyOnWriteArrayList();
	}
	
	public abstract void reloadSession();
	
	public abstract void saveSession();
	
	protected abstract void destroySession();
	
	@Override
	public PermissibleType getType()
	{
		return PermissibleType.lookup( "HTTP" );
	}
	
	@Override
	public Set<PermissibleType> getTypes()
	{
		Set<PermissibleType> types = Sets.newHashSet();
		types.add( getType() );
		return types;
	}
	
	@Override
	public String getIpAddr()
	{
		if ( ipAddr == null && sessionProviders.size() > 0 )
		{
			for ( SessionProvider sp : sessionProviders )
				if ( sp.getRequest() != null )
				{
					String _ipAddr = sp.getRequest().getRemoteAddr();
					if ( _ipAddr != null && !_ipAddr.isEmpty() )
					{
						ipAddr = _ipAddr;
						break;
					}
				}
		}
		
		return ipAddr;
	}
	
	@Override
	public Set<String> getIpAddrs()
	{
		Set<String> ipAddrs = Sets.newHashSet();
		ipAddrs.add( getIpAddr() );
		return ipAddrs;
	}
}
