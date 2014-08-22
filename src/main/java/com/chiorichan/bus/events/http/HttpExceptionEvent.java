package com.chiorichan.bus.events.http;

import com.chiorichan.http.HttpRequest;
import com.chiorichan.http.HttpResponse;

public class HttpExceptionEvent extends HttpEvent
{
	private final Throwable cause;
	private int httpCode = -1;
	private final HttpRequest request;
	private String errorHtml = "";
	private boolean isDevelopmentMode;
	
	public HttpExceptionEvent(HttpRequest _request, Throwable _cause, boolean _isDevelopmentMode)
	{
		cause = _cause;
		request = _request;
		isDevelopmentMode = _isDevelopmentMode;
	}
	
	public boolean isDevelopmentMode()
	{
		return isDevelopmentMode;
	}
	
	public HttpRequest getRequest()
	{
		return request;
	}
	
	public HttpResponse getResponse()
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
