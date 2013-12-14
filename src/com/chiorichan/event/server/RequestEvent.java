package com.chiorichan.event.server;

import java.util.Map;

import com.chiorichan.event.Cancellable;
import com.chiorichan.framework.Framework;

public class RequestEvent extends ServerEvent implements Cancellable
{
	private int statusNo = 200;
	private String reason;
	private Framework _fw;
	private boolean cancelled = false;
	
	public RequestEvent( Framework _fw0 )
	{
		_fw = _fw0;
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
	
	public Framework getFramework()
	{
		return _fw;
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
