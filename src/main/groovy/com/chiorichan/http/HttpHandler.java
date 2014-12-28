package com.chiorichan.http;

import static io.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.Loader;
import com.chiorichan.bus.bases.EventException;
import com.chiorichan.bus.events.server.RenderEvent;
import com.chiorichan.bus.events.server.RequestEvent;
import com.chiorichan.bus.events.server.ServerVars;
import com.chiorichan.exceptions.HttpErrorException;
import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.factory.CodeEvalFactory;
import com.chiorichan.factory.CodeMetaData;
import com.chiorichan.framework.Site;
import com.chiorichan.framework.SiteException;
import com.chiorichan.http.session.SessionProvider;
import com.chiorichan.util.Versioning;
import com.google.common.collect.Maps;

public class HttpHandler extends SimpleChannelInboundHandler<Object>
{
	protected static Map<ServerVars, Object> staticServerVars = Maps.newLinkedHashMap();
	protected HttpRequestWrapper request;
	protected HttpResponseWrapper response;
	
	public HttpHandler()
	{
		// Initalize Static Server Vars
		staticServerVars.put( ServerVars.SERVER_SOFTWARE, Versioning.getProduct() );
		staticServerVars.put( ServerVars.SERVER_ADMIN, Loader.getConfig().getString( "server.admin", "webmaster@example.com" ) );
		staticServerVars.put( ServerVars.SERVER_SIGNATURE, Versioning.getProduct() + " Version " + Versioning.getVersion() );
	}
	
	@Override
	public void channelReadComplete( ChannelHandlerContext ctx ) throws Exception
	{
		ctx.flush();
	}
	
	@Override
	protected void messageReceived( ChannelHandlerContext ctx, Object msg ) throws Exception
	{
		if ( msg instanceof HttpRequest )
		{
			request = new HttpRequestWrapper( ctx.channel(), (HttpRequest) msg );
			response = request.getResponse();
			
			if ( is100ContinueExpected( (HttpRequest) msg ) )
			{
				send100Continue( ctx );
			}
			
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
				 * TODO!!! Proper Exception Handling. Consider the ability to have these exceptions cached and/or delivered by e-mail.
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
					// response.sendError( 500, null, "<pre>" + ExceptionUtils.getStackTrace( e ) + "</pre>" );
				}
			}
			catch ( Exception e )
			{
				/**
				 * XXX Temporary way of capturing exception that were unexpected by the server.
				 * Exceptions caught here should have proper exception captures implemented.
				 */
				Loader.getLogger().warning( "WARNING THIS IS AN UNCAUGHT EXCEPTION! PLEASE FIX THE CODE!", e );
			}
			
			try
			{
				SessionProvider sess = request.getSessionNoWarning();
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
			
			//response.sendResponse();
		}
		
		if ( msg instanceof HttpContent )
		{
			HttpContent httpContent = (HttpContent) msg;
			
			Loader.getLogger().debug( "Got a HTTPCONTENT: " + httpContent );
			
			response.sendResponse();
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
		Site currentSite = request.getSite();
		
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
		WebInterpreter fi = new WebInterpreter( request, currentSite.getRoutes() );
		
		Loader.getLogger().info( "Request '" + subdomain + "." + domain + "' '" + uri + "' '" + fi.toString() + "'" );
		
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
				throw new HttpErrorException( 401, "Loading of this page (" + file + ") is not allowed since its hard protected in the site configs." );
			
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
		
		// Deprecated!!!
		if ( req == null )
			req = fi.get( "reqlevel" );
		
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
				Loader.getLogger().warning( "Requester of page '" + file + "' has been redirected to the login page." );
				response.sendRedirect( loginForm + "?msg=You must be logged in to view that page!&target=http://" + request.getDomain() + request.getURI() );
				// TODO: Come up with a better way to handle the URI used in the target. ie. Params are lost.
				return;
			}
			else if ( !req.equals( "1" ) && !sess.getParentSession().getAccount().hasPermission( req ) )
			{
				if ( req.equals( "0" ) )
					response.sendError( 401, "This page is limited to Operators only!" );
				
				response.sendError( 401, "This page is limited to users with access to the \"" + req + "\" permission." );
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
}
