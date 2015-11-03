/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.terminal.commands.advanced;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandSyntax
{
	protected List<String> arguments = new LinkedList<String>();
	protected String originalSyntax;
	protected String regexp;
	
	public CommandSyntax( String syntax )
	{
		originalSyntax = syntax;
		
		regexp = prepareSyntaxRegexp( syntax );
	}
	
	public Map<String, String> getMatchedArguments( String str )
	{
		Map<String, String> matchedArguments = new HashMap<String, String>( arguments.size() );
		
		if ( arguments.size() > 0 )
		{
			Matcher argMatcher = Pattern.compile( regexp ).matcher( str );
			
			if ( argMatcher.find() )
				for ( int index = 1; index <= argMatcher.groupCount(); index++ )
				{
					String argumentValue = argMatcher.group( index );
					if ( argumentValue == null || argumentValue.isEmpty() )
						continue;
					
					if ( argumentValue.startsWith( "\"" ) && argumentValue.endsWith( "\"" ) )
						argumentValue = argumentValue.substring( 1, argumentValue.length() - 1 );
					
					matchedArguments.put( arguments.get( index - 1 ), argumentValue );
				}
		}
		return matchedArguments;
	}
	
	public String getRegexp()
	{
		return regexp;
	}
	
	public boolean isMatch( String str )
	{
		return str.matches( regexp );
	}
	
	private String prepareSyntaxRegexp( String syntax )
	{
		String expression = syntax;
		
		Matcher argMatcher = Pattern.compile( "(?:[\\s]+)?((\\<|\\[)([^\\>\\]]+)(?:\\>|\\]))" ).matcher( expression );
		// Matcher argMatcher = Pattern.compile("(\\<|\\[)([^\\>\\]]+)(?:\\>|\\])").matcher(expression);
		
		int index = 0;
		while ( argMatcher.find() )
		{
			if ( argMatcher.group( 2 ).equals( "[" ) )
				expression = expression.replace( argMatcher.group( 0 ), "(?:(?:[\\s]+)(\"[^\"]+\"|[^\\s]+))?" );
			else
				expression = expression.replace( argMatcher.group( 1 ), "(\"[^\"]+\"|[\\S]+)" );
			
			arguments.add( index++, argMatcher.group( 3 ) );
		}
		
		return expression;
	}
}
