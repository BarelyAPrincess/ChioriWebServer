/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.http;

import static io.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
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

import org.codehaus.groovy.runtime.NullObject;

import com.chiorichan.ConsoleColor;
import com.chiorichan.ContentTypes;
import com.chiorichan.Loader;
import com.chiorichan.event.EventException;
import com.chiorichan.event.server.RenderEvent;
import com.chiorichan.event.server.RequestEvent;
import com.chiorichan.event.server.ServerVars;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.factory.EvalFactoryResult;
import com.chiorichan.factory.EvalMetaData;
import com.chiorichan.lang.ApacheParser;
import com.chiorichan.lang.EvalFactoryException;
import com.chiorichan.lang.HttpError;
import com.chiorichan.lang.SiteException;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.net.NetworkSecurity;
import com.chiorichan.permission.PermissionDefault;
import com.chiorichan.permission.PermissionResult;
import com.chiorichan.session.SessionProvider;
import com.chiorichan.site.Site;
import com.chiorichan.util.ObjectFunc;
import com.chiorichan.util.Versioning;
import com.chiorichan.util.WebFunc;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Handles both HTTP and HTTPS connections for Netty.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class HttpHandler extends SimpleChannelInboundHandler<Object>
{
	protected static Map<ServerVars, Object> staticServerVars = Maps.newLinkedHashMap();
	
	private WebSocketServerHandshaker handshaker = null;
	private static HttpDataFactory factory;
	private HttpPostRequestDecoder decoder;
	private HttpResponseWrapper response;
	private FullHttpRequest requestOrig;
	private HttpRequestWrapper request;
	private boolean ssl;
	
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
		staticServerVars.put( ServerVars.SERVER_ADMIN, Loader.getConfig().getString( "server.admin", "webmaster@example.com" ) );
		staticServerVars.put( ServerVars.SERVER_SIGNATURE, Versioning.getProduct() + " Version " + Versioning.getVersion() );
	}
	
	public HttpHandler( boolean ssl )
	{
		this.ssl = ssl;
		
		
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
	}
	
	@Override
	public void channelReadComplete( ChannelHandlerContext ctx ) throws Exception
	{
		ctx.flush();
	}
	
	@Override
	public void channelActive( final ChannelHandlerContext ctx ) throws Exception
	{
		
	}
	
	@Override
	public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception
	{
		if ( cause instanceof HttpError )
			response.sendError( ( HttpError ) cause );
		else if ( cause instanceof IOException && cause.getCause() != null )
		{
			// cause.getCause().printStackTrace();
			response.sendException( cause.getCause() );
		}
		else if ( cause instanceof IndexOutOfBoundsException || cause instanceof NullPointerException || cause instanceof IOException || cause instanceof SiteException )
		{
			/*
			 * TODO Proper Exception Handling. Consider the ability to have these exceptions cached and/or delivered by e-mail to developer and/or server administrator.
			 */
			// cause.getCause().printStackTrace();
			response.sendException( cause );
		}
		else
		{
			response.sendException( cause );
			
			/*
			 * XXX Temporary way of capturing exceptions that were unexpected by the server.
			 * Exceptions caught here should have proper exception captures implemented.
			 */
			NetworkManager.getLogger().severe( "WARNING THIS IS AN UNCAUGHT EXCEPTION! WOULD YOU KINDLY REPORT THIS STACKTRACE TO THE DEVELOPER?", cause );
		}
		
		try
		{
			finish();
		}
		catch ( Throwable t )
		{
			Loader.getLogger().debug( "Finish throw an exception!" );
			t.printStackTrace();
		}
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
	protected void messageReceived( ChannelHandlerContext ctx, Object msg ) throws Exception
	{
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
			
			
			requestOrig = ( FullHttpRequest ) msg;
			request = new HttpRequestWrapper( ctx.channel(), requestOrig, ssl );
			response = request.getResponse();
			
			if ( is100ContinueExpected( ( HttpRequest ) msg ) )
				send100Continue( ctx );
			
			if ( NetworkSecurity.isIPBanned( request.getRemoteAddr() ) )
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
					{
						WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse( ctx.channel() );
					}
					else
					{
						handshaker.handshake( ctx.channel(), requestOrig );
					}
				}
				catch ( WebSocketHandshakeException e )
				{
					NetworkManager.getLogger().severe( "A request was made on the websocket uri '/fw/websocket' but it failed to handshake for reason '" + e.getMessage() + "'." );
					response.sendError( 500, null, "This URI is for websocket requests only<br />" + e.getMessage() );
				}
				return;
			}
			
			if ( !request.getMethod().equals( HttpMethod.GET ) )
			{
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
			}
			
			request.addContentLength( requestOrig.content().readableBytes() );
			
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
					// TODO Handle This! Maybe?
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
			{
				throw new UnsupportedOperationException( String.format( "%s frame types not supported", frame.getClass().getName() ) );
			}
			
			String request = ( ( TextWebSocketFrame ) frame ).text();
			NetworkManager.getLogger().fine( "Received '" + request + "' over WebSocket connection '" + ctx.channel() + "'" );
			ctx.channel().write( new TextWebSocketFrame( request.toUpperCase() ) );
		}
		else if ( msg instanceof DefaultHttpRequest )
		{
			// Do Nothing!
		}
		else
		{
			NetworkManager.getLogger().warning( "Received Object '" + msg.getClass() + "' and had nothing to do with it, is this a bug?" );
		}
	}
	
	private void finish() throws IOException
	{
		if ( !response.isCommitted() )
			response.sendResponse();
		
		SessionProvider sess;
		if ( ( sess = request.getSession( false ) ) != null )
		{
			sess.saveSession( false );
			sess.onFinished();
			
			EvalFactory factory = sess.getEvalFactory( false );
			if ( factory != null )
				factory.onFinished();
		}
		
		request = null;
		response = null;
	}
	
	private void readHttpDataChunkByChunk() throws IOException
	{
		try
		{
			while ( decoder.hasNext() )
			{
				InterfaceHttpData data = decoder.next();
				if ( data != null )
				{
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
		}
		else if ( data.getHttpDataType() == HttpDataType.FileUpload )
		{
			FileUpload fileUpload = ( FileUpload ) data;
			if ( fileUpload.isCompleted() )
			{
				try
				{
					request.putUpload( fileUpload.getName(), new UploadedFile( fileUpload ) );
				}
				catch ( IOException e )
				{
					e.printStackTrace();
					response.sendException( e );
				}
			}
			else
			{
				NetworkManager.getLogger().warning( "File to be continued but should not!" );
			}
		}
	}
	
	private static void send100Continue( ChannelHandlerContext ctx )
	{
		FullHttpResponse response = new DefaultFullHttpResponse( HTTP_1_1, CONTINUE );
		ctx.write( response );
	}
	
	public void handleHttp( HttpRequestWrapper request, HttpResponseWrapper response ) throws IOException, HttpError, SiteException
	{
		String uri = request.getURI();
		String domain = request.getParentDomain();
		String subdomain = request.getSubDomain();
		
		request.initServerVars( staticServerVars );
		
		SessionProvider sess = request.getSession();
		
		if ( response.getStage() == HttpResponseStage.CLOSED )
			return;
		
		RequestEvent requestEvent = new RequestEvent( request );
		
		try
		{
			Loader.getEventBus().callEventWithException( requestEvent );
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
		sess.getParentSession().setSite( currentSite );
		File docRoot = currentSite.getAbsoluteRoot( subdomain );
		
		ApacheParser htaccess = new ApacheParser().appendWithDir( docRoot );
		
		response.setApacheParser( htaccess );
		
		if ( fi.getStatus() != HttpResponseStatus.OK )
			throw new HttpError( fi.getStatus() );
		
		NetworkManager.getLogger().info( ConsoleColor.BLUE + "Http" + ( ( ssl ) ? "s" : "" ) + "Request{httpCode=" + response.getHttpCode() + ",httpMsg=" + response.getHttpMsg() + ",subdomain=" + subdomain + ",domain=" + domain + ",uri=" + uri + ",remoteIp=" + request.getRemoteAddr() + ",details=" + fi.toString() + "}" );
		
		if ( !fi.hasFile() && !fi.hasHTML() )
			throw new HttpError( 500 );
		
		if ( fi.hasFile() )
			htaccess.appendWithDir( fi.getFile().getParentFile() );
		
		sess.setGlobal( "__FILE__", fi.getFile() );
		
		request.putRewriteParams( fi.getRewriteParams() );
		response.setContentType( fi.getContentType() );
		response.setEncoding( fi.getEncoding() );
		
		request.putServerVar( ServerVars.DOCUMENT_ROOT, docRoot );
		
		sess.setGlobal( "_SERVER", request.getServerStrings() );
		sess.setGlobal( "_POST", request.getPostMap() );
		sess.setGlobal( "_GET", request.getGetMap() );
		sess.setGlobal( "_REWRITE", request.getRewriteMap() );
		sess.setGlobal( "_FILES", request.getUploadedFiles() );
		
		if ( Loader.getConfig().getBoolean( "advanced.security.requestMapEnabled", true ) )
			sess.setGlobal( "_REQUEST", request.getRequestMap() );
		
		ByteBuf rendered = Unpooled.buffer();
		EvalFactory factory = sess.getEvalFactory();
		factory.setEncoding( fi.getEncoding() );
		
		NetworkSecurity.isForbidden( htaccess, currentSite, fi );
		
		String req = fi.get( "reqperm" );
		
		if ( req == null )
			req = "-1";
		
		/**
		 * -1, everybody, everyone = Allow All!
		 * 0, op, root | sys.op = OP Only!
		 * admin | sys.admin = Admin Only!
		 */
		
		PermissionResult perm = sess.getParentSession().checkPermission( req );
		
		if ( perm.getPermission() != PermissionDefault.EVERYBODY.getNode() )
		{
			if ( perm.getEntity() == null )
			{
				String loginForm = request.getSite().getYaml().getString( "scripts.login-form", "/login" );
				// NetworkManager.getLogger().warning( "Requester of page '" + file + "' has been redirected to the login page." );
				response.sendRedirect( loginForm + "?msg=You must be logged in to view that page!&target=http://" + request.getDomain() + request.getURI() );
				// TODO: Come up with a better way to handle the URI used in the target, i.e., currently params are being lost in the redirect.
				return;
			}
			
			if ( !perm.isTrue() )
			{
				if ( perm.getPermission() == PermissionDefault.OP.getNode() )
					throw new HttpError( 401, "This page is limited to Operators only!" );
				else
					throw new HttpError( 401, "This page is limited to users with access to the \"" + perm.getPermission().getNamespace() + "\" permission." );
			}
		}
		
		try
		{
			// Enhancement: Allow html to be ran under different shells. Default is embedded.
			if ( fi.hasHTML() )
			{
				EvalMetaData meta = new EvalMetaData();
				meta.shell = "embedded";
				meta.contentType = fi.getContentType();
				meta.params = Maps.newHashMap();
				meta.params.putAll( fi.getRewriteParams() );
				meta.params.putAll( request.getGetMap() );
				EvalFactoryResult result = factory.eval( fi.getHTML(), meta, currentSite );
				
				if ( result.hasExceptions() )
				{
					if ( Loader.getConfig().getBoolean( "server.throwInternalServerErrorOnWarnings", false ) )
					{
						throw new IOException( "Ignorable Exceptions were thrown, disable this behavior with the `throwInternalServerErrorOnWarnings` option in config.", result.getExceptions()[0] );
					}
					else
					{
						for ( Exception e : result.getExceptions() )
						{
							NetworkManager.getLogger().warning( e.getMessage() );
							NetworkManager.getLogger().warning( "" + e.getStackTrace()[0] );
						}
					}
				}
				
				if ( result.isSuccessful() )
				{
					rendered.writeBytes( result.getResult() );
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
			}
		}
		catch ( EvalFactoryException e )
		{
			throw new IOException( "Exception encountered during shell execution of requested file.", e );
		}
		
		try
		{
			if ( fi.hasFile() )
			{
				if ( fi.isDirectoryRequest() )
				{
					processDirectoryListing( fi );
					return;
				}
				
				EvalMetaData meta = new EvalMetaData();
				meta.params = Maps.newHashMap();
				meta.params.putAll( request.getRewriteMap() );
				meta.params.putAll( request.getGetMap() );
				EvalFactoryResult result = factory.eval( fi, meta, currentSite );
				
				if ( result.hasExceptions() )
				{
					if ( Loader.getConfig().getBoolean( "server.throwInternalServerErrorOnWarnings", false ) )
					{
						throw new IOException( "Ignorable Exceptions were thrown, disable this behavior with the `throwInternalServerErrorOnWarnings` option in config.", result.getExceptions()[0] );
					}
					else
					{
						for ( Exception e : result.getExceptions() )
						{
							NetworkManager.getLogger().warning( e.getMessage() );
							NetworkManager.getLogger().warning( "" + e.getStackTrace()[0] );
						}
					}
				}
				
				if ( result.isSuccessful() )
				{
					rendered.writeBytes( result.getResult() );
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
			}
		}
		catch ( EvalFactoryException e )
		{
			throw new IOException( "Exception encountered during shell execution of requested file.", e );
		}
		
		// TODO: Possible theme'ing of error pages.
		// if the connection was in a MultiPart mode, wait for the mode to change then return gracefully.
		if ( response.stage == HttpResponseStage.MULTIPART )
		{
			while ( response.stage == HttpResponseStage.MULTIPART )
			{
				// I wonder if there is a better way to handle an on going multipart response.
				try
				{
					Thread.sleep( 100 );
				}
				catch ( InterruptedException e )
				{
					throw new HttpError( 500, "Internal Server Error encountered during multipart execution." );
				}
			}
			
			return;
		}
		// If the connection was closed from page redirect, return gracefully.
		else if ( response.stage == HttpResponseStage.CLOSED || response.stage == HttpResponseStage.WRITTEN )
			return;
		
		// Allows scripts to directly override interpreter values. For example: Themes, Views, Titles
		for ( Entry<String, String> kv : response.pageDataOverrides.entrySet() )
		{
			fi.put( kv.getKey(), kv.getValue() );
		}
		
		RenderEvent renderEvent = new RenderEvent( sess, rendered, fi.getEncoding(), fi.getParams() );
		
		try
		{
			Loader.getEventBus().callEventWithException( renderEvent );
			if ( renderEvent.getSource() != null )
				rendered = renderEvent.getSource();
		}
		catch ( EventException ex )
		{
			throw new IOException( "Exception encountered during render event call, most likely the fault of a plugin.", ex );
		}
		
		response.write( rendered );
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
		sb.append( "<h1>Index of " + request.getURI() + "</h1>" );
		
		for ( File f : files )
		{
			List<String> l = Lists.newArrayList();
			String type = ContentTypes.getContentType( f );
			String mainType = ( type.contains( "/" ) ) ? type.substring( 0, type.indexOf( "/" ) ) : type;
			
			l.add( "<img src=\"/fw/icons/" + mainType + "\" />" );
			l.add( "<a href=\"" + request.getURI() + "/" + f.getName() + "\">" + f.getName() + "</a>" );
			l.add( sdf.format( f.lastModified() ) );
			
			if ( f.isDirectory() )
			{
				l.add( "-" );
			}
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
	
	protected HttpRequestWrapper getRequest()
	{
		return request;
	}
	
	protected HttpResponseWrapper getResponse()
	{
		return response;
	}
}
