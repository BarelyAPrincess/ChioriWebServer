/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
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
