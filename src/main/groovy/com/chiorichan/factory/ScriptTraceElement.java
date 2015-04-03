/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.factory;


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
	EvalMetaData metaData;
	
	ScriptTraceElement( StackTraceElement ste, EvalMetaData metaData )
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
