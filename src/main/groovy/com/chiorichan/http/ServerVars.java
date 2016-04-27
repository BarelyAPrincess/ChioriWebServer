/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.http;

/**
 * Server Variable Enum
 * Used to map http headers to their string variance
 */
public enum ServerVars
{
	SERVER_ADDR,
	SERVER_NAME,
	SERVER_ID,
	SERVER_SOFTWARE,
	SERVER_PROTOCAL,
	REQUEST_METHOD,
	REQUEST_TIME,
	REQUEST_URI,
	QUERY_STRING,
	DOCUMENT_ROOT,
	HTTP_VERSION,
	HTTP_ACCEPT,
	HTTP_ACCEPT_CHARSET,
	HTTP_ACCEPT_ENCODING,
	HTTP_ACCEPT_LANGUAGE,
	HTTP_CONNECTION,
	HTTP_HOST,
	HTTP_USER_AGENT,
	HTTPS,
	REMOTE_ADDR,
	REMOTE_HOST,
	REMOTE_PORT,
	REMOTE_USER,
	SERVER_ADMIN,
	SERVER_IP,
	SERVER_PORT,
	SERVER_SIGNATURE,
	AUTH_DIGEST,
	AUTH_USER,
	AUTH_PW,
	AUTH_TYPE,
	CONTENT_LENGTH,
	SESSION,
	PHP_SELF,
	HTTP_X_REQUESTED_WITH,
	SERVER_VERSION;

	public static ServerVars parse( String key )
	{
		for ( ServerVars sv : ServerVars.values() )
			if ( sv.name().equalsIgnoreCase( key ) || ( "PHP_" + sv.name() ).equalsIgnoreCase( key ) )
				return sv;

		return null;
	}
}
