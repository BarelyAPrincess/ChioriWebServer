/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.http;

import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.codehaus.groovy.runtime.NullObject;

import com.chiorichan.ConsoleColor;
import com.chiorichan.ContentTypes;
import com.chiorichan.Loader;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventException;
import com.chiorichan.event.server.RenderEvent;
import com.chiorichan.event.server.RequestEvent;
import com.chiorichan.factory.EvalExecutionContext;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.factory.EvalFactoryResult;
import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.lang.ApacheParser;
import com.chiorichan.lang.ErrorReporting;
import com.chiorichan.lang.EvalException;
import com.chiorichan.lang.EvalMultipleException;
import com.chiorichan.lang.HttpError;
import com.chiorichan.lang.SiteException;
import com.chiorichan.logger.LogEvent;
import com.chiorichan.logger.LogManager;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.net.NetworkSecurity;
import com.chiorichan.net.NetworkSecurity.IpStrikeType;
import com.chiorichan.permission.lang.PermissionDeniedException;
import com.chiorichan.permission.lang.PermissionDeniedException.PermissionDeniedReason;
import com.chiorichan.permission.lang.PermissionException;
import com.chiorichan.session.Session;
import com.chiorichan.session.SessionException;
import com.chiorichan.site.Site;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.ObjectFunc;
import com.chiorichan.util.Versioning;
import com.chiorichan.util.WebFunc;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Handles both HTTP and HTTPS connections for Netty.
 */
public class HttpHandler extends SimpleChannelInboundHandler<Object>
{
	private static HttpDataFactory factory;
	
	protected static Map<ServerVars, Object> staticServerVars = Maps.newLinkedHashMap();
	static
	{
		/**
		 * Determines the minimum file size required to create a temporary file.
		 * See {@link DefaultHttpDataFactory#DefaultHttpDataFactory(boolean)} and {@link DefaultHttpDataFactory#DefaultHttpDataFactory(long)}
		 */
		long minsize = Loader.getConfig().getLong( "server.fileUploadMinInMemory", DefaultHttpDataFactory.MINSIZE );
		
		if ( minsize < 1 ) // Less then 1kb = always
			factory = new DefaultHttpDataFactory( true );
		if ( minsize > 102400 ) // Greater then 100mb = never
			factory = new DefaultHttpDataFactory( false );
		else
			factory = new DefaultHttpDataFactory( minsize );
		
		setTempDirectory( Loader.getTempFileDirectory() );
		
		// Initialize static server variables
		staticServerVars.put( ServerVars.SERVER_SOFTWARE, Versioning.getProduct() );
		staticServerVars.put( ServerVars.SERVER_ADMIN, Loader.getConfig().getString( "server.admin", "me@chiorichan.com" ) );
		staticServerVars.put( ServerVars.SERVER_SIGNATURE, Versioning.getProduct() + " Version " + Versioning.getVersion() );
	}
	
	private HttpPostRequestDecoder decoder;
	
	private WebSocketServerHandshaker handshaker = null;
	
	private LogEvent log;
	private HttpRequestWrapper request;
	private boolean requestFinished = false;
	private FullHttpRequest requestOrig;
	private HttpResponseWrapper response;
	
	private boolean ssl;
	
	public HttpHandler( boolean ssl )
	{
		this.ssl = ssl;
		log = LogManager.logEvent( "" + hashCode() );
	}
	
	private static void send100Continue( ChannelHandlerContext ctx )
	{
		FullHttpResponse response = new DefaultFullHttpResponse( HTTP_1_1, CONTINUE );
		ctx.write( response );
	}
	
	public static void setTempDirectory( File tmpDir )
	{
		// TODO Config option to delete temporary files on exit?
		// DiskFileUpload.deleteOnExitTemporaryFile = true;
		// DiskAttribute.deleteOnExitTemporaryFile = true;
		
		DiskFileUpload.baseDirectory = tmpDir.getAbsolutePath();
		DiskAttribute.baseDirectory = tmpDir.getAbsolutePath();
	}
	
	@Override
	public void channelInactive( ChannelHandlerContext ctx ) throws Exception
	{
		if ( decoder != null )
		{
			decoder.cleanFiles();
			decoder.destroy();
			decoder = null;
		}
		
		// Nullify references
		handshaker = null;
		response = null;
		requestOrig = null;
		request = null;
		log = null;
		requestFinished = false;
	}
	
	@Override
	public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception
	{
		try
		{
			if ( request == null || response == null )
			{
				NetworkManager.getLogger().severe( ConsoleColor.NEGATIVE + "" + ConsoleColor.RED + "We got an unexpected exception before the connection was processed:", cause );
				
				StringBuilder sb = new StringBuilder();
				sb.append( "<h1>500 - Internal Server Error</h1>\n" );
				sb.append( "<p>The server had encountered an unexpected exception before it could process your request, so no debug information is available.</p>\n" );
				sb.append( "<p>The exception has been logged to the console, we can only hope the exception is noticed and resolved. We apoligize for any inconvenience.</p>\n" );
				sb.append( "<p><i>You have a good day now and we will see you again soon. :)</i></p>\n" );
				sb.append( "<hr>\n" );
				sb.append( "<small>Running <a href=\"https://github.com/ChioriGreene/ChioriWebServer\">" + Versioning.getProduct() + "</a> Version " + Versioning.getVersion() + " (Build #" + Versioning.getBuildNumber() + ")<br />" + Versioning.getCopyright() + "</small>" );
				
				FullHttpResponse response = new DefaultFullHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf( 500 ), Unpooled.wrappedBuffer( sb.toString().getBytes() ) );
				ctx.write( response );
				
				return;
			}
			
			String ip = request.getIpAddr();
			
			if ( requestFinished && cause instanceof HttpError )
			{
				int code = ( ( HttpError ) cause ).getHttpCode();
				
				if ( code >= 400 && code <= 499 )
					NetworkSecurity.addStrikeToIp( ip, IpStrikeType.HTTP_ERROR_400 );
				if ( code >= 500 && code <= 599 )
					NetworkSecurity.addStrikeToIp( ip, IpStrikeType.HTTP_ERROR_500 );
				
				if ( response.getStage() != HttpResponseStage.CLOSED )
					response.sendError( ( HttpError ) cause );
				else
					NetworkManager.getLogger().severe( ConsoleColor.NEGATIVE + "" + ConsoleColor.RED + " [" + ip + "] For reasons unknown, we caught the HttpError but the connection was already closed.", cause );
				return;
			}
			
			if ( requestFinished && "Connection reset by peer".equals( cause.getMessage() ) )
			{
				NetworkManager.getLogger().warning( ConsoleColor.NEGATIVE + "" + ConsoleColor.RED + " [" + ip + "] The connection was closed before we could finish the request, if the IP continues to abuse the system it WILL BE BANNED!" );
				NetworkSecurity.addStrikeToIp( ip, IpStrikeType.CLOSED_EARLY );
				return;
			}
			
			EvalException evalOrig = null;
			
			/*
			 * Unpackage the EvalFactoryException.
			 * Not sure if exceptions from the EvalFactory should be handled differently or not.
			 * XXX Maybe skip generating exception pages for errors that were caused internally and report them to Chiori-chan unless the server is in development mode?
			 */
			if ( cause instanceof EvalException && cause.getCause() != null )
			{
				evalOrig = ( EvalException ) cause;
				cause = cause.getCause();
			}
			
			/*
			 * Presently we can only send one exception to the client
			 * So for now we only send the most severe one
			 */
			if ( cause instanceof EvalMultipleException )
			{
				EvalException most = null;
				
				// The lower the intValue() to more important it became
				for ( EvalException e : ( ( EvalMultipleException ) cause ).getExceptions() )
					if ( most == null || most.errorLevel().intValue() > e.errorLevel().intValue() )
						most = e;
				
				evalOrig = most;
				cause = most.getCause();
			}
			
			/*
			 * TODO Proper Exception Handling. Consider the ability to have these exceptions cached and/or delivered by e-mail to developer and/or server administrator.
			 */
			if ( cause instanceof HttpError )
				response.sendError( ( HttpError ) cause );
			else if ( cause instanceof PermissionDeniedException )
			{
				PermissionDeniedException pde = ( PermissionDeniedException ) cause;
				
				if ( pde.getReason() == PermissionDeniedReason.LOGIN_PAGE )
					response.sendLoginPage( pde.getReason().getMessage() );
				else
					/*
					 * TODO generate a special permission denied page for these!!!
					 */
					response.sendError( ( ( PermissionDeniedException ) cause ).getHttpCode(), cause.getMessage() );
			}
			else if ( evalOrig == null )
			{
				// Was not caught by EvalFactory
				log.log( Level.SEVERE, ConsoleColor.NEGATIVE + "" + ConsoleColor.RED + "Exception %s thrown in file '%s' at line %s, message '%s'", cause.getClass().getName(), cause.getStackTrace()[0].getFileName(), cause.getStackTrace()[0].getLineNumber(), cause.getMessage() );
				response.sendException( cause );
			}
			else
			{
				if ( evalOrig.isScriptingException() )
				{
					ScriptTraceElement element = evalOrig.getScriptTrace()[0];
					log.log( Level.SEVERE, ConsoleColor.NEGATIVE + "" + ConsoleColor.RED + "Exception %s thrown in file '%s' at line %s:%s, message '%s'", cause.getClass().getName(), element.context().filename(), element.getLineNumber(), ( element.getColumnNumber() > 0 ) ? element.getColumnNumber() : 0, cause.getMessage() );
				}
				else
					log.log( Level.SEVERE, ConsoleColor.NEGATIVE + "" + ConsoleColor.RED + "Exception %s thrown in file '%s' at line %s, message '%s'", cause.getClass().getName(), cause.getStackTrace()[0].getFileName(), cause.getStackTrace()[0].getLineNumber(), cause.getMessage() );
				
				response.sendException( evalOrig );
			}
			
			// Loader.getLogger().warning( "Could not run file '" + fileName + "' because of error '" + t.getMessage() + "'" );
			
			finish();
		}
		catch ( Throwable t )
		{
			Loader.getLogger().severe( ConsoleColor.NEGATIVE + "" + ConsoleColor.RED + "This is an uncaught exception from the exceptionCaught() method:", t );
			// ctx.fireExceptionCaught( t );
		}
	}
	
	private void finish()
	{
		try
		{
			log.log( Level.INFO, "%s {code=%s}", response.getHttpMsg(), response.getHttpCode() );
			
			if ( !response.isCommitted() )
				response.sendResponse();
			
			EvalFactory factory;
			if ( ( factory = request.getEvalFactory() ) != null )
				factory.onFinished();
			
			Session sess;
			if ( ( sess = request.getSessionWithoutException() ) != null )
				sess.save();
			
			requestFinished = true;
			
			// Loader.getDatabase().queryUpdate( "INSERT INTO `testing` (`epoch`, `time`, `uri`, `result`, ip) VALUES ('" + Timings.epoch() + "', '" + Timings.mark( this ) + "', 'http://" + request.getDomain() + request.getUri() + "', '" +
			// response.getHttpCode() + "', '" + request.getIpAddr() + "');" );
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
		}
	}
	
	@Override
	public void flush( ChannelHandlerContext ctx ) throws Exception
	{
		log.flushAndClose();
		ctx.flush();
	}
	
	public HttpRequestWrapper getRequest()
	{
		return request;
	}
	
	public HttpResponseWrapper getResponse()
	{
		return response;
	}
	
	public Session getSession()
	{
		return request.getSession();
	}
	
	public void handleHttp( HttpRequestWrapper request, HttpResponseWrapper response ) throws IOException, HttpError, SiteException, PermissionException, EvalMultipleException, EvalException, SessionException
	{
		// String uri = request.getUri();
		// String domain = request.getParentDomain();
		String subdomain = request.getSubDomain();
		
		log.log( Level.INFO, request.getMethodString() + " " + request.getFullUrl() );
		
		request.startSession();
		
		request.initServerVars( staticServerVars );
		
		Session sess = request.getSession();
		
		log.log( Level.FINE, "Session {id=%s,timeout=%s,new=%s}", sess.getSessId(), sess.getTimeout(), sess.isNew() );
		
		if ( sess.isLoginPresent() )
			log.log( Level.FINE, "Account {id=%s,displayName=%s}", sess.getAcctId(), sess.getDisplayName() );
		
		if ( response.getStage() == HttpResponseStage.CLOSED )
			return;
		
		RequestEvent requestEvent = new RequestEvent( request );
		
		try
		{
			EventBus.INSTANCE.callEventWithException( requestEvent );
		}
		catch ( EventException ex )
		{
			throw new IOException( "Exception encountered during request event call, most likely the fault of a plugin.", ex );
		}
		
		response.setStatus( requestEvent.getStatus() );
		
		if ( requestEvent.isCancelled() )
		{
			int status = requestEvent.getStatus();
			String reason = requestEvent.getReason();
			
			if ( status == 200 )
			{
				status = 502;
				reason = "Navigation Cancelled by Plugin Event";
			}
			
			NetworkManager.getLogger().warning( "Navigation was cancelled by Plugin Event" );
			
			throw new HttpError( status, reason );
		}
		
		if ( response.isCommitted() )
			return;
		
		// Throws IOException and HttpError
		WebInterpreter fi = new WebInterpreter( request );
		
		Site currentSite = request.getSite();
		sess.setSite( currentSite ); // setSite?
		File docRoot = currentSite.getAbsoluteRoot( subdomain );
		
		ApacheParser htaccess = new ApacheParser().appendWithDir( docRoot );
		
		response.setApacheParser( htaccess );
		
		if ( fi.getStatus() != HttpResponseStatus.OK )
			throw new HttpError( fi.getStatus() );
		
		// TODO Improve the result of having no content to display, maybe empty page and log it to console.
		if ( !fi.hasFile() && !fi.hasHTML() )
			throw new HttpError( 500, null, "We found what appears to be a mapping for your request but it contained no content to display, deffinite bug." );
		
		// NetworkManager.getLogger().info( ConsoleColor.BLUE + "Http" + ( ( ssl ) ? "s" : "" ) + "Request{httpCode=" + response.getHttpCode() + ",httpMsg=" + response.getHttpMsg() + ",subdomain=" + subdomain + ",domain=" + domain + ",uri="
		// + uri + ",remoteIp=" + request.getIpAddr() + ",sessionId=" + sess.getSessId() + ",acct=" + sess.isLoginPresent() + ( sess.isLoginPresent() ? "(" + sess.getAcctId() + ")" : "" ) + ",details=" + fi.toString() + "}" );
		
		if ( fi.hasFile() )
			htaccess.appendWithDir( fi.getFile().getParentFile() );
		
		sess.setGlobal( "__FILE__", fi.getFile() );
		
		request.putRewriteParams( fi.getRewriteParams() );
		response.setContentType( fi.getContentType() );
		response.setEncoding( fi.getEncoding() );
		
		request.putServerVar( ServerVars.DOCUMENT_ROOT, docRoot );
		
		request.setGlobal( "_SERVER", request.getServerStrings() );
		request.setGlobal( "_POST", request.getPostMap() );
		request.setGlobal( "_GET", request.getGetMap() );
		request.setGlobal( "_REWRITE", request.getRewriteMap() );
		request.setGlobal( "_FILES", request.getUploadedFiles() );
		
		if ( !request.getUploadedFiles().isEmpty() )
			log.log( Level.INFO, "Uploads {" + Joiner.on( "," ).join( request.getUploadedFiles().values() ) + "}" );
		
		if ( !request.getGetMap().isEmpty() )
			log.log( Level.INFO, "Params GET {" + Joiner.on( "," ).withKeyValueSeparator( "=" ).join( request.getGetMap() ) + "}" );
		
		if ( !request.getPostMap().isEmpty() )
			log.log( Level.INFO, "Params POST {" + Joiner.on( "," ).withKeyValueSeparator( "=" ).join( request.getPostMap() ) + "}" );
		
		if ( !request.getRewriteMap().isEmpty() )
			log.log( Level.INFO, "Params REWRITE {" + Joiner.on( "," ).withKeyValueSeparator( "=" ).join( request.getRewriteMap() ) + "}" );
		
		if ( Loader.getConfig().getBoolean( "advanced.security.requestMapEnabled", true ) )
			request.setGlobal( "_REQUEST", request.getRequestMap() );
		
		ByteBuf rendered = Unpooled.buffer();
		
		EvalFactory factory = request.getEvalFactory();
		factory.setEncoding( fi.getEncoding() );
		
		NetworkSecurity.isForbidden( htaccess, currentSite, fi );
		
		String req = fi.get( "reqperm" );
		
		if ( req == null )
			req = "-1";
		
		sess.requirePermission( req );
		
		// Enhancement: Allow HTML to be ran under different shells. Default is embedded.
		if ( fi.hasHTML() )
		{
			EvalFactoryResult result = factory.eval( EvalExecutionContext.fromSource( fi.getHTML(), "<html>" ).request( request ).site( currentSite ) );
			
			if ( result.hasExceptions() )
				// TODO Print notices to output like PHP does
				for ( EvalException e : result.getExceptions() )
				{
					ErrorReporting.throwExceptions( e );
					
					log.exceptions( e );
					if ( e.errorLevel().isEnabledLevel() )
						rendered.writeBytes( e.getMessage().getBytes() );
				}
			
			if ( result.isSuccessful() )
			{
				rendered.writeBytes( result.content() );
				if ( result.getObject() != null && ! ( result.getObject() instanceof NullObject ) )
					try
					{
						rendered.writeBytes( ObjectFunc.castToStringWithException( result.getObject() ).getBytes() );
					}
					catch ( Exception e )
					{
						e.printStackTrace();
					}
			}
			
			log.log( Level.INFO, "EvalHtml {file=%s,timing=%sms,success=%s}", fi.getFilePath(), Timings.mark( this ), result.isSuccessful() );
		}
		
		if ( fi.hasFile() )
		{
			if ( fi.isDirectoryRequest() )
			{
				processDirectoryListing( fi );
				return;
			}
			
			EvalFactoryResult result = factory.eval( EvalExecutionContext.fromFile( fi ).request( request ).site( currentSite ) );
			
			if ( result.hasExceptions() )
				// TODO Print notices to output like PHP does
				for ( EvalException e : result.getExceptions() )
				{
					ErrorReporting.throwExceptions( e );
					
					log.exceptions( e );
					if ( e.errorLevel().isEnabledLevel() )
						rendered.writeBytes( e.getMessage().getBytes() );
				}
			
			if ( result.isSuccessful() )
			{
				rendered.writeBytes( result.content() );
				if ( result.getObject() != null && ! ( result.getObject() instanceof NullObject ) )
					try
					{
						rendered.writeBytes( ObjectFunc.castToStringWithException( result.getObject() ).getBytes() );
					}
					catch ( Exception e )
					{
						e.printStackTrace();
					}
			}
			
			log.log( Level.INFO, "EvalFile {file=%s,timing=%sms,success=%s}", fi.getFilePath(), Timings.mark( this ), result.isSuccessful() );
		}
		
		// if the connection was in a MultiPart mode, wait for the mode to change then return gracefully.
		if ( response.stage == HttpResponseStage.MULTIPART )
		{
			while ( response.stage == HttpResponseStage.MULTIPART )
				// I wonder if there is a better way to handle on going multipart response.
				try
				{
					Thread.sleep( 100 );
				}
				catch ( InterruptedException e )
				{
					throw new HttpError( 500, "Internal Server Error encountered during multipart execution." );
				}
			
			return;
		}
		// If the connection was closed from page redirect, return gracefully.
		else if ( response.stage == HttpResponseStage.CLOSED || response.stage == HttpResponseStage.WRITTEN )
			return;
		
		// Allows scripts to directly override interpreter values. For example: Themes, Views, Titles
		for ( Entry<String, String> kv : response.pageDataOverrides.entrySet() )
			fi.put( kv.getKey(), kv.getValue() );
		
		RenderEvent renderEvent = new RenderEvent( this, rendered, fi.getEncoding(), fi.getParams() );
		
		try
		{
			EventBus.INSTANCE.callEventWithException( renderEvent );
			if ( renderEvent.getSource() != null )
				rendered = renderEvent.getSource();
		}
		catch ( EventException ex )
		{
			throw new EvalException( ErrorReporting.E_ERROR, "Caught EventException while trying to fire the RenderEvent", ex.getCause(), factory.getShellFactory() );
		}
		
		log.log( Level.INFO, "Written {bytes=%s,total_timing=%sms}", rendered.readableBytes(), Timings.finish( this ) );
		
		response.write( rendered );
	}
	
	@Override
	protected void messageReceived( ChannelHandlerContext ctx, Object msg ) throws Exception
	{
		Timings.start( this );
		
		if ( msg instanceof FullHttpRequest )
		{
			if ( !Loader.hasFinishedStartup() )
			{
				// Outputs a very crude raw message if we are running in a low level mode a.k.a. Startup or Reload.
				// Much of the server API is unavailable while the server is in this mode, that is why we do this.
				
				StringBuilder sb = new StringBuilder();
				sb.append( "<h1>503 - Service Unavailable</h1>\n" );
				sb.append( "<p>I'm sorry to have to be the one to tell you this but the server is currently unavailable.</p>\n" );
				sb.append( "<p>This is most likely due to many possibilities, most commonly being it's currently booting up. Which would be great news because it means your request should succeed if you try again.</p>\n" );
				sb.append( "<p>But it is also possible that the server is actually running in a low level mode or could be offline for some other reason. If you feel this is a mistake, might I suggest you talk with the server admin.</p>\n" );
				sb.append( "<p><i>You have a good day now and we will see you again soon. :)</i></p>\n" );
				sb.append( "<hr>\n" );
				sb.append( "<small>Running <a href=\"https://github.com/ChioriGreene/ChioriWebServer\">" + Versioning.getProduct() + "</a> Version " + Versioning.getVersion() + " (Build #" + Versioning.getBuildNumber() + ")<br />" + Versioning.getCopyright() + "</small>" );
				
				FullHttpResponse response = new DefaultFullHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf( 503 ), Unpooled.wrappedBuffer( sb.toString().getBytes() ) );
				ctx.write( response );
				
				return;
			}
			
			requestFinished = false;
			requestOrig = ( FullHttpRequest ) msg;
			request = new HttpRequestWrapper( ctx.channel(), requestOrig, ssl, log );
			response = request.getResponse();
			
			log.header( "[id: %s, %s:%s => %s:%s]", hashCode(), request.getIpAddr(), request.getRemotePort(), request.getLocalIpAddr(), request.getLocalPort() );
			
			if ( HttpHeaderUtil.is100ContinueExpected( ( HttpRequest ) msg ) )
				send100Continue( ctx );
			
			if ( NetworkSecurity.isIpBanned( request.getIpAddr() ) )
			{
				response.sendError( 403 );
				return;
			}
			
			Site currentSite = request.getSite();
			
			File tmpFileDirectory = ( currentSite != null ) ? currentSite.getTempFileDirectory() : Loader.getTempFileDirectory();
			
			setTempDirectory( tmpFileDirectory );
			
			if ( request.isWebsocketRequest() )
			{
				try
				{
					WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory( request.getWebSocketLocation( requestOrig ), null, true );
					handshaker = wsFactory.newHandshaker( requestOrig );
					if ( handshaker == null )
						WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse( ctx.channel() );
					else
						handshaker.handshake( ctx.channel(), requestOrig );
				}
				catch ( WebSocketHandshakeException e )
				{
					NetworkManager.getLogger().severe( "A request was made on the websocket uri '/fw/websocket' but it failed to handshake for reason '" + e.getMessage() + "'." );
					response.sendError( 500, null, "This URI is for websocket requests only<br />" + e.getMessage() );
				}
				return;
			}
			
			if ( !request.getMethod().equals( HttpMethod.GET ) )
				try
				{
					decoder = new HttpPostRequestDecoder( factory, requestOrig );
				}
				catch ( ErrorDataDecoderException e )
				{
					e.printStackTrace();
					response.sendException( e );
					return;
				}
			
			request.contentSize += requestOrig.content().readableBytes();
			
			if ( decoder != null )
			{
				try
				{
					decoder.offer( requestOrig );
				}
				catch ( ErrorDataDecoderException e )
				{
					e.printStackTrace();
					response.sendError( e );
					// ctx.channel().close();
					return;
				}
				catch ( IllegalArgumentException e )
				{
					// TODO Handle this further? maybe?
					// java.lang.IllegalArgumentException: empty name
				}
				readHttpDataChunkByChunk();
			}
			
			handleHttp( request, response );
			
			finish();
		}
		else if ( msg instanceof WebSocketFrame )
		{
			WebSocketFrame frame = ( WebSocketFrame ) msg;
			
			// Check for closing frame
			if ( frame instanceof CloseWebSocketFrame )
			{
				handshaker.close( ctx.channel(), ( CloseWebSocketFrame ) frame.retain() );
				return;
			}
			
			if ( frame instanceof PingWebSocketFrame )
			{
				ctx.channel().write( new PongWebSocketFrame( frame.content().retain() ) );
				return;
			}
			
			if ( ! ( frame instanceof TextWebSocketFrame ) )
				throw new UnsupportedOperationException( String.format( "%s frame types are not supported", frame.getClass().getName() ) );
			
			String request = ( ( TextWebSocketFrame ) frame ).text();
			NetworkManager.getLogger().fine( "Received '" + request + "' over WebSocket connection '" + ctx.channel() + "'" );
			ctx.channel().write( new TextWebSocketFrame( request.toUpperCase() ) );
		}
		else if ( msg instanceof DefaultHttpRequest )
		{
			// Do Nothing!
		}
		else
			NetworkManager.getLogger().warning( "Received Object '" + msg.getClass() + "' and had nothing to do with it, is this a bug?" );
	}
	
	public void processDirectoryListing( WebInterpreter fi ) throws HttpError, IOException
	{
		File dir = fi.getFile();
		
		if ( !dir.exists() || !dir.isDirectory() )
			throw new HttpError( 500 );
		
		response.setContentType( "text/html" );
		response.setEncoding( Charsets.UTF_8 );
		
		File[] files = dir.listFiles();
		List<Object> tbl = Lists.newArrayList();
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat sdf = new SimpleDateFormat( "dd-MMM-yyyy HH:mm:ss" );
		
		sb.append( "<style>.altrowstable { border-spacing: 12px; }</style>" );
		sb.append( "<h1>Index of " + request.getUri() + "</h1>" );
		
		for ( File f : files )
		{
			List<String> l = Lists.newArrayList();
			String type = ContentTypes.getContentType( f );
			String mainType = ( type.contains( "/" ) ) ? type.substring( 0, type.indexOf( "/" ) ) : type;
			
			l.add( "<img src=\"/fw/icons/" + mainType + "\" />" );
			l.add( "<a href=\"" + request.getUri() + "/" + f.getName() + "\">" + f.getName() + "</a>" );
			l.add( sdf.format( f.lastModified() ) );
			
			if ( f.isDirectory() )
				l.add( "-" );
			else
			{
				InputStream stream = null;
				try
				{
					URL url = f.toURI().toURL();
					stream = url.openStream();
					l.add( String.valueOf( stream.available() ) + "kb" );
				}
				finally
				{
					if ( stream != null )
						stream.close();
				}
			}
			
			l.add( type );
			
			tbl.add( l );
		}
		
		sb.append( WebFunc.createTable( tbl, Arrays.asList( new String[] {"", "Name", "Last Modified", "Size", "Type"} ) ) );
		sb.append( "<hr>" );
		sb.append( "<small>Running <a href=\"https://github.com/ChioriGreene/ChioriWebServer\">" + Versioning.getProduct() + "</a> Version " + Versioning.getVersion() + "<br />" + Versioning.getCopyright() + "</small>" );
		
		response.print( sb.toString() );
		response.sendResponse();
		
		// throw new HttpErrorException( 403, "Sorry, Directory Listing has not been implemented on this Server!" );
	}
	
	private void readHttpDataChunkByChunk() throws IOException
	{
		try
		{
			while ( decoder.hasNext() )
			{
				InterfaceHttpData data = decoder.next();
				if ( data != null )
					try
					{
						writeHttpData( data );
					}
					finally
					{
						// This method deletes the temp file from disk!
						// data.release();
					}
			}
		}
		catch ( EndOfDataDecoderException e )
		{
			// END OF CONTENT
		}
	}
	
	private void writeHttpData( InterfaceHttpData data ) throws IOException
	{
		if ( data.getHttpDataType() == HttpDataType.Attribute )
		{
			Attribute attribute = ( Attribute ) data;
			String value;
			try
			{
				value = attribute.getValue();
			}
			catch ( IOException e )
			{
				e.printStackTrace();
				response.sendException( e );
				return;
			}
			
			request.putPostMap( attribute.getName(), value );
			
			/*
			 * Should resolve the problem described in Issue #9 on our GitHub
			 */
			attribute.delete();
		}
		else if ( data.getHttpDataType() == HttpDataType.FileUpload )
		{
			FileUpload fileUpload = ( FileUpload ) data;
			if ( fileUpload.isCompleted() )
				try
				{
					request.putUpload( fileUpload.getName(), new UploadedFile( fileUpload ) );
				}
				catch ( IOException e )
				{
					e.printStackTrace();
					response.sendException( e );
				}
			else
				NetworkManager.getLogger().warning( "File to be continued but should not!" );
		}
	}
}
