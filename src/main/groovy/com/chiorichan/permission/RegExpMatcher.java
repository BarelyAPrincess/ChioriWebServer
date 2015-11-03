/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegExpMatcher
{
	public static final String RAW_REGEX_CHAR = "$";
	protected static Pattern rangeExpression = Pattern.compile( "(\\d+)-(\\d+)" );
	
	protected static HashMap<String, Pattern> patternCache = new HashMap<String, Pattern>();
	
	public static String prepareRegexp( String expression )
	{
		if ( expression.startsWith( "-" ) )
			expression = expression.substring( 1 );
		
		if ( expression.startsWith( "#" ) )
			expression = expression.substring( 1 );
		
		boolean rawRegexp = expression.startsWith( RAW_REGEX_CHAR );
		if ( rawRegexp )
			expression = expression.substring( 1 );
		
		String regexp = rawRegexp ? expression : expression.replace( ".", "\\." ).replace( "*", "(.*)" );
		
		try
		{
			Matcher rangeMatcher = rangeExpression.matcher( regexp );
			while ( rangeMatcher.find() )
			{
				StringBuilder range = new StringBuilder();
				int from = Integer.parseInt( rangeMatcher.group( 1 ) );
				int to = Integer.parseInt( rangeMatcher.group( 2 ) );
				
				if ( from > to )
				{
					int temp = from;
					from = to;
					to = temp;
				} // swap them
				
				range.append( "(" );
				
				for ( int i = from; i <= to; i++ )
				{
					range.append( i );
					if ( i < to )
						range.append( "|" );
				}
				
				range.append( ")" );
				
				regexp = regexp.replace( rangeMatcher.group( 0 ), range.toString() );
			}
		}
		catch ( Throwable e )
		{
		}
		
		return regexp;
	}
	
	protected Pattern createPattern( String expression )
	{
		try
		{
			return Pattern.compile( prepareRegexp( expression ), Pattern.CASE_INSENSITIVE );
		}
		catch ( PatternSyntaxException e )
		{
			return Pattern.compile( Pattern.quote( expression ), Pattern.CASE_INSENSITIVE );
		}
	}
	
	public boolean isMatches( Permission expression, String permission )
	{
		return isMatches( expression.getNamespace(), permission );
	}
	
	public boolean isMatches( String expression, String permission )
	{
		Pattern permissionMatcher = patternCache.get( expression );
		
		if ( permissionMatcher == null )
			patternCache.put( expression, permissionMatcher = createPattern( expression ) );
		
		return permissionMatcher.matcher( permission ).matches();
	}
}
