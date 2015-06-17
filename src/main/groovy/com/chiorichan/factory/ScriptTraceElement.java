/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.factory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Similar to StackTraceElement except only used for Groovy Scripts
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class ScriptTraceElement
{
	private final String fileName;
	private final String methodName;
	private final String className;
	private final int lineNum;
	private final int colNum;
	private final EvalExecutionContext context;
	
	public ScriptTraceElement( EvalExecutionContext context, int lineNum, int colNum )
	{
		this( context, lineNum, colNum, "", "" );
	}
	
	public ScriptTraceElement( EvalExecutionContext context, int lineNum, int colNum, String methodName, String className )
	{
		assert ( context.script() != null );
		
		this.context = context;
		fileName = context.scriptName();
		
		this.lineNum = lineNum;
		this.colNum = colNum;
		
		if ( ( className == null || className.isEmpty() ) && context.scriptName() != null )
			if ( context.scriptName().contains( "." ) )
				className = context.scriptName().substring( 0, context.scriptName().indexOf( "." ) );
			else
				className = context.scriptName();
		
		this.methodName = methodName;
		this.className = className;
	}
	
	public ScriptTraceElement( EvalExecutionContext context, String msg )
	{
		assert ( context.script() != null );
		
		this.context = context;
		fileName = context.scriptName();
		methodName = "run";
		className = ( context.scriptName() == null || context.scriptName().isEmpty() ) ? "<Unknown Class>" : context.scriptName().substring( 0, context.scriptName().lastIndexOf( "." ) );
		
		msg = msg.replaceAll( "\n", "" );
		
		// org.codehaus.groovy.control.MultipleCompilationErrorsException: startup failed: GroovyScript44898378.chi: 69: expecting '}', found ':' @ line 69, column 20. instream.close(): ^ 1 error
		// startup failed:GroovyScript26427446.chi: 7: unable to resolve class BASE64Decoder @ line 7, column 14. def data = new BASE64Decoder().decodeBuffer( file.data ); ^1 error
		
		// Pattern p1 = Pattern.compile( "line[: ]?([0-9]*), column[: ]?([0-9]*)\\. (.*)\\^" );
		Pattern p1 = Pattern.compile( "line[: ]?([0-9]*), column[: ]?([0-9]*)" );
		Matcher m1 = p1.matcher( msg );
		
		// Loader.getLogger().debug( msg );
		
		if ( m1.find() )
		{
			lineNum = Integer.parseInt( m1.group( 1 ) );
			colNum = Integer.parseInt( m1.group( 2 ) );
			// methodName = m1.group( 3 ).trim();
		}
		else
		{
			lineNum = -1;
			colNum = -1;
		}
	}
	
	public ScriptTraceElement( EvalExecutionContext context, StackTraceElement ste )
	{
		assert ( context.script() != null );
		
		this.context = context;
		fileName = ste.getFileName();
		methodName = ste.getMethodName();
		className = ste.getClassName();
		lineNum = ste.getLineNumber();
		colNum = -1;
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
	
	public EvalExecutionContext context()
	{
		return context;
	}
	
	@Override
	public String toString()
	{
		return String.format( "%s.%s(%s:%s)", className, methodName, fileName, lineNum );
	}
}
