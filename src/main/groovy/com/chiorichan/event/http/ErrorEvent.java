/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.event.http;

import com.chiorichan.http.HttpCode;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;

public class ErrorEvent extends HttpEvent
{
	private final int statusNo;
	private final String reason;
	private final HttpRequestWrapper request;
	private String errorHtml = "";
	
	public ErrorEvent( HttpRequestWrapper request )
	{
		this( request, 500, null );
	}
	
	public ErrorEvent( HttpRequestWrapper request, int statusNo )
	{
		this( request, statusNo, null );
	}
	
	public ErrorEvent( HttpRequestWrapper request, int statusNo, String reason )
	{
		if ( reason == null )
			reason = HttpCode.msg( statusNo );
		
		this.request = request;
		this.statusNo = statusNo;
		this.reason = reason;
	}
	
	public String getReason()
	{
		return reason;
	}
	
	public int getStatus()
	{
		return statusNo;
	}
	
	public HttpRequestWrapper getRequest()
	{
		return request;
	}
	
	public HttpResponseWrapper getResponse()
	{
		return request.getResponse();
	}
	
	public String getErrorHtml()
	{
		if ( errorHtml.isEmpty() )
			return null;
		
		return errorHtml;
	}
	
	public void setErrorHtml( String errorHtml )
	{
		this.errorHtml = errorHtml;
	}
}
