/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.http;

import io.netty.buffer.ByteBuf;
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
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.http.ErrorEvent;
import com.chiorichan.event.http.HttpExceptionEvent;
import com.chiorichan.lang.ApacheParser;
import com.chiorichan.lang.HttpError;
import com.chiorichan.logger.LogEvent;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.session.Session;
import com.chiorichan.session.SessionException;
import com.chiorichan.util.Versioning;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

/**
 * Wraps the Netty HttpResponse to provide easy methods for manipulating the result of each request
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class HttpResponseWrapper
{
	final HttpRequestWrapper request;
	ByteBuf output = Unpooled.buffer();
	HttpResponseStatus httpStatus = HttpResponseStatus.OK;
	String httpContentType = "text/html";
	Charset encoding = Charsets.UTF_8;
	HttpResponseStage stage = HttpResponseStage.READING;
	final Map<String, String> pageDataOverrides = Maps.newHashMap();
	final Map<String, String> headers = Maps.newHashMap();
	ApacheParser htaccess = null;
	final LogEvent log;
	
	protected HttpResponseWrapper( HttpRequestWrapper request, LogEvent log )
	{
		this.request = request;
		this.log = log;
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
		if ( e instanceof HttpError )
			sendError( ( ( HttpError ) e ).getHttpCode(), ( ( HttpError ) e ).getReason(), ( ( HttpError ) e ).getMessage() );
		else
			sendError( 500, e.getMessage() );
	}
	
	public void sendError( int httpCode ) throws IOException
	{
		sendError( httpCode, null );
	}
	
	public void sendError( int httpCode, String httpMsg ) throws IOException
	{
		sendError( httpCode, httpMsg, null );
	}
	
	public void sendError( int status, String httpMsg, String msg ) throws IOException
	{
		if ( status < 1 )
			status = 500;
		
		sendError( HttpResponseStatus.valueOf( status ), httpMsg, msg );
	}
	
	public void sendError( HttpResponseStatus status, String httpMsg, String msg ) throws IOException
	{
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access sendError method within this HttpResponse because the connection has been closed." );
		
		if ( httpMsg == null )
			httpMsg = status.reasonPhrase();
		
		// NetworkManager.getLogger().info( ConsoleColor.RED + "HttpError{httpCode=" + status.code() + ",httpMsg=" + httpMsg + ",subdomain=" + request.getSubDomain() + ",domain=" + request.getDomain() + ",uri=" + request.getUri() +
		// ",remoteIp=" + request.getIpAddr() + "}" );
		
		if ( msg == null || msg.length() > 100 )
			log.log( Level.SEVERE, "%s {code=%s}", httpMsg, status.code() );
		else
			log.log( Level.SEVERE, "%s {code=%s,reason=%s}", httpMsg, status.code(), msg );
		
		resetBuffer();
		
		// Trigger an internal Error Event to notify plugins of a possible problem.
		ErrorEvent event = new ErrorEvent( request, status.code(), httpMsg );
		EventBus.INSTANCE.callEvent( event );
		
		// TODO Make these error pages a bit more creative and/or informational to developers.
		
		if ( event.getErrorHtml() != null && !event.getErrorHtml().isEmpty() )
		{
			print( event.getErrorHtml() );
			sendResponse();
		}
		else
		{
			boolean contin = true;
			
			if ( htaccess != null && htaccess.getErrorDocument( status.code() ) != null )
			{
				String resp = htaccess.getErrorDocument( status.code() ).getResponse();
				
				if ( resp.startsWith( "/" ) )
				{
					sendRedirect( request.getBaseUrl() + resp );
					contin = false;
				}
				else if ( resp.startsWith( "http" ) )
				{
					sendRedirect( resp );
					contin = false;
				}
				else
					httpMsg = resp;
			}
			
			if ( contin )
			{
				println( "<h1>" + status.code() + " - " + httpMsg + "</h1>" );
				
				if ( msg != null && !msg.isEmpty() )
					println( "<p>" + msg + "</p>" );
				
				println( "<hr>" );
				println( "<small>Running <a href=\"https://github.com/ChioriGreene/ChioriWebServer\">" + Versioning.getProduct() + "</a> Version " + Versioning.getVersion() + " (Build #" + Versioning.getBuildNumber() + ")<br />" + Versioning.getCopyright() + "</small>" );
				
				sendResponse();
			}
		}
	}
	
	public void resetBuffer()
	{
		output = Unpooled.buffer();
	}
	
	public void sendException( Throwable cause ) throws IOException
	{
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access sendException method within this HttpResponse because the connection has been closed." );
		
		if ( cause instanceof HttpError )
		{
			sendError( ( HttpError ) cause );
			return;
		}
		
		HttpExceptionEvent event = new HttpExceptionEvent( request, cause, Loader.getConfig().getBoolean( "server.developmentMode" ) );
		EventBus.INSTANCE.callEvent( event );
		
		int httpCode = event.getHttpCode();
		
		if ( httpCode < 1 )
			httpCode = 500;
		
		httpStatus = HttpResponseStatus.valueOf( httpCode );
		
		// NetworkManager.getLogger().info( ConsoleColor.RED + "HttpError{httpCode=" + httpCode + ",httpMsg=" + HttpCode.msg( httpCode ) + ",domain=" + request.getSubDomain() + "." + request.getDomain() + ",uri=" + request.getUri() +
		// ",remoteIp=" + request.getIpAddr() + "}" );
		
		if ( Loader.getConfig().getBoolean( "server.developmentMode" ) )
		{
			if ( event.getErrorHtml() != null )
			{
				log.log( Level.SEVERE, "%s {code=500}", HttpCode.msg( 500 ) );
				
				resetBuffer();
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
	
	public ByteBuf getOutput()
	{
		return output;
	}
	
	public byte[] getOutputBytes()
	{
		byte[] bytes = new byte[output.writerIndex()];
		int inx = output.readerIndex();
		output.readerIndex( 0 );
		output.readBytes( bytes );
		output.readerIndex( inx );
		return bytes;
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
		setStatus( HttpResponseStatus.valueOf( status ) );
	}
	
	public void setStatus( HttpResponseStatus httpStatus )
	{
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access setStatus method within this HttpResponse because the connection has been closed." );
		
		this.httpStatus = httpStatus;
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
		/*
		 * TODO: Come up with a better way to handle the URI used in the target, i.e., currently params are being lost in all redirects.
		 */
		String loginForm = request.getSite().getYaml().getString( "scripts.login-form", "/login" );
		sendRedirect( loginForm + "?msg=&target=http://" + request.getDomain() + request.getUri() );
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
	 *            The destination url. Can be relative or absolute.
	 * @param httpStatus
	 *            What http code to use.
	 * @param autoRedirect
	 *            Use Header or Javascript Script
	 */
	private void sendRedirect( String target, int httpStatus, boolean insteadRedirect )
	{
		NetworkManager.getLogger().info( ConsoleColor.DARK_GRAY + "Sending page redirect to `" + target + "` using httpCode `" + httpStatus + " - " + HttpCode.msg( httpStatus ) + "`" );
		
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access sendRedirect method within this HttpResponse because the connection has been closed." );
		
		if ( insteadRedirect && !isCommitted() )
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
				println( "<script type=\"text/javascript\">window.location = '" + target + "';</script>" );
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
	 * Writes a ByteBuf to the buffered output
	 * 
	 * @param var
	 *            byte buffer to print
	 * @throws IOException
	 *             if there was a problem with the output buffer.
	 */
	public void write( ByteBuf buf ) throws IOException
	{
		if ( stage != HttpResponseStage.MULTIPART )
			stage = HttpResponseStage.WRITTING;
		
		output.writeBytes( buf );
	}
	
	/**
	 * Writes a byte array to the buffered output.
	 * 
	 * @param var
	 *            byte array to print
	 * @throws IOException
	 *             if there was a problem with the output buffer.
	 */
	public void write( byte[] bytes ) throws IOException
	{
		if ( stage != HttpResponseStage.MULTIPART )
			stage = HttpResponseStage.WRITTING;
		
		output.writeBytes( bytes );
	}
	
	@Deprecated
	public void print( byte[] bytes ) throws IOException
	{
		write( bytes );
	}
	
	/**
	 * Prints a single string of text to the buffered output
	 * 
	 * @param var1
	 *            string of text.
	 * @throws IOException
	 *             if there was a problem with the output buffer.
	 */
	public void print( String var ) throws IOException
	{
		if ( var != null && !var.isEmpty() )
			write( var.getBytes( encoding ) );
	}
	
	/**
	 * Prints a single string of text with a line return to the buffered output
	 * 
	 * @param var1
	 *            string of text.
	 * @throws IOException
	 *             if there was a problem with the output buffer.
	 */
	public void println( String var ) throws IOException
	{
		if ( var != null && !var.isEmpty() )
			write( ( var + "\n" ).getBytes( encoding ) );
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
		
		FullHttpResponse response = new DefaultFullHttpResponse( HttpVersion.HTTP_1_1, httpStatus, output );
		HttpHeaders h = response.headers();
		
		Session session = request.getSessionWithoutException();
		if ( session != null )
		{
			/**
			 * Initiate the Session Persistence Method.
			 * This is usually done with a cookie but we should make a param optional
			 */
			session.processSessionCookie();
			
			for ( HttpCookie c : request.getSession().getCookies().values() )
				if ( c.needsUpdating() )
					h.add( "Set-Cookie", c.toHeaderValue() );
			
			if ( session.getSessionCookie().needsUpdating() )
				h.add( "Set-Cookie", session.getSessionCookie().toHeaderValue() );
		}
		
		if ( h.get( "Server" ) == null )
			h.add( "Server", Versioning.getProduct() + " Version " + Versioning.getVersion() );
		
		// This might be a temporary measure - TODO Properly set the charset for each request.
		h.set( "Content-Type", httpContentType + "; charset=" + encoding.name() );
		
		h.add( "Access-Control-Allow-Origin", request.getSite().getYaml().getString( "web.allowed-origin", "*" ) );
		
		for ( Entry<String, String> header : headers.entrySet() )
		{
			h.add( header.getKey(), header.getValue() );
		}
		
		// Expires: Wed, 08 Apr 2015 02:32:24 GMT
		// DateTimeFormatter formatter = DateTimeFormat.forPattern( "EE, dd-MMM-yyyy HH:mm:ss zz" );
		
		// h.set( HttpHeaders.Names.EXPIRES, formatter.print( DateTime.now( DateTimeZone.UTC ).plusDays( 1 ) ) );
		// h.set( HttpHeaders.Names.CACHE_CONTROL, "public, max-age=86400" );
		
		h.set( HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes() );
		h.set( HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE );
		
		stage = HttpResponseStage.WRITTEN;
		
		request.getChannel().writeAndFlush( response );
	}
	
	public void close()
	{
		request.getChannel().close();
		stage = HttpResponseStage.CLOSED;
	}
	
	public void finishMultipart() throws IOException
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
			try
			{
				request.getSession().save();
			}
			catch ( SessionException e )
			{
				e.printStackTrace();
			}
			
			for ( HttpCookie c : request.getCookies() )
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
						NetworkManager.getLogger().info( "Transfer progress: " + progress );
					}
					else
					{
						NetworkManager.getLogger().info( "Transfer progress: " + progress + " / " + total );
					}
				}
				
				@Override
				public void operationComplete( ChannelProgressiveFuture future ) throws Exception
				{
					NetworkManager.getLogger().info( "Transfer complete." );
				}
			} );
		}
	}
	
	public void setEncoding( String encoding )
	{
		this.encoding = Charset.forName( encoding );
	}
	
	public void setEncoding( Charset encoding )
	{
		this.encoding = encoding;
	}
	
	public void setHeader( String key, String val )
	{
		headers.put( key, val );
	}
	
	public int getHttpCode()
	{
		return httpStatus.code();
	}
	
	public String getHttpMsg()
	{
		return HttpCode.msg( httpStatus.code() );
	}
	
	public void setApacheParser( ApacheParser htaccess )
	{
		this.htaccess = htaccess;
	}
}
