package com.chiorichan.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.chiorichan.Loader;
import com.chiorichan.util.Versioning;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class HttpResponse
{
	protected HttpRequest request;
	protected ByteArrayOutputStream output = new ByteArrayOutputStream();
	protected int httpStatus = 200;
	protected String httpContentType = "text/html";
	
	protected HttpResponse(HttpRequest _request)
	{
		request = _request;
	}
	
	public void sendError( int var1 )
	{
		sendError( var1, null );
	}
	
	public void sendError( int var1, String var2 )
	{
		sendError( var1, var2, null );
	}
	
	public void sendError( int var1, String var2, String var3 )
	{
		if ( var1 < 1 )
			var1 = 500;
		
		if ( var2 == null )
			var2 = request.getFramework().getServer().getStatusDescription( var1 );
		
		Loader.getLogger().severe( "HttpError: " + var1 + " - " + var2 );
		
		httpStatus = var1;
		
		output.reset();
		
		try
		{
			println( "<h1>" + var1 + " - " + var2 + "</h1>" );
			
			if ( var3 != null && !var3.isEmpty() )
				println( "<p>" + var3 + "</p>" );
			
			println( "<hr>" );
			println( "<small>Running <a href=\"https://github.com/ChioriGreene/ChioriWebServer\">" + Versioning.getProduct() + "</a> Version " + Versioning.getVersion() + "<br />" + Versioning.getCopyright() + "</small>" );
			
			sendResponse();
		}
		catch ( IOException e )
		{
			if ( e.getMessage().equals( "Broken pipe" ) )
				Loader.getLogger().severe( "Broken Pipe: The browser closed the connection before data could be written to it." );
			else
				e.printStackTrace();
		}
	}
	
	public ByteArrayOutputStream getOutput()
	{
		return output;
	}
	
	public boolean isCommitted()
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	public void setStatus( int _status )
	{
		httpStatus = _status;
	}
	
	public void sendRedirect( String target )
	{
		request.getOriginal().getResponseHeaders().add( "Location", target );
	}
	
	public void print( String var1 ) throws IOException
	{
		output.write( var1.getBytes() );
	}
	
	public void println( String var1 ) throws IOException
	{
		output.write( ( var1 + "\n" ).getBytes() );
	}
	
	public void setContentType( String type )
	{
		if ( type == null || type.isEmpty() )
			type = "text/html";
		
		httpContentType = type;
	}
	
	public void sendResponse() throws IOException
	{
		HttpExchange http = request.getOriginal();
		
		Headers h = http.getResponseHeaders();
		
		for ( Candy c : request.getCandies() )
		{
			if ( c.needsUpdating() )
				h.add( "Set-Cookie", c.toHeaderValue() );
		}
		
		request.getSession().saveSession();
		
		if ( h.get( "Server" ) == null )
			h.add( "Server", Versioning.getProduct() + " Version " + Loader.getVersion() );
		
		if ( h.get( "Content-Type" ) == null )
			h.add( "Content-Type", httpContentType );
		
		http.sendResponseHeaders( httpStatus, output.size() );
		
		OutputStream os = http.getResponseBody();
		os.write( output.toByteArray() );
		output.close();
		os.close(); // This terminates the HttpExchange and frees the resources. We should not do anything past this
						// point.
	}
}
