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
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.bases.Sentient;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.account.helpers.LoginException;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.factory.BindingProvider;
import com.chiorichan.factory.CodeEvalFactory;
import com.chiorichan.framework.ConfigurationManagerWrapper;
import com.chiorichan.framework.Site;
import com.chiorichan.util.Common;
import com.chiorichan.util.StringUtil;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * This class is used to carry data that is to be persistent from request to request.
 * If you need to sync data across requests then we recommend using Session Vars for Security.
 * 
 * @author Chiori Greene
 * @copyright Greenetree LLC
 */
public class PersistentSession implements SentientHandler, BindingProvider
{
	protected Map<String, String> data = new LinkedHashMap<String, String>();
	protected long timeout = 0;
	protected int requestCnt = 0;
	protected String candyId = "", candyName = "candyId", ipAddr = null;
	protected Candy sessionCandy;
	protected Account currentAccount = null;
	protected List<String> pendingMessages = Lists.newArrayList();
	
	protected Map<String, Candy> candies = new LinkedHashMap<String, Candy>();
	protected HttpRequest request;
	protected Site failoverSite;
	protected boolean stale = false;
	protected boolean isValid = true;
	protected boolean changesMade = false;
	
	protected final Binding binding = new Binding();
	
	protected CodeEvalFactory factory = null;
	
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
	
	protected PersistentSession(ResultSet rs) throws SessionException
	{
		this();
		
		try
		{
			request = null;
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
				failoverSite = Loader.getSiteManager().getFrameworkSite();
			else
				failoverSite = Loader.getSiteManager().getSiteById( rs.getString( "sessionSite" ) );
			
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
		
		if ( request != null )
		{
			binding.setVariable( "request", request );
			binding.setVariable( "response", request.getResponse() );
		}
		binding.setVariable( "__FILE__", new File( "" ) );
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
	
	protected void handleUserProtocols()
	{
		if ( !pendingMessages.isEmpty() )
		{
			// request.getResponse().sendMessage( pendingMessages.toArray( new String[0] ) );
		}
		
		if ( request == null )
		{
			Loader.getLogger().warning( "PersistentSession: Request was misteriously empty for an unknown reason." );
			return;
		}
		
		String username = request.getArgument( "user" );
		String password = request.getArgument( "pass" );
		String remember = request.getArgumentBoolean( "remember" ) ? "true" : "false";
		String target = request.getArgument( "target" );
		
		if ( request.getArgument( "logout", "", true ) != null )
		{
			logoutAccount();
			
			if ( target.isEmpty() )
				target = request.getSite().getYaml().getString( "scripts.login-form", "/login" );
			
			request.getResponse().sendRedirect( target + "?ok=You have been successfully logged out." );
			return;
		}
		
		if ( !username.isEmpty() && !password.isEmpty() )
		{
			try
			{
				Account user = Loader.getAccountsManager().attemptLogin( this, username, password );
				
				currentAccount = user;
				
				String loginPost = ( target.isEmpty() ) ? request.getSite().getYaml().getString( "scripts.login-post", "/panel" ) : target;
				
				setArgument( "remember", remember );
				
				Loader.getLogger().info( ChatColor.GREEN + "Login Success `Username \"" + username + "\", Password \"" + password + "\", UserId \"" + user.getAccountId() + "\", Display Name \"" + user.getDisplayName() + "\"`" );
				request.getResponse().sendRedirect( loginPost );
				
			}
			catch ( LoginException l )
			{
				//l.printStackTrace();
				
				String loginForm = request.getSite().getYaml().getString( "scripts.login-form", "/login" );
				
				if ( l.getAccount() != null )
					Loader.getLogger().warning( "Login Failed `Username \"" + username + "\", Password \"" + password + "\", UserId \"" + l.getAccount().getAccountId() + "\", Display Name \"" + l.getAccount().getDisplayName() + "\", Reason \"" + l.getMessage() + "\"`" );
				
				request.getResponse().sendRedirect( loginForm + "?ok=" + l.getMessage() + "&target=" + target );
			}
		}
		else if ( currentAccount == null )
		{
			username = getArgument( "user" );
			password = getArgument( "pass" );
			
			if ( username != null && !username.isEmpty() && password != null && !password.isEmpty() )
			{
				try
				{
					Account user = Loader.getAccountsManager().attemptLogin( this, username, password );
					
					currentAccount = user;
					
					Loader.getLogger().info( ChatColor.GREEN + "Login Success `Username \"" + username + "\", Password \"" + password + "\", UserId \"" + user.getAccountId() + "\", Display Name \"" + user.getDisplayName() + "\"`" );
				}
				catch ( LoginException l )
				{
					Loader.getLogger().warning( ChatColor.GREEN + "Login Failed `No Valid Login Present`" );
				}
			}
			else
				currentAccount = null;
		}
		else
		{
			try
			{
				currentAccount.reloadAndValidate(); // <- Is this being overly redundant?
				Loader.getLogger().info( ChatColor.GREEN + "Current Login `Username \"" + currentAccount.getName() + "\", Password \"" + currentAccount.getMetaData().getPassword() + "\", UserId \"" + currentAccount.getAccountId() + "\", Display Name \"" + currentAccount.getDisplayName() + "\"`" );
			}
			catch ( LoginException e )
			{
				currentAccount = null;
				Loader.getLogger().warning( ChatColor.GREEN + "Login Failed `There was a login present but it failed validation with error: " + e.getMessage() + "`" );
			}
		}
		
		if ( currentAccount != null )
			currentAccount.putHandler( this );
		
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
	
	protected void initSession()
	{
		DatabaseEngine sql = Loader.getPersistenceManager().getDatabase();
		
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
					
					List<PersistentSession> sessions = Loader.getPersistenceManager().getSessionsByIp( ipAddr );
					if ( sessions.size() > Loader.getConfig().getInt( "sessions.maxSessionsPerIP" ) )
					{
						long oldestTime = Common.getEpoch();
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
			
			if ( request.getSite().getDomain() != null && !request.getSite().getDomain().isEmpty() && request.getParentDomain().toLowerCase().contains( request.getSite().getDomain().toLowerCase() ) )
				sessionCandy.setDomain( "." + request.getSite().getDomain() );
			else if ( request.getParentDomain() != null && !request.getParentDomain().isEmpty() )
				sessionCandy.setDomain( "." + request.getParentDomain() );
			
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
	
	protected void saveSession( boolean force )
	{
		if ( force || changesMade )
		{
			DatabaseEngine sql = Loader.getPersistenceManager().getDatabase();
			
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
		String _candyName = request.getSite().getYaml().getString( "sessions.cookie-name", Loader.getConfig().getString( "sessions.defaultSessionName", "sessionId" ) );
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
		if ( value == null )
			data.remove( key );
		
		data.put( key, value );
		changesMade = true;
	}
	
	public String getArgument( String key )
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
		
		PersistenceManager.destroySession( this );
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
	
	// TODO: Future add of setDomain, setCookieName, setSecure (http verses https)
	
	public boolean getUserState()
	{
		return ( currentAccount != null );
	}
	
	public Account getCurrentAccount()
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
	
	public HttpRequest getRequest()
	{
		return request;
	}
	
	public HttpResponse getResponse()
	{
		return request.getResponse();
	}
	
	/**
	 * Called when request has finished so that this stale session can nullify unneeded stuff such as the HttpRequest
	 */
	public void releaseResources()
	{
		request = null;
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
	
	public Site getSite()
	{
		if ( getRequest() != null )
			return getRequest().getSite();
		else if ( failoverSite == null )
			return Loader.getSiteManager().getFrameworkSite();
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
	
	/**
	 * First checks in an account is present, sends to login page if not.
	 * Second checks if the present accounts has the specified permission.
	 * 
	 * @param permission
	 * @throws IOException
	 */
	public void requireLogin( String permission ) throws IOException
	{
		if ( currentAccount == null )
			request.getResponse().sendLoginPage();
		
		if ( permission != null )
			if ( !currentAccount.hasPermission( permission ) )
				request.getResponse().sendError( HttpCode.HTTP_FORBIDDEN, "You must have the `" + permission + "` in order to view this page!" );
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
	
	/**
	 * It's HIGHLY important that you call the .unlock() method on the factory once your done with it or else it will build up in memory.
	 */
	@Override
	public CodeEvalFactory getCodeFactory()
	{
		if ( factory == null )
			factory = CodeEvalFactory.create( binding );
		
		return factory;
	}
	
	public ConfigurationManagerWrapper getConfigurationManager()
	{
		return new ConfigurationManagerWrapper( request.getSession() );
	}

	@Override
	public String getName()
	{
		if ( currentAccount == null )
			return "(NULL)";
		
		return currentAccount.getName();
	}
}
