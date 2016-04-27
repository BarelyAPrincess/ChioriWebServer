/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.http;

import io.netty.handler.codec.http.HttpHeaderNames;

import com.chiorichan.util.SecureFunc;
import com.chiorichan.util.StringFunc;

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

		String auth = SecureFunc.base64DecodeString( StringFunc.regexCapture( getAuthorization(), "Basic (.*)" ) );
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

		String auth = SecureFunc.base64DecodeString( StringFunc.regexCapture( getAuthorization(), "Basic (.*)" ) );
		return auth.substring( 0, auth.indexOf( ":" ) );
	}

	public boolean isBasic()
	{
		String var = getAuthorization();
		return var == null ? false : var.startsWith( "Basic" );
	}

	public boolean isDigest()
	{
		String var = getAuthorization();
		return var == null ? false : var.startsWith( "Digest" );
	}
}
