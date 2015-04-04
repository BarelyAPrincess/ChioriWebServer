/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.factory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.chiorichan.Loader;


/**
 * Similar to StackTraceElement except only used for Groovy Scripts
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class ScriptTraceElement
{
	String fileName = "<unknown>";
	String methodName;
	String className;
	int lineNum = -1;
	int colNum = -1;
	EvalMetaData metaData;
	
	public ScriptTraceElement( String msg, EvalMetaData metaData )
	{
		fileName = metaData.scriptName;
		methodName = "";
		className = "";
		msg = msg.replaceAll( "\n", "" );
		
		// org.codehaus.groovy.control.MultipleCompilationErrorsException: startup failed: GroovyScript44898378.chi: 69: expecting '}', found ':' @ line 69, column 20. instream.close(): ^ 1 error
		
		Pattern p1 = Pattern.compile( "line[: ]?([0-9]*), column[: ]?([0-9]*)\\. (.*):" );
		Matcher m1 = p1.matcher( msg );
		
		if ( m1.find() )
		{
			try
			{
				lineNum = Integer.parseInt( m1.group( 1 ) );
				colNum = Integer.parseInt( m1.group( 2 ) );
				methodName = m1.group( 3 ).trim();
			}
			catch ( IndexOutOfBoundsException e )
			{
				e.printStackTrace();
			}
		}
		
		this.metaData = metaData;
	}
	
	public ScriptTraceElement( StackTraceElement ste, EvalMetaData metaData )
	{
		fileName = ste.getFileName();
		methodName = ste.getMethodName();
		className = ste.getClassName();
		lineNum = ste.getLineNumber();
		this.metaData = metaData;
	}
	
	public String getFileName()
	{
		return fileName;
	}
	
	public String getMethodName()
	{
		return methodName;
	}
	
	public String getClassName()
	{
		return className;
	}
	
	public int getLineNumber()
	{
		return lineNum;
	}
	
	public int getColumnNumber()
	{
		return colNum;
	}
	
	public EvalMetaData getMetaData()
	{
		return metaData;
	}
	
	@Override
	public String toString()
	{
		return fileName + "(" + lineNum + ")" + ":" + className + "." + methodName;
	}
}
