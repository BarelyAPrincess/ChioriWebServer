/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.event.http;

import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;

public class HttpExceptionEvent extends HttpEvent
{
	private final Throwable cause;
	private int httpCode = -1;
	private final HttpRequestWrapper request;
	private String errorHtml = "";
	private boolean isDevelopmentMode;
	
	public HttpExceptionEvent( HttpRequestWrapper request, Throwable cause, boolean isDevelopmentMode )
	{
		this.cause = cause;
		this.request = request;
		this.isDevelopmentMode = isDevelopmentMode;
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
	
	public void setErrorHtml( String errorHtml )
	{
		this.errorHtml = errorHtml;
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
