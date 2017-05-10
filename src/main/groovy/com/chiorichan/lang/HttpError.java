/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.lang;

import io.netty.handler.codec.http.HttpResponseStatus;

public class HttpError extends Exception implements IException
{
	private static final long serialVersionUID = 8116947267974772489L;

	int statusCode;
	String statusReason;

	public HttpError( HttpResponseStatus status )
	{
		this( status, null );
	}

	public HttpError( HttpResponseStatus status, String developerMessage )
	{
		super( developerMessage == null ? status.reasonPhrase().toString() : developerMessage );
		statusCode = status.code();
		statusReason = status.reasonPhrase().toString();
	}

	public HttpError( int statusCode )
	{
		this( statusCode, null );
	}

	public HttpError( int statusCode, String statusReason )
	{
		this( statusCode, statusReason, null );
	}

	public HttpError( int statusCode, String statusReason, String developerMessage )
	{
		super( developerMessage == null ? statusReason : developerMessage );

		this.statusCode = statusCode;
		this.statusReason = statusReason;
	}

	public HttpError( Throwable cause, String developerMessage )
	{
		super( developerMessage == null ? HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase().toString() : developerMessage, cause );

		statusCode = 500;
		statusReason = HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase().toString();
	}

	public String getReason()
	{
		return statusReason == null ? HttpResponseStatus.valueOf( statusCode ).reasonPhrase().toString() : statusReason;
	}

	public int getHttpCode()
	{
		return statusCode < 100 ? 500 : statusCode;
	}

	public HttpResponseStatus getHttpResponseStatus()
	{
		return HttpResponseStatus.valueOf( statusCode );
	}

	@Override
	public ReportingLevel reportingLevel()
	{
		return ReportingLevel.E_ERROR;
	}

	@Override
	public ReportingLevel handle( ExceptionReport report, ExceptionContext context )
	{
		report.addException( this );
		return ReportingLevel.E_ERROR;
	}

	@Override
	public boolean isIgnorable()
	{
		return false;
	}
}
