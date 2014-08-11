package com.chiorichan.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

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
		
		// TODO Allow the ErrorEvent to change the http code and reason.
		if ( event.getErrorHtml() != null && !event.getErrorHtml().isEmpty() )
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
	
	@SuppressWarnings( "unused" )
	public void sendException( Throwable cause ) throws IOException
	{
		if ( stage == HttpResponseStage.CLOSED )
			throw new IllegalStateException( "You can't access setter methods within this HttpResponse because the connection has been closed." );
		
		if ( false )// !developmentMode )
		{
			sendError( 500, null, "<pre>" + ExceptionUtils.getStackTrace( cause ) + "</pre>" );
		}
		else
		{
			HttpExceptionEvent event = new HttpExceptionEvent( request, cause );
			Loader.getEventBus().callEvent( event );
			
			int httpCode = event.getHttpCode();
			
			if ( httpCode < 1 )
				httpCode = 500;
			
			if ( event.getErrorHtml() != null )
			{
				Loader.getLogger().warning( "HttpError: " + httpCode + " - " + HttpCode.msg( httpCode ) + "... '" + request.getSubDomain() + "." + request.getParentDomain() + "' '" + request.getURI() + "'" );
				
				output.reset();
				print( event.getErrorHtml() );
				httpStatus = httpCode;
				sendResponse();
			}
			else
				sendError( httpStatus, null, "<pre>" + ExceptionUtils.getStackTrace( cause ) + "</pre>" );
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
	
	public void sendLoginPage()
	{
		sendLoginPage( "You must be logged in to view this page!" );
	}
	
	public void sendLoginPage( String msg )
	{
		String loginPage = request.getSite().getYaml().getString( "scripts.login-form", "/login" );
		sendRedirect( loginPage + "?msg=" + msg );
	}
	
	public void sendRedirect( String target )
	{
		sendRedirect( target, 302, true );
	}
	
	public void sendRedirect( String target, int httpStatus )
	{
		sendRedirect( target, httpStatus, true );
	}
	
	// autoRedirect argument needs to be working before this method is made public
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
	
	public void print( byte[] var1 ) throws IOException
	{
		stage = HttpResponseStage.WRITTING;
		output.write( var1 );
	}
	
	public void print( String var1 ) throws IOException
	{
		if ( stage != HttpResponseStage.MULTIPART )
			stage = HttpResponseStage.WRITTING;
		
		if ( var1 != null && !var1.isEmpty() )
			output.write( var1.getBytes( encoding ) );
	}
	
	public void println( String var1 ) throws IOException
	{
		if ( stage != HttpResponseStage.MULTIPART )
			stage = HttpResponseStage.WRITTING;
		
		output.write( ( var1 + "\n" ).getBytes( encoding ) );
	}
	
	public void setContentType( String type )
	{
		if ( type == null || type.isEmpty() )
			type = "text/html";
		
		httpContentType = type;
	}
	
	public void sendResponse() throws IOException
	{
		if ( stage == HttpResponseStage.CLOSED || stage == HttpResponseStage.WRITTEN )
			return;
		
		stage = HttpResponseStage.WRITTEN;
		
		HttpExchange http = request.getOriginal();
		
		Headers h = http.getResponseHeaders();
		
		for ( Candy c : request.getCandies() )
		{
			if ( c.needsUpdating() )
				h.add( "Set-Cookie", c.toHeaderValue() );
		}
		
		if ( h.get( "Server" ) == null )
			h.add( "Server", Versioning.getProduct() + " Version " + Versioning.getVersion() );
		
		// NOTE: Why did I make it check this again?
		// if ( h.get( "Content-Type" ) == null )
		// h.add( "Content-Type", httpContentType );
		
		// This might be a temporary measure - TODO Properly set the charset for each request.
		h.set( "Content-Type", httpContentType + "; charset=" + encoding.toLowerCase() );
		
		h.add( "Access-Control-Allow-Origin", request.getSite().getYaml().getString( "web.allowed-origin", "*" ) );
		
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
}
