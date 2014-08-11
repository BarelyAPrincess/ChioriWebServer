package com.chiorichan.bus.events.http;

import com.chiorichan.http.HttpCode;
import com.chiorichan.http.HttpRequest;
import com.chiorichan.http.HttpResponse;
import com.sun.net.httpserver.Headers;

public class HttpExceptionEvent extends HttpEvent
{
	private final Throwable cause;
	private int httpCode = -1;
	private final HttpRequest request;
	private String errorHtml = "";
	
	public HttpExceptionEvent(HttpRequest _request, Throwable _cause)
	{
		cause = _cause;
		request = _request;
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
