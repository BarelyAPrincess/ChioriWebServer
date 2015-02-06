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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.chiorichan.Loader;
import com.chiorichan.event.http.ErrorEvent;
import com.chiorichan.event.http.HttpExceptionEvent;
import com.chiorichan.exception.HttpErrorException;
import com.chiorichan.util.Versioning;
import com.google.common.collect.Maps;

public class HttpResponseWrapper
{
	protected HttpRequestWrapper request;
	protected ByteArrayOutputStream output = new ByteArrayOutputStream();
	protected int httpStatus = 200;
	protected String httpContentType = "text/html";
	protected String encoding = "UTF-8";
	protected HttpResponseStage stage = HttpResponseStage.READING;
	protected Map<String, String> pageDataOverrides = Maps.newHashMap();
	protected Map<String, String> headers = Maps.newHashMap();
	
	protected HttpResponseWrapper( HttpRequestWrapper request )
	{
		this.request = request;
	}
	
	public void mergeOverrides( Map<String, String> overrides )
	{
		pageDataOverrides.putAll( overrides );
	}
	
	public void setOverride( String key, String val )
	{
		pageDataOverrides.put( key, val );
	}
	
	public void sendError( Exception e ) throws IOException
	{
		if ( e instanceof HttpErrorException )
			sendError( ( ( HttpErrorException ) e ).getHttpCode(), ( ( HttpErrorException ) e ).getReason() );
		else
			sendError( 500, e.getMessage() );
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
	
	public void setStatus( int status )
	{
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access setter methods within this HttpResponse because the connection has been closed." );
		
		httpStatus = status;
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
	 * 
	 * @param msg
	 *            , a message to pass to the login page as a argumnet. ie. ?msg=Please login!
	 */
	public void sendLoginPage( String msg )
	{
		String loginPage = request.getSite().getYaml().getString( "scripts.login-form", "/login" );
		sendRedirect( loginPage + "?msg=" + msg );
	}
	
	/**
	 * Send the client to a specified page with http code 302 automatically.
	 * 
	 * @param target
	 *            , destination url. Can be relative or absolute.
	 */
	public void sendRedirect( String target )
	{
		sendRedirect( target, 302, true );
	}
	
	/**
	 * Sends the client to a specified page with specified http code.
	 * 
	 * @param target
	 *            , destination url. Can be relative or absolute.
	 * @param httpStatus
	 *            , http code to use.
	 */
	public void sendRedirect( String target, int httpStatus )
	{
		sendRedirect( target, httpStatus, true );
	}
	
	/**
	 * XXX: autoRedirect argument needs to be working before this method is made public
	 * Sends the client to a specified page with specified http code but with the option to not automatically go.
	 * 
	 * @param target
	 *            , destination url. Can be relative or absolute.
	 * @param httpStatus
	 *            , http code to use.
	 * @param autoRedirect
	 *            , Automatically go.
	 */
	private void sendRedirect( String target, int httpStatus, boolean autoRedirect )
	{
		Loader.getLogger().info( "Sending page redirect to `" + target + "`" );
		
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access setter methods within this HttpResponse because the connection has been closed." );
		
		if ( autoRedirect )
		{
			setStatus( httpStatus );
			setHeader( "Location", target );
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
	 * 
	 * @param var1
	 *            byte array to print
	 * @throws IOException
	 *             if there was a problem with the output buffer.
	 */
	public void print( byte[] var1 ) throws IOException
	{
		stage = HttpResponseStage.WRITTING;
		output.write( var1 );
	}
	
	/**
	 * Prints a single string of text to the buffered output
	 * 
	 * @param var1
	 *            string of text.
	 * @throws IOException
	 *             if there was a problem with the output buffer.
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
	 * 
	 * @param var1
	 *            string of text.
	 * @throws IOException
	 *             if there was a problem with the output buffer.
	 */
	public void println( String var1 ) throws IOException
	{
		if ( stage != HttpResponseStage.MULTIPART )
			stage = HttpResponseStage.WRITTING;
		
		output.write( ( var1 + "\n" ).getBytes( encoding ) );
	}
	
	/**
	 * Sets the ContentType header.
	 * 
	 * @param type
	 *            e.g., text/html or application/xml
	 */
	public void setContentType( String type )
	{
		if ( type == null || type.isEmpty() )
			type = "text/html";
		
		httpContentType = type;
	}
	
	/**
	 * Sends the data to the client. Internal Use.
	 * 
	 * @throws IOException
	 *             if there was a problem sending the data, like the connection was unexpectedly closed.
	 */
	public void sendResponse() throws IOException
	{
		if ( stage == HttpResponseStage.CLOSED || stage == HttpResponseStage.WRITTEN )
			return;
		
		stage = HttpResponseStage.WRITTEN;
		
		// HttpRequest http = request.getOriginal();
		
		FullHttpResponse response = new DefaultFullHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf( httpStatus ), Unpooled.copiedBuffer( output.toByteArray() ) );
		HttpHeaders h = response.headers();
		
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
		
		h.set( HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes() );
		h.set( HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE );
		
		stage = HttpResponseStage.CLOSED;
		
		request.getChannel().write( response );
	}
	
	public void closeMultipart() throws IOException
	{
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access closeMultipart unless you start MULTIPART with sendMultipart." );
		
		stage = HttpResponseStage.CLOSED;
		
		// Write the end marker
		ChannelFuture lastContentFuture = request.getChannel().writeAndFlush( LastHttpContent.EMPTY_LAST_CONTENT );
		
		// Decide whether to close the connection or not.
		// if ( !isKeepAlive( request ) )
		{
			// Close the connection when the whole content is written out.
			lastContentFuture.addListener( ChannelFutureListener.CLOSE );
		}
	}
	
	public void sendMultipart( byte[] bytesToWrite ) throws IOException
	{
		if ( request.getMethod().equals( HttpMethod.HEAD ) )
			throw new IllegalStateException( "You can't start MULTIPART mode on a HEAD Request." );
		
		if ( stage != HttpResponseStage.MULTIPART )
		{
			stage = HttpResponseStage.MULTIPART;
			HttpResponse response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.OK );
			
			HttpHeaders h = response.headers();
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
			
			// if ( isKeepAlive( request ) )
			{
				// response.headers().set( CONNECTION, HttpHeaders.Values.KEEP_ALIVE );
			}
			
			request.getChannel().write( response );
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
			
			ChannelFuture sendFuture = request.getChannel().write( new ChunkedStream( new ByteArrayInputStream( ba.toByteArray() ) ), request.getChannel().newProgressivePromise() );
			
			ba.close();
			
			sendFuture.addListener( new ChannelProgressiveFutureListener()
			{
				@Override
				public void operationProgressed( ChannelProgressiveFuture future, long progress, long total )
				{
					if ( total < 0 )
					{ // total unknown
						System.err.println( "Transfer progress: " + progress );
					}
					else
					{
						System.err.println( "Transfer progress: " + progress + " / " + total );
					}
				}
				
				@Override
				public void operationComplete( ChannelProgressiveFuture future ) throws Exception
				{
					System.err.println( "Transfer complete." );
				}
			} );
		}
	}
	
	public void setEncoding( String encoding )
	{
		this.encoding = encoding;
	}
	
	public void setHeader( String key, String val )
	{
		headers.put( key, val );
	}
}
