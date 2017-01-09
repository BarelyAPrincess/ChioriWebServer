/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.http;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.util.internal.InternalThreadLocalMap;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * A <a href="http://tools.ietf.org/html/rfc6265">RFC6265</a> compliant cookie decoder to be used server side
 * with modification to fix a few issues we were having..
 * <p>
 * Only name and value fields are expected, so old fields are not populated (path, domain, etc).
 * <p>
 * Old <a href="http://tools.ietf.org/html/rfc2965">RFC2965</a> cookies are still supported,
 * old fields will simply be ignored.
 */
public final class CookieDecoder
{
	private CookieDecoder()
	{
		// unused
	}

	/**
	 * Decodes the specified Set-Cookie HTTP header value into a {@link Cookie}.
	 *
	 * @return the decoded {@link Cookie}
	 */
	public static Set<Cookie> decode( String header )
	{

		if ( header == null )
			throw new NullPointerException( "header" );

		final int headerLen = header.length();

		if ( headerLen == 0 )
			return Collections.emptySet();

		Set<Cookie> cookies = new TreeSet<Cookie>();

		int i = 0;

		boolean rfc2965Style = false;
		if ( header.regionMatches( true, 0, "$Version", 0, 8 ) )
		{
			// RFC 2965 style cookie, move to after version value
			i = header.indexOf( ';' ) + 1;
			rfc2965Style = true;
		}

		loop:
		for ( ; ; )
		{

			// Skip spaces and separators.
			for ( ; ; )
			{
				if ( i == headerLen )
					break loop;
				char c = header.charAt( i );
				if ( c == '\t' || c == '\n' || c == 0x0b || c == '\f' || c == '\r' || c == ' ' || c == ',' || c == ';' )
				{
					i++;
					continue;
				}
				break;
			}

			int newNameStart = i;
			int newNameEnd = i;
			String value;

			if ( i == headerLen )
				value = null;
			else
				keyValLoop:
						for ( ; ; )
						{
							char curChar = header.charAt( i );
							if ( curChar == ';' )
							{
								// NAME; (no value till ';')
								newNameEnd = i;
								value = null;
								break keyValLoop;
							}
							else if ( curChar == '=' )
							{
								// NAME=VALUE
								newNameEnd = i;
								i++;
								if ( i == headerLen )
								{
									// NAME= (empty value, i.e. nothing after '=')
									value = "";
									break keyValLoop;
								}

								int newValueStart = i;
								char c = header.charAt( i );
								if ( c == '"' )
								{
									// NAME="VALUE"
									StringBuilder newValueBuf = InternalThreadLocalMap.get().stringBuilder();

									final char q = c;
									boolean hadBackslash = false;
									i++;
									for ( ; ; )
									{
										if ( i == headerLen )
										{
											value = newValueBuf.toString();
											break keyValLoop;
										}
										if ( hadBackslash )
										{
											hadBackslash = false;
											c = header.charAt( i++ );
											if ( c == '\\' || c == '"' )
												// Escape last backslash.
												newValueBuf.setCharAt( newValueBuf.length() - 1, c );
											else
												// Do not escape last backslash.
												newValueBuf.append( c );
										}
										else
										{
											c = header.charAt( i++ );
											if ( c == q )
											{
												value = newValueBuf.toString();
												break keyValLoop;
											}
											newValueBuf.append( c );
											if ( c == '\\' )
												hadBackslash = true;
										}
									}
								}
								else
								{
									// NAME=VALUE;
									int semiPos = header.indexOf( ';', i );
									if ( semiPos > 0 )
									{
										value = header.substring( newValueStart, semiPos );
										i = semiPos;
									}
									else
									{
										value = header.substring( newValueStart );
										i = headerLen;
									}
								}
								break keyValLoop;
							}
							else
								i++;

							if ( i == headerLen )
							{
								// NAME (no value till the end of string)
								newNameEnd = headerLen;
								value = null;
								break;
							}
						}

			if ( !rfc2965Style || ( !header.regionMatches( newNameStart, "$Path", 0, "$Path".length() ) && !header.regionMatches( newNameStart, "$Domain", 0, "$Domain".length() ) && !header.regionMatches( newNameStart, "$Port", 0, "$Port".length() ) ) )
			{

				// skip obsolete RFC2965 fields
				String name = header.substring( newNameStart, newNameEnd );
				cookies.add( new DefaultCookie( name, value ) );
			}
		}

		return cookies;
	}
}
