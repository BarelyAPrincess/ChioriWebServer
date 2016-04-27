/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.event.http;

import com.chiorichan.event.Cancellable;
import com.chiorichan.event.application.ApplicationEvent;
import com.chiorichan.http.HttpRequestWrapper;

public class RequestEvent extends ApplicationEvent implements Cancellable
{
	private int statusNo = 200;
	private String reason;
	private HttpRequestWrapper request;
	private boolean cancelled = false;
	
	public RequestEvent( HttpRequestWrapper request )
	{
		this.request = request;
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
	 * public Long getServerLong( ServerVars serverVar )
	 * {
	 * try
	 * {
	 * return (Long) _server.get( serverVar );
	 * }
	 * catch ( Exception e )
	 * {
	 * return 0L;
	 * }
	 * }
	 * public Integer getServerInt( ServerVars serverVar )
	 * {
	 * try
	 * {
	 * return (Integer) _server.get( serverVar );
	 * }
	 * catch ( Exception e )
	 * {
	 * return 0;
	 * }
	 * }
	 * public String getServerString( ServerVars serverVar )
	 * {
	 * try
	 * {
	 * return (String) _server.get( serverVar );
	 * }
	 * catch ( Exception e )
	 * {
	 * return "";
	 * }
	 * }
	 */
	
	public HttpRequestWrapper getFramework()
	{
		return request;
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
