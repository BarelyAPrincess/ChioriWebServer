package com.chiorichan.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.chiorichan.Main;
import com.chiorichan.event.server.RequestEvent;
import com.chiorichan.event.server.ServerVars;

public class DefaultFilter implements Filter
{
	
	@Override
	public void destroy()
	{
		
	}
	
	@Override
	public void doFilter( ServletRequest request0, ServletResponse response0, FilterChain chain ) throws ServletException, IOException
	{
		HttpServletRequest request = (HttpServletRequest) request0;
		HttpServletResponse response = (HttpServletResponse) response0;
		
		Map<ServerVars, Object> _server = new HashMap<ServerVars, Object>();
		
		_server.put( ServerVars.DOCUMENT_ROOT, Main.getConfig().getString( "settings.webroot", "webroot" ) );
		_server.put( ServerVars.HTTP_ACCEPT, request.getHeader( "Accept" ) );
		_server.put( ServerVars.HTTP_USER_AGENT, request.getHeader( "User-Agent" ) );
		_server.put( ServerVars.HTTP_CONNECTION, request.getHeader( "Connection" ) );
		_server.put( ServerVars.HTTP_HOST, request.getHeader( "Host" ) );
		_server.put( ServerVars.HTTP_ACCEPT_ENCODING, request.getHeader( "Accept-Encoding" ) );
		_server.put( ServerVars.HTTP_ACCEPT_LANGUAGE, request.getHeader( "Accept-Language" ) );
		_server.put( ServerVars.REMOTE_HOST, request.getRemoteHost() );
		_server.put( ServerVars.REMOTE_ADDR, request.getRemoteAddr() );
		_server.put( ServerVars.REMOTE_PORT, request.getRemotePort() );
		_server.put( ServerVars.REQUEST_TIME, System.currentTimeMillis() );
		_server.put( ServerVars.REQUEST_URI, request.getRequestURI() );
		_server.put( ServerVars.CONTENT_LENGTH, request.getContentLength() );
		_server.put( ServerVars.AUTH_TYPE, request.getAuthType() );
		_server.put( ServerVars.SERVER_NAME, request.getServerName() );
		_server.put( ServerVars.SERVER_PORT, request.getServerPort() );
		_server.put( ServerVars.HTTPS, request.isSecure() );
		_server.put( ServerVars.SESSION, request.getSession() );
		_server.put( ServerVars.SERVER_SOFTWARE, "Chiori Web Server" );
		_server.put( ServerVars.SERVER_ADMIN, Main.getConfig().getString( "server.admin", "webmaster@" + request.getServerName() ) );
		_server.put( ServerVars.SERVER_ID, Main.getConfig().getString( "server.id", "applebloom" ) );
		_server.put( ServerVars.SERVER_SIGNATURE, "Chiori Web Server Version " + Main.getVersion() );
		
		RequestEvent event = new RequestEvent( _server );
		
		Main.getPluginManager().callEvent( event );
		
		if ( event.isCancelled() )
		{
			Main.getLogger().warning( "Navigation was cancelled by a plugin for ip '" + request.getRemoteAddr() + "' '" + request.getHeader( "Host" ) + request.getRequestURI() + "'" );
			
			int status = event.getStatus();
			String reason = event.getReason();
			
			if ( status < 400 && status > 599 )
			{
				status = 502;
				reason = "Navigation Cancelled by Internal Event";
			}
			
			response.sendError( status, reason );
		}
		
		// Request is considered successful pass this point!
		
		boolean requestRewritten = false;
		
		String newUrl = "/fw/framework.php?page=../test.php";
		request.getRequestDispatcher( newUrl ).forward( request, response );
		
		//if ( requestRewritten )
			//chain.doFilter( request, response );
	}
	
	@Override
	public void init( FilterConfig arg0 ) throws ServletException
	{
		
	}
	
}
