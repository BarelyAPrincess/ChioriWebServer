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

import com.chiorichan.http.HttpCode;

public class HttpErrorException extends Exception
{
	private static final long serialVersionUID = 8116947267974772489L;
	int httpCode = 200;
	String reason = null;
	
	public HttpErrorException( int i, String reason )
	{
		super( reason );
		
		httpCode = i;
		this.reason = reason;
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
