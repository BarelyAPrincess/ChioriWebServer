/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import com.chiorichan.exception.ShellExecuteException;
import com.chiorichan.util.StringUtil;

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
	
	public String runParser( String source ) throws ShellExecuteException
	{
		if ( source == null || source.isEmpty() )
			return "";
		
		Matcher m1 = p1.matcher( source );
		Matcher m2 = p2.matcher( source );
		
		while ( m1.find() && m2.find() )
		{
			String[] args = m1.group( 1 ).split( "[ ]?,[ ]?" );
			String[] args2 = new String[args.length];
			
			for ( int i = 0; i < args.length; i++ )
			{
				args2[i] = StringUtil.trimAll( args[i].trim(), '"' );
			}
			
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
	
	public abstract String resolveMethod( String... args ) throws ShellExecuteException;
}
