/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.lang;

import io.netty.handler.codec.http.HttpResponseStatus;

import com.chiorichan.http.HttpCode;

public class HttpError extends Exception
{
	private static final long serialVersionUID = 8116947267974772489L;
	HttpResponseStatus status = HttpResponseStatus.OK;
	String reason = null;
	
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
		super( msg );
		
		this.reason = status.reasonPhrase().toString();
		this.status = status;
	}
	
	public HttpError( int i, String reason )
	{
		this( i, reason, null );
	}
	
	public HttpError( int i, String reason, String msg )
	{
		super( msg );
		
		status = HttpResponseStatus.valueOf( i );
		this.reason = reason;
	}
	
	public HttpError( String msg, Throwable cause )
	{
		super( msg, cause );
		
		status = HttpResponseStatus.valueOf( 500 );
		reason = status.reasonPhrase().toString();
	}
	
	public String getReason()
	{
		return reason;
	}
	
	public int getHttpCode()
	{
		return status.code();
	}
	
	public String getHttpReason()
	{
		return status.reasonPhrase().toString();
	}
	
	public HttpResponseStatus getHttpResponseStatus()
	{
		return status;
	}
}
