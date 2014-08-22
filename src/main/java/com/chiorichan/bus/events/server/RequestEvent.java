package com.chiorichan.bus.events.server;

import com.chiorichan.bus.bases.Cancellable;
import com.chiorichan.http.HttpRequest;

public class RequestEvent extends ServerEvent implements Cancellable
{
	private int statusNo = 200;
	private String reason;
	private HttpRequest _request;
	private boolean cancelled = false;
	
	public RequestEvent( HttpRequest request )
	{
		_request = request;
	}
	
	public void clearError()
	{
		statusNo = 200;
		reason = null;
	}
	
	public void setStatus( int statusNo )
	{
		setStatus( statusNo, null );
	}
	
	public void setStatus( int statusNo0, String reason0 )
	{
		statusNo = statusNo0;
		reason = reason0;
	}
	
	public String getReason()
	{
		return reason;
	}
	
	public int getStatus()
	{
		return statusNo;
	}
	
	/*
	public Long getServerLong( ServerVars serverVar )
	{
		try
		{
			return (Long) _server.get( serverVar );
		}
		catch ( Exception e )
		{
			return 0L;
		}
	}
	
	public Integer getServerInt( ServerVars serverVar )
	{
		try
		{
			return (Integer) _server.get( serverVar );
		}
		catch ( Exception e )
		{
			return 0;
		}
	}
	
	public String getServerString( ServerVars serverVar )
	{
		try
		{
			return (String) _server.get( serverVar );
		}
		catch ( Exception e )
		{
			return "";
		}
	}
	*/
	
	public HttpRequest getFramework()
	{
		return _request;
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
