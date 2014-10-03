/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http.session;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.bases.Sentient;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.account.helpers.LoginException;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.framework.Site;
import com.chiorichan.framework.WebUtils;
import com.chiorichan.http.Candy;
import com.chiorichan.http.HttpRequest;
import com.chiorichan.util.Common;
import com.chiorichan.util.StringUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * This class is used to carry data that is to be persistent from request to request.
 * If you need to sync data across requests then we recommend using Session Vars for Security.
 */
public class Session implements SentientHandler
{
	protected Map<String, String> data = new LinkedHashMap<String, String>();
	protected long timeout = 0;
	protected int requestCnt = 0;
	protected String candyId = "", candyName = "candyId", ipAddr = null;
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
	
	/**
	 * Initializes a new session.
	 */
	protected Session( Site _site )
	{
		candyName = Loader.getConfig().getString( "sessions.defaultSessionName", candyName );
		site = _site;
	}
	
	protected Session(ResultSet rs) throws SessionException
	{
		try
		{
			stale = true;
			
			timeout = rs.getInt( "timeout" );
			ipAddr = rs.getString( "ipAddr" );
			
			if ( !rs.getString( "data" ).isEmpty() )
				data = new Gson().fromJson( rs.getString( "data" ), new TypeToken<Map<String, String>>()
				{
					private static final long serialVersionUID = 2808406085740098578L;
				}.getType() );
			
			if ( rs.getString( "sessionName" ) != null && !rs.getString( "sessionName" ).isEmpty() )
				candyName = rs.getString( "sessionName" );
			candyId = rs.getString( "sessionId" );
			
			if ( timeout < Common.getEpoch() )
				throw new SessionException( "This session expired at " + timeout + " epoch!" );
			
			if ( rs.getString( "sessionSite" ) == null || rs.getString( "sessionSite" ).isEmpty() )
				site = Loader.getSiteManager().getFrameworkSite();
			else
				site = Loader.getSiteManager().getSiteById( rs.getString( "sessionSite" ) );
			
			sessionCandy = new Candy( candyName, rs.getString( "sessionId" ) );
			candies.put( candyName, sessionCandy );
			
			loginSessionUser();
			
			Loader.getLogger().info( ChatColor.DARK_AQUA + "Session Restored `" + this + "`" );
		}
		catch ( SQLException e )
		{
			throw new SessionException( e );
		}
	}
	
	protected void loginSessionUser()
	{
		String username = getArgument( "user" );
		String password = getArgument( "pass" );
		
		try
		{
			Account user = Loader.getAccountsManager().attemptLogin( this, username, password );
			currentAccount = user;
			Loader.getLogger().info( ChatColor.GREEN + "Login Restored `Username \"" + username + "\", Password \"" + password + "\", UserId \"" + user.getAccountId() + "\", Display Name \"" + user.getDisplayName() + "\"`" );
		}
		catch ( LoginException l )
		{
			// Loader.getLogger().warning( ChatColor.GREEN + "Login Status: No Valid Login Present" );
		}
	}
	
	protected void initSession()
	{
		DatabaseEngine sql = Loader.getSessionManager().getDatabase();
		
		if ( sessionCandy != null )
		{
			ResultSet rs = null;
			try
			{
				rs = sql.query( "SELECT * FROM `sessions` WHERE `sessionId` = '" + sessionCandy.getValue() + "'" );
			}
			catch ( SQLException e1 )
			{
				e1.printStackTrace();
			}
			
			candyId = sessionCandy.getValue();
			
			if ( rs == null || sql.getRowCount( rs ) < 1 )
				sessionCandy = null;
			else
			{
				try
				{
					timeout = rs.getInt( "timeout" );
					String _ipAddr = rs.getString( "ipAddr" );
					
					if ( !rs.getString( "data" ).isEmpty() )
					{
						Map<String, String> tmpData = new Gson().fromJson( rs.getString( "data" ), new TypeToken<Map<String, String>>()
						{
							private static final long serialVersionUID = -1734352198651744570L;
						}.getType() );
						
						if ( changesMade )
						{
							tmpData.putAll( data );
							data = tmpData;
						}
						else
							data.putAll( tmpData );
					}
					
					// Possible Session Hijacking! nullify!!!
					if ( !_ipAddr.equals( ipAddr ) && !Loader.getConfig().getBoolean( "sessions.allowIPChange" ) )
					{
						sessionCandy = null;
					}
					
					ipAddr = _ipAddr;
					
					List<Session> sessions = Loader.getSessionManager().getSessionsByIp( ipAddr );
					if ( sessions.size() > Loader.getConfig().getInt( "sessions.maxSessionsPerIP" ) )
					{
						long oldestTime = Common.getEpoch();
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
							SessionManager.destroySession( oldest );
					}
				}
				catch ( JsonSyntaxException | SQLException e )
				{
					e.printStackTrace();
					sessionCandy = null;
				}
			}
		}
		
		if ( sessionCandy == null )
		{
			int defaultLife = ( getSite().getYaml() != null ) ? getSite().getYaml().getInt( "sessions.default-life", 604800 ) : 604800;
			
			if ( candyId == null || candyId.isEmpty() )
				candyId = StringUtil.md5( WebUtils.createGUID( "sessionGen" ) + System.currentTimeMillis() );
			
			sessionCandy = new Candy( candyName, candyId );
			
			sessionCandy.setMaxAge( defaultLife );
			
			sessionCandy.setDomain( "." + getSite().getDomain() );
			
			sessionCandy.setPath( "/" );
			
			candies.put( candyName, sessionCandy );
			
			String dataJson = new Gson().toJson( data );
			
			timeout = Common.getEpoch() + Loader.getConfig().getInt( "sessions.defaultTimeout", 3600 );
			
			try
			{
				sql.queryUpdate( "INSERT INTO `sessions` (`sessionId`, `timeout`, `ipAddr`, `sessionName`, `sessionSite`, `data`)VALUES('" + candyId + "', '" + timeout + "', '" + ipAddr + "', '" + candyName + "', '" + getSite().getName() + "', '" + dataJson + "');" );
			}
			catch ( SQLException e )
			{
				e.printStackTrace();
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
			DatabaseEngine sql = Loader.getSessionManager().getDatabase();
			
			String dataJson = new Gson().toJson( data );
			
			if ( sql != null )
				try
				{
					sql.queryUpdate( "UPDATE `sessions` SET `data` = '" + dataJson + "', `timeout` = '" + timeout + "', `sessionName` = '" + candyName + "', `ipAddr` = '" + ipAddr + "', `sessionSite` = '" + getSite().getName() + "' WHERE `sessionId` = '" + candyId + "';" );
				}
				catch ( SQLException e )
				{
					Loader.getLogger().severe( "There was an exception thorwn while trying to save the session.", e );
				}
			else
				Loader.getLogger().severe( "SQL is NULL. Can't save session." );
			
			changesMade = false;
		}
	}
	
	public String toString()
	{
		String extra = "";
		
		if ( site != null )
			extra += ",site=" + site.getName();
		
		return candyName + "{id=" + candyId + ",ipAddr=" + ipAddr + ",timeout=" + timeout + ",data=" + data + ",stale=" + stale + ",requestCount=" + requestCnt + extra + "}";
	}
	
	/**
	 * Determines if this session belongs to the supplied HttpRequest based on the SessionId cookie.
	 * 
	 * @param request
	 * @return boolean
	 */
	protected boolean matchClient( HttpRequest request )
	{
		String _candyName = request.getSite().getYaml().getString( "sessions.cookie-name", Loader.getConfig().getString( "sessions.defaultSessionName", "sessionId" ) );
		Map<String, Candy> requestCandys = SessionUtils.poleCandies( request );
		
		return ( requestCandys.containsKey( _candyName ) && getCandy( candyName ).compareTo( requestCandys.get( _candyName ) ) );
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
	
	@Deprecated
	public void setArgument( String key, String value )
	{
		setVariable( key, value );
	}
	
	@Deprecated
	public String getArgument( String key )
	{
		return getVariable( key );
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
	
	public void destroy() throws SQLException
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
			
			if ( StringUtil.isTrue( getArgument( "remember" ) ) )
				defaultTimeout = Loader.getConfig().getInt( "sessions.defaultTimeoutRememberMe", 604800 );
			
			if ( Loader.getConfig().getBoolean( "allowNoTimeoutPermission" ) && currentAccount.hasPermission( "chiori.noTimeout" ) )
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
		setArgument( "user", null );
		setArgument( "pass", null );
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
	public String getIpAddr()
	{
		return ipAddr;
	}
	
	// A session can't handle just any sentient object.
	// At least for the time being.
	@Override
	public void attachSentient( Sentient sentient )
	{
		if ( sentient instanceof Account )
			currentAccount = (Account) sentient;
		else
			isValid = false;
	}
	
	@Override
	public boolean isValid()
	{
		return isValid;
	}
	
	@Override
	public Sentient getSentient()
	{
		return currentAccount;
	}
	
	@Override
	public void removeSentient()
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

	public SessionProvider getSessionProvider( HttpRequest request )
	{
		return new SessionProviderWeb( this, request );
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
}
