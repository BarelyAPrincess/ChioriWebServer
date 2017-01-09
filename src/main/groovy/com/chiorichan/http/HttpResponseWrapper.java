/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * <p>
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.http;

import com.chiorichan.factory.api.Builtin;
import com.chiorichan.factory.api.Server;
import com.chiorichan.util.WebFunc;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
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
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.chiorichan.AppConfig;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.http.ErrorEvent;
import com.chiorichan.event.http.HttpExceptionEvent;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.lang.HttpError;
import com.chiorichan.logger.experimental.LogEvent;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.session.Session;
import com.chiorichan.session.SessionException;
import com.chiorichan.util.ObjectFunc;
import com.chiorichan.util.Versioning;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

/**
 * Wraps the Netty HttpResponse to provide easy methods for manipulating the result of each request
 */
public class HttpResponseWrapper
{
	Charset encoding = Charsets.UTF_8;
	final Map<String, String> headers = Maps.newHashMap();
	ApacheHandler htaccess = null;
	String httpContentType = "text/html";
	HttpResponseStatus httpStatus = HttpResponseStatus.OK;
	final LogEvent log;
	ByteBuf output = Unpooled.buffer();
	final Map<String, String> annotations = Maps.newHashMap();
	final HttpRequestWrapper request;
	HttpResponseStage stage = HttpResponseStage.READING;

	protected HttpResponseWrapper( HttpRequestWrapper request, LogEvent log )
	{
		this.request = request;
		this.log = log;
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

	public String getAnnotation( String key )
	{
		return annotations.get( key );
	}

	public int getHttpCode()
	{
		return httpStatus.code();
	}

	public String getHttpMsg()
	{
		return HttpCode.msg( httpStatus.code() );
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

	/**
	 * @return HttpResponseStage
	 */
	public HttpResponseStage getStage()
	{
		return stage;
	}

	public boolean isCommitted()
	{
		return stage == HttpResponseStage.CLOSED || stage == HttpResponseStage.WRITTEN;
	}

	@Deprecated
	public void print( byte[] bytes ) throws IOException
	{
		write( bytes );
	}

	/**
	 * Prints a single string of text to the buffered output
	 *
	 * @param var string of text.
	 * @throws IOException if there was a problem with the output buffer.
	 */
	public void print( String var ) throws IOException
	{
		if ( var != null && !var.isEmpty() )
			write( var.getBytes( encoding ) );
	}

	/**
	 * Prints a single string of text with a line return to the buffered output
	 *
	 * @param var string of text.
	 * @throws IOException if there was a problem with the output buffer.
	 */
	public void println( String var ) throws IOException
	{
		if ( var != null && !var.isEmpty() )
			write( ( var + "\n" ).getBytes( encoding ) );
	}

	public void resetBuffer()
	{
		output = Unpooled.buffer();
	}

	public void sendError( Exception e ) throws IOException
	{
		if ( e instanceof HttpError )
			sendError( ( ( HttpError ) e ).getHttpCode(), ( ( HttpError ) e ).getReason(), ( ( HttpError ) e ).getMessage() );
		else
			sendError( 500, e.getMessage() );
	}

	public void sendError( HttpResponseStatus status ) throws IOException
	{
		sendError( status, null, null );
	}

	public void sendError( HttpResponseStatus status, String httpMsg ) throws IOException
	{
		sendError( status, httpMsg, null );
	}

	public void sendError( HttpResponseStatus status, String httpMsg, String msg ) throws IOException
	{
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access sendError method within this HttpResponse because the connection has been closed." );

		if ( httpMsg == null )
			httpMsg = status.reasonPhrase().toString();

		// NetworkManager.getLogger().info( ConsoleColor.RED + "HttpError{httpCode=" + status.code() + ",httpMsg=" + httpMsg + ",subdomain=" + request.getSubDomain() + ",domain=" + request.getDomain() + ",uri=" + request.getUri() +
		// ",remoteIp=" + request.getIpAddr() + "}" );

		if ( msg == null || msg.length() > 255 )
			log.log( Level.SEVERE, "%s {code=%s}", httpMsg, status.code() );
		else
			log.log( Level.SEVERE, "%s {code=%s,reason=%s}", httpMsg, status.code(), msg );

		resetBuffer();

		// Trigger an internal Error Event to notify plugins of a possible problem.
		ErrorEvent event = new ErrorEvent( request, status.code(), httpMsg );
		EventBus.instance().callEvent( event );

		if ( event.getErrorHtml() == null || event.getErrorHtml().length() == 0 )
		{
			boolean printHtml = true;

			if ( htaccess != null && htaccess.getErrorDocument( status.code() ) != null )
			{
				String resp = htaccess.getErrorDocument( status.code() ).getResponse();

				if ( resp.startsWith( "/" ) )
				{
					sendRedirect( request.getBaseUrl() + resp );
					printHtml = false;
				}
				else if ( resp.startsWith( "http" ) )
				{
					sendRedirect( resp );
					printHtml = false;
				}
				else
					httpMsg = resp;
			}

			if ( printHtml )
			{
				println( "<html><head>" );
				println( "<title>" + status.code() + " - " + httpMsg + "</title>" );
				println( "<style>body { margin: 0; padding: 0; } h1, h2, h3, h4, h5, h6 { margin: 0; } .container { padding: 8px; } .debug-header { display: block; margin: 15px 0 0; font-size: 18px; color: #303030; font-weight: bold; } #debug-table { border: 1px solid; width: 100%; } #debug-table thead { background-color: #eee; } #debug-table #col_0 { width: 20%; min-width: 130px; overflow: hidden; font-weight: bold; color: #463C54; padding-right: 5px; } #debug-table #tblStringRow { color: rgba(0, 0, 0, .3); font-weight: 300; }</style>" );
				println( "</head><body>" );

				println( "<div class=\"container\" style=\" background-color: #eee; \">" );
				println( "<h1>" + status.code() + " - " + httpMsg + "</h1>" );
				println( "</div>" );
				println( "<div class=\"container\">" );

				if ( msg != null && !msg.isEmpty() )
					println( "<p>" + msg + "</p>" );

				if ( Versioning.isDevelopment() )
				{
					println( "<h3>Debug &amp; Environment Details:</h3>" );

					println( "<span class=\"debug-header\">GET Data</span>" );
					printMap( request.getGetMap() );

					println( "<span class=\"debug-header\">POST Data</span>" );
					printMap( request.getPostMap() );

					println( "<span class=\"debug-header\">Files</span>" );
					Collection<UploadedFile> files = request.getUploadedFiles().values();
					ArrayList<Object> tbl = new ArrayList<Object>()
					{{
						if ( files == null || files.size() == 0 )
							add( "empty" );
						else
							for ( UploadedFile file : files )
								add( new ArrayList<Object>()
								{{
									add( file.getOrigFileName() );
									add( file.getFileSize() );
									add( file.getMimeType() );
									add( file.getMD5() );
								}} );
					}};
					List<String> cols = new ArrayList<String>()
					{{
						add( "Filename" );
						add( "Mime Type" );
						add( "File Size" );
						add( "MD5 Hash" );
					}};
					println( Builtin.createTable( tbl, cols, "debug-table" ) );

					println( "<span class=\"debug-header\">Cookies</span>" );
					printMap( new HashMap<String, Object>()
					{{
						for ( HttpCookie cookie : request.getCookies() )
							put( cookie.getKey(), cookie.getValue() );
						for ( HttpCookie cookie : request.getServerCookies() )
							put( cookie.getKey() + " <span style=\"color: #eee; font-weight: 300;\">(protected)</span>", cookie.getValue() );
					}} );

					if ( request.hasSession() )
					{
						println( "<span class=\"debug-header\">Session</span>" );
						printMap( request.getSession().getDataMap() );
					}

					println( "<span class=\"debug-header\">Server/Header Data</span>" );
					printMap( request.getServer().asMap() );

					// TODO Environment Variables
				}

				println( "<hr>" );
				println( "<small>Running <a href=\"https://github.com/ChioriGreene/ChioriWebServer\">" + Versioning.getProduct() + "</a> Version " + Versioning.getVersion() + " (Build #" + Versioning.getBuildNumber() + ")<br />" + Versioning.getCopyright() + "</small>" );
				println( "</div>" );
				println( "</body></html>" );

				sendResponse();
			}
		}
		else
		{
			print( event.getErrorHtml() );
			sendResponse();
		}
	}

	private void printMap( Map<String, ?> map ) throws IOException
	{
		ArrayList<Object> tbl = new ArrayList<Object>()
		{{
			if ( map == null || map.size() == 0 )
				add( "empty" );
			else
				for ( Entry<String, ?> e : map.entrySet() )
					add( new ArrayList<Object>()
					{{
						add( "<b>" + WebFunc.escapeHTML( e.getKey() ) + "</b>" );
						try
						{
							add( WebFunc.escapeHTML( ( String ) e.getValue() ) );
						}
						catch ( ClassCastException e )
						{
							add( "(non-string)" );
						}
					}} );
		}};

		println( Builtin.createTable( tbl, "debug-table" ) );
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

	public void sendException( Throwable cause ) throws IOException
	{
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access sendException method within this HttpResponse because the connection has been closed." );

		if ( cause instanceof HttpError )
		{
			sendError( ( HttpError ) cause );
			return;
		}

		HttpExceptionEvent event = new HttpExceptionEvent( request, cause, AppConfig.get().getBoolean( "server.developmentMode" ) );
		EventBus.instance().callEvent( event );

		int httpCode = event.getHttpCode();

		if ( httpCode < 1 )
			httpCode = 500;

		httpStatus = HttpResponseStatus.valueOf( httpCode );

		// NetworkManager.getLogger().info( ConsoleColor.RED + "HttpError{httpCode=" + httpCode + ",httpMsg=" + HttpCode.msg( httpCode ) + ",domain=" + request.getSubDomain() + "." + request.getDomain() + ",uri=" + request.getUri() +
		// ",remoteIp=" + request.getIpAddr() + "}" );

		if ( Versioning.isDevelopment() )
		{
			if ( event.getErrorHtml() != null )
			{
				log.log( Level.SEVERE, "%s {code=500}", HttpCode.msg( 500 ) );

				resetBuffer();
				print( event.getErrorHtml() );
				sendResponse();
			}
			else
			{
				String stackTrace = ExceptionUtils.getStackTrace( cause );

				if ( request.getEvalFactory() != null )
					for ( Entry<String, ScriptingContext> e : request.getEvalFactory().stack().getScriptTraceHistory().entrySet() )
						stackTrace = stackTrace.replace( e.getKey(), e.getValue().filename() );

				sendError( httpStatus, null, "<pre>" + stackTrace + "</pre>" );
			}
		}
		else
		{
			StringBuilder sb = new StringBuilder();
			sb.append( "<p>The server encountered an exception and unforchantly the server is not in development mode, so no debug information is available.</p>\n" );
			sb.append( "<p>If you are the server owner or developer, you can turn development on by changing 'server.developmentMode' to true in the config file.</p>\n" );
			sendError( 500, null, sb.toString() );
		}
	}

	/**
	 * Sends the client to the site login page found in configuration and also sends a please login message along with it.
	 */
	public void sendLoginPage()
	{
		sendLoginPage( "You must be logged in to view this page" );
	}

	/**
	 * Sends the client to the site login page
	 *
	 * @param msg The message to pass to the login page
	 */
	public void sendLoginPage( String msg )
	{
		sendLoginPage( msg, null );
	}

	/**
	 * Sends the client to the site login page
	 *
	 * @param msg   The message to pass to the login page
	 * @param level The severity level of this login page redirect
	 */
	public void sendLoginPage( String msg, String level )
	{
		sendLoginPage( msg, level, null );
	}

	/**
	 * Sends the client to the site login page
	 *
	 * @param msg    The message to pass to the login page
	 * @param level  The severity level of this login page redirect
	 * @param target The target to redirect to once we receive a successful login
	 */
	public void sendLoginPage( String msg, String level, String target )
	{
		Nonce nonce = request.getSession().getNonce();
		nonce.mapValues( "msg", msg );
		nonce.mapValues( "level", level == null || level.length() == 0 ? "danger" : level );
		nonce.mapValues( "target", target == null || target.length() == 0 ? request.getFullUrl() : target );
		String loginForm = request.getLocation().getLoginForm();
		if ( !loginForm.toLowerCase().startsWith( "http" ) )
			loginForm = ( request.isSecure() ? "https://" : "http://" ) + loginForm;
		sendRedirect( String.format( "%s?%s=%s", loginForm, nonce.key(), nonce.value() ) );
	}

	public void sendMultipart( byte[] bytesToWrite ) throws IOException
	{
		if ( request.method() == HttpMethod.HEAD )
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
				if ( c.needsUpdating() )
					h.add( "Set-Cookie", c.toHeaderValue() );

			if ( h.get( "Server" ) == null )
				h.add( "Server", Versioning.getProduct() + " Version " + Versioning.getVersion() );

			h.add( "Access-Control-Allow-Origin", request.getLocation().getConfig().getString( "site.web-allowed-origin", "*" ) );
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
				public void operationComplete( ChannelProgressiveFuture future ) throws Exception
				{
					NetworkManager.getLogger().info( "Transfer complete." );
				}

				@Override
				public void operationProgressed( ChannelProgressiveFuture future, long progress, long total )
				{
					if ( total < 0 )
						NetworkManager.getLogger().info( "Transfer progress: " + progress );
					else
						NetworkManager.getLogger().info( "Transfer progress: " + progress + " / " + total );
				}
			} );
		}
	}

	/**
	 * Send the client to a specified page with http code 302 automatically.
	 *
	 * @param target The destination URL. Can either be relative or absolute.
	 */
	public void sendRedirect( String target )
	{
		sendRedirect( target, 302 );
	}

	/**
	 * Sends the client to a specified page with specified http code but with the option to not automatically go.
	 *
	 * @param target     The destination url. Can be relative or absolute.
	 * @param httpStatus What http code to use.
	 */
	public void sendRedirect( String target, int httpStatus )
	{
		sendRedirect( target, httpStatus, null );
	}

	public void sendRedirect( String target, int httpStatus, Map<String, String> nonceValues )
	{
		// NetworkManager.getLogger().info( ConsoleColor.DARK_GRAY + "Sending page redirect to `" + target + "` using httpCode `" + httpStatus + " - " + HttpCode.msg( httpStatus ) + "`" );
		log.log( Level.INFO, "Redirect {uri=%s,httpCode=%s,status=%s}", target, httpStatus, HttpCode.msg( httpStatus ) );

		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access sendRedirect method within this HttpResponse because the connection has been closed." );

		if ( nonceValues != null && nonceValues.size() > 0 )
		{
			target += ( target.contains( "?" ) ? "&" : "?" ) + request.getSession().getNonce().query();
			request.getSession().nonce().mapValues( nonceValues );
		}

		if ( !isCommitted() )
		{
			setStatus( httpStatus );
			setHeader( "Location", target );
		}
		else
			try
			{
				sendError( 301, "The requested URL has been relocated to '" + target + "'" );
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

	public void sendRedirect( String target, Map<String, String> nonceValues )
	{
		sendRedirect( target, 302, nonceValues );
	}

	public void sendRedirectRepost( String target )
	{
		sendRedirect( target, request.getHttpVersion() == HttpVersion.HTTP_1_0 ? 302 : 307 );
	}

	/**
	 * Sends the data to the client. Internal Use.
	 *
	 * @throws IOException if there was a problem sending the data, like the connection was unexpectedly closed.
	 */
	public void sendResponse() throws IOException
	{
		if ( stage == HttpResponseStage.CLOSED || stage == HttpResponseStage.WRITTEN )
			return;

		FullHttpResponse response = new DefaultFullHttpResponse( HttpVersion.HTTP_1_1, httpStatus, output );
		HttpHeaders h = response.headers();

		if ( request.hasSession() )
		{
			Session session = request.getSession();

			/**
			 * Initiate the Session Persistence Method.
			 * This is usually done with a cookie but we should make a param optional
			 */
			session.processSessionCookie( request.getDomain() );

			for ( HttpCookie c : session.getCookies().values() )
				if ( c.needsUpdating() )
					h.add( HttpHeaderNames.SET_COOKIE, c.toHeaderValue() );

			if ( session.getSessionCookie().needsUpdating() )
				h.add( HttpHeaderNames.SET_COOKIE, session.getSessionCookie().toHeaderValue() );
		}

		/*
		 * We define all header keys as lowercase to support HTTP/2 requirements while also not
		 * violating HTTP/1.x requirements.  New header names should always be lowercase.
		 * We apologize that there is currently no way to disable this behavior.
		 */

		h.set( HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE );
		h.add( HttpHeaderNames.SERVER, Versioning.getProduct() + " Version " + Versioning.getVersion() );
		h.setInt( HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes() );

		// This might be a temporary measure - TODO Properly set the charset for each request.
		h.set( HttpHeaderNames.CONTENT_TYPE, httpContentType + "; charset=" + encoding.name() );

		h.add( HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, request.getLocation().getConfig().getString( "site.web-allowed-origin", "*" ) );

		for ( Entry<String, String> header : headers.entrySet() )
			h.add( header.getKey().toLowerCase(), header.getValue() );

		// Expires: Wed, 08 Apr 2015 02:32:24 GMT
		// DateTimeFormatter formatter = DateTimeFormat.forPattern( "EE, dd-MMM-yyyy HH:mm:ss zz" );

		// h.set( HttpHeaderNames.EXPIRES, formatter.print( DateTime.now( DateTimeZone.UTC ).plusDays( 1 ) ) );
		// h.set( HttpHeaderNames.CACHE_CONTROL, "public, max-age=86400" );

		stage = HttpResponseStage.WRITTEN;

		request.getChannel().writeAndFlush( response );
	}

	public void setAnnotation( String key, String val )
	{
		annotations.put( key, val );
	}

	public void setApacheParser( ApacheHandler htaccess )
	{
		this.htaccess = htaccess;
	}

	public void setContentLength( long length )
	{
		setHeader( "Content-Length", length );
	}

	/**
	 * Sets the ContentType header.
	 *
	 * @param type, e.g., text/html or application/xml
	 */
	public void setContentType( String type )
	{
		if ( type == null || type.isEmpty() )
			type = "text/html";

		httpContentType = type;
	}

	public void setEncoding( Charset encoding )
	{
		this.encoding = encoding;
	}

	public void setEncoding( String encoding )
	{
		this.encoding = Charset.forName( encoding );
	}

	public void setHeader( String key, Object val )
	{
		headers.put( key, ObjectFunc.castToStringWithException( val ) );
	}

	public void setStatus( HttpResponseStatus httpStatus )
	{
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access setStatus( status ) method within this HttpResponse because the connection has been closed." );

		this.httpStatus = httpStatus;
	}

	public void setStatus( int status )
	{
		setStatus( HttpResponseStatus.valueOf( status ) );
	}

	/**
	 * Redirects the current page load to a secure HTTPS connection
	 */
	public boolean switchToSecure()
	{
		if ( !NetworkManager.isHttpsRunning() )
		{
			log.log( Level.SEVERE, "We were going to attempt to switch to a secure HTTPS connection and aborted due to the HTTPS server not running." );
			return false;
		}

		if ( request.isSecure() )
			return true;

		sendRedirectRepost( request.getFullUrl( true ) + request.getQuery() );
		return true;
	}

	/**
	 * Redirects the current page load to an unsecure HTTP connection
	 */
	public boolean switchToUnsecure()
	{
		if ( !NetworkManager.isHttpRunning() )
		{
			log.log( Level.SEVERE, "We were going to attempt to switch to an unsecure HTTP connection and aborted due to the HTTP server not running." );
			return false;
		}

		if ( !request.isSecure() )
			return true;

		sendRedirectRepost( request.getFullUrl( false ) + request.getQuery() );
		return true;
	}

	/**
	 * Writes a byte array to the buffered output.
	 *
	 * @param bytes byte array to print
	 * @throws IOException if there was a problem with the output buffer.
	 */
	public void write( byte[] bytes ) throws IOException
	{
		if ( stage != HttpResponseStage.MULTIPART )
			stage = HttpResponseStage.WRITTING;

		output.writeBytes( bytes );
	}

	/**
	 * Writes a ByteBuf to the buffered output
	 *
	 * @param buf byte buffer to print
	 * @throws IOException if there was a problem with the output buffer.
	 */
	public void write( ByteBuf buf ) throws IOException
	{
		if ( stage != HttpResponseStage.MULTIPART )
			stage = HttpResponseStage.WRITTING;

		output.writeBytes( buf.retain() );
	}
}
