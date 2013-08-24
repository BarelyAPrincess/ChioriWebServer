package com.chiorichan.server;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.chiorichan.Main;

public class Framework2 implements Servlet
{
	@Override
	public ServletConfig getServletConfig()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServletInfo()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init( ServletConfig arg0 ) throws ServletException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void service( ServletRequest request, ServletResponse response ) throws IOException, ServletException
	{
		if ( request instanceof HttpServletRequest )
		{
			String url = ( (HttpServletRequest) request ).getRequestURL().toString();
			String queryString = ( (HttpServletRequest) request ).getQueryString();
			
			url = url.replace( "https://", "" ).replace( "http://", "" );
			url = url.substring( url.indexOf( "/" ) );
			
			System.out.println( "/SuBrOoT-BaCkDoOr" + url + " <> " + queryString );
			
			String html = Server.getResinServer().request( "/SuBrOoT-BaCkDoOr" + url );
			
			System.out.println( html );
			
			response.getWriter().print( "<? include(\"index.html\"); ?>" );
		}
		else
		{
			System.err.println( "I just don't know what went wrong!, request was not an instance of HttpServletRequest." );
		}
	}

	@Override
	public void destroy()
	{
		// TODO Auto-generated method stub
		
	}
	
}
