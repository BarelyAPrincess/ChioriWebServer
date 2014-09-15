package com.chiorichan.http.session;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.chiorichan.http.Candy;
import com.chiorichan.http.HttpRequest;

public class SessionUtils
{
	public static Map<String, Candy> poleCandies( HttpRequest request )
	{
		Map<String, Candy> candies = new LinkedHashMap<String, Candy>();
		List<String> var1 = request.getHeaders().get( "Cookie" );
		
		if ( var1 == null || var1.isEmpty() )
			return candies;
		
		String[] var2 = var1.get( 0 ).split( "\\;" );
		
		for ( String var3 : var2 )
		{
			String[] var4 = var3.trim().split( "\\=" );
			
			if ( var4.length == 2 )
			{
				candies.put( var4[0], new Candy( var4[0], var4[1] ) );
			}
		}
		
		return candies;
	}
}
