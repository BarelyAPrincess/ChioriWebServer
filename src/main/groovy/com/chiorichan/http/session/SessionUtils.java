package com.chiorichan.http.session;

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
