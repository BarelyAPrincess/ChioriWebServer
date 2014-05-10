/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2014, Atom Node LLC. All Right Reserved.
 */
package com.chiorichan.exceptions;

import com.chiorichan.http.HttpCode;

/**
 *
 * @author Chiori Greene
 */
public class HttpErrorException extends Exception
{
	int httpCode = 200;
	String reason = null;

	public HttpErrorException( int i, String _reason )
	{
		super( _reason );

		httpCode = i;
		reason = _reason;
	}

	public HttpErrorException( int i )
	{
		this( i, HttpCode.msg( i ) );
	}

	public String getReason()
	{
		return reason;
	}

	public int getHttpCode()
	{
		return httpCode;
	}
}
