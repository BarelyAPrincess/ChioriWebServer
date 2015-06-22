/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.http;

/**
 * Used to override default error pages
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class ErrorDocument
{
	int httpCode;
	String resp;
	
	public ErrorDocument( int httpCode, String resp )
	{
		this.httpCode = httpCode;
		this.resp = resp;
	}
	
	public int getHttpCode()
	{
		return httpCode;
	}
	
	public String getResponse()
	{
		return resp;
	}
	
	public static ErrorDocument parseArgs( String... args )
	{
		if ( args.length > 1 )
			return new ErrorDocument( Integer.parseInt( args[0] ), args[1] );
		return null;
	}
}
