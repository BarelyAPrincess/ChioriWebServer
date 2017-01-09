/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.factory.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import com.chiorichan.util.StringFunc;

/**
 * Used for basic parsing of code blocks, e.g., < !-- this_is_a_method(argument) -->
 */
public abstract class BasicParser
{
	private Pattern p1;
	private Pattern p2;

	public BasicParser( String patternOne, String patternTwo )
	{
		Validate.notEmpty( patternOne );
		Validate.notEmpty( patternTwo );

		p1 = Pattern.compile( patternOne );
		p2 = Pattern.compile( patternTwo );
	}

	public String runParser( String source ) throws Exception
	{
		if ( source == null || source.isEmpty() )
			return "";

		Matcher m1 = p1.matcher( source );
		Matcher m2 = p2.matcher( source );

		while ( m1.find() && m2.find() )
		{
			String[] args = m1.group( 1 ).split( "[ ]?,[ ]?" );
			String[] args2 = new String[args.length + 1];

			args2[0] = m1.group( 0 );

			for ( int i = 0; i < args.length; i++ )
				args2[i + 1] = StringFunc.trimAll( args[i].trim(), '"' );

			String result = resolveMethod( args2 );

			if ( result == null )
				result = "";

			source = new StringBuilder( source ).replace( m2.start( 1 ), m2.end( 1 ), result ).toString();

			// We have to reset the matcher since the source changes with each loop
			m1 = p1.matcher( source );
			m2 = p2.matcher( source );
		}

		return source;
	}

	public abstract String resolveMethod( String... args ) throws Exception;
}
