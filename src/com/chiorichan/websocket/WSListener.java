package com.chiorichan.websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketFrame;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.extensions.Frame;

public abstract class WSListener
{
	@OnWebSocketMessage
	public void onText( Session session, String message )
	{
		if ( session.isOpen() )
		{
			session.getRemote().sendStringByFuture( "This is an empty response message. You should never see this message over a websocket connection." );
		}
	}
	
	@OnWebSocketClose
	public void onClose( Session session, int closeCode, String closeReason )
	{
		
	}
	
	@OnWebSocketError
	public void onError( Session session, Throwable exception )
	{
		
	}
	
	@OnWebSocketConnect
	public void onConnect( Session session )
	{
		
	}
	
	@OnWebSocketFrame
	public void onFrame( Session session, Frame frame )
	{
		
	}
}
