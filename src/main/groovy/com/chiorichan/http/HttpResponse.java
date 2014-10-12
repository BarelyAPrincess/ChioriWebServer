/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.chiorichan.Loader;
import com.chiorichan.bus.events.http.ErrorEvent;
import com.chiorichan.bus.events.http.HttpExceptionEvent;
import com.chiorichan.exceptions.HttpErrorException;
import com.chiorichan.util.Versioning;
import com.google.common.collect.Maps;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

// NOTE: Change to consider, Have headers sent before data can be written to the output stream.
// This will allow for quicker responses but might make it harder for spontaneous header changes.
public class HttpResponse
{
	protected HttpRequest request;
	protected ByteArrayOutputStream output = new ByteArrayOutputStream();
	protected int httpStatus = 200;
	protected String httpContentType = "text/html";
	protected String encoding = "UTF-8";
	protected HttpResponseStage stage = HttpResponseStage.READING;
	protected Map<String, String> pageDataOverrides = Maps.newHashMap();
	protected Map<String, String> headers = Maps.newHashMap();
	
	protected HttpResponse(HttpRequest _request)
	{
		request = _request;
	}
	
	public void mergeOverrides( Map<String, String> overrides )
	{
		pageDataOverrides.putAll( overrides );
	}
	
	public void setOverride( String key, String val )
	{
		pageDataOverrides.put( key, val );
	}
	
	public void sendError( HttpErrorException e ) throws IOException
	{
		sendError( e.getHttpCode(), e.getReason() );
	}
	
	public void sendError( int var1 ) throws IOException
	{
		sendError( var1, null );
	}
	
	public void sendError( int var1, String var2 ) throws IOException
	{
		sendError( var1, var2, null );
	}
	
	public void sendError( int var1, String var2, String var3 ) throws IOException
	{
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access setter methods within this HttpResponse because the connection has been closed." );
		
		if ( var1 < 1 )
			var1 = 500;
		
		if ( var2 == null )
			var2 = HttpCode.msg( var1 );
		
		Loader.getLogger().warning( "HttpError: " + var1 + " - " + var2 + "... '" + request.getSubDomain() + "." + request.getParentDomain() + "' '" + request.getURI() + "'" );
		
		httpStatus = var1;
		
		output.reset();
		
		// Trigger an internal Error Event to notify plugins of a possible problem.
		ErrorEvent event = new ErrorEvent( request, var1, var2 );
		Loader.getEventBus().callEvent( event );
		
		// TODO Make these error pages a bit more creative and/or informational to developers.
		
		if ( event.getErrorHtml() == null || event.getErrorHtml().isEmpty() )
		{
			println( "<h1>" + var1 + " - " + var2 + "</h1>" );
			
			if ( var3 != null && !var3.isEmpty() )
				println( "<p>" + var3 + "</p>" );
			
			println( "<hr>" );
			println( "<small>Running <a href=\"https://github.com/ChioriGreene/ChioriWebServer\">" + Versioning.getProduct() + "</a> Version " + Versioning.getVersion() + "<br />" + Versioning.getCopyright() + "</small>" );
			
		}
		else
		{
			print( event.getErrorHtml() );
		}
		
		sendResponse();
	}
	
	public void sendException( Throwable cause ) throws IOException
	{
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access setter methods within this HttpResponse because the connection has been closed." );
		
		HttpExceptionEvent event = new HttpExceptionEvent( request, cause, Loader.getConfig().getBoolean( "server.developmentMode" ) );
		Loader.getEventBus().callEvent( event );
		
		int httpCode = event.getHttpCode();
		
		if ( httpCode < 1 )
			httpCode = 500;
		
		httpStatus = httpCode;
		
		Loader.getLogger().warning( "HttpError: " + httpCode + " - " + HttpCode.msg( httpCode ) + "... '" + request.getSubDomain() + "." + request.getParentDomain() + "' '" + request.getURI() + "'" );
		
		if ( Loader.getConfig().getBoolean( "server.developmentMode" ) )
		{
			if ( event.getErrorHtml() != null )
			{
				output.reset();
				print( event.getErrorHtml() );
				sendResponse();
			}
			else
				sendError( httpStatus, null, "<pre>" + ExceptionUtils.getStackTrace( cause ) + "</pre>" );
		}
		else
		{
			sendError( 500, null, "<pre>" + ExceptionUtils.getStackTrace( cause ) + "</pre>" );
		}
	}
	
	/**
	 * Clears the output buffer of all content
	 */
	public void resetOutput()
	{
		output.reset();
	}
	
	public ByteArrayOutputStream getOutput()
	{
		return output;
	}
	
	public boolean isCommitted()
	{
		return stage == HttpResponseStage.CLOSED || stage == HttpResponseStage.WRITTEN;
	}
	
	/**
	 * 
	 * @return HttpResponseStage
	 */
	public HttpResponseStage getStage()
	{
		return stage;
	}
	
	public void setStatus( int _status )
	{
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access setter methods within this HttpResponse because the connection has been closed." );
		
		httpStatus = _status;
	}
	
	/**
	 * Sends the client to the site login page found in configs and also sends a please login message along with it.
	 */
	public void sendLoginPage()
	{
		sendLoginPage( "You must be logged in to view this page!" );
	}
	
	/**
	 * Sends the client to the site login page found in configs.
	 * @param msg, a message to pass to the login page as a argumnet. ie. ?msg=Please login!
	 */
	public void sendLoginPage( String msg )
	{
		String loginPage = request.getSite().getYaml().getString( "scripts.login-form", "/login" );
		sendRedirect( loginPage + "?msg=" + msg );
	}
	
	/**
	 * Send the client to a specified page with http code 302 automatically.
	 * @param target, destination url. Can be relative or absolute.
	 */
	public void sendRedirect( String target )
	{
		sendRedirect( target, 302, true );
	}
	
	/**
	 * Sends the client to a specified page with specified http code.
	 * @param target, destination url. Can be relative or absolute.
	 * @param httpStatus, http code to use.
	 */
	public void sendRedirect( String target, int httpStatus )
	{
		sendRedirect( target, httpStatus, true );
	}
	
	/**
	 * XXX: autoRedirect argument needs to be working before this method is made public
	 * Sends the client to a specified page with specified http code but with the option to not automatically go.
	 * @param target, destination url. Can be relative or absolute.
	 * @param httpStatus, http code to use.
	 * @param autoRedirect, Automatically go.
	 */
	private void sendRedirect( String target, int httpStatus, boolean autoRedirect )
	{
		Loader.getLogger().info( "Sending page redirect to `" + target + "`" );
		
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access setter methods within this HttpResponse because the connection has been closed." );
		
		if ( autoRedirect )
		{
			setStatus( httpStatus );
			request.getOriginal().getResponseHeaders().set( "Location", target );
		}
		else
			// TODO: Send client a redirection page.
			// "The Request URL has been relocated to: " . $StrURL .
			// "<br />Please change any bookmarks to reference this new location."
			
			try
			{
				println( "<script>window.location = '" + target + "';</script>" );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		
		try
		{
			sendResponse();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Prints a byte array to the buffered output
	 * @param var1 byte array to print
	 * @throws IOException if there was a problem with the output buffer.
	 */
	public void print( byte[] var1 ) throws IOException
	{
		stage = HttpResponseStage.WRITTING;
		output.write( var1 );
	}
	
	/**
	 * Prints a single string of text to the buffered output
	 * @param var1 string of text.
	 * @throws IOException if there was a problem with the output buffer.
	 */
	public void print( String var1 ) throws IOException
	{
		if ( stage != HttpResponseStage.MULTIPART )
			stage = HttpResponseStage.WRITTING;
		
		if ( var1 != null && !var1.isEmpty() )
			output.write( var1.getBytes( encoding ) );
	}
	
	/**
	 * Prints a single string of text with a line return to the buffered output
	 * @param var1 string of text.
	 * @throws IOException if there was a problem with the output buffer.
	 */
	public void println( String var1 ) throws IOException
	{
		if ( stage != HttpResponseStage.MULTIPART )
			stage = HttpResponseStage.WRITTING;
		
		output.write( ( var1 + "\n" ).getBytes( encoding ) );
	}
	
	/**
	 * Sets the ContentType header.
	 * @param ContentType. ie. text/html or application/xml
	 */
	public void setContentType( String type )
	{
		if ( type == null || type.isEmpty() )
			type = "text/html";
		
		httpContentType = type;
	}
	
	/**
	 * Sends the data to the client. Internal Use.
	 * @throws IOException if there was a problem sending the data, like the connection was unexpectedly closed.
	 */
	public void sendResponse() throws IOException
	{
		if ( stage == HttpResponseStage.CLOSED || stage == HttpResponseStage.WRITTEN )
			return;
		
		stage = HttpResponseStage.WRITTEN;
		
		HttpExchange http = request.getOriginal();
		
		Headers h = http.getResponseHeaders();
		
		for ( Candy c : request.getCandies() )
			if ( c.needsUpdating() )
				h.add( "Set-Cookie", c.toHeaderValue() );
		
		if ( h.get( "Server" ) == null )
			h.add( "Server", Versioning.getProduct() + " Version " + Versioning.getVersion() );
		
		// NOTE: Why did I make it check this again?
		// if ( h.get( "Content-Type" ) == null )
		// h.add( "Content-Type", httpContentType );
		
		// This might be a temporary measure - TODO Properly set the charset for each request.
		h.set( "Content-Type", httpContentType + "; charset=" + encoding.toLowerCase() );
		
		h.add( "Access-Control-Allow-Origin", request.getSite().getYaml().getString( "web.allowed-origin", "*" ) );
		
		for ( Entry<String, String> header : headers.entrySet() )
		{
			h.add( header.getKey(), header.getValue() );
		}
		
		http.sendResponseHeaders( httpStatus, output.size() );
		
		// Fixes an issue with requests coming from CURL with --head argument.
		if ( !http.getRequestMethod().equalsIgnoreCase( "HEAD" ) )
		{
			OutputStream os = http.getResponseBody();
			os.write( output.toByteArray() );
			output.close();
			os.close(); // This terminates the HttpExchange and frees the resources.
		}
		
		stage = HttpResponseStage.CLOSED;
	}
	
	public void closeMultipart() throws IOException
	{
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access closeMultipart unless you start MULTIPART with sendMultipart." );
		
		stage = HttpResponseStage.CLOSED;
		
		HttpExchange http = request.getOriginal();
		OutputStream os = http.getResponseBody();
		os.close();
		
		output.close();
	}
	
	public void sendMultipart( byte[] bytesToWrite ) throws IOException
	{
		HttpExchange http = request.getOriginal();
		
		if ( http.getRequestMethod().equalsIgnoreCase( "HEAD" ) )
			throw new IllegalStateException( "You can't start MULTIPART mode on a HEAD Request." );
		
		if ( stage != HttpResponseStage.MULTIPART )
		{
			stage = HttpResponseStage.MULTIPART;
			Headers h = http.getResponseHeaders();
			request.getSession().saveSession( false );
			
			for ( Candy c : request.getCandies() )
			{
				if ( c.needsUpdating() )
					h.add( "Set-Cookie", c.toHeaderValue() );
			}
			
			if ( h.get( "Server" ) == null )
				h.add( "Server", Versioning.getProduct() + " Version " + Versioning.getVersion() );
			
			h.add( "Access-Control-Allow-Origin", request.getSite().getYaml().getString( "web.allowed-origin", "*" ) );
			h.add( "Connection", "close" );
			h.add( "Cache-Control", "no-cache" );
			h.add( "Cache-Control", "private" );
			h.add( "Pragma", "no-cache" );
			h.set( "Content-Type", "multipart/x-mixed-replace; boundary=--cwsframe" );
			
			http.sendResponseHeaders( 200, 0 );
		}
		else
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append( "--cwsframe\r\n" );
			sb.append( "Content-Type: " + httpContentType + "\r\n" );
			sb.append( "Content-Length: " + bytesToWrite.length + "\r\n\r\n" );
			
			ByteArrayOutputStream ba = new ByteArrayOutputStream();
			
			ba.write( sb.toString().getBytes( encoding ) );
			ba.write( bytesToWrite );
			ba.flush();
			
			OutputStream os = http.getResponseBody();
			os.write( ba.toByteArray() );
			ba.close();
			os.flush();
		}
	}
	
	public void setEncoding( String _encoding )
	{
		encoding = _encoding;
	}
	
	public void setHeader( String key, String val )
	{
		headers.put( key, val );
	}
}
