package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Session
{
	private Map<String, String> data = new HashMap<String, String>();
	private int expires = 0;
	private String sessId = "", sessionName = "ChioriSessionId";
	private Cookie cookie;
	
	public Session(Framework fw)
	{
		sessionName = fw.getCurrentSite().getYaml().getString( "sessions.cookie-name", "ChioriSessionId" );
		SqlConnector sql = fw.getDatabase();
		
		cookie = getCookie( fw.getRequest(), sessionName );
		
		if ( cookie != null )
		{
			ResultSet rs = sql.query( "SELECT * FROM `sessions` WHERE `sessid` = '" + cookie.getValue() + "'" );
			
			sessId = cookie.getValue();
			
			if ( sql.getRowCount( rs ) < 1 )
				cookie = null;
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
					cookie = null;
				}
			}
		}
		
		if ( cookie == null )
		{
			int defaultLife = fw.getCurrentSite().getYaml().getInt( "sessions.default-life", 604800 );
			
			cookie = new Cookie( sessionName, fw.getRequestId() );
			
			sessId = fw.getRequestId();
			
			cookie.setMaxAge( defaultLife );
			cookie.setDomain( "." + fw.siteDomain );
			cookie.setPath( "/" );
			
			fw.getResponse().addCookie( cookie );
			
			data.put( "ipAddr", fw.getRequest().getRemoteAddr() );
			String dataJson = new Gson().toJson( data );
			
			expires = (int) (System.currentTimeMillis() / 1000) + defaultLife;
			
			sql.queryUpdate( "INSERT INTO `sessions` (`sessid`, `expires`, `data`)VALUES('" + fw.getRequestId() + "', '" + expires + "', '" + dataJson + "');" );
		}
		
		Loader.getConsole().info( "InitSessions: " + this );
	}
	
	public void saveSession(Framework fw)
	{
		SqlConnector sql = fw.getDatabase();
		
		data.put( "ipAddr", fw.getRequest().getRemoteAddr() );
		String dataJson = new Gson().toJson( data );
		
		sql.queryUpdate( "UPDATE `sessions` SET `data` = '" + dataJson + "', `expires` = '" + expires + "' WHERE `sessid` = '" + sessId + "';" );
	}
	
	public String toString()
	{
		return sessionName + "{sessId=" + sessId + ",expires=" + expires + ",data=" + data + "}";
	}
	
	public Session()
	{
		
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
		cookie.setMaxAge( valid );
	}
	
	public void destroy()
	{
		setCookieExpiry( 0 );
	}
	
	// TODO: Future add of setDomain, setCookieName
}
