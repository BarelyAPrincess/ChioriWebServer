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

import com.chiorichan.utils.UtilStrings;
import io.netty.handler.codec.http.HttpHeaderNames;

import com.chiorichan.utils.UtilEncryption;

public class HttpAuthenticator
{
	private final HttpRequestWrapper request;
	private String cached = null;

	public HttpAuthenticator( HttpRequestWrapper request )
	{
		this.request = request;
	}

	public String getAuthorization()
	{
		if ( cached == null )
			cached = request.getHeader( HttpHeaderNames.AUTHORIZATION );
		return cached;
	}

	public String getDigest()
	{
		if ( !isDigest() )
			throw new IllegalStateException( "Authorization is invalid!" );

		return null;
	}

	public String getPassword()
	{
		if ( !isBasic() )
			throw new IllegalStateException( "Authorization is invalid!" );

		String auth = UtilEncryption.base64DecodeString( UtilStrings.regexCapture( getAuthorization(), "Basic (.*)" ) );
		return auth.substring( auth.indexOf( ":" ) + 1 );
	}

	public String getType()
	{
		return isBasic() ? "Basic" : "Digest";
	}

	public String getUsername()
	{
		if ( !isBasic() )
			throw new IllegalStateException( "Authorization is invalid!" );

		String auth = UtilEncryption.base64DecodeString( UtilStrings.regexCapture( getAuthorization(), "Basic (.*)" ) );
		return auth.substring( 0, auth.indexOf( ":" ) );
	}

	public boolean isBasic()
	{
		String var = getAuthorization();
		return var != null && var.startsWith( "Basic" );
	}

	public boolean isDigest()
	{
		String var = getAuthorization();
		return var != null && var.startsWith( "Digest" );
	}
}
