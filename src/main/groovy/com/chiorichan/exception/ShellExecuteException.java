/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.exception;

import java.util.List;

import com.chiorichan.factory.EvalMetaData;
import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.factory.ShellFactory;

/**
 *
 * @author Chiori Greene
 */
public class ShellExecuteException extends Exception
{
	private static final long serialVersionUID = -1611181613618341914L;
	
	List<ScriptTraceElement> scriptTrace;
	
	public ShellExecuteException( String message, List<ScriptTraceElement> scriptTrace )
	{
		super( message );
		this.scriptTrace = scriptTrace;
	}
	
	
	public ShellExecuteException( String message, Throwable cause, List<ScriptTraceElement> scriptTrace )
	{
		super( message, cause );
		this.scriptTrace = scriptTrace;
	}
	
	public ShellExecuteException( Throwable cause, List<ScriptTraceElement> scriptTrace )
	{
		super( cause );
		this.scriptTrace = scriptTrace;
	}
	
	public ShellExecuteException( List<ScriptTraceElement> scriptTrace )
	{
		this.scriptTrace = scriptTrace;
	}
	
	public ShellExecuteException( String message, ShellFactory shellFactory )
	{
		super( message );
		scriptTrace = shellFactory.examineStackTrace( getStackTrace() );
	}
	
	public ShellExecuteException( String message, Throwable cause, ShellFactory shellFactory )
	{
		super( message, cause );
		scriptTrace = shellFactory.examineStackTrace( cause.getStackTrace() );
	}
	
	/**
	 * Used for parsing errors where no script element was recorded,
	 * so we have to fake one instead
	 * 
	 * @param cause
	 *            The causing exception
	 * @param shellFactory
	 *            The ShellFactory used
	 * @param metaData
	 *            The script EvalMetaData
	 */
	public ShellExecuteException( Throwable cause, ShellFactory shellFactory, EvalMetaData metaData )
	{
		super( cause );
		scriptTrace = shellFactory.examineStackTrace( cause.getStackTrace() );
		
		scriptTrace.add( new ScriptTraceElement( cause.getMessage(), metaData ) );
	}
	
	public ShellExecuteException( Throwable cause, ShellFactory shellFactory )
	{
		super( cause );
		scriptTrace = shellFactory.examineStackTrace( cause.getStackTrace() );
	}
	
	public ShellExecuteException( ShellFactory shellFactory )
	{
		scriptTrace = shellFactory.examineStackTrace( getStackTrace() );
	}
	
	public ScriptTraceElement[] getScriptTrace()
	{
		return scriptTrace.toArray( new ScriptTraceElement[0] );
	}
}
