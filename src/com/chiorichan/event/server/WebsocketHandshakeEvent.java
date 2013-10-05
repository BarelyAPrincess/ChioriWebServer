package com.chiorichan.event.server;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;

import com.chiorichan.event.Cancellable;
import com.chiorichan.websocket.WSListener;

public class WebsocketHandshakeEvent extends ServerEvent implements Cancellable
{
	WSListener listener = null;;
	ServletUpgradeRequest request;
	String protocal;
	private boolean cancelled = false;
	
	public WebsocketHandshakeEvent(ServletUpgradeRequest request2)
	{
		request = request2;
	}
	
	public void setProtocal( String p )
	{
		protocal = p;
	}
	
	public String getProtocal()
	{
		return protocal;
	}
	
	public ServletUpgradeRequest getRequest()
	{
		return request;
	}
	
	public WSListener getListener()
	{
		return listener;
	}
	
	public void setListener( WSListener listener0 )
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
