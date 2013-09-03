package com.chiorichan.event.server;

import javax.servlet.http.HttpServletRequest;

import com.caucho.websocket.WebSocketListener;
import com.chiorichan.event.Cancellable;

public class WebsocketHandshakeEvent extends ServerEvent implements Cancellable
{
	WebSocketListener listener = null;;
	HttpServletRequest request;
	String protocal;
	private boolean cancelled = false;
	
	public WebsocketHandshakeEvent(HttpServletRequest request0)
	{
		request = request0;
	}
	
	public void setProtocal( String p )
	{
		protocal = p;
	}
	
	public String getProtocal()
	{
		return protocal;
	}
	
	public HttpServletRequest getRequest()
	{
		return request;
	}
	
	public WebSocketListener getListener()
	{
		return listener;
	}
	
	public void setListener( WebSocketListener listener0 )
	{
		listener = listener0;
	}
	
	public boolean isListenerSet()
	{
		return ( listener != null );
	}
	
	public String getHost()
	{
		return request.getHeader( "Host" );
	}
	
	public String getOrigin()
	{
		return request.getHeader( "Origin" );
	}
	
	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}
	
	@Override
	public void setCancelled( boolean cancel )
	{
		cancelled = cancel;
	}
}
