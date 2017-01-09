/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.factory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;


/**
 * Similar to StackTraceElement except only used for Groovy Scripts
 */
public class ScriptTraceElement
{
	private final String fileName;
	private final String methodName;
	private final String className;
	private int lineNum = -1;
	private int colNum = -1;
	private final ScriptingContext context;

	public ScriptTraceElement( ScriptingContext context, int lineNum, int colNum )
	{
		this( context, lineNum, colNum, "", "" );
	}

	public ScriptTraceElement( ScriptingContext context, int lineNum, int colNum, String methodName, String className )
	{
		this.context = context;
		fileName = context.scriptName();

		this.lineNum = lineNum;
		this.colNum = colNum;

		if ( ( className == null || className.isEmpty() ) && context.scriptName() != null )
			className = context.scriptSimpleName();

		this.methodName = methodName;
		this.className = className;
	}

	public ScriptTraceElement( ScriptingContext context, StackTraceElement ste )
	{
		this.context = context;
		fileName = ste.getFileName();
		methodName = ste.getMethodName();
		className = ste.getClassName();
		lineNum = ste.getLineNumber();
		colNum = -1;
	}

	public ScriptTraceElement( ScriptingContext context, String msg )
	{
		this.context = context;
		fileName = context.scriptName();
		methodName = "run";
		className = context.scriptName() == null || context.scriptName().length() == 0 ? "<Unknown Class>" : context.scriptSimpleName();

		if ( msg != null && !msg.isEmpty() )
			examineMessage( msg );
	}

	public ScriptingContext context()
	{
		return context;
	}

	public ScriptTraceElement examineMessage( String msg )
	{
		Validate.notNull( msg );

		// Improve parsing for line and column numbers

		msg = msg.replaceAll( "\n", "" );
		Pattern p1 = Pattern.compile( "line[: ]?([0-9]*), column[: ]?([0-9]*)" );
		Matcher m1 = p1.matcher( msg );

		if ( m1.find() )
		{
			lineNum = Integer.parseInt( m1.group( 1 ) );
			colNum = Integer.parseInt( m1.group( 2 ) );
		}

		return this;
	}

	public String getClassName()
	{
		return className;
	}

	public int getColumnNumber()
	{
		return colNum;
	}

	public String getFileName()
	{
		return fileName;
	}

	public int getLineNumber()
	{
		return lineNum;
	}

	public String getMethodName()
	{
		return methodName;
	}

	@Override
	public String toString()
	{
		return String.format( "%s.%s(%s:%s)", className, methodName, fileName, lineNum );
	}
}
