/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
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
	
	public ErrorEvent( HttpRequestWrapper _request )
	{
		this( _request, 500, null );
	}
	
	public ErrorEvent( HttpRequestWrapper _request, int _statusNo )
	{
		this( _request, _statusNo, null );
	}
	
	public ErrorEvent( HttpRequestWrapper _request, int _statusNo, String _reason )
	{
		if ( _reason == null )
			_reason = HttpCode.msg( _statusNo );
		
		request = _request;
		statusNo = _statusNo;
		reason = _reason;
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
	
	public void setErrorHtml( String _errorHtml )
	{
		errorHtml = _errorHtml;
	}
}
