/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.bus.events.server.ServerVars;
import com.chiorichan.framework.Site;
import com.chiorichan.http.session.SessionProvider;
import com.chiorichan.util.Common;
import com.chiorichan.util.StringUtil;
import com.chiorichan.util.Versioning;
import com.google.common.collect.Maps;

public class HttpRequestWrapper
{
	protected Map<ServerVars, Object> serverVars = Maps.newLinkedHashMap();
	protected Site currentSite;
	protected SessionProvider sess = null;
	protected HttpResponseWrapper response;
	protected Map<String, String> getMap, postMap,
			rewriteMap = Maps.newLinkedHashMap();
	protected int requestTime = 0;
	protected Map<String, UploadedFile> uploadedFiles = new HashMap<String, UploadedFile>();
	
	protected HttpRequest http;
	protected Channel channel;
	
	protected HttpRequestWrapper(Channel _channel, HttpRequest _http) throws IOException
	{
		channel = _channel;
		http = _http;
		
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
		
		// try
		// {
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
		
		/*
		 * if ( http.getRequestBody().available() > 0 )
		 * {
		 * if ( MultiPartRequestParser.isMultipart( this ) )
		 * { // Multipart Request - File Upload Usually.
		 * try
		 * {
		 * postMap = new HashMap<String, String>();
		 * MultiPartRequestParser parser = new MultiPartRequestParser( this );
		 * File tmpFileDirectory = ( currentSite != null ) ? currentSite.getTempFileDirectory() : Loader.getTempFileDirectory();
		 * if ( !tmpFileDirectory.exists() )
		 * tmpFileDirectory.mkdirs();
		 * if ( !tmpFileDirectory.isDirectory() )
		 * throw new IOException( "The temp directory specified in the server configs is not a directory, File Uploads will continue to fail until this problem is resolved." );
		 * if ( !tmpFileDirectory.canWrite() )
		 * throw new IOException( "The temp directory specified in the server configs is not writable, File Uploads will continue to fail until this problem is resolved." );
		 * Part part;
		 * while ( ( part = parser.readNextPart() ) != null )
		 * {
		 * String name = part.getName();
		 * if ( name == null )
		 * {
		 * throw new IOException( "Malformed input: parameter name missing (known Opera 7 bug)" );
		 * }
		 * if ( part.isParam() )
		 * {
		 * ParamPart paramPart = (ParamPart) part;
		 * String value = paramPart.getStringValue();
		 * /*
		 * Vector existingValues = (Vector) parameters.get( name );
		 * if ( existingValues == null )
		 * {
		 * existingValues = new Vector();
		 * postMap.put( name, existingValues );
		 * }
		 * existingValues.addElement( value );
		 * XXX Should we use vectors in our Get and Post Maps?
		 * postMap.put( name, value );
		 * }
		 * else if ( part.isFile() )
		 * {
		 * FilePart filePart = (FilePart) part;
		 * String fileName = filePart.getFileName();
		 * if ( fileName != null )
		 * {
		 * long size = -1;
		 * String msg = "The file uploaded successfully.";
		 * try
		 * {
		 * size = filePart.writeTo( tmpFileDirectory );
		 * }
		 * catch ( IOException e )
		 * {
		 * msg = e.getMessage();
		 * }
		 * String tmpFileName = filePart.getTmpFileName();
		 * File newFile = new File( tmpFileDirectory, tmpFileName );
		 * newFile.deleteOnExit();
		 * uploadedFiles.put( name, new UploadedFile( newFile, fileName, size, msg ) );
		 * }
		 * }
		 * }
		 * }
		 * catch ( HttpErrorException e )
		 * {
		 * response.sendError( e.getHttpCode(), null, e.getReason() );
		 * }
		 * catch ( IOException e )
		 * {
		 * response.sendException( e );
		 * }
		 * }
		 * else
		 * {
		 * byte[] queryBytes = new byte[http.getRequestBody().available()];
		 * IOUtils.readFully( http.getRequestBody(), queryBytes );
		 * postMap = queryToMap( new String( queryBytes ) );
		 * }
		 * }
		 * }
		 * catch ( IOException e )
		 * {
		 * Loader.getLogger().severe( "There was a severe error reading the " + http.getMethod().toString().toUpperCase() + " query.", e );
		 * response.sendException( e );
		 * }
		 */
	}
	
	protected void initSession()
	{
		sess = Loader.getSessionManager().find( this );
		sess.handleUserProtocols();
	}
	
	protected Map<String, String> queryToMap( String query ) throws UnsupportedEncodingException
	{
		Map<String, String> result = new HashMap<String, String>();
		
		if ( query == null )
			return result;
		
		for ( String param : query.split( "&" ) )
		{
			String pair[] = param.split( "=" );
			try
			{
				if ( pair.length > 1 )
					result.put( URLDecoder.decode( StringUtil.trimEnd( pair[0], '%' ), "ISO-8859-1" ), URLDecoder.decode( StringUtil.trimEnd( pair[1], '%' ), "ISO-8859-1" ) );
				else if ( pair.length == 1 )
					result.put( URLDecoder.decode( StringUtil.trimEnd( pair[0], '%' ), "ISO-8859-1" ), "" );
			}
			catch ( IllegalArgumentException e )
			{
				Loader.getLogger().warning( "Malformed URL exception was thrown, key: `" + pair[0] + "`, val: '" + pair[1] + "'" );
			}
		}
		return result;
	}
	
	public Boolean getArgumentBoolean( String key )
	{
		String rtn = getArgument( key, "0" ).toLowerCase();
		return ( rtn.equals( "true" ) || rtn.equals( "1" ) );
	}
	
	public String getArgument( String key )
	{
		return getArgument( key, "" );
	}
	
	public String getArgument( String key, String def )
	{
		return getArgument( key, "", false );
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
	
	protected SessionProvider getSessionNoWarning()
	{
		return sess;
	}
	
	public SessionProvider getSession()
	{
		if ( sess == null )
			initSession();
		
		return sess;
	}
	
	public HttpResponseWrapper getResponse()
	{
		return response;
	}
	
	/**
	 * getPath() method was removing the beginning of a uri if it started with a double slash ie. //pages/about.gsp
	 * This method might need some work up to make it more reliable.
	 */
	public String getURI()
	{
		/*String uri = http.getUri().toString();
		
		if ( uri.contains( "?" ) )
			uri = uri.substring( 0, uri.indexOf( "?" ) );
		
		if ( !uri.startsWith( "/" ) )
			uri = "/" + uri;
		
		return uri;*/
		
		return sanitizeUri( http.getUri() );
	}
	
	private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
	
	private static String sanitizeUri( String uri )
	{
		// Decode the path.
		try
		{
			uri = URLDecoder.decode( uri, "UTF-8" );
		}
		catch ( UnsupportedEncodingException e )
		{
			try
			{
				uri = URLDecoder.decode( uri, "ISO-8859-1" );
			}
			catch ( UnsupportedEncodingException e1 )
			{
				throw new Error();
			}
		}
		
		if ( !uri.startsWith( "/" ) )
		{
			return null;
		}
		
		// TODO Add a security check on the URI!
		if ( uri.contains( File.separator + '.' ) || uri.contains( '.' + File.separator ) || uri.startsWith( "." ) || uri.endsWith( "." ) || INSECURE_URI.matcher( uri ).matches() )
		{
			return null;
		}
		
		return uri;
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
	
	public String getMethod()
	{
		return http.getMethod().toString();
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
		return ((InetSocketAddress) channel.remoteAddress()).getHostName();
	}
	
	/**
	 * This method uses a checker that makes it possible for our server to get the correct remote IP even if using it with CloudFlare.
	 * I believe there are other CDN services like CloudFlare. I'd love it if people inform me, so I can implement similar methods.
	 * https://support.cloudflare.com/hc/en-us/articles/200170786-Why-do-my-server-logs-show-CloudFlare-s-IPs-using-CloudFlare-
	 */
	public String getRemoteAddr()
	{
		if ( http.headers().contains( "CF-Connecting-IP" ) )
			return http.headers().get( "CF-Connecting-IP" );
		else
			return ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
	}
	
	public int getRemotePort()
	{
		return ((InetSocketAddress) channel.remoteAddress()).getPort();
	}
	
	public int getContentLength()
	{
		//try
		{
			//http.getRequestBody().reset();
			//return http.getRequestBody().available();
			return 0; // XXX FIX THIS!!!
		}
		//catch ( IOException e )
		//{
		//	return -1;
		//}
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
		return ((InetSocketAddress) channel.localAddress()).getPort();
	}
	
	public String getServerName()
	{
		return ((InetSocketAddress) channel.localAddress()).getHostName();
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
					hash.put( "0", (String) o );
					hash.put( key, val );
					result.put( var, hash );
				}
				else if ( o instanceof Map )
				{
					Map<String, String> map = (Map<String, String>) o;
					
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
		return ((InetSocketAddress) channel.localAddress()).getHostName();
	}
	
	public String getLocalAddr()
	{
		return ((InetSocketAddress) channel.localAddress()).getAddress().getHostAddress();
	}
	
	public String getUserAgent()
	{
		return getHeader( "User-Agent" );
	}
	
	void initServerVars( Map<ServerVars, Object> staticServerVars )
	{
		// Not sure of the need to do a try... catch.
		// Instead of a blanket catch, we should make this more of a personal check for each put.
		try
		{
			serverVars = staticServerVars;
			serverVars.put( ServerVars.DOCUMENT_ROOT, getSite().getAbsoluteRoot( null ) );
			serverVars.put( ServerVars.HTTP_ACCEPT, getHeader( "Accept" ) );
			serverVars.put( ServerVars.HTTP_USER_AGENT, getUserAgent() );
			serverVars.put( ServerVars.HTTP_CONNECTION, getHeader( "Connection" ) );
			serverVars.put( ServerVars.HTTP_HOST, getLocalHost() );
			serverVars.put( ServerVars.HTTP_ACCEPT_ENCODING, getHeader( "Accept-Encoding" ) );
			serverVars.put( ServerVars.HTTP_ACCEPT_LANGUAGE, getHeader( "Accept-Language" ) );
			serverVars.put( ServerVars.HTTP_X_REQUESTED_WITH, getHeader( "X-requested-with" ) );
			serverVars.put( ServerVars.REMOTE_HOST, getRemoteHost() );
			serverVars.put( ServerVars.REMOTE_ADDR, getRemoteAddr() );
			serverVars.put( ServerVars.REMOTE_PORT, getRemotePort() );
			serverVars.put( ServerVars.REQUEST_TIME, getRequestTime() );
			serverVars.put( ServerVars.REQUEST_URI, getURI() );
			serverVars.put( ServerVars.CONTENT_LENGTH, getContentLength() );
			serverVars.put( ServerVars.AUTH_TYPE, getAuthType() );
			serverVars.put( ServerVars.SERVER_NAME, getServerName() );
			serverVars.put( ServerVars.SERVER_PORT, getServerPort() );
			serverVars.put( ServerVars.HTTPS, isSecure() );
			serverVars.put( ServerVars.SESSION, getSession() );
			serverVars.put( ServerVars.SERVER_SOFTWARE, Versioning.getProduct() );
			// serverVars.put( ServerVars.SERVER_VERSION, Versioning.getVersion() );
			serverVars.put( ServerVars.SERVER_ADMIN, Loader.getConfig().getString( "server.admin", "webmaster@" + getDomain() ) );
			serverVars.put( ServerVars.SERVER_SIGNATURE, Versioning.getProduct() + " Version " + Versioning.getVersion() );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
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
}
