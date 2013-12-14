package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Session
{
	private Map<String, String> data = new LinkedHashMap<String, String>();
	private int expires = 0;
	private String sessId = "", sessionName = "ChioriSessionId";
	private Cookie cookie;
	
	public Session(Framework fw)
	{
		this( fw.getCurrentSite(), fw.getRequest(), fw.getResponse() );
	}
	
	public Session( Site site, HttpServletRequest request, HttpServletResponse response )
	{
		// Exception being thrown near this line is a sign that the server might need to reboot. (ie. The site)
		// TODO: Reload site yaml on exception.
		
		sessionName = site.getYaml().getString( "sessions.cookie-name", "ChioriSessionId" );
		SqlConnector sql = Framework.getDatabase();
		
		cookie = getCookie( request, sessionName );
		
		if ( cookie != null )
		{
			ResultSet rs = null;
			try
			{
				rs = sql.query( "SELECT * FROM `sessions` WHERE `sessid` = '" + cookie.getValue() + "'" );
			}
			catch ( SQLException e1 )
			{
				e1.printStackTrace();
			}
			
			sessId = cookie.getValue();
			
			if ( rs == null || sql.getRowCount( rs ) < 1 )
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
			int defaultLife = site.getYaml().getInt( "sessions.default-life", 604800 );
			
			sessId = request.getSession( true ).getId();
			
			cookie = new Cookie( sessionName, sessId );
			
			cookie.setMaxAge( defaultLife );
			cookie.setDomain( "." + site.domain );
			cookie.setPath( "/" );
			
			response.addCookie( cookie );
			
			data.put( "ipAddr", request.getRemoteAddr() );
			String dataJson = new Gson().toJson( data );
			
			expires = (int) (System.currentTimeMillis() / 1000) + defaultLife;
			
			sql.queryUpdate( "INSERT INTO `sessions` (`sessid`, `expires`, `data`)VALUES('" + sessId + "', '" + expires + "', '" + dataJson + "');" );
		}
		
		Loader.getLogger().info( "InitSessions: " + this );
	}
	
	public void saveSession(Framework fw)
	{
		SqlConnector sql = Framework.getDatabase();
		
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
	
	// TODO: Fix ME
	public void destroy()
	{
		expires = 0;
		setCookieExpiry( 0 );
	}
	
	public String getId()
	{
		return sessId;
	}
	
	// TODO: Future add of setDomain, setCookieName
}
