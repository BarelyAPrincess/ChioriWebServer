/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.lang;

import io.netty.handler.codec.http.HttpResponseStatus;

import com.chiorichan.http.HttpCode;

public class HttpError extends Exception
{
	private static final long serialVersionUID = 8116947267974772489L;
	HttpResponseStatus status = HttpResponseStatus.OK;
	String reason = null;
	String msg;
	
	public HttpError( int i )
	{
		this( i, HttpCode.msg( i ), null );
	}
	
	public HttpError( HttpResponseStatus status )
	{
		this( status, null );
	}
	
	public HttpError( HttpResponseStatus status, String msg )
	{
		this.status = status;
		this.reason = status.reasonPhrase();
		this.msg = msg;
	}
	
	public HttpError( int i, String reason )
	{
		this( i, reason, null );
	}
	
	public HttpError( int i, String reason, String msg )
	{
		super( reason );
		
		status = HttpResponseStatus.valueOf( i );
		this.reason = reason;
		this.msg = msg;
	}
	
	public String getReason()
	{
		return reason;
	}
	
	public String getMessage()
	{
		return msg;
	}
	
	public int getHttpCode()
	{
		return status.code();
	}
	
	public String getHttpReason()
	{
		return status.reasonPhrase();
	}
	
	public HttpResponseStatus getHttpResponseStatus()
	{
		return status;
	}
}
