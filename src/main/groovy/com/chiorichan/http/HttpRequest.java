/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;

import com.chiorichan.Loader;
import com.chiorichan.bus.events.server.ServerVars;
import com.chiorichan.exceptions.HttpErrorException;
import com.chiorichan.framework.Site;
import com.chiorichan.http.multipart.FilePart;
import com.chiorichan.http.multipart.MultiPartRequestParser;
import com.chiorichan.http.multipart.ParamPart;
import com.chiorichan.http.multipart.Part;
import com.chiorichan.http.session.SessionProvider;
import com.chiorichan.util.Common;
import com.chiorichan.util.Versioning;
import com.google.common.collect.Maps;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class HttpRequest
{
	protected Map<ServerVars, Object> serverVars = Maps.newLinkedHashMap();
	protected Site currentSite;
	protected HttpExchange http;
	protected SessionProvider sess = null;
	protected HttpResponse response;
	protected Map<String, String> getMap, postMap,
			rewriteMap = Maps.newLinkedHashMap();
	protected int requestTime = 0;
	protected Map<String, UploadedFile> uploadedFiles = new HashMap<String, UploadedFile>();
	
	protected HttpRequest(HttpExchange _http) throws IOException
	{
		http = _http;
		requestTime = Common.getEpoch();
		
		response = new HttpResponse( this );
		
		String domain = getParentDomain();
		
		currentSite = Loader.getSiteManager().getSiteByDomain( domain );
		
		if ( currentSite == null )
			if ( domain.isEmpty() )
				currentSite = Loader.getSiteManager().getSiteById( "framework" );
			else
				currentSite = new Site( "default", Loader.getConfig().getString( "framework.sites.defaultTitle", "Unnamed Chiori Framework Site" ), domain );
		
		try
		{
			getMap = queryToMap( http.getRequestURI().getQuery() );
			
			if ( http.getRequestBody().available() > 0 )
			{
				if ( MultiPartRequestParser.isMultipart( this ) )
				{ // Multipart Request - File Upload Usually.
					try
					{
						postMap = new HashMap<String, String>();
						MultiPartRequestParser parser = new MultiPartRequestParser( this );
						
						File tmpFileDirectory = ( currentSite != null ) ? currentSite.getTempFileDirectory() : Loader.getTempFileDirectory();
						
						if ( !tmpFileDirectory.exists() )
							tmpFileDirectory.mkdirs();
						
						if ( !tmpFileDirectory.isDirectory() )
							throw new IOException( "The temp directory specified in the server configs is not a directory, File Uploads will continue to fail until this problem is resolved." );
						
						if ( !tmpFileDirectory.canWrite() )
							throw new IOException( "The temp directory specified in the server configs is not writable, File Uploads will continue to fail until this problem is resolved." );
						
						Part part;
						while ( ( part = parser.readNextPart() ) != null )
						{
							String name = part.getName();
							if ( name == null )
							{
								throw new IOException( "Malformed input: parameter name missing (known Opera 7 bug)" );
							}
							if ( part.isParam() )
							{
								ParamPart paramPart = (ParamPart) part;
								String value = paramPart.getStringValue();
								/*
								 * Vector existingValues = (Vector) parameters.get( name );
								 * if ( existingValues == null )
								 * {
								 * existingValues = new Vector();
								 * postMap.put( name, existingValues );
								 * }
								 * existingValues.addElement( value );
								 * XXX Should we use vectors in our Get and Post Maps?
								 */
								postMap.put( name, value );
							}
							else if ( part.isFile() )
							{
								FilePart filePart = (FilePart) part;
								String fileName = filePart.getFileName();
								if ( fileName != null )
								{
									long size = -1;
									String msg = "The file uploaded successfully.";
									
									try
									{
										size = filePart.writeTo( tmpFileDirectory );
									}
									catch ( IOException e )
									{
										msg = e.getMessage();
									}
									
									String tmpFileName = filePart.getTmpFileName();
									
									File newFile = new File( tmpFileDirectory, tmpFileName );
									newFile.deleteOnExit();
									
									uploadedFiles.put( name, new UploadedFile( newFile, fileName, size, msg ) );
								}
							}
						}
						
					}
					catch ( HttpErrorException e )
					{
						response.sendError( e.getHttpCode(), null, e.getReason() );
					}
					catch ( IOException e )
					{
						response.sendException( e );
					}
				}
				else
				{
					byte[] queryBytes = new byte[http.getRequestBody().available()];
					IOUtils.readFully( http.getRequestBody(), queryBytes );
					postMap = queryToMap( new String( queryBytes ) );
				}
			}
		}
		catch ( IOException e )
		{
			Loader.getLogger().severe( "There was a severe error reading the " + http.getRequestMethod().toUpperCase() + " query.", e );
			response.sendException( e );
		}
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
			if ( pair.length > 1 )
				result.put( URLDecoder.decode( pair[0], "ISO-8859-1" ), URLDecoder.decode( pair[1], "ISO-8859-1" ) );
			else if ( pair.length == 1 )
				result.put( URLDecoder.decode( pair[0], "ISO-8859-1" ), "" );
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
	
	public Headers getHeaders()
	{
		return http.getRequestHeaders();
	}
	
	protected SessionProvider getSessionNoWarning()
	{
		return sess;
	}
	
	public SessionProvider getSession()
	{
		if ( sess == null )
			Loader.getLogger().warning( "The Session is NULL! This usually happens because initSession() was not called at the proper time." );
		
		return sess;
	}
	
	public HttpResponse getResponse()
	{
		return response;
	}
	
	/**
	 * getPath() method was removing the beginning of a uri if it started with a double slash ie. //pages/about.gsp
	 * This method might need some work up to make it more reliable.
	 */
	public String getURI()
	{
		String uri = http.getRequestURI().toString();
		
		if ( uri.contains( "?" ) )
			uri = uri.substring( 0, uri.indexOf( "?" ) );
		
		if ( uri.startsWith( "/" ) )
			uri = uri.substring( 1 );
		
		return uri;
	}
	
	// Cached domain names.
	protected String parentDomainName = null;
	protected String childDomainName = null;
	
	public String getDomain()
	{
		try
		{
			String domain = http.getRequestHeaders().get( "Host" ).get( 0 );
			domain = domain.split( "\\:" )[0];
			
			return domain;
		}
		catch ( NullPointerException e )
		{
			return "";
		}
	}
	
	public String getParentDomain()
	{
		if ( parentDomainName == null || childDomainName == null )
			calculateDomainName();
		
		return ( parentDomainName == null ) ? "" : parentDomainName;
	}
	
	public String getSubDomain()
	{
		if ( parentDomainName == null || childDomainName == null )
			calculateDomainName();
		
		return ( childDomainName == null ) ? "" : childDomainName;
	}
	
	public void calculateDomainName()
	{
		if ( http.getRequestHeaders().get( "Host" ) == null )
		{
			childDomainName = "";
			parentDomainName = "";
		}
		else
		{
			String domain = http.getRequestHeaders().get( "Host" ).get( 0 );
			domain = domain.split( "\\:" )[0];
			
			if ( domain.equalsIgnoreCase( "localhost" ) || domain.equalsIgnoreCase( "127.0.0.1" ) || domain.equalsIgnoreCase( getLocalAddr() ) || domain.equalsIgnoreCase( getLocalHost() ) )
				domain = "";
			
			String[] var1 = domain.split( "\\." );
			
			if ( var1.length == 4 && NumberUtils.isNumber( var1[0] ) && NumberUtils.isNumber( var1[1] ) && NumberUtils.isNumber( var1[2] ) && NumberUtils.isNumber( var1[3] ) )
			{
				// This should be an IP Address
				childDomainName = "";
				parentDomainName = domain;
			}
			else if ( var1.length > 2 )
			{
				// This will not work if there is more then one subdomain like s1.t2.example.com
				var1 = domain.split( "\\.", 2 );
				
				// This should be a domain with subdomain
				childDomainName = var1[0];
				parentDomainName = var1[1];
			}
			else
			{
				// This should be a domain without a subdomain
				childDomainName = "";
				parentDomainName = domain;
			}
		}
	}
	
	public String getMethod()
	{
		return http.getRequestMethod();
	}
	
	public String getHeader( String key )
	{
		try
		{
			return http.getRequestHeaders().get( key ).get( 0 );
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
		return http.getRemoteAddress().getHostName();
	}
	
	public String getRemoteAddr()
	{
		// This is a checker that makes it possible for our server to get the correct remote IP even if using it with CloudFlare.
		// I believe there are other similar services to CloudFlare. I'd love it if beta testers can let me know if they find anyone else.
		// https://support.cloudflare.com/hc/en-us/articles/200170786-Why-do-my-server-logs-show-CloudFlare-s-IPs-using-CloudFlare-
		if ( http.getRequestHeaders().containsKey( "CF-Connecting-IP" ) )
			return http.getRequestHeaders().get( "CF-Connecting-IP" ).get( 0 );
		else
			return http.getRemoteAddress().getAddress().getHostAddress();
	}
	
	public int getRemotePort()
	{
		return http.getRemoteAddress().getPort();
	}
	
	public int getContentLength()
	{
		try
		{
			http.getRequestBody().reset();
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
	
	public void setHeader( String key, String value )
	{
		http.getResponseHeaders().set( key, value );
	}
	
	public HttpExchange getOriginal()
	{
		return http;
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
	
	public Map<String, String> getPostMap()
	{
		if ( postMap == null )
			postMap = new HashMap<String, String>();
		
		return postMap;
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
		return http.getLocalAddress().getAddress().getCanonicalHostName();
	}
	
	public String getLocalAddr()
	{
		return http.getLocalAddress().getAddress().getHostAddress();
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
}
