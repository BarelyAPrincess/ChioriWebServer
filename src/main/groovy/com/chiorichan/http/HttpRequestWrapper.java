/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.ssl.SslHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.event.server.ServerVars;
import com.chiorichan.framework.Site;
import com.chiorichan.session.SessionProvider;
import com.chiorichan.util.Common;
import com.chiorichan.util.StringUtil;
import com.chiorichan.util.Versioning;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

public class HttpRequestWrapper
{
	protected Map<ServerVars, Object> serverVars = Maps.newLinkedHashMap();
	protected Site currentSite;
	protected SessionProvider sess = null;
	protected HttpResponseWrapper response;
	protected Map<String, String> getMap, postMap, rewriteMap = Maps.newLinkedHashMap();
	protected int requestTime = 0;
	protected Map<String, UploadedFile> uploadedFiles = new HashMap<String, UploadedFile>();
	protected String uri = null;
	protected int contentSize = 0;
	protected boolean ssl;
	
	protected HttpRequest http;
	protected Channel channel;
	
	protected HttpRequestWrapper( Channel channel, HttpRequest http, boolean ssl ) throws IOException
	{
		this.channel = channel;
		this.http = http;
		this.ssl = ssl;
		
		requestTime = Common.getEpoch();
		
		response = new HttpResponseWrapper( this );
		
		String domain = getParentDomain();
		
		currentSite = Loader.getSiteManager().getSiteByDomain( domain );
		
		if ( currentSite == null )
			if ( !domain.isEmpty() )
			{
				// Attempt to get the catch all default site. Will use the framework site is not configured or does not exist.
				String defaultSite = Loader.getConfig().getString( "framework.sites.defaultSite", null );
				if ( defaultSite != null && !defaultSite.isEmpty() )
					currentSite = Loader.getSiteManager().getSiteById( defaultSite );
			}
		
		if ( currentSite == null )
			currentSite = Loader.getSiteManager().getSiteById( "framework" );
		
		getMap = Maps.newTreeMap();
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder( http.getUri() );
		Map<String, List<String>> params = queryStringDecoder.parameters();
		if ( !params.isEmpty() )
		{
			for ( Entry<String, List<String>> p : params.entrySet() )
			{
				String key = p.getKey();
				List<String> vals = p.getValue();
				for ( String val : vals )
				{
					getMap.put( key, val );
				}
			}
		}
	}
	
	protected void initSession()
	{
		sess = Loader.getSessionManager().find( this );
		sess.handleUserProtocols();
	}
	
	public Boolean getArgumentBoolean( String key )
	{
		String rtn = getArgument( key, "0" ).toLowerCase();
		return StringUtil.isTrue( rtn );
	}
	
	public String getArgument( String key )
	{
		return getArgument( key, "" );
	}
	
	public String getArgument( String key, String def )
	{
		return getArgument( key, def, false );
	}
	
	public String getArgument( String key, String def, boolean rtnNull )
	{
		String val = getMap.get( key );
		
		if ( val == null && postMap != null )
			val = postMap.get( key );
		
		if ( val == null && rewriteMap != null )
			val = rewriteMap.get( key );
		
		if ( val == null && rtnNull )
			return null;
		
		if ( val == null || val.isEmpty() )
			return def;
		
		return val.trim();
	}
	
	public Collection<Candy> getCandies()
	{
		if ( sess == null )
			return new LinkedHashMap<String, Candy>().values();
		
		return getSession().getParentSession().getCandies().values();
	}
	
	public HttpHeaders getHeaders()
	{
		return http.headers();
	}
	
	public SessionProvider getSession()
	{
		return getSession( true );
	}
	
	public SessionProvider getSession( boolean initIfNull )
	{
		if ( sess == null && initIfNull )
			initSession();
		
		return sess;
	}
	
	public HttpResponseWrapper getResponse()
	{
		return response;
	}
	
	public String getURI()
	{
		if ( uri == null )
			uri = http.getUri();
		
		try
		{
			uri = URLDecoder.decode( uri, Charsets.UTF_8.name() );
		}
		catch ( UnsupportedEncodingException e )
		{
			try
			{
				uri = URLDecoder.decode( uri, Charsets.ISO_8859_1.name() );
			}
			catch ( UnsupportedEncodingException e1 )
			{
				throw new Error();
			}
		}
		catch ( IllegalArgumentException e1 )
		{
			// [ni..up-3-1] 02-05 00:17:10.273 [WARNING] [HttpHdl] WARNING THIS IS AN UNCAUGHT EXCEPTION! CAN YOU KINDLY REPORT THIS STACKTRACE TO THE DEVELOPER?
			// java.lang.IllegalArgumentException: URLDecoder: Illegal hex characters in escape (%) pattern - For input string: "im"
		}
		
		// if ( uri.contains( File.separator + '.' ) || uri.contains( '.' + File.separator ) || uri.startsWith( "." ) || uri.endsWith( "." ) || INSECURE_URI.matcher( uri ).matches() )
		// {
		// return "/";
		// }
		
		if ( uri.contains( "?" ) )
			uri = uri.substring( 0, uri.indexOf( "?" ) );
		
		if ( !uri.startsWith( "/" ) )
			uri = "/" + uri;
		
		return uri;
	}
	
	public String getHost()
	{
		return http.headers().get( "Host" );
	}
	
	// Cached domain names.
	protected String parentDomainName = null;
	protected String childDomainName = null;
	
	public String getDomain()
	{
		try
		{
			String domain = http.headers().get( "Host" );
			domain = domain.split( "\\:" )[0];
			
			return domain;
		}
		catch ( NullPointerException e )
		{
			return "";
		}
	}
	
	/**
	 *
	 * @return a string containing the main domain from the request. ie. test.example.com or example.com = "example.com"
	 */
	public String getParentDomain()
	{
		if ( parentDomainName == null || childDomainName == null )
			calculateDomainName();
		
		return ( parentDomainName == null ) ? "" : parentDomainName;
	}
	
	/**
	 *
	 * @return A string containing the subdomain from the request. ie. test.example.com = "test"
	 */
	public String getSubDomain()
	{
		if ( parentDomainName == null || childDomainName == null )
			calculateDomainName();
		
		return ( childDomainName == null ) ? "" : childDomainName;
	}
	
	/**
	 * Calculates both the SubDomain and ParentDomain from the Host Header and saves them in Strings
	 */
	public void calculateDomainName()
	{
		childDomainName = "";
		parentDomainName = "";
		
		if ( http.headers().get( "Host" ) != null )
		{
			String domain = http.headers().get( "Host" );
			
			if ( domain.contains( ":" ) )
				domain = domain.substring( 0, domain.indexOf( ":" ) ).trim(); // Remove port number.
				
			if ( domain.toLowerCase().endsWith( "localhost" ) || domain.equalsIgnoreCase( "127.0.0.1" ) || domain.equalsIgnoreCase( getLocalAddr() ) || domain.toLowerCase().endsWith( getLocalHost() ) )
				domain = "";
			
			if ( domain == null || domain.isEmpty() )
				return;
			
			if ( domain.startsWith( "." ) )
				domain = domain.substring( 1 );
			
			if ( StringUtil.validateIpAddress( domain ) )
			{
				// This should be an IP Address
				parentDomainName = domain;
			}
			else
			{
				int periodCount = StringUtils.countMatches( domain, "." );
				
				if ( periodCount < 2 )
					parentDomainName = domain;
				else
				{
					childDomainName = domain.substring( 0, domain.lastIndexOf( ".", domain.lastIndexOf( "." ) - 1 ) );
					parentDomainName = domain.substring( domain.lastIndexOf( ".", domain.lastIndexOf( "." ) - 1 ) + 1 );
				}
			}
		}
	}
	
	public String getMethodString()
	{
		return http.getMethod().toString();
	}
	
	public HttpMethod getMethod()
	{
		return http.getMethod();
	}
	
	public String getHeader( String key )
	{
		try
		{
			return http.headers().get( key );
		}
		catch ( NullPointerException | IndexOutOfBoundsException e )
		{
			return "";
		}
	}
	
	/**
	 * Not a guaranteed method to determined if a request was made with AJAX since this header is not always set.
	 */
	public boolean isAjaxRequest()
	{
		return ( getHeader( "X-requested-with" ).equals( "XMLHttpRequest" ) );
	}
	
	public String getRemoteHost()
	{
		return ( ( InetSocketAddress ) channel.remoteAddress() ).getHostName();
	}
	
	/**
	 * Similar to {@link #getRemoteAddr(boolean)} except defaults to true
	 * 
	 * @return
	 *         the remote connections IP address as a string
	 */
	public String getRemoteAddr()
	{
		return getRemoteAddr( true );
	}
	
	/**
	 * This method uses a checker that makes it possible for our server to get the correct remote IP even if using it with CloudFlare.
	 * I believe there are other CDN services like CloudFlare. I'd love it if people could inform me, so I can implement similar methods.
	 * https://support.cloudflare.com/hc/en-us/articles/200170786-Why-do-my-server-logs-show-CloudFlare-s-IPs-using-CloudFlare-
	 * 
	 * @param detectCDN
	 *            Try to detect the use of CDNs, e.g., CloudFlare, IP headers when set to false.
	 * @return
	 *         the remote connections IP address as a string
	 */
	public String getRemoteAddr( boolean detectCDN )
	{
		if ( detectCDN && http.headers().contains( "CF-Connecting-IP" ) )
			return http.headers().get( "CF-Connecting-IP" );
		
		return ( ( InetSocketAddress ) channel.remoteAddress() ).getAddress().getHostAddress();
	}
	
	/**
	 * Similar to {@link #getRemoteAddr()}
	 * 
	 * @return
	 *         the remote connections IP address
	 */
	public InetAddress getRemoteInetAddr()
	{
		return getRemoteInetAddr( true );
	}
	
	/**
	 * Similar to {@link #getRemoteAddr(boolean)}
	 * 
	 * @param detectCDN
	 *            Try to detect the use of CDNs, e.g., CloudFlare, IP headers when set to false.
	 * @return
	 *         the remote connections IP address
	 */
	public InetAddress getRemoteInetAddr( boolean detectCDN )
	{
		if ( detectCDN && http.headers().contains( "CF-Connecting-IP" ) )
			try
			{
				return InetAddress.getByName( http.headers().get( "CF-Connecting-IP" ) );
			}
			catch ( UnknownHostException e )
			{
				e.printStackTrace();
				return null;
			}
		
		return ( ( InetSocketAddress ) channel.remoteAddress() ).getAddress();
	}
	
	public int getRemotePort()
	{
		return ( ( InetSocketAddress ) channel.remoteAddress() ).getPort();
	}
	
	public boolean isSecure()
	{
		return ( channel.pipeline().get( SslHandler.class ) != null );
	}
	
	public int getServerPort()
	{
		return ( ( InetSocketAddress ) channel.localAddress() ).getPort();
	}
	
	public String getServerName()
	{
		return ( ( InetSocketAddress ) channel.localAddress() ).getHostName();
	}
	
	public String getParameter( String key )
	{
		return null;
	}
	
	protected void setSite( Site site )
	{
		currentSite = site;
	}
	
	public Site getSite()
	{
		if ( currentSite == null )
			return Loader.getSiteManager().getSiteById( "framework" );
		
		return currentSite;
	}
	
	public String getAttribute( String string )
	{
		return null;
	}
	
	public HttpRequest getOriginal()
	{
		return http;
	}
	
	private Map<String, Object> parseMapArrays( Map<String, String> origMap )
	{
		Map<String, Object> result = Maps.newLinkedHashMap();
		
		for ( Entry<String, String> e : origMap.entrySet() )
		{
			String var = null;
			String key = null;
			String val = e.getValue();
			
			if ( e.getKey().contains( "[" ) && e.getKey().endsWith( "]" ) )
			{
				var = e.getKey().substring( 0, e.getKey().indexOf( "[" ) );
				
				if ( e.getKey().length() - e.getKey().indexOf( "[" ) > 1 )
					key = e.getKey().substring( e.getKey().indexOf( "[" ) + 1, e.getKey().length() - 1 );
				else
					key = "";
			}
			else
			{
				var = e.getKey();
			}
			
			if ( result.containsKey( var ) )
			{
				Object o = result.get( var );
				if ( o instanceof String )
				{
					if ( key == null || key.isEmpty() )
						key = "1";
					
					Map<String, String> hash = Maps.newLinkedHashMap();
					hash.put( "0", ( String ) o );
					hash.put( key, val );
					result.put( var, hash );
				}
				else if ( o instanceof Map )
				{
					@SuppressWarnings( "unchecked" )
					Map<String, String> map = ( Map<String, String> ) o;
					
					if ( key == null || key.isEmpty() )
					{
						int cnt = 0;
						while ( map.containsKey( cnt ) )
						{
							cnt++;
						}
						key = "" + cnt;
					}
					
					map.put( key, val );
				}
				else
				{
					if ( key == null )
						result.put( var, val );
					else
					{
						if ( key.isEmpty() )
							key = "0";
						
						Map<String, String> hash = Maps.newLinkedHashMap();
						hash.put( key, val );
						result.put( var, hash );
					}
				}
				
			}
			else
			{
				if ( key == null )
				{
					result.put( e.getKey(), e.getValue() );
				}
				else
				{
					if ( key.isEmpty() )
						key = "0";
					
					Map<String, String> hash = Maps.newLinkedHashMap();
					hash.put( key, val );
					result.put( var, hash );
				}
			}
		}
		
		return result;
	}
	
	public Map<String, Object> getRequestMapParsed()
	{
		return parseMapArrays( getRequestMap() );
	}
	
	public Map<String, String> getRequestMap()
	{
		Map<String, String> requestMap = new HashMap<String, String>();
		
		if ( getMap != null )
			requestMap.putAll( getMap );
		
		if ( postMap != null )
			requestMap.putAll( postMap );
		
		if ( rewriteMap != null )
			requestMap.putAll( rewriteMap );
		
		return requestMap;
	}
	
	public Map<String, Object> getPostMapParsed()
	{
		return parseMapArrays( getPostMap() );
	}
	
	public Map<String, String> getPostMap()
	{
		if ( postMap == null )
			postMap = new HashMap<String, String>();
		
		return postMap;
	}
	
	public Map<String, Object> getGetMapParsed()
	{
		return parseMapArrays( getGetMap() );
	}
	
	public Map<String, String> getGetMap()
	{
		if ( getMap == null )
			getMap = new HashMap<String, String>();
		
		return getMap;
	}
	
	public Map<String, UploadedFile> getUploadMap()
	{
		return uploadedFiles;
	}
	
	protected void putUpload( String name, UploadedFile uploadedFile )
	{
		uploadedFiles.put( name, uploadedFile );
	}
	
	public int getRequestTime()
	{
		return requestTime;
	}
	
	public String getRequestHost()
	{
		return getHeader( "Host" );
	}
	
	public String getLocalHost()
	{
		return ( ( InetSocketAddress ) channel.localAddress() ).getHostName();
	}
	
	public String getLocalAddr()
	{
		return ( ( InetSocketAddress ) channel.localAddress() ).getAddress().getHostAddress();
	}
	
	public String getUserAgent()
	{
		return getHeader( "User-Agent" );
	}
	
	void putServerVarSafe( ServerVars key, Object value )
	{
		try
		{
			serverVars.put( key, value );
		}
		catch ( Exception e )
		{
			
		}
	}
	
	void initServerVars( Map<ServerVars, Object> staticServerVars )
	{
		// Not sure of the need to do a try... catch.
		// Instead of a blanket catch, we should make this more of a personal check for each put.
		serverVars = staticServerVars;
		putServerVarSafe( ServerVars.DOCUMENT_ROOT, getSite().getAbsoluteRoot() );
		putServerVarSafe( ServerVars.HTTP_ACCEPT, getHeader( "Accept" ) );
		putServerVarSafe( ServerVars.HTTP_USER_AGENT, getUserAgent() );
		putServerVarSafe( ServerVars.HTTP_CONNECTION, getHeader( "Connection" ) );
		putServerVarSafe( ServerVars.HTTP_HOST, getLocalHost() );
		putServerVarSafe( ServerVars.HTTP_ACCEPT_ENCODING, getHeader( "Accept-Encoding" ) );
		putServerVarSafe( ServerVars.HTTP_ACCEPT_LANGUAGE, getHeader( "Accept-Language" ) );
		putServerVarSafe( ServerVars.HTTP_X_REQUESTED_WITH, getHeader( "X-requested-with" ) );
		putServerVarSafe( ServerVars.REMOTE_HOST, getRemoteHost() );
		putServerVarSafe( ServerVars.REMOTE_ADDR, getRemoteAddr() );
		putServerVarSafe( ServerVars.REMOTE_PORT, getRemotePort() );
		putServerVarSafe( ServerVars.REQUEST_TIME, getRequestTime() );
		putServerVarSafe( ServerVars.REQUEST_URI, getURI() );
		putServerVarSafe( ServerVars.CONTENT_LENGTH, getContentLength() );
		// putServerVarSafe( ServerVars.AUTH_TYPE, getAuthType() );
		putServerVarSafe( ServerVars.SERVER_NAME, getServerName() );
		putServerVarSafe( ServerVars.SERVER_PORT, getServerPort() );
		putServerVarSafe( ServerVars.HTTPS, isSecure() );
		putServerVarSafe( ServerVars.SESSION, getSession() );
		putServerVarSafe( ServerVars.SERVER_SOFTWARE, Versioning.getProduct() );
		putServerVarSafe( ServerVars.SERVER_VERSION, Versioning.getVersion() );
		putServerVarSafe( ServerVars.SERVER_ADMIN, Loader.getConfig().getString( "server.admin", "webmaster@" + getDomain() ) );
		putServerVarSafe( ServerVars.SERVER_SIGNATURE, Versioning.getProduct() + " Version " + Versioning.getVersion() );
	}
	
	public Map<ServerVars, Object> getServerVars()
	{
		return serverVars;
	}
	
	public Map<String, Object> getServerStrings()
	{
		Map<String, Object> server = Maps.newLinkedHashMap();
		
		for ( Map.Entry<ServerVars, Object> en : serverVars.entrySet() )
		{
			server.put( en.getKey().getName().toLowerCase(), en.getValue() );
			server.put( en.getKey().getName().toUpperCase(), en.getValue() );
			server.put( en.getKey().getName(), en.getValue() );
		}
		
		return server;
	}
	
	public Map<String, String> getRewriteMap()
	{
		return rewriteMap;
	}
	
	protected void putServerVar( ServerVars type, Object value )
	{
		Validate.notNull( type );
		Validate.notNull( value );
		
		serverVars.put( type, value );
	}
	
	protected void putRewriteParam( String key, String val )
	{
		rewriteMap.put( key, val );
	}
	
	protected void putRewriteParams( Map<String, String> map )
	{
		rewriteMap.putAll( map );
	}
	
	public Map<String, UploadedFile> getUploadedFiles()
	{
		return uploadedFiles;
	}
	
	public Channel getChannel()
	{
		return channel;
	}
	
	protected void addContentLength( int size )
	{
		contentSize += size;
	}
	
	public int getContentLength()
	{
		return contentSize;
	}
	
	protected void setUri( String uri )
	{
		this.uri = uri;
		
		if ( !uri.startsWith( "/" ) )
			uri = "/" + uri;
	}
	
	public boolean isWebsocketRequest()
	{
		return getURI().equals( "/fw/websocket" );
	}
	
	public String getWebSocketLocation( HttpObject req )
	{
		String location = getHost() + "/fw/websocket";
		if ( ssl )
		{
			return "wss://" + location;
		}
		else
		{
			return "ws://" + location;
		}
	}
	
	public String getBaseUrl()
	{
		String url = getDomain();
		
		if ( getSubDomain() != null && !getSubDomain().isEmpty() )
			url = getSubDomain() + "." + url;
		
		return ( ( isSecure() ) ? "https://" : "http://" ) + url;
	}
}
