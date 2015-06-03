/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.lang;

import groovy.lang.GroovyRuntimeException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.control.CompilationFailedException;

import com.chiorichan.util.WebFunc;

/**
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class CodeParsingException extends Exception
{
	private static final long serialVersionUID = 6622374555743321786L;
	
	protected int lineNumber = -1;
	protected int columnNumber = -1;
	protected String fileName = null;
	protected String sourceCode = "";
	protected String message = "";
	
	public CodeParsingException( GroovyRuntimeException e, String code )
	{
		super();
		
		message = WebFunc.escapeHTML( e.getMessage() );
		
		Pattern p = Pattern.compile( "startup failed: (.*):.*line ([0-9]+), column ([0-9]+).*?" );
		Matcher m = p.matcher( e.getMessage() );
		
		if ( m.find() )
		{
			fileName = m.group( 1 );
			lineNumber = Integer.parseInt( m.group( 2 ) );
			columnNumber = Integer.parseInt( m.group( 3 ) );
		}
		else
		{
			p = Pattern.compile( "line ([0-9]+), column ([0-9]+)" );
			m = p.matcher( e.getMessage() );
			
			if ( m.find() )
			{
				lineNumber = Integer.parseInt( m.group( 1 ) );
				columnNumber = Integer.parseInt( m.group( 2 ) );
			}
		}
		
		// System.out.println( lineNumber + " -- " + columnNumber + " -- " + fileName );
		
		sourceCode = code;
	}
	
	public CodeParsingException( CompilationFailedException e )
	{
		this( e, "" );
	}
	
	public void setMessage( String msg )
	{
		message = msg;
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public String getSourceCode()
	{
		return sourceCode;
	}
	
	public int getLineNumber()
	{
		return lineNumber;
	}
	
	public int getColumnNumber()
	{
		return columnNumber;
	}
	
	public String getSourceFile()
	{
		return fileName;
	}
}
