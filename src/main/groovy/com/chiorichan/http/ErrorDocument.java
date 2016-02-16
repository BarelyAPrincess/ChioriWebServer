/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.http;

import java.util.List;

/**
 * Used to override default error pages
 */
public class ErrorDocument
{
	public static ErrorDocument parseArgs( List<String> args )
	{
		return parseArgs( args.toArray( new String[0] ) );
	}
	public static ErrorDocument parseArgs( String... args )
	{
		if ( args.length > 1 )
			return new ErrorDocument( Integer.parseInt( args[0] ), args[1] );
		return null;
	}

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
}