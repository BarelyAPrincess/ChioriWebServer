/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
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
