/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.lang;

import org.apache.commons.lang3.Validate;

/**
 * Used to track line and column numbers for SecurityExceptions thrown from GroovySandbox
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class SandboxSecurityException extends SecurityException
{
	private static final long serialVersionUID = -3520264898037710187L;
	
	int lineNum = -1;
	int colNum = -1;
	String methodName = "";
	String className = "";
	
	public SandboxSecurityException( String msg )
	{
		super( msg );
	}
	
	public SandboxSecurityException( String msg, Throwable cause )
	{
		super( msg, cause );
	}
	
	public void setLineNumber( int lineNum )
	{
		this.lineNum = lineNum;
	}
	
	public void setLineNumber( int lineNum, int colNum )
	{
		this.lineNum = lineNum;
		this.colNum = colNum;
	}
	
	public int getLineNumber()
	{
		return lineNum;
	}
	
	public int getLineColumnNumber()
	{
		return colNum;
	}
	
	public void setLineColumnNumber( int colNum )
	{
		this.colNum = colNum;
	}
	
	public void setMethodName( String methodName )
	{
		Validate.notNull( methodName );
		this.methodName = methodName;
	}
	
	public void setClassName( String className )
	{
		Validate.notNull( className );
		this.className = className;
	}
	
	public String getMethodName()
	{
		return methodName;
	}
	
	public String getClassName()
	{
		return className;
	}
}
