package com.chiorichan.websocket;

import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.chiorichan.Loader;
import com.chiorichan.event.server.WebsocketHandshakeEvent;

@SuppressWarnings( "serial" )
public class WebsocketServlet extends WebSocketServlet
{
	@Override
	public void configure( WebSocketServletFactory factory )
	{
		factory.getPolicy().setIdleTimeout( 10000 );
		factory.getPolicy().setMaxMessageSize( 256 * 1024 );
		factory.setCreator( new WSCreator() );
	}
	
	private class WSCreator implements WebSocketCreator
	{
		@Override
		public Object createWebSocket( UpgradeRequest request, UpgradeResponse response )
		{
			System.out.println( "New websocket request!" );
			
			WebsocketHandshakeEvent wshe = new WebsocketHandshakeEvent( (ServletUpgradeRequest) request );
			
			WSListener webSocket = null;
			
			for ( String p : request.getSubProtocols() )
			{
				wshe.setProtocal( p );
				
				Loader.getPluginManager().callEvent( wshe );
				
				if ( wshe.isCancelled() )
					break;
				
				webSocket = wshe.getListener();
				
				if ( webSocket != null )
					break;
			}
			
			if ( webSocket != null )
			{
				Loader.getLogger().info( "Created a new WebSocket connection to " + ( (ServletUpgradeRequest) request ).getRemoteAddress() );
				return webSocket;
			}
			else
			{
				// response.sendError( HttpServletResponse.SC_SERVICE_UNAVAILABLE,
				// "There are no plugins registered to take the websocket connection you requested!" );
				return null;
			}
		}
	}
}
