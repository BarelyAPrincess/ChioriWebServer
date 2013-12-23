package com.chiorichan.http;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.chiorichan.Loader;
import com.chiorichan.framework.Framework;
import com.chiorichan.framework.Site;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class HttpRequest
{
	private HttpExchange http;
	private PersistentSession sess;
	private HttpResponse response;
	private Map<String, String> queryMap;
	
	protected HttpRequest(HttpExchange _http)
	{
		http = _http;
		
		queryMap = queryToMap( http.getRequestURI().getQuery() );
		
		sess = Loader.getPersistenceManager().find( this );
		
		response = new HttpResponse( this );
	}
	
	protected Map<String, String> queryToMap( String query )
	{
		Map<String, String> result = new HashMap<String, String>();
		
		if ( query == null )
			return result;
		
		for ( String param : query.split( "&" ) )
		{
			String pair[] = param.split( "=" );
			if ( pair.length > 1 )
			{
				result.put( pair[0], pair[1] );
			}
			else
			{
				result.put( pair[0], "" );
			}
		}
		return result;
	}
	
	public Map<String, String> getQueryMap()
	{
		return queryMap;
	}
	
	public String getArgument( String key )
	{
		return ( queryMap.containsKey( key ) ) ? queryMap.get( key ) : "";
	}
	
	public Collection<Candy> getCandies()
	{
		return sess.candies.values();
	}
	
	public Headers getHeaders()
	{
		return http.getRequestHeaders();
	}
	
	public Framework getFramework()
	{
		return sess.getFramework();
	}
	
	public PersistentSession getSession()
	{
		return sess;
	}
	
	protected HttpResponse getResponse()
	{
		return response;
	}
	
	public URI getURI()
	{
		return http.getRequestURI();
	}
	
	public String getDomain()
	{
		try
		{
			String host = http.getRequestHeaders().get( "Host" ).get( 0 );
			return host.split( "\\:" )[0];
		}
		catch ( Exception e )
		{
			return "";
		}
	}
	
	public String getMethod()
	{
		return http.getRequestMethod();
	}
	
	public String getLocalAddr()
	{
		return http.getLocalAddress().getHostName();
	}
	
	public String getHeader( String key )
	{
		return http.getRequestHeaders().get( key ).get( 0 );
	}
	
	public String getRemoteHost()
	{
		return http.getRemoteAddress().getHostName();
	}
	
	public String getRemoteAddr()
	{
		// This is a checker that makes it possible for our server to get the correct remote IP even if using it with CloudFlare.
		// https://support.cloudflare.com/hc/en-us/articles/200170786-Why-do-my-server-logs-show-CloudFlare-s-IPs-using-CloudFlare-
		if ( http.getRequestHeaders().containsKey( "CF-Connecting-IP" ) )
		{
			return http.getRequestHeaders().get( "CF-Connecting-IP" ).get( 0 );
		}
		else
		{
			return http.getRemoteAddress().getAddress().getHostAddress();
		}
	}
	
	public int getRemotePort()
	{
		return http.getRemoteAddress().getPort();
	}
	
	public int getContentLength()
	{
		try
		{
			return http.getRequestBody().available();
		}
		catch ( IOException e )
		{
			return -1;
		}
	}
	
	public Object getAuthType()
	{
		return "";
	}
	
	/**
	 * Update once https is available
	 */
	public boolean isSecure()
	{
		return false;
	}
	
	public int getServerPort()
	{
		return http.getLocalAddress().getPort();
	}
	
	public String getServerName()
	{
		return http.getLocalAddress().getHostName();
	}
	
	public String getParameter( String key )
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	protected Site currentSite;
	
	protected void setSite( Site site )
	{
		currentSite = site;
	}
	
	public Site getSite()
	{
		if ( currentSite == null )
			return Loader.getPersistenceManager().getSiteManager().getSiteById( "framework" );
		
		return currentSite;
	}
	
	public String getAttribute( String string )
	{
		return null;
	}
	
	public void setHeader( String key, String value )
	{
		http.getResponseHeaders().add( key, value );
	}
	
	protected HttpExchange getOriginal()
	{
		return http;
	}
}
