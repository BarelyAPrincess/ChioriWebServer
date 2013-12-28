package com.chiorichan.http;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.framework.Framework;
import com.chiorichan.util.Common;
import com.chiorichan.util.StringUtil;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * This class is used to carry data that is to be persistent from request to request. If you need to sync data across
 * requests then we recommend using Session Vars for Security.
 * 
 * @author Chiori Greene
 * @copyright Greenetree LLC
 */
public class PersistentSession
{
	protected Map<String, String> data = new LinkedHashMap<String, String>();
	protected int expires = 0, defaultLife = 86400000; // 1 Day!
	protected int timeout = 0, requestCnt = 0, defaultTimeout = 10000; // 10 minutes
	protected String candyId = "", candyName = "candyId";
	protected Candy sessionCandy;
	
	protected Map<String, Candy> candies = new LinkedHashMap<String, Candy>();
	protected Framework framework = null;
	protected HttpRequest request;
	protected Boolean stale = false;
	
	/**
	 * Returns an instance of Framework relevant to this session. Instigates a new one if not already done.
	 * 
	 * @return Framework
	 */
	public Framework getFramework()
	{
		if ( framework == null )
			framework = new Framework( request, request.getResponse() );
		else
		{
			framework.setRequest( request );
			framework.setResponse( request.getResponse() );
		}
		
		return framework;
	}
	
	/**
	 * Initializes a new session based on the supplied HttpRequest.
	 * 
	 * @param _request
	 */
	protected PersistentSession(HttpRequest _request)
	{
		setRequest( _request, false );
	}
	
	protected void setRequest( HttpRequest _request, Boolean _stale )
	{
		request = _request;
		stale = _stale;
		
		rearmTimeout();
		
		candyName = request.getSite().getYaml().getString( "sessions.cookie-name", candyName );
		
		candies = pullCandies( _request );
		sessionCandy = candies.get( candyName );
		
		initSession();
	}
	
	protected void initSession()
	{
		SqlConnector sql = Loader.getPersistenceManager().getSql();
		
		if ( sessionCandy != null )
		{
			ResultSet rs = null;
			try
			{
				rs = sql.query( "SELECT * FROM `sessions` WHERE `sessid` = '" + sessionCandy.getValue() + "'" );
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
					expires = rs.getInt( "expires" );
					data = new Gson().fromJson( rs.getString( "data" ), Map.class );
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
			int defaultLife = request.getSite().getYaml().getInt( "sessions.default-life", 604800 );
			
			if ( candyId == null || candyId.isEmpty() )
				candyId = StringUtil.md5( request.getURI().toString() + System.currentTimeMillis() );
			
			sessionCandy = new Candy( candyName, candyId );
			
			sessionCandy.setMaxAge( defaultLife );
			sessionCandy.setDomain( "." + request.getSite().domain );
			sessionCandy.setPath( "/" );
			
			candies.put( candyName, sessionCandy );
			
			data.put( "ipAddr", request.getRemoteAddr() );
			String dataJson = new Gson().toJson( data );
			
			expires = Common.getEpoch() + defaultLife;
			
			sql.queryUpdate( "INSERT INTO `sessions` (`sessid`, `expires`, `data`)VALUES('" + candyId + "', '" + expires + "', '" + dataJson + "');" );
		}
		
		Loader.getLogger().info( "Session Initalized: " + this );
	}
	
	public void saveSession()
	{
		SqlConnector sql = Loader.getPersistenceManager().getSql();
		
		data.put( "ipAddr", request.getRemoteAddr() );
		String dataJson = new Gson().toJson( data );
		
		sql.queryUpdate( "UPDATE `sessions` SET `data` = '" + dataJson + "', `expires` = '" + expires + "' WHERE `sessid` = '" + candyId + "';" );
	}
	
	public String toString()
	{
		return candyName + "{id=" + candyId + ",expires=" + expires + ",data=" + data + "}";
	}
	
	/**
	 * Determines if this session belongs to the supplied HttpRequest based on the SessionId cookie.
	 * 
	 * @param request
	 * @return boolean
	 */
	public boolean matchClient( HttpRequest request )
	{
		Map<String, Candy> requestCandys = pullCandies( request );
		return ( requestCandys.containsKey( candyName ) && getCandy( candyName ).compareTo( requestCandys.get( candyName ) ) );
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
	
	private Cookie getCookie( HttpServletRequest request, String name )
	{
		if ( request.getCookies() != null )
			for ( Cookie c : request.getCookies() )
			{
				if ( c.getName().equals( name ) )
					return c;
			}
		
		return null;
	}
	
	public void setArgument( String key, String value )
	{
		data.put( key, value );
	}
	
	public String getArgument( String key )
	{
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
	
	// TODO: Fix ME
	public void destroy()
	{
		expires = 0;
		setCookieExpiry( 0 );
	}
	
	public void rearmTimeout()
	{
		// TODO: Extend timeout even longer if a user is logged in.
		
		// Grant the timeout an additional 2 minutes per request
		if ( requestCnt < 6 )
			requestCnt++;
		
		timeout = Common.getEpoch() + defaultTimeout + ( requestCnt * 120000 );
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
}
