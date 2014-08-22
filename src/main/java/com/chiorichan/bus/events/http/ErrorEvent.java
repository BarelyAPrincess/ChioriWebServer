package com.chiorichan.bus.events.http;

import com.chiorichan.http.HttpCode;
import com.chiorichan.http.HttpRequest;
import com.chiorichan.http.HttpResponse;

public class ErrorEvent extends HttpEvent
{
	private final int statusNo;
	private final String reason;
	private final HttpRequest request;
	private String errorHtml = "";
	
	public ErrorEvent( HttpRequest _request )
	{
		this( _request, 500, null );
	}
	
	public ErrorEvent( HttpRequest _request, int _statusNo )
	{
		this( _request, _statusNo, null );
	}
	
	public ErrorEvent( HttpRequest _request, int _statusNo, String _reason )
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
}
