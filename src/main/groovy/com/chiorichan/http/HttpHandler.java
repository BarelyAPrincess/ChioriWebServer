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

import static io.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.ConsoleLogger;
import com.chiorichan.Loader;
import com.chiorichan.event.EventException;
import com.chiorichan.event.server.RenderEvent;
import com.chiorichan.event.server.RequestEvent;
import com.chiorichan.event.server.ServerVars;
import com.chiorichan.exception.HttpErrorException;
import com.chiorichan.exception.ShellExecuteException;
import com.chiorichan.factory.CodeEvalFactory;
import com.chiorichan.factory.CodeMetaData;
import com.chiorichan.framework.Site;
import com.chiorichan.framework.SiteException;
import com.chiorichan.http.session.SessionProvider;
import com.chiorichan.util.Versioning;
import com.google.common.collect.Maps;

public class HttpHandler extends SimpleChannelInboundHandler<HttpObject>
{
	protected static Map<ServerVars, Object> staticServerVars = Maps.newLinkedHashMap();
	
	private HttpRequest requestOrig;
	private HttpRequestWrapper request;
	private HttpResponseWrapper response;
	private HttpPostRequestDecoder decoder;
	private static DirectoryInterpreter dirInter = new DirectoryInterpreter();
	private static HttpDataFactory factory;
	
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
		
		DiskFileUpload.deleteOnExitTemporaryFile = true;
		DiskFileUpload.baseDirectory = Loader.getTempFileDirectory().getAbsolutePath();
		DiskAttribute.deleteOnExitTemporaryFile = true;
		DiskAttribute.baseDirectory = Loader.getTempFileDirectory().getAbsolutePath();
	}
	
	public HttpHandler()
	{
		// Initalize Static Server Vars
		staticServerVars.put( ServerVars.SERVER_SOFTWARE, Versioning.getProduct() );
		staticServerVars.put( ServerVars.SERVER_ADMIN, Loader.getConfig().getString( "server.admin", "webmaster@example.com" ) );
		staticServerVars.put( ServerVars.SERVER_SIGNATURE, Versioning.getProduct() + " Version " + Versioning.getVersion() );
	}
	
	@Override
	public void channelInactive( ChannelHandlerContext ctx ) throws Exception
	{
		if ( decoder != null )
		{
			decoder.cleanFiles();
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
	protected void messageReceived( ChannelHandlerContext ctx, HttpObject msg ) throws Exception
	{
		if ( msg instanceof HttpRequest )
		{
			requestOrig = ( HttpRequest ) msg;
			request = new HttpRequestWrapper( ctx.channel(), requestOrig );
			response = request.getResponse();
			
			if ( is100ContinueExpected( ( HttpRequest ) msg ) )
				send100Continue( ctx );
			
			Site currentSite = request.getSite();
			
			File tmpFileDirectory = ( currentSite != null ) ? currentSite.getTempFileDirectory() : Loader.getTempFileDirectory();
			
			if ( !tmpFileDirectory.exists() )
				tmpFileDirectory.mkdirs();
			
			if ( !tmpFileDirectory.isDirectory() )
				Loader.getLogger().severe( "The temp directory specified in the server configs is not a directory, File Uploads will FAIL until this problem is resolved." );
			
			if ( !tmpFileDirectory.canWrite() )
				Loader.getLogger().severe( "The temp directory specified in the server configs is not writable, File Uploads will FAIL until this problem is resolved." );
			
			DiskFileUpload.baseDirectory = tmpFileDirectory.getAbsolutePath();
			DiskAttribute.baseDirectory = tmpFileDirectory.getAbsolutePath();
			
			if ( request.getMethod().equals( HttpMethod.GET ) )
			{
				return;
			}
			
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
		else if ( msg instanceof HttpContent )
		{
			HttpContent chunk = ( HttpContent ) msg;
			
			request.addContentLength( chunk.content().readableBytes() );
			
			if ( decoder != null )
			{
				try
				{
					decoder.offer( chunk );
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
				
				if ( chunk instanceof LastHttpContent )
					finishRequest();
			}
			else
				finishRequest();
		}
	}
	
	private void finishRequest() throws IOException
	{
		try
		{
			handleHttp( request, response );
		}
		catch ( HttpErrorException e )
		{
			response.sendError( e );
			return;
		}
		catch ( IndexOutOfBoundsException | NullPointerException | IOException | SiteException e )
		{
			/**
			 * TODO!!! Proper Exception Handling. Consider the ability to have these exceptions cached and/or delivered by e-mail to developer.
			 */
			if ( e instanceof IOException && e.getCause() != null )
			{
				e.getCause().printStackTrace();
				response.sendException( e.getCause() );
			}
			else
			{
				e.printStackTrace();
				response.sendException( e );
			}
		}
		catch ( Exception e )
		{
			/**
			 * XXX Temporary way of capturing exceptions that were unexpected by the server.
			 * Exceptions caught here should have proper exception captures implemented.
			 */
			Loader.getLogger().warning( "WARNING THIS IS AN UNCAUGHT EXCEPTION! WOULD YOU KINDLY REPORT THIS STACKTRACE TO THE DEVELOPER?", e );
		}
		
		try
		{
			SessionProvider sess = request.getSession( false );
			if ( sess != null )
			{
				sess.saveSession( false );
				sess.onFinished();
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		
		response.sendResponse();
		reset();
	}
	
	private void reset()
	{
		request = null;
		response = null;
		if ( decoder != null )
		{
			decoder.destroy();
			decoder = null;
		}
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
			
			request.getPostMap().put( attribute.getName(), value );
		}
		else if ( data.getHttpDataType() == HttpDataType.FileUpload )
		{
			FileUpload fileUpload = ( FileUpload ) data;
			if ( fileUpload.isCompleted() )
			{
				Loader.getLogger().debug( "" + fileUpload );
				
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
				Loader.getLogger().warning( "File to be continued but should not!" );
			}
		}
	}
	
	private static void send100Continue( ChannelHandlerContext ctx )
	{
		FullHttpResponse response = new DefaultFullHttpResponse( HTTP_1_1, CONTINUE );
		ctx.write( response );
	}
	
	@Override
	public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception
	{
		cause.printStackTrace();
		ctx.close();
	}
	
	public void handleHttp( HttpRequestWrapper request, HttpResponseWrapper response ) throws IOException, HttpErrorException, SiteException
	{
		String uri = request.getURI();
		String domain = request.getParentDomain();
		String subdomain = request.getSubDomain();
		
		request.initServerVars( staticServerVars );
		
		SessionProvider sess = request.getSession();
		
		RequestEvent requestEvent = new RequestEvent( request );
		
		try
		{
			Loader.getEventBus().callEventWithException( requestEvent );
		}
		catch ( EventException ex )
		{
			throw new IOException( "Exception encountered during request event call, most likely the fault of a plugin.", ex );
		}
		
		if ( requestEvent.isCancelled() )
		{
			Loader.getLogger().warning( "Navigation was cancelled by a Server Plugin" );
			
			int status = requestEvent.getStatus();
			String reason = requestEvent.getReason();
			
			if ( status < 400 && status > 599 )
			{
				status = 502;
				reason = "Navigation Cancelled by Internal Plugin Event";
			}
			
			response.sendError( status, reason );
			return;
		}
		
		if ( response.isCommitted() )
			return;
		
		// Throws IOException and HttpErrorException
		WebInterpreter fi = new WebInterpreter( request );
		
		Site currentSite = request.getSite();
		sess.getParentSession().setSite( currentSite );
		
		Loader.getLogger().info( "Request '" + currentSite.getSiteId() + "' '" + subdomain + "." + domain + "' '" + uri + "' '" + fi.toString() + "'" );
		
		if ( fi.isDirectoryRequest() )
		{
			dirInter.processDirectoryListing( this, fi );
			return;
		}
		
		request.putRewriteParams( fi.getRewriteParams() );
		
		response.setContentType( fi.getContentType() );
		response.setEncoding( fi.getEncoding() );
		
		String file = fi.get( "file" );
		String html = fi.get( "html" );
		
		if ( file == null )
			file = "";
		
		if ( html == null )
			html = "";
		
		if ( file.isEmpty() && html.isEmpty() )
			throw new HttpErrorException( 500, "Internal Server Error Encountered While Handling Request" );
		
		File docRoot = currentSite.getAbsoluteRoot( subdomain );
		
		File requestFile = null;
		if ( !file.isEmpty() )
		{
			if ( currentSite.protectCheck( file ) )
				throw new HttpErrorException( 401, "Loading of this page (" + file + ") is not allowed since its hard protected in the configs." );
			
			requestFile = new File( docRoot, file );
			sess.setGlobal( "__FILE__", requestFile );
		}
		
		request.putServerVar( ServerVars.DOCUMENT_ROOT, docRoot );
		
		sess.setGlobal( "_SERVER", request.getServerStrings() );
		sess.setGlobal( "_POST", request.getPostMapParsed() );
		sess.setGlobal( "_GET", request.getGetMapParsed() );
		sess.setGlobal( "_REWRITE", request.getRewriteMap() );
		sess.setGlobal( "_FILES", request.getUploadedFiles() );
		
		if ( Loader.getConfig().getBoolean( "advanced.security.requestMapEnabled", true ) )
			sess.setGlobal( "_REQUEST", request.getRequestMapParsed() );
		
		StringBuilder source = new StringBuilder();
		CodeEvalFactory factory = sess.getCodeFactory();
		factory.setEncoding( fi.getEncoding() );
		
		String req = fi.get( "reqperm" );
		
		if ( req == null )
			req = "-1";
		
		/**
		 * -1 = Allow All!
		 * 0 = OP Only!
		 * 1 = Valid Accounts Only!
		 * All Others = Per Account
		 */
		
		if ( !req.equals( "-1" ) )
			if ( sess.getParentSession().getAccount() == null )
			{
				String loginForm = request.getSite().getYaml().getString( "scripts.login-form", "/login" );
				// Loader.getLogger().warning( "Requester of page '" + file + "' has been redirected to the login page." );
				response.sendRedirect( loginForm + "?msg=You must be logged in to view that page!&target=http://" + request.getDomain() + request.getURI() );
				// TODO: Come up with a better way to handle the URI used in the target, i.e., currently params are being lost in the redirect.
				return;
			}
			else if ( !req.equals( "1" ) && !sess.getParentSession().checkPermission( req ).isTrue() )
			{
				if ( req.equals( "0" ) )
					response.sendError( 401, "This page is limited to Operators only!" );
				else
					response.sendError( 401, "This page is limited to users with access to the \"" + req + "\" permission." );
				return;
			}
		
		try
		{
			// Enhancement: Allow html to be ran under different shells. Default is embedded.
			if ( !html.isEmpty() )
			{
				CodeMetaData meta = new CodeMetaData();
				meta.shell = "embedded";
				meta.contentType = fi.getContentType();
				meta.params = Maps.newHashMap();
				meta.params.putAll( fi.getRewriteParams() );
				meta.params.putAll( request.getGetMap() );
				source.append( factory.eval( html, meta, currentSite ) );
			}
		}
		catch ( ShellExecuteException e )
		{
			throw new IOException( "Exception encountered during shell execution of requested file.", e );
		}
		
		try
		{
			if ( !file.isEmpty() )
			{
				CodeMetaData meta = new CodeMetaData();
				meta.params = Maps.newHashMap();
				meta.params.putAll( request.getRewriteMap() );
				meta.params.putAll( request.getGetMap() );
				source.append( factory.eval( fi, meta, currentSite ) );
			}
		}
		catch ( ShellExecuteException e )
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
					throw new HttpErrorException( 500, "Internal Server Error encountered during multipart execution." );
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
		
		RenderEvent renderEvent = new RenderEvent( sess, source.toString(), fi.getParams() );
		
		try
		{
			Loader.getEventBus().callEventWithException( renderEvent );
			
			if ( renderEvent.sourceChanged() )
				source = new StringBuilder( renderEvent.getSource() );
		}
		catch ( EventException ex )
		{
			throw new IOException( "Exception encountered during render event call, most likely the fault of a plugin.", ex );
		}
		
		response.getOutput().write( source.toString().getBytes( fi.getEncoding() ) );
	}
	
	protected HttpRequestWrapper getRequest()
	{
		return request;
	}
	
	protected HttpResponseWrapper getResponse()
	{
		return response;
	}
	
	public static ConsoleLogger getLogger()
	{
		return Loader.getLogger( "HttpHdl" );
	}
}
