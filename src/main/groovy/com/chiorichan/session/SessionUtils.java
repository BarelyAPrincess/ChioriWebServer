/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.session;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.chiorichan.http.Candy;
import com.chiorichan.http.HttpRequestWrapper;

public class SessionUtils
{
	public static Map<String, Candy> poleCandies( HttpRequestWrapper request )
	{
		Map<String, Candy> candies = new LinkedHashMap<String, Candy>();
		String cookies = request.getHeaders().get( "Cookie" );
		if ( cookies == null )
			return candies;
		
		Set<Cookie> var1 = CookieDecoder.decode( cookies );
		
		if ( var1 == null || var1.isEmpty() )
			return candies;
		
		for ( Cookie cookie : var1 )
		{
			candies.put( cookie.getName(), new Candy( cookie.getName(), cookie.getValue() ) );
		}
		
		return candies;
	}
}
