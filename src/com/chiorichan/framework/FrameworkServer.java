package com.chiorichan.framework;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FrameworkServer
{
	protected HttpServletRequest request;
	protected HttpServletResponse response;
	protected FilterChain chain;
	
	public FrameworkServer(HttpServletRequest request0, HttpServletResponse response0, FilterChain chain0)
	{
		request = request0;
		response = response0;
		chain = chain0;
	}
	
	public void sendRedirect( String target, int httpStatus )
	{
		sendRedirect( target, httpStatus, true );
	}
	
	public void sendRedirect( String target, int httpStatus, boolean autoRedirect )
	{
		if ( autoRedirect )
		{
			try
			{
				response.setHeader( "HTTP/1.1", httpStatus + "" );
				response.sendRedirect( target );
				
				//request.getRequestDispatcher( target ).forward( request, response );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
		else
		{	
			// TODO: Send client a redirection page.
			// "The Request URL has been relocated to: " . $StrURL . "<br />Please change any bookmarks to reference this new location."
		}
	}
}
