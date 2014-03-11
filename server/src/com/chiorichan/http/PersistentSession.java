package com.chiorichan.http;

import groovy.lang.Binding;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.framework.Evaling;
import com.chiorichan.framework.Framework;
import com.chiorichan.framework.Site;
import com.chiorichan.user.LoginException;
import com.chiorichan.user.User;
import com.chiorichan.user.UserHandler;
import com.chiorichan.util.Common;
import com.chiorichan.util.StringUtil;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * This class is used to carry data that is to be persistent from request to request.
 * If you need to sync data across requests then we recommend using Session Vars for Security.
 * 
 * @author Chiori Greene
 * @copyright Greenetree LLC
 */
public class PersistentSession implements UserHandler
{
	protected Map<String, String> data = new LinkedHashMap<String, String>();
	protected int timeout = 0, requestCnt = 0;
	protected String candyId = "", candyName = "candyId", ipAddr = null;
	protected Candy sessionCandy;
	protected Binding binding = new Binding();
	protected Evaling eval;
	protected User currentUser = null;
	protected List<String> pendingMessages = Lists.newArrayList();
	
	protected Map<String, Candy> candies = new LinkedHashMap<String, Candy>();
	protected Framework framework = null;
	protected HttpRequest request;
	protected Site failoverSite;
	protected Boolean stale = false;
	
	/**
	 * Returns an instance of Framework relevant to this session. Instigates a new one if not already done.
	 * 
	 * @return Framework
	 */
	public Framework getFramework()
	{
		if ( framework == null )
			framework = new Framework( this );
		
		binding.setVariable( "chiori", framework );
		
		return framework;
	}
	
	private PersistentSession()
	{
		candyName = Loader.getConfig().getString( "sessions.defaultSessionName", candyName );
	}
	
	/**
	 * Initializes a new session based on the supplied HttpRequest.
	 * 
	 * @param _request
	 */
	protected PersistentSession(HttpRequest _request)
	{
		this();
		setRequest( _request, false );
	}
	
	@SuppressWarnings( "unchecked" )
	protected PersistentSession(ResultSet rs) throws SessionException
	{
		this();
		
		try
		{
			request = null;
			stale = true;
			
			timeout = rs.getInt( "timeout" );
			ipAddr = rs.getString( "ipAddr" );
			data = new Gson().fromJson( rs.getString( "data" ), Map.class );
			
			if ( rs.getString( "sessionName" ) != null && !rs.getString( "sessionName" ).isEmpty() )
				candyName = rs.getString( "sessionName" );
			candyId = rs.getString( "sessionId" );
			
			if ( timeout < Common.getEpoch() )
				throw new SessionException( "This session expired at " + timeout + " epoch!" );
			
			if ( rs.getString( "sessionSite" ) == null || rs.getString( "sessionSite" ).isEmpty() )
				failoverSite = Loader.getPersistenceManager().getSiteManager().getFrameworkSite();
			else
				failoverSite = Loader.getPersistenceManager().getSiteManager().getSiteById( rs.getString( "sessionSite" ) );
			
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
	
	protected void setRequest( HttpRequest _request, Boolean _stale )
	{
		request = _request;
		stale = _stale;
		
		failoverSite = request.getSite();
		ipAddr = request.getRemoteAddr();
		
		Map<String, Candy> pulledCandies = pullCandies( _request );
		pulledCandies.putAll( candies );
		candies = pulledCandies;
		
		if ( request.getSite().getYaml() != null )
		{
			String _candyName = request.getSite().getYaml().getString( "sessions.cookie-name", candyName );
			
			if ( !_candyName.equals( candyName ) )
				if ( candies.containsKey( candyName ) )
				{
					candies.put( _candyName, candies.get( candyName ) );
					candies.remove( candyName );
					candyName = _candyName;
				}
				else
				{
					candyName = _candyName;
				}
		}
		
		sessionCandy = candies.get( candyName );
		
		initSession();
		
		binding.setVariable( "chiori", null );
		binding.setVariable( "request", request );
		binding.setVariable( "response", request.getResponse() );
		binding.setVariable( "__FILE__", new File( "" ) );
	}
	
	protected void loginSessionUser()
	{
		String username = getArgument( "user" );
		String password = getArgument( "pass" );
		
		try
		{
			User user = Loader.getUserManager().attemptLogin( this, username, password );
			currentUser = user;
			Loader.getLogger().info( ChatColor.GREEN + "Login Restored `Username \"" + username + "\", Password \"" + password + "\", UserId \"" + user.getUserId() + "\", Display Name \"" + user.getDisplayName() + "\"`" );
		}
		catch ( LoginException l )
		{
			// Loader.getLogger().warning( ChatColor.GREEN + "Login Status: No Valid Login Present" );
		}
	}
	
	protected void handleUserProtocols()
	{
		if ( !pendingMessages.isEmpty() )
		{
			// request.getResponse().sendMessage( pendingMessages.toArray( new String[0] ) );
		}
		
		String username = request.getArgument( "user" );
		String password = request.getArgument( "pass" );
		String remember = request.getArgumentBoolean( "remember" ) ? "true" : "false";
		String target = request.getArgument( "target" );
		
		if ( request.getArgument( "logout", "", true ) != null )
		{
			logoutUser();
			
			if ( target.isEmpty() )
				target = request.getSite().getYaml().getString( "scripts.login-form", "/login" );
			
			request.getResponse().sendRedirect( target + "?ok=You have been successfully logged out." );
			return;
		}
		
		if ( !username.isEmpty() && !password.isEmpty() )
		{
			try
			{
				User user = Loader.getUserManager().attemptLogin( this, username, password );
				
				currentUser = user;
				
				String loginPost = ( target.isEmpty() ) ? request.getSite().getYaml().getString( "scripts.login-post", "/panel" ) : target;
				
				setArgument( "remember", remember );
				
				Loader.getLogger().info( ChatColor.GREEN + "Login Success `Username \"" + username + "\", Password \"" + password + "\", UserId \"" + user.getUserId() + "\", Display Name \"" + user.getDisplayName() + "\"`" );
				request.getResponse().sendRedirect( loginPost );
				
			}
			catch ( LoginException l )
			{
				String loginForm = request.getSite().getYaml().getString( "scripts.login-form", "/login" );
				
				if ( l.getUser() != null )
					Loader.getLogger().warning( "Login Failed `Username \"" + username + "\", Password \"" + password + "\", UserId \"" + l.getUser().getUserId() + "\", Display Name \"" + l.getUser().getDisplayName() + "\", Reason \"" + l.getMessage() + "\"`" );
				
				request.getResponse().sendRedirect( loginForm + "?msg=" + l.getMessage() + "&target=" + target );
			}
		}
		else if ( currentUser == null )
		{
			username = getArgument( "user" );
			password = getArgument( "pass" );
			
			if ( !username.isEmpty() && !password.isEmpty() )
			{
				try
				{
					User user = Loader.getUserManager().attemptLogin( this, username, password );
					
					currentUser = user;
					
					Loader.getLogger().info( ChatColor.GREEN + "Login Success `Username \"" + username + "\", Password \"" + password + "\", UserId \"" + user.getUserId() + "\", Display Name \"" + user.getDisplayName() + "\"`" );
				}
				catch ( LoginException l )
				{
					Loader.getLogger().warning( ChatColor.GREEN + "Login Failed `No Valid Login Present`" );
				}
			}
			else
				currentUser = null;
		}
		else
		{
			try
			{
				currentUser.reloadAndValidate();
				Loader.getLogger().info( ChatColor.GREEN + "Current Login `Username \"" + currentUser.getName() + "\", Password \"" + currentUser.getMetaData().getPassword() + "\", UserId \"" + currentUser.getUserId() + "\", Display Name \"" + currentUser.getDisplayName() + "\"`" );
			}
			catch ( LoginException e )
			{
				currentUser = null;
				Loader.getLogger().warning( ChatColor.GREEN + "Login Failed `There was a login present but it failed validation with error: " + e.getMessage() + "`" );
			}
		}
		
		if ( currentUser != null )
			currentUser.putHandler( this );
		
		if ( !stale || Loader.getConfig().getBoolean( "sessions.rearmTimeoutWithEachRequest" ) )
			rearmTimeout();
	}
	
	public void setGlobal( String key, Object val )
	{
		binding.setVariable( key, val );
	}
	
	public Object getGlobal( String key )
	{
		return binding.getVariable( key );
	}
	
	@SuppressWarnings( "unchecked" )
	public Map<String, Object> getGlobals()
	{
		return binding.getVariables();
	}
	
	protected Binding getBinding()
	{
		return binding;
	}
	
	@SuppressWarnings( "unchecked" )
	protected void initSession()
	{
		SqlConnector sql = Loader.getPersistenceManager().getSql();
		
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
					data = new Gson().fromJson( rs.getString( "data" ), Map.class );
					
					// Possible Session Hijacking! nullify!!!
					if ( !_ipAddr.equals( ipAddr ) && !Loader.getConfig().getBoolean( "sessions.allowIPChange" ) )
					{
						sessionCandy = null;
					}
					
					ipAddr = _ipAddr;
					
					List<PersistentSession> sessions = Loader.getPersistenceManager().getSessionsByIp( ipAddr );
					if ( sessions.size() > Loader.getConfig().getInt( "sessions.maxSessionsPerIP" ) )
					{
						int oldestTime = Common.getEpoch();
						PersistentSession oldest = null;
						
						for ( PersistentSession s : sessions )
						{
							if ( s != this && s.getTimeout() < oldestTime )
							{
								oldest = s;
								oldestTime = s.getTimeout();
							}
						}
						
						if ( oldest != null )
							PersistenceManager.destroySession( oldest );
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
			int defaultLife = ( request != null && request.getSite().getYaml() != null ) ? request.getSite().getYaml().getInt( "sessions.default-life", 604800 ) : 604800;
			
			if ( candyId == null || candyId.isEmpty() )
				candyId = StringUtil.md5( request.getURI().toString() + System.currentTimeMillis() );
			
			sessionCandy = new Candy( candyName, candyId );
			
			sessionCandy.setMaxAge( defaultLife );
			sessionCandy.setDomain( "." + request.getSite().domain );
			sessionCandy.setPath( "/" );
			
			candies.put( candyName, sessionCandy );
			
			String dataJson = new Gson().toJson( data );
			
			timeout = Common.getEpoch() + Loader.getConfig().getInt( "sessions.defaultTimeout", 3600 );
			
			sql.queryUpdate( "INSERT INTO `sessions` (`sessionId`, `timeout`, `ipAddr`, `sessionName`, `sessionSite`, `data`)VALUES('" + candyId + "', '" + timeout + "', '" + ipAddr + "', '" + candyName + "', '" + getSite().getName() + "', '" + dataJson + "');" );
		}
		
		if ( stale )
			Loader.getLogger().info( ChatColor.DARK_AQUA + "Session Requested `" + this + "`" );
		else
			Loader.getLogger().info( ChatColor.DARK_AQUA + "Session Created `" + this + "`" );
	}
	
	protected void saveSession()
	{
		SqlConnector sql = Loader.getPersistenceManager().getSql();
		
		String dataJson = new Gson().toJson( data );
		
		sql.queryUpdate( "UPDATE `sessions` SET `data` = '" + dataJson + "', `timeout` = '" + timeout + "', `sessionName` = '" + candyName + "', `ipAddr` = '" + ipAddr + "', `sessionSite` = '" + getSite().getName() + "' WHERE `sessionId` = '" + candyId + "';" );
	}
	
	public String toString()
	{
		String extra = "";
		
		if ( failoverSite != null )
			extra += ",site=" + failoverSite.getName();
		
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
		String _candyName = request.getSite().getYaml().getString( "sessions.cookie-name", Loader.getConfig().getString( "sessions.defaultSessionName", "candyId" ) );
		Map<String, Candy> requestCandys = pullCandies( request );
		
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
	
	public Map<String, Candy> pullCandies( HttpRequest request )
	{
		Map<String, Candy> candies = new LinkedHashMap<String, Candy>();
		List<String> var1 = request.getHeaders().get( "Cookie" );
		
		if ( var1 == null || var1.isEmpty() )
			return candies;
		
		String[] var2 = var1.get( 0 ).split( "\\;" );
		
		for ( String var3 : var2 )
		{
			String[] var4 = var3.trim().split( "\\=" );
			
			if ( var4.length == 2 )
			{
				candies.put( var4[0], new Candy( var4[0], var4[1] ) );
			}
		}
		
		return candies;
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
	
	public void setArgument( String key, String value )
	{
		data.put( key, value );
	}
	
	public String getArgument( String key )
	{
		if ( !data.containsKey( key ) )
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
		
		PersistenceManager.destroySession( this );
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
			
			if ( StringUtil.isTrue( getArgument( "remember" ) ) )
				defaultTimeout = Loader.getConfig().getInt( "sessions.defaultTimeoutRememberMe", 604800 );
			
			if ( Loader.getConfig().getBoolean( "allowNoTimeoutPermission" ) && currentUser.hasPermission( "chiori.noTimeout" ) )
				defaultTimeout = Integer.MAX_VALUE;
		}
		
		timeout = Common.getEpoch() + defaultTimeout + ( Math.min( requestCnt, 6 ) * 600 );
	}
	
	public int getTimeout()
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
	
	// TODO: Future add of setDomain, setCookieName, setSecure (http verses https)
	
	public boolean getUserState()
	{
		return ( currentUser != null );
	}
	
	public User getCurrentUser()
	{
		return currentUser;
	}
	
	/**
	 * Logout the current logged in user.
	 */
	public void logoutUser()
	{
		if ( currentUser != null )
			Loader.getLogger().info( ChatColor.GREEN + "User Logout `" + currentUser + "`" );
		
		// setArgument( "remember", null );
		setArgument( "user", null );
		setArgument( "pass", null );
		currentUser = null;
		
		for ( User u : Loader.getInstance().getOnlineUsers() )
			u.removeHandler( this );
	}
	
	public HttpRequest getRequest()
	{
		return request;
	}
	
	public HttpResponse getResponse()
	{
		return request.getResponse();
	}
	
	public Evaling getEvaling()
	{
		if ( eval == null )
			eval = new Evaling( binding );
		
		return eval;
	}
	
	/**
	 * Called when request has finished so that this stale session can nullify unneeded stuff such as the HttpRequest
	 */
	public void releaseResources()
	{
		request = null;
	}
	
	@Override
	public void kick( String kickMessage )
	{
		logoutUser();
		pendingMessages.add( kickMessage );
	}
	
	@Override
	public void sendMessage( String[] messages )
	{
		for ( String m : messages )
			pendingMessages.add( m );
	}
	
	@Override
	public Site getSite()
	{
		if ( getRequest() != null )
			return getRequest().getSite();
		else if ( failoverSite == null )
			return Loader.getPersistenceManager().getSiteManager().getFrameworkSite();
		else
			return failoverSite;
	}
	
	@Override
	public String getIpAddr()
	{
		return ipAddr;
	}
	
	public void requireLogin() throws IOException
	{
		requireLogin( null );
	}
	
	public void requireLogin( String permission ) throws IOException
	{
		if ( currentUser == null )
			request.getResponse().sendLoginPage();
		
		if ( permission != null )
			if ( !currentUser.hasPermission( permission ) )
				request.getResponse().sendError( HttpCode.HTTP_FORBIDDEN, "You must have the `" + permission + "` in order to view this page!" );
	}
}
