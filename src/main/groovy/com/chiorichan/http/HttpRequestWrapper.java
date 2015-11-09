/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.http;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.ssl.SslHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.LogColor;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.auth.AccountAuthenticator;
import com.chiorichan.account.lang.AccountDescriptiveReason;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.logger.LogEvent;
import com.chiorichan.messaging.MessageSender;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.session.Session;
import com.chiorichan.session.SessionContext;
import com.chiorichan.session.SessionManager;
import com.chiorichan.session.SessionWrapper;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.NetworkFunc;
import com.chiorichan.util.ObjectFunc;
import com.chiorichan.util.StringFunc;
import com.chiorichan.util.Versioning;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Wraps the Netty HttpRequest and provides shortcut methods
 */
public class HttpRequestWrapper extends SessionWrapper implements SessionContext
{
	private static final Map<Thread, WeakReference<HttpRequestWrapper>> references = Maps.newConcurrentMap();
	
	/**
	 * Return maps as unmodifiable
	 */
	private static boolean unmodifiableMaps = Loader.getConfig().getBoolean( "advanced.security.unmodifiableMapsEnabled", true );
	
	/**
	 * The original Netty Channel
	 */
	private final Channel channel;
	
	protected String childDomainName = null;
	
	/**
	 * The size of the posted content
	 */
	int contentSize = 0;
	
	/**
	 * Cookie Cache
	 */
	final Set<HttpCookie> cookies = Sets.newHashSet();
	
	/**
	 * The Get Map
	 */
	final Map<String, String> getMap = Maps.newTreeMap();
	
	/**
	 * The original Netty Http Request
	 */
	private final HttpRequest http;
	
	/**
	 * Instance of LogEvent used by this request
	 */
	final LogEvent log;
	
	// Cached domain names.
	protected String parentDomainName = null;
	
	/**
	 * The Post Map
	 */
	final Map<String, String> postMap = Maps.newTreeMap();
	
	/**
	 * The time of this request
	 */
	final int requestTime;
	
	/**
	 * The paired HttpResponseWrapper
	 */
	final HttpResponseWrapper response;
	
	/**
	 * The URI Rewrite Map
	 */
	final Map<String, String> rewriteMap = Maps.newTreeMap();
	
	/**
	 * Server Cookie Cache
	 */
	final Set<HttpCookie> serverCookies = Sets.newHashSet();
	
	/**
	 * Server Variables
	 */
	Map<ServerVars, Object> serverVars = Maps.newLinkedHashMap();
	
	/**
	 * The Site associated with this request
	 */
	Site site;
	
	/**
	 * Is this a SSL request
	 */
	final boolean ssl;
	
	/**
	 * Files uploaded with this request
	 */
	final Map<String, UploadedFile> uploadedFiles = new HashMap<String, UploadedFile>();
	
	/**
	 * The requested URI
	 */
	private String uri = null;
	
	HttpRequestWrapper( Channel channel, HttpRequest http, boolean ssl, LogEvent log ) throws IOException
	{
		this.channel = channel;
		this.http = http;
		this.ssl = ssl;
		this.log = log;
		
		putRequest( this );
		
		// Set Time of this Request
		requestTime = Timings.epoch();
		
		// Create a matching HttpResponseWrapper
		response = new HttpResponseWrapper( this, log );
		
		// Get Site based on requested domain
		String domain = getParentDomain();
		site = SiteManager.INSTANCE.getSiteByDomain( domain );
		
		if ( site == null )
			site = SiteManager.INSTANCE.getDefaultSite();
		
		// Decode Get Map
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder( http.uri() );
		Map<String, List<String>> params = queryStringDecoder.parameters();
		if ( !params.isEmpty() )
			for ( Entry<String, List<String>> p : params.entrySet() )
			{
				// XXX This is overriding the key, why would their there be multiple values???
				String key = p.getKey();
				List<String> vals = p.getValue();
				for ( String val : vals )
					getMap.put( key, val );
			}
		
		// Decode Cookies
		// String var1 = URLDecoder.decode( http.headers().getAndConvert( "Cookie" ), Charsets.UTF_8.displayName() );
		String var1 = http.headers().getAndConvert( "Cookie" );
		
		// TODO Find a way to fix missing invalid stuff
		
		if ( var1 != null )
			try
			{
				Set<Cookie> var2 = CookieDecoder.decode( var1 );
				for ( Cookie cookie : var2 )
					if ( cookie.name().startsWith( "_ws" ) )
						serverCookies.add( new HttpCookie( cookie ) );
					else
						cookies.add( new HttpCookie( cookie ) );
			}
			catch ( IllegalArgumentException | NullPointerException e )
			{
				Loader.getLogger().debug( var1 );
				
				NetworkManager.getLogger().severe( "Failed to parse cookie for reason: " + e.getMessage() );
				// NetworkManager.getLogger().warning( "There was a problem decoding the request cookie.", e );
				// NetworkManager.getLogger().debug( "Cookie: " + var1 );
				// NetworkManager.getLogger().debug( "Headers: " + Joiner.on( "," ).withKeyValueSeparator( "=" ).join( http.headers() ) );
			}
		
		initServerVars();
	}
	
	public static HttpRequestWrapper getRequest()
	{
		if ( !references.containsKey( Thread.currentThread() ) || references.get( Thread.currentThread() ).get() == null )
			throw new IllegalStateException( "Thread '" + Thread.currentThread().getName() + "' does not seem to currently link to any existing http requests, please try again or notify an administrator." );
		return references.get( Thread.currentThread() ).get();
	}
	
	private static void putRequest( HttpRequestWrapper request )
	{
		references.put( Thread.currentThread(), new WeakReference<HttpRequestWrapper>( request ) );
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
			String domain = http.headers().getAndConvert( "Host" ).toLowerCase();
			
			assert ( domain != null );
			
			if ( domain.contains( ":" ) )
				domain = domain.substring( 0, domain.indexOf( ":" ) ).trim(); // Remove port number.
				
			if ( domain.toLowerCase().endsWith( "localhost" ) || domain.equalsIgnoreCase( "127.0.0.1" ) || domain.equalsIgnoreCase( getLocalIpAddr() ) || domain.toLowerCase().endsWith( getLocalHostName() ) )
				domain = "";
			
			if ( domain == null || domain.isEmpty() )
				return;
			
			if ( domain.startsWith( "." ) )
				domain = domain.substring( 1 );
			
			if ( NetworkFunc.isValidIPv4( domain ) || NetworkFunc.isValidIPv6( domain ) )
				// We can't get subdomains from IPv4 or IPv6 addresses.
				parentDomainName = domain;
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
	
	@Override
	protected void finish0()
	{
		// Do Nothing
	}
	
	public String getArgument( String key )
	{
		String val = getMap.get( key );
		
		if ( val == null && postMap != null )
			val = postMap.get( key );
		
		if ( val == null && rewriteMap != null )
			val = rewriteMap.get( key );
		
		return val;
	}
	
	public String getArgument( String key, String def )
	{
		String val = getArgument( key );
		return ( val == null ) ? def : val;
	}
	
	public boolean getArgumentBoolean( String key )
	{
		String rtn = getArgument( key, "0" ).toLowerCase();
		return StringFunc.isTrue( rtn );
	}
	
	public double getArgumentDouble( String key )
	{
		Object obj = getArgument( key, "-1.0" );
		return ObjectFunc.castToDouble( obj );
	}
	
	public int getArgumentInt( String key )
	{
		Object obj = getArgument( key, "-1" );
		return ObjectFunc.castToInt( obj );
	}
	
	public Set<String> getArgumentKeys()
	{
		Set<String> keys = Sets.newHashSet();
		keys.addAll( getMap.keySet() );
		keys.addAll( postMap.keySet() );
		keys.addAll( rewriteMap.keySet() );
		return keys;
	}
	
	public long getArgumentLong( String key )
	{
		Object obj = getArgument( key, "-1" );
		return ObjectFunc.castToLong( obj );
	}
	
	public String getBaseUrl()
	{
		String url = getDomain();
		
		if ( getSubDomain() != null && !getSubDomain().isEmpty() )
			url = getSubDomain() + "." + url;
		
		return ( ( isSecure() ) ? "https://" : "http://" ) + url;
	}
	
	public Channel getChannel()
	{
		return channel;
	}
	
	public int getContentLength()
	{
		return contentSize;
	}
	
	@Override
	public HttpCookie getCookie( String key )
	{
		for ( HttpCookie cookie : cookies )
			if ( cookie.getKey().equals( key ) )
				return cookie;
		return null;
	}
	
	@Override
	public Set<HttpCookie> getCookies()
	{
		return Collections.unmodifiableSet( cookies );
	}
	
	public String getDomain()
	{
		try
		{
			String domain = http.headers().getAndConvert( "Host" );
			domain = domain.split( "\\:" )[0];
			
			return domain;
		}
		catch ( NullPointerException e )
		{
			return "";
		}
	}
	
	public String getFullDomain()
	{
		return getFullDomain( ssl );
	}
	
	public String getFullDomain( boolean ssl )
	{
		return ( ssl ? "https://" : "http://" ) + getDomain();
	}
	
	public String getFullUrl()
	{
		return getFullUrl( ssl );
	}
	
	public String getFullUrl( boolean ssl )
	{
		return getFullDomain( ssl ) + getUri();
	}
	
	public Map<String, Object> getGetMap()
	{
		return parseMapArrays( getGetMapRaw() );
	}
	
	public Map<String, String> getGetMapRaw()
	{
		if ( unmodifiableMaps )
			return Collections.unmodifiableMap( getMap );
		
		return getMap;
	}
	
	public String getHeader( String key )
	{
		try
		{
			return http.headers().getAndConvert( key );
		}
		catch ( NullPointerException | IndexOutOfBoundsException e )
		{
			return "";
		}
	}
	
	public HttpHeaders getHeaders()
	{
		return http.headers();
	}
	
	public String getHost()
	{
		return http.headers().getAndConvert( "Host" );
	}
	
	public HttpVersion getHttpVersion()
	{
		return http.protocolVersion();
	}
	
	/**
	 * Similar to {@link #getRemoteAddr()}
	 * 
	 * @return
	 *         the remote connections IP address
	 */
	public InetAddress getInetAddr()
	{
		return getInetAddr( true );
	}
	
	/**
	 * Similar to {@link #getRemoteAddr(boolean)}
	 * 
	 * @param detectCDN
	 *            Try to detect the use of CDNs, e.g., CloudFlare, IP headers when set to false.
	 * @return
	 *         the remote connections IP address
	 */
	public InetAddress getInetAddr( boolean detectCDN )
	{
		if ( detectCDN && http.headers().contains( "CF-Connecting-IP" ) )
			try
			{
				return InetAddress.getByName( http.headers().getAndConvert( "CF-Connecting-IP" ) );
			}
			catch ( UnknownHostException e )
			{
				e.printStackTrace();
				return null;
			}
		
		return ( ( InetSocketAddress ) channel.remoteAddress() ).getAddress();
	}
	
	/**
	 * Similar to {@link #getRemoteAddr(boolean)} except defaults to true
	 * 
	 * @return
	 *         the remote connections IP address as a string
	 */
	@Override
	public String getIpAddr()
	{
		return getIpAddr( true );
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
	public String getIpAddr( boolean detectCDN )
	{
		if ( detectCDN && http.headers().contains( "CF-Connecting-IP" ) )
			return http.headers().getAndConvert( "CF-Connecting-IP" );
		
		return ( ( InetSocketAddress ) channel.remoteAddress() ).getAddress().getHostAddress();
	}
	
	public String getLocalHostName()
	{
		return ( ( InetSocketAddress ) channel.localAddress() ).getHostName();
	}
	
	public String getLocalIpAddr()
	{
		return ( ( InetSocketAddress ) channel.localAddress() ).getAddress().getHostAddress();
	}
	
	public int getLocalPort()
	{
		return ( ( InetSocketAddress ) channel.localAddress() ).getPort();
	}
	
	public HttpMethod getMethod()
	{
		return http.method();
	}
	
	public String getMethodString()
	{
		return http.method().toString();
	}
	
	public HttpRequest getOriginal()
	{
		return http;
	}
	
	public String getParameter( String key )
	{
		return null;
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
	
	public Map<String, Object> getPostMap()
	{
		return parseMapArrays( getPostMapRaw() );
	}
	
	public Map<String, String> getPostMapRaw()
	{
		if ( unmodifiableMaps )
			return Collections.unmodifiableMap( postMap );
		
		return postMap;
	}
	
	public String getRemoteHostname()
	{
		return ( ( InetSocketAddress ) channel.remoteAddress() ).getHostName();
	}
	
	public int getRemotePort()
	{
		return ( ( InetSocketAddress ) channel.remoteAddress() ).getPort();
	}
	
	public String getRequestHost()
	{
		return getHeader( "Host" );
	}
	
	public Map<String, Object> getRequestMap()
	{
		return parseMapArrays( getRequestMapRaw() );
	}
	
	public Map<String, String> getRequestMapRaw()
	{
		Map<String, String> requestMap = new HashMap<String, String>();
		
		if ( getMap != null )
			requestMap.putAll( getMap );
		
		if ( postMap != null )
			requestMap.putAll( postMap );
		
		if ( rewriteMap != null )
			requestMap.putAll( rewriteMap );
		
		if ( unmodifiableMaps )
			return Collections.unmodifiableMap( requestMap );
		
		return requestMap;
	}
	
	public int getRequestTime()
	{
		return requestTime;
	}
	
	public HttpResponseWrapper getResponse()
	{
		return response;
	}
	
	public Map<String, String> getRewriteMap()
	{
		if ( unmodifiableMaps )
			return Collections.unmodifiableMap( rewriteMap );
		
		return rewriteMap;
	}
	
	@Override
	protected HttpCookie getServerCookie( String key )
	{
		for ( HttpCookie cookie : serverCookies )
			if ( cookie.getKey().equals( key ) )
				return cookie;
		
		return null;
	}
	
	public Map<String, Object> getServerStrings()
	{
		Map<String, Object> server = Maps.newLinkedHashMap();
		
		// Adds server variables to map in default, lower case, and upper case variations.
		for ( Map.Entry<ServerVars, Object> en : serverVars.entrySet() )
		{
			server.put( en.getKey().name().toLowerCase(), en.getValue() );
			server.put( en.getKey().name().toUpperCase(), en.getValue() );
			server.put( en.getKey().name(), en.getValue() );
		}
		
		if ( unmodifiableMaps )
			return Collections.unmodifiableMap( server );
		
		return server;
	}
	
	public Map<ServerVars, Object> getServerVars()
	{
		if ( unmodifiableMaps )
			return Collections.unmodifiableMap( serverVars );
		return serverVars;
	}
	
	@Override
	public Site getSite()
	{
		if ( site == null )
			site = SiteManager.INSTANCE.getDefaultSite();
		return site;
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
	
	public Map<String, UploadedFile> getUploadedFiles()
	{
		return Collections.unmodifiableMap( uploadedFiles );
	}
	
	Map<String, UploadedFile> getUploadedFilesRaw()
	{
		return uploadedFiles;
	}
	
	public String getUri()
	{
		if ( uri == null )
		{
			uri = http.uri();
			
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
		}
		
		return uri;
	}
	
	public String getUserAgent()
	{
		return getHeader( "User-Agent" );
	}
	
	public String getWebSocketLocation( HttpObject req )
	{
		String location = getHost() + "/fw/websocket";
		if ( ssl )
			return "wss://" + location;
		else
			return "ws://" + location;
	}
	
	public boolean hasArgument( String key )
	{
		return getMap.containsKey( key ) || postMap.containsKey( key ) || rewriteMap.containsKey( key );
	}
	
	/**
	 * Initializes the serverVars with initial information from this request
	 */
	private void initServerVars()
	{
		putServerVarSafe( ServerVars.SERVER_SOFTWARE, Versioning.getProduct() );
		putServerVarSafe( ServerVars.SERVER_VERSION, Versioning.getVersion() );
		putServerVarSafe( ServerVars.SERVER_ADMIN, Loader.getConfig().getString( "server.admin", "me@chiorichan.com" ) );
		putServerVarSafe( ServerVars.SERVER_SIGNATURE, Versioning.getProduct() + " Version " + Versioning.getVersion() );
		putServerVarSafe( ServerVars.HTTP_VERSION, http.protocolVersion() );
		putServerVarSafe( ServerVars.HTTP_ACCEPT, getHeader( "Accept" ) );
		putServerVarSafe( ServerVars.HTTP_USER_AGENT, getUserAgent() );
		putServerVarSafe( ServerVars.HTTP_CONNECTION, getHeader( "Connection" ) );
		putServerVarSafe( ServerVars.HTTP_HOST, getLocalHostName() );
		putServerVarSafe( ServerVars.HTTP_ACCEPT_ENCODING, getHeader( "Accept-Encoding" ) );
		putServerVarSafe( ServerVars.HTTP_ACCEPT_LANGUAGE, getHeader( "Accept-Language" ) );
		putServerVarSafe( ServerVars.HTTP_X_REQUESTED_WITH, getHeader( "X-requested-with" ) );
		putServerVarSafe( ServerVars.REMOTE_HOST, getRemoteHostname() );
		putServerVarSafe( ServerVars.REMOTE_ADDR, getIpAddr() );
		putServerVarSafe( ServerVars.REMOTE_PORT, getRemotePort() );
		putServerVarSafe( ServerVars.REQUEST_TIME, getRequestTime() );
		putServerVarSafe( ServerVars.REQUEST_URI, getUri() );
		putServerVarSafe( ServerVars.CONTENT_LENGTH, getContentLength() );
		// putServerVarSafe( ServerVars.AUTH_TYPE, getAuthType() ); -- Implement Authentication
		putServerVarSafe( ServerVars.SERVER_IP, getLocalIpAddr() );
		putServerVarSafe( ServerVars.SERVER_NAME, Versioning.getProductSimple() );
		putServerVarSafe( ServerVars.SERVER_PORT, getLocalPort() );
		putServerVarSafe( ServerVars.HTTPS, isSecure() );
		
		// TODO These need initializing once known
		putServerVarSafe( ServerVars.DOCUMENT_ROOT, Loader.getWebRoot() );
		putServerVarSafe( ServerVars.SESSION, null );
	}
	
	/**
	 * Tries to check the "X-requested-with" header.
	 * Not a guaranteed method to determined if a request was made with AJAX since this header is not always set.
	 * 
	 * @return Was the request made with AJAX
	 */
	public boolean isAjaxRequest()
	{
		return getHeader( "X-requested-with" ) == null ? false : getHeader( "X-requested-with" ).equals( "XMLHttpRequest" );
	}
	
	public boolean isCDN()
	{
		return http.headers().contains( "CF-Connecting-IP" );
	}
	
	public boolean isSecure()
	{
		return ( channel.pipeline().get( SslHandler.class ) != null );
	}
	
	public boolean isWebsocketRequest()
	{
		return "/fw/websocket".equals( getUri() );
	}
	
	public HttpMethod method()
	{
		return http.method();
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
				var = e.getKey();
			
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
							cnt++;
						key = "" + cnt;
					}
					
					map.put( key, val );
				}
				else if ( key == null )
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
			else if ( key == null )
				result.put( e.getKey(), e.getValue() );
			else
			{
				if ( key.isEmpty() )
					key = "0";
				
				Map<String, String> hash = Maps.newLinkedHashMap();
				hash.put( key, val );
				result.put( var, hash );
			}
		}
		
		return result;
	}
	
	protected void putAllGetMap( Map<String, String> map )
	{
		getMap.putAll( map );
	}
	
	protected void putAllPostMap( Map<String, String> map )
	{
		postMap.putAll( map );
	}
	
	protected void putGetMap( String key, String value )
	{
		getMap.put( key, value );
	}
	
	protected void putPostMap( String key, String value )
	{
		postMap.put( key, value );
	}
	
	protected void putRewriteParam( String key, String val )
	{
		rewriteMap.put( key, val );
	}
	
	protected void putRewriteParams( Map<String, String> map )
	{
		rewriteMap.putAll( map );
	}
	
	protected void putServerVar( ServerVars type, Object value )
	{
		Validate.notNull( type );
		Validate.notNull( value );
		
		serverVars.put( type, value );
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
	
	protected void putUpload( String name, UploadedFile uploadedFile )
	{
		uploadedFiles.put( name, uploadedFile );
	}
	
	// XXX Better Implement
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
		if ( !getSession().isLoginPresent() )
			getResponse().sendLoginPage();
		
		if ( permission != null )
			if ( !getSession().checkPermission( permission ).isTrue() )
				getResponse().sendError( HttpCode.HTTP_FORBIDDEN, "You must have the permission `" + permission + "` in order to view this page!" );
	}
	
	@Override
	public void sendMessage( MessageSender sender, Object... objs )
	{
		// Do Nothing
	}
	
	@Override
	public void sendMessage( Object... objs )
	{
		// Do Nothing
	}
	
	@Override
	protected void sessionStarted()
	{
		getBinding().setVariable( "request", this );
		getBinding().setVariable( "response", getResponse() );
		
		String loginForm = getSite().getYaml().getString( "scripts.login-form", "/login" );
		
		Session session = getSession();
		
		if ( getArgument( "logout" ) != null )
		{
			AccountResult result = getSession().logout();
			
			if ( result.isSuccess() )
			{
				getResponse().sendRedirect( loginForm + "?msg=" + result.getMessage() );
				return;
			}
		}
		
		// TODO Implement One Time Tokens
		
		String username = getArgument( "user" );
		String password = getArgument( "pass" );
		boolean remember = getArgumentBoolean( "remember" );
		String target = getArgument( "target" );
		
		String loginPost = ( target == null || target.isEmpty() ) ? getSite().getYaml().getString( "scripts.login-post", "/" ) : target;
		
		if ( loginPost.isEmpty() )
			loginPost = "/";
		
		if ( username != null && password != null )
			try
			{
				AccountResult result = getSession().loginWithException( AccountAuthenticator.PASSWORD, username, password );
				
				Account acct = result.getAccountWithException();
				
				session.remember( remember );
				
				SessionManager.getLogger().info( LogColor.GREEN + "Successful Login: [id='" + acct.getId() + "',siteId='" + acct.getSiteId() + "',authenticator='plaintext']" );
				getResponse().sendRedirect( loginPost );
			}
			catch ( AccountException e )
			{
				AccountResult result = e.getResult();
				
				String msg = result.getFormattedMessage();
				
				if ( !result.isIgnorable() && result.hasCause() )
				{
					result.getCause().printStackTrace();
					msg = result.getCause().getMessage();
				}
				
				AccountManager.getLogger().warning( LogColor.RED + "Failed Login [id='" + username + "',hasPassword='" + ( password != null && !password.isEmpty() ) + "',authenticator='plaintext'`,reason='" + msg + "']" );
				getResponse().sendRedirect( loginForm + "?msg=" + result.getMessage() + ( ( target == null || target.isEmpty() ) ? "" : "&target=" + target ) );
			}
			catch ( Throwable t )
			{
				AccountManager.getLogger().severe( "Login has thrown an internal server error", t );
				getResponse().sendRedirect( loginForm + "?msg=" + AccountDescriptiveReason.INTERNAL_ERROR.getMessage() + ( ( target == null || target.isEmpty() ) ? "" : "&target=" + target ) );
			}
		else if ( session.isLoginPresent() )
		{
			// XXX Should we revalidate logins with each request? It could be something worth considering for extra security. Maybe a config option?
			
			/*
			 * Maybe make this a server configuration option, e.g., sessions.revalidateLogins
			 * 
			 * try
			 * {
			 * session.currentAccount.reloadAndValidate(); // <- Is this being overly redundant?
			 * Loader.getLogger().info( ChatColor.GREEN + "Current Login `Username \"" + session.currentAccount.getName() + "\", Password \"" + session.currentAccount.getMetaData().getPassword() + "\", UserId \"" +
			 * session.currentAccount.getAccountId() + "\", Display Name \"" + session.currentAccount.getDisplayName() + "\"`" );
			 * }
			 * catch ( LoginException e )
			 * {
			 * session.currentAccount = null;
			 * Loader.getLogger().warning( ChatColor.GREEN + "Login Failed `There was a login present but it failed validation with error: " + e.getMessage() + "`" );
			 * }
			 */
		}
		
		// Will we ever be using a session on more than one domains?
		if ( !getParentDomain().isEmpty() && session.getSessionCookie() != null && !session.getSessionCookie().getDomain().isEmpty() )
			if ( !session.getSessionCookie().getDomain().endsWith( getParentDomain() ) )
				NetworkManager.getLogger().warning( "The site `" + site.getSiteId() + "` specifies the session cookie domain as `" + session.getSessionCookie().getDomain() + "` but the request was made on parent domain `" + getParentDomain() + "`. The session will not remain persistent." );
	}
	
	protected void setSite( Site site )
	{
		Validate.notNull( site );
		this.site = site;
	}
	
	void setUri( String uri )
	{
		this.uri = uri;
		
		if ( !uri.startsWith( "/" ) )
			uri = "/" + uri;
	}
}
