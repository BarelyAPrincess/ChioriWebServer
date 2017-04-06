package com.chiorichan.event.http;

import com.chiorichan.event.EventBus;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;

public class HttpErrorEvent extends HttpEvent
{
	private int statusCode;
	private String statusReason;
	private final HttpRequestWrapper request;
	private String errorHtml = "";
	private boolean isDevelopmentMode;

	public HttpErrorEvent( HttpRequestWrapper request, int statusCode, String statusReason, boolean isDevelopmentMode )
	{
		this.statusCode = statusCode;
		this.statusReason = statusReason;
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

	public String getHttpReason()
	{
		return statusReason;
	}

	public void setHttpReason( String statusReason )
	{
		this.statusReason = statusReason;
	}

	public int getHttpCode()
	{
		return statusCode;
	}

	public void setHttpCode( int statusCode )
	{
		this.statusCode = statusCode;
	}
}
