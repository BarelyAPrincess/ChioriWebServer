/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.bus.events.http;

import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;

public class HttpExceptionEvent extends HttpEvent
{
	private final Throwable cause;
	private int httpCode = -1;
	private final HttpRequestWrapper request;
	private String errorHtml = "";
	private boolean isDevelopmentMode;
	
	public HttpExceptionEvent(HttpRequestWrapper _request, Throwable _cause, boolean _isDevelopmentMode)
	{
		cause = _cause;
		request = _request;
		isDevelopmentMode = _isDevelopmentMode;
	}
	
	public boolean isDevelopmentMode()
	{
		return isDevelopmentMode;
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
	
	public int getHttpCode()
	{
		return httpCode;
	}
	
	public Throwable getThrowable()
	{
		return cause;
	}
}
