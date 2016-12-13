/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * <p>
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.http;

import com.chiorichan.lang.ExceptionReport;
import com.chiorichan.lang.MapCollisionException;
import com.chiorichan.util.*;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpHeaderNames;
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
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.lang3.Validate;

import com.chiorichan.AppConfig;
import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.auth.AccountAuthenticator;
import com.chiorichan.account.lang.AccountDescriptiveReason;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.lang.EnumColor;
import com.chiorichan.logger.experimental.LogEvent;
import com.chiorichan.messaging.MessageSender;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.session.Session;
import com.chiorichan.session.SessionContext;
import com.chiorichan.session.SessionManager;
import com.chiorichan.session.SessionWrapper;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.site.SiteMapping;
import com.chiorichan.tasks.Timings;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Wraps the Netty HttpRequest and provides shortcut methods
 */
public class HttpRequestWrapper extends SessionWrapper implements SessionContext
{
	private static final Map<Thread, WeakReference<HttpRequestWrapper>> references = Maps.newConcurrentMap();

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
	 * The original Netty Channel
	 */
	private final Channel channel;

	/**
	 * The size of the posted content
	 */
	int contentSize = 0;

	/**
	 * Cookie Cache
	 */
	final Set<HttpCookie> cookies = new HashSet<>();

	/**
	 * The Get Map
	 */
	final Map<String, String> getMap = new TreeMap<>();

	/**
	 * The {@link HttpHandler} for this request
	 */
	final HttpHandler handler;

	/**
	 * The original Netty Http Request
	 */
	private final HttpRequest http;

	/**
	 * Instance of LogEvent used by this request
	 */
	final LogEvent log;

	/**
	 * The Post Map
	 */
	final Map<String, String> postMap = new TreeMap<>();

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
	final Map<String, String> rewriteMap = new TreeMap<>();

	/**
	 * Server Cookie Cache
	 */
	final Set<HttpCookie> serverCookies = new HashSet<>();

	/**
	 * Server Variables
	 */
	HttpVariableMapper vars = new HttpVariableMapper();

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

	/**
	 * The requested root domain
	 */
	private String parentDomain;

	/**
	 * The requested child domain
	 */
	private String childDomain = "";

	private boolean nonceProcessed = false;

	private HttpAuthenticator auth = null;

	HttpRequestWrapper( Channel channel, HttpRequest http, HttpHandler handler, boolean ssl, LogEvent log ) throws IOException
	{
		this.channel = channel;
		this.http = http;
		this.handler = handler;
		this.ssl = ssl;
		this.log = log;

		putRequest( this );

		// Set Time of this Request
		requestTime = Timings.epoch();

		// Create a matching HttpResponseWrapper
		response = new HttpResponseWrapper( this, log );

		String host = getHostDomain();

		if ( host == null || host.length() == 0 )
		{
			parentDomain = "";
			site = SiteManager.instance().getDefaultSite();
		}
		else if ( NetworkFunc.isValidIPv4( host ) || NetworkFunc.isValidIPv6( host ) )
		{
			parentDomain = host;
			site = SiteManager.instance().getSiteByIp( host ).get( 0 );
		}
		else
		{
			Pair<String, SiteMapping> match = SiteMapping.get( host );

			if ( match == null )
			{
				parentDomain = host;
				site = SiteManager.instance().getDefaultSite();
			}
			else
			{
				parentDomain = match.getKey();
				Namespace hostNamespace = new Namespace( host );
				Namespace parentNamespace = new Namespace( parentDomain );
				Namespace childNamespace = hostNamespace.subNamespace( 0, hostNamespace.getNodeCount() - parentNamespace.getNodeCount() );
				assert hostNamespace.getNodeCount() - parentNamespace.getNodeCount() == childNamespace.getNodeCount();
				childDomain = childNamespace.getNamespace();

				site = match.getValue().getSite();
			}
		}

		if ( site == null )
			site = SiteManager.instance().getDefaultSite();

		if ( site == SiteManager.instance().getDefaultSite() && getUri().startsWith( "/~" ) )
		{
			List<String> uris = Splitter.on( "/" ).omitEmptyStrings().splitToList( getUri() );
			String siteId = uris.get( 0 ).substring( 1 );

			Site siteTmp = SiteManager.instance().getSiteById( siteId );
			if ( !siteId.equals( "wisp" ) && siteTmp != null )
			{
				site = siteTmp;
				uri = "/" + Joiner.on( "/" ).join( uris.subList( 1, uris.size() ) );

				// TODO Implement both a virtual and real URI for use in redirects and url_to()
				String[] domains = site.getDomains().keySet().toArray( new String[0] );
				parentDomain = domains.length == 0 ? host : domains[0];
			}
		}

		// log.log( Level.INFO, "SiteId: " + site.getSiteId() + ", ParentDomain: " + parentDomain + ", ChildDomain: " + childDomain );

		try
		{
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
		}
		catch ( IllegalStateException e )
		{
			log.log( Level.SEVERE, "Failed to decode the GET map because " + e.getMessage() );
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
				//NetworkManager.getLogger().debug( var1 );

				NetworkManager.getLogger().severe( "Failed to parse cookie for reason: " + e.getMessage() );
				// NetworkManager.getLogger().warning( "There was a problem decoding the request cookie.", e );
				// NetworkManager.getLogger().debug( "Cookie: " + var1 );
				// NetworkManager.getLogger().debug( "Headers: " + Joiner.on( "," ).withKeyValueSeparator( "=" ).join( http.headers() ) );
			}

		initServerVars();
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
		return val == null ? def : val;
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

	public HttpAuthenticator getAuth()
	{
		if ( auth == null )
			initAuthorization();
		return auth;
	}

	public String getBaseUrl()
	{
		String url = getDomain();

		if ( getSubdomain() != null && !getSubdomain().isEmpty() )
			url = getSubdomain() + "." + url;

		return ( isSecure() ? "https://" : "http://" ) + url;
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

	public Set<HttpCookie> getServerCookies()
	{
		return Collections.unmodifiableSet( serverCookies );
	}

	public String getDomain()
	{
		return parentDomain == null ? "" : parentDomain;
	}

	public String getFullDomain()
	{
		return getFullDomain( null, ssl );
	}

	public String getFullDomain( boolean ssl )
	{
		return getFullDomain( null, ssl );
	}

	public String getFullDomain( String subdomain )
	{
		return getFullDomain( subdomain, ssl );
	}

	public String getFullDomain( String subdomain, boolean ssl )
	{
		return ( ssl ? "https://" : "http://" ) + ( subdomain == null || subdomain.isEmpty() ? "" : subdomain + "." ) + getDomain() + "/";
	}

	public String getFullUrl()
	{
		return getFullUrl( null, ssl );
	}

	public String getFullUrl( boolean ssl )
	{
		return getFullUrl( null, ssl );
	}

	public String getFullUrl( String subdomain )
	{
		return getFullUrl( subdomain, ssl );
	}

	public String getFullUrl( String subdomain, boolean ssl )
	{
		return getFullDomain( subdomain, ssl ) + getUri().substring( 1 );
	}

	public Map<String, Object> getGetMap()
	{
		return parseMapArrays( getGetMapRaw() );
	}

	public Map<String, String> getGetMapRaw()
	{
		return getMap;
	}

	public String getHeader( CharSequence key )
	{
		try
		{
			return http.headers().getAndConvert( key );
		}
		catch ( NullPointerException | IndexOutOfBoundsException e )
		{
			return null;
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

	private String getHostDomain()
	{
		if ( http.headers().contains( "Host" ) )
			return http.headers().getAndConvert( "Host" ).split( "\\:" )[0];
		return null;
	}

	public HttpVersion getHttpVersion()
	{
		return http.protocolVersion();
	}

	/**
	 * Similar to {@link #getInetAddr()}
	 *
	 * @return the remote connections IP address
	 */
	public InetAddress getInetAddr()
	{
		return getInetAddr( true );
	}

	/**
	 * Similar to {@link #getInetAddr(boolean)}
	 *
	 * @param detectCDN Try to detect the use of CDNs, e.g., CloudFlare, IP headers when set to false.
	 * @return the remote connections IP address
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

	public WebInterpreter getInterpreter()
	{
		return handler.getInterpreter();
	}

	/**
	 * Similar to {@link #getIpAddr(boolean)} except defaults to true
	 *
	 * @return the remote connections IP address as a string
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
	 * @param detectCDN Try to detect the use of CDNs, e.g., CloudFlare, IP headers when set to false.
	 * @return the remote connections IP address as a string
	 */
	public String getIpAddr( boolean detectCDN )
	{
		// TODO Implement other CDNs
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

	@Override
	public Site getLocation()
	{
		if ( site == null )
			site = SiteManager.instance().getDefaultSite();
		return site;
	}

	public HttpRequest getOriginal()
	{
		return http;
	}

	public String getParameter( String key )
	{
		return null;
	}

	public Map<String, Object> getPostMap()
	{
		return parseMapArrays( getPostMapRaw() );
	}

	public Map<String, String> getPostMapRaw()
	{
		return postMap;
	}

	public String getQuery()
	{
		if ( getMap.isEmpty() )
			return "";
		return "?" + Joiner.on( "&" ).withKeyValueSeparator( "=" ).join( getMap );
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

	public Map<String, Object> getRequestMap() throws Exception
	{
		return parseMapArrays( getRequestMapRaw() );
	}

	public Map<String, String> getRequestMapRaw() throws Exception
	{
		Map<String, String> requestMap = new TreeMap<>();

		if ( getMap != null )
			mergeMaps( requestMap, getMap );

		if ( postMap != null )
			mergeMaps( requestMap, postMap );

		if ( rewriteMap != null )
			mergeMaps( requestMap, rewriteMap );

		return requestMap;
	}

	public <T extends Object> void mergeMaps( Map<String, T> desc, Map<String, T>... maps ) throws Exception
	{
		if ( maps == null || maps.length == 0 )
			return;

		// It's important to note that each of these maps will overwrite the current map. If it's rewriteMap contains a key that exists in getMap, the ladder will be overwritten.

		for ( Map<String, T> map : maps )
		{
			boolean hadConflicts = false;

			for ( Entry<String, T> entry : map.entrySet() )
			{
				if ( desc.containsKey( entry.getKey() ) )
					hadConflicts = true;
				desc.put( entry.getKey(), entry.getValue() );
			}

			if ( hadConflicts )
			{
				MapCollisionException e = new MapCollisionException();
				ExceptionReport.throwExceptions( e );
				log.exceptions( e );
			}
		}
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
		return rewriteMap;
	}

	public HttpVariableMapper getServer()
	{
		return vars;
	}

	@Override
	protected HttpCookie getServerCookie( String key )
	{
		for ( HttpCookie cookie : serverCookies )
			if ( cookie.getKey().equals( key ) )
				return cookie;

		return null;
	}

	public Site getSite()
	{
		return getLocation();
	}

	public String getSubdomain()
	{
		return childDomain == null ? "" : childDomain;
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

	private void initAuthorization()
	{
		if ( auth == null && getHeader( HttpHeaderNames.AUTHORIZATION ) != null )
			auth = new HttpAuthenticator( this );
	}

	/**
	 * Initializes the serverVars with initial information from this request
	 */
	private void initServerVars()
	{
		vars.put( ServerVars.SERVER_SOFTWARE, Versioning.getProduct() );
		vars.put( ServerVars.SERVER_VERSION, Versioning.getVersion() );
		vars.put( ServerVars.SERVER_ADMIN, AppConfig.get().getString( "server.admin", "me@chiorichan.com" ) );
		vars.put( ServerVars.SERVER_SIGNATURE, Versioning.getProduct() + " Version " + Versioning.getVersion() );
		vars.put( ServerVars.HTTP_VERSION, http.protocolVersion() );
		vars.put( ServerVars.HTTP_ACCEPT, getHeader( "Accept" ) );
		vars.put( ServerVars.HTTP_USER_AGENT, getUserAgent() );
		vars.put( ServerVars.HTTP_CONNECTION, getHeader( "Connection" ) );
		vars.put( ServerVars.HTTP_HOST, getLocalHostName() );
		vars.put( ServerVars.HTTP_ACCEPT_ENCODING, getHeader( "Accept-Encoding" ) );
		vars.put( ServerVars.HTTP_ACCEPT_LANGUAGE, getHeader( "Accept-Language" ) );
		vars.put( ServerVars.HTTP_X_REQUESTED_WITH, getHeader( "X-requested-with" ) );
		vars.put( ServerVars.REMOTE_HOST, getRemoteHostname() );
		vars.put( ServerVars.REMOTE_ADDR, getIpAddr() );
		vars.put( ServerVars.REMOTE_PORT, getRemotePort() );
		vars.put( ServerVars.REQUEST_TIME, getRequestTime() );
		vars.put( ServerVars.REQUEST_URI, getUri() );
		vars.put( ServerVars.CONTENT_LENGTH, getContentLength() );
		vars.put( ServerVars.SERVER_IP, getLocalIpAddr() );
		vars.put( ServerVars.SERVER_NAME, Versioning.getProductSimple() );
		vars.put( ServerVars.SERVER_PORT, getLocalPort() );
		vars.put( ServerVars.HTTPS, isSecure() );
		vars.put( ServerVars.DOCUMENT_ROOT, Loader.getWebRoot() );
		vars.put( ServerVars.SESSION, this );

		if ( getAuth() != null )
		{
			// Implement authorization as an optional builtin manageable feature, e.g., .htdigest.

			if ( auth.isDigest() )
				vars.put( ServerVars.AUTH_DIGEST, getAuth().getDigest() );

			if ( auth.isBasic() )
			{
				vars.put( ServerVars.AUTH_USER, getAuth().getUsername() );
				vars.put( ServerVars.AUTH_PW, getAuth().getPassword() );
			}

			vars.put( ServerVars.AUTH_TYPE, getAuth().getType() );
		}
	}

	/**
	 * Tries to check the "X-requested-with" header.
	 * Not a guaranteed method to determine if a request was made with AJAX since this header is not always set.
	 *
	 * @return Was the request made with AJAX
	 */
	public boolean isAjaxRequest()
	{
		return getHeader( "X-requested-with" ) == null ? false : getHeader( "X-requested-with" ).equals( "XMLHttpRequest" );
	}

	public boolean isCDN()
	{
		// TODO Implement additional CDN detection methods
		return http.headers().contains( "CF-Connecting-IP" );
	}

	public boolean isSecure()
	{
		return channel.pipeline().get( SslHandler.class ) != null;
	}

	public boolean isWebsocketRequest()
	{
		return "/fw/websocket".equals( getUri() );
	}

	public HttpMethod method()
	{
		return http.method();
	}

	public String methodString()
	{
		return http.method().toString();
	}

	public boolean nonceProcessed()
	{
		return nonceProcessed;
	}

	void nonceProcessed( boolean processed )
	{
		nonceProcessed = processed;
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
	public void sessionStarted()
	{
		getBinding().setVariable( "request", this );
		getBinding().setVariable( "response", getResponse() );
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

	protected boolean validateLogins()
	{
		Session session = getSession();

		if ( getArgument( "logout" ) != null )
		{
			AccountResult result = getSession().logout();

			if ( result.isSuccess() )
			{
				getResponse().sendLoginPage( result.getMessage() );
				return true;
			}
		}

		// TODO Implement One Time Tokens

		String username = getArgument( "user" );
		String password = getArgument( "pass" );
		boolean remember = getArgumentBoolean( "remember" );
		String target = getArgument( "target" );

		String loginPost = target == null || target.isEmpty() ? getLocation().getConfig().getString( "scripts.login-post", "/" ) : target;

		if ( loginPost.isEmpty() )
			loginPost = "/";

		if ( username != null && password != null )
		{
			try
			{
				if ( !ssl )
					AccountManager.getLogger().warning( "It is highly recommended that account logins are submitted over SSL. Without SSL, passwords are at great risk." );

				if ( !nonceProcessed() && AppConfig.get().getBoolean( "accounts.requireLoginWithNonce" ) )
					throw new AccountException( AccountDescriptiveReason.NONCE_REQUIRED, username );

				AccountResult result = getSession().loginWithException( AccountAuthenticator.PASSWORD, username, password );

				Account acct = result.getAccountWithException();

				session.remember( remember );

				SessionManager.getLogger().info( EnumColor.GREEN + "Successful Login: [id='" + acct.getId() + "',siteId='" + ( acct.getLocation() == null ? null : acct.getLocation().getId() ) + "',authenticator='plaintext']" );

				if ( site.getLoginPost() != null )
					getResponse().sendRedirect( site.getLoginPost() );
				else
					getResponse().sendLoginPage( "Your have been successfully logged in!", "success" );
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

				AccountManager.getLogger().warning( EnumColor.RED + "Failed Login [id='" + username + "',hasPassword='" + ( password != null && password.length() > 0 ) + "',authenticator='plaintext',reason='" + msg + "']" );
				getResponse().sendLoginPage( result.getMessage(), null, target );
			}
			catch ( Throwable t )
			{
				AccountManager.getLogger().severe( "Login has thrown an internal server error", t );
				getResponse().sendLoginPage( AccountDescriptiveReason.INTERNAL_ERROR.getMessage(), null, target );
			}
			return true;
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
		if ( !getDomain().isEmpty() && session.getSessionCookie() != null && !session.getSessionCookie().getDomain().isEmpty() )
			if ( !session.getSessionCookie().getDomain().endsWith( getDomain() ) )
				NetworkManager.getLogger().warning( "The site `" + site.getId() + "` specifies the session cookie domain as `" + session.getSessionCookie().getDomain() + "` but the request was made on domain `" + getDomain() + "`. The session will not remain persistent." );

		return false;
	}
}
