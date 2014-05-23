package com.chiorichan.http;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.chiorichan.Loader;
import com.chiorichan.event.EventException;
import com.chiorichan.event.server.RenderEvent;
import com.chiorichan.event.server.RequestEvent;
import com.chiorichan.event.server.ServerVars;
import com.chiorichan.exceptions.HttpErrorException;
import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.framework.Evaling;
import com.chiorichan.framework.Site;
import com.chiorichan.util.Versioning;
import com.google.common.collect.Maps;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * This class is not thread safe.
 * Be sure to keep all references out of global name space in case of concurrent requests.
 * 
 * @author Chiori Greene
 */
public class WebHandler implements HttpHandler
{
	protected static Map<ServerVars, Object> staticServerVars = Maps.newLinkedHashMap();
	
	public WebHandler()
	{
		// Initalize Static Server Vars
		staticServerVars.put( ServerVars.SERVER_SOFTWARE, Versioning.getProduct() );
		staticServerVars.put( ServerVars.SERVER_ADMIN, Loader.getConfig().getString( "server.admin", "webmaster@example.com" ) );
		staticServerVars.put( ServerVars.SERVER_SIGNATURE, Versioning.getProduct() + " Version " + Versioning.getVersion() );
	}
	
	@Override
	public void handle( HttpExchange t ) throws IOException
	{
		HttpRequest request = new HttpRequest( t );
		HttpResponse response = request.getResponse();
		
		// TODO Catch Broken Pipes.
		try
		{
			handleHttp( request, response );
		}
		catch ( HttpErrorException e )
		{
			if ( e.getHttpCode() < 400 && e.getHttpCode() > 499 )
				e.printStackTrace();
			
			response.sendError( e );
			return;
		}
		catch ( IndexOutOfBoundsException | NullPointerException | IOException e )
		{
			/**
			 * TODO!!! Proper Exception Handling. PRETTY EXCEPTIONS A MUST FOR HTTP RESPONSE!!!
			 * We should even consider the ability to have these exceptions cached and/or delivered to your e-mail address.
			 */
			if ( e instanceof IOException && e.getCause() != null )
			{
				e.getCause().printStackTrace();
				response.sendError( 500, null, "<pre>" + ExceptionUtils.getStackTrace( e.getCause() ) + "</pre>" );
			}
			else
			{
				e.printStackTrace();
				response.sendError( 500, null, "<pre>" + ExceptionUtils.getStackTrace( e ) + "</pre>" );
			}
		}
		catch ( Exception e )
		{
			// Temp until all exceptions can be found!
			Loader.getLogger().warning( "WARNING THIS IS AN UNCAUGHT EXCEPTION! PLEASE FIX THE CODE!" );
			e.printStackTrace();
		}
		finally
		{
			/*
			 * PersistentSession sess = request.getSessionNoWarning();
			 * if ( sess != null )
			 * {
			 * sess.releaseResources();
			 * sess.saveSession();
			 * }
			 */
			
			response.sendResponse();
			
			request.getSession().getEvaling().reset();
			
			// Too many files open error. Is this a fix? FIFO Pipes.
			request.getOriginal().getRequestBody().close();
			request.getOriginal().getResponseBody().close();
			
			t.close();
		}
	}
	
	public void handleHttp( HttpRequest request, HttpResponse response ) throws IOException, HttpErrorException
	{
		String uri = request.getURI();
		String domain = request.getParentDomain();
		String subdomain = request.getSubDomain();
		
		Site currentSite = Loader.getSiteManager().getSiteByDomain( domain );
		
		if ( currentSite == null )
			if ( domain.isEmpty() )
				currentSite = Loader.getSiteManager().getSiteById( "framework" );
			else
				currentSite = new Site( "default", Loader.getConfig().getString( "framework.sites.defaultTitle", "Unnamed Chiori Framework Site" ), domain );
		
		request.setSite( currentSite );
		
		request.initSession();
		
		request.initServerVars( staticServerVars );
		
		PersistentSession sess = request.getSession();
		
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
		
		// Throws IOException and HttpErrorException
		WebInterpreter fi = new WebInterpreter( request );
		
		Loader.getLogger().info( "&dNew page request '" + subdomain + "." + domain + "' '" + uri + "' '" + fi.toString() + "'" );
		
		request.rewriteVars.putAll( fi.getRewriteParams() );
		
		response.setContentType( fi.getContentType() );
		
		String file = fi.get( "file" );
		String html = fi.get( "html" );
		
		if ( file == null )
			file = "";
		
		if ( html == null )
			html = "";
		
		if ( file.isEmpty() && html.isEmpty() )
			throw new HttpErrorException( 500, "Internal Server Error Encountered While Rendering Request" );
		
		File docRoot = currentSite.getAbsoluteRoot( subdomain );
		
		File requestFile = null;
		if ( !file.isEmpty() )
		{
			if ( currentSite.protectCheck( file ) )
				throw new HttpErrorException( 401, "Loading of this page (" + file + ") is not allowed since its hard protected in the site configs." );
			
			requestFile = new File( file );
			sess.setGlobal( "__FILE__", requestFile );
			
			docRoot = new File( requestFile.getParent() );
		}
		
		request.putServerVar( ServerVars.DOCUMENT_ROOT, docRoot );
		
		sess.setGlobal( "_SERVER", request.getServerStrings() );
		sess.setGlobal( "_REQUEST", request.getRequestMap() );
		sess.setGlobal( "_POST", request.getPostMap() );
		sess.setGlobal( "_GET", request.getGetMap() );
		sess.setGlobal( "_REWRITE", request.getRewriteVars() );
		
		Evaling eval = sess.getEvaling();
		eval.reset(); // Reset eval so any left over output from any previous requests does not leak into this request.
		
		String req = fi.get( "reqlevel" );
		
		if ( !req.equals( "-1" ) )
			if ( sess.getCurrentUser() == null )
			{
				String loginForm = request.getSite().getYaml().getString( "scripts.login-form", "/login" );
				Loader.getLogger().warning( "Requester of page '" + file + "' has been redirected to the login page." );
				response.sendRedirect( loginForm + "?msg=You must be logged in to view that page!&target=" + request.getURI() );
				// TODO: Come up with a better way to handle the URI used in the target. ie. Params are lost.
				return;
			}
			else if ( !sess.getCurrentUser().hasPermission( req ) )
			{
				if ( req.equals( "0" ) )
					response.sendError( 401, "This page is limited to Operators only!" );
				
				response.sendError( 401, "This page is limited to users with access to the \"" + req + "\" permission." );
			}
		
		try
		{
			// Enhancement: Allow html to be ran under different shells. Default is GROOVY.
			if ( !html.isEmpty() )
				if ( !eval.shellExecute( "groovy", html ) )
					eval.write( html.getBytes( "ISO-8859-1" ) );
		}
		catch ( ShellExecuteException e )
		{
			throw new IOException( "Exception encountered during shell execution of requested file.", e );
		}
		
		try
		{
			if ( requestFile != null )
				if ( !eval.shellExecute( fi.get( "shell" ), fi ) )
					eval.write( fi.getContent() );
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
		
		String source = currentSite.applyAlias( eval.reset() );
		
		RenderEvent renderEvent = new RenderEvent( sess, source, fi.getParams() );
		
		try
		{
			Loader.getEventBus().callEventWithException( renderEvent );
			
			if ( renderEvent.sourceChanged() )
				source = renderEvent.getSource();
		}
		catch ( EventException ex )
		{
			throw new IOException( "Exception encountered during render event call, most likely the fault of a plugin.", ex );
		}
		
		response.getOutput().write( source.getBytes( "ISO-8859-1" ) );
	}
}
