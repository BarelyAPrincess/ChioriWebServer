package com.chiorichan.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.websocket.WebSocketListener;
import com.caucho.websocket.WebSocketServletRequest;
import com.chiorichan.Loader;
import com.chiorichan.event.server.WebsocketHandshakeEvent;
import com.chiorichan.framework.Framework;

public class DefaultServlet extends HttpServlet
{
	private static final long serialVersionUID = -4531330369021028992L;
	
	@Override
	public void destroy()
	{
		
	}
	
	@Override
	public void service( HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException
	{
		if ( "websocket".equalsIgnoreCase( request.getHeader( "Upgrade" ) ) )
		{
			String protocol = request.getHeader( "Sec-WebSocket-Protocol" );
			
			WebsocketHandshakeEvent wshe = new WebsocketHandshakeEvent( request );
			
			WebSocketListener listener = null;
			String protocal = "";
			for ( String p : parseProtocols( protocol ) )
			{
				wshe.setProtocal( p );
				
				Loader.getPluginManager().callEvent( wshe );
				
				if ( wshe.isCancelled() )
					break;
				
				listener = wshe.getListener();
				
				if ( listener != null )
				{
					protocol = p;
					break;
				}
			}
			
			if ( listener != null )
			{
				Loader.getLogger().info( "Created a new WebSocket connection to " + request.getRemoteAddr() );
				
				response.setHeader( "Sec-WebSocket-Protocol", protocal );
				WebSocketServletRequest wsReq = (WebSocketServletRequest) request;
				wsReq.startWebSocket( listener );
			}
			else
			{
				response.sendError( HttpServletResponse.SC_SERVICE_UNAVAILABLE );
			}
		}
		else
		{
			Framework fw = new Framework( request, response, request.getServletContext() );
			fw.init();
		}
	}
	
	public String[] parseProtocols( String protocol )
	{
		if ( protocol == null )
			return new String[] { null };
		protocol = protocol.trim();
		if ( protocol == null || protocol.length() == 0 )
			return new String[] { null };
		String[] passed = protocol.split( "\\s*,\\s*" );
		String[] protocols = new String[passed.length + 1];
		System.arraycopy( passed, 0, protocols, 0, passed.length );
		return protocols;
	}
}
