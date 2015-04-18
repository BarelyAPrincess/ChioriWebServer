/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.lang;

import java.util.List;

import com.chiorichan.factory.EvalMetaData;
import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.factory.ShellFactory;

/**
 *
 * @author Chiori Greene
 */
public class EvalFactoryException extends Exception
{
	private static final long serialVersionUID = -1611181613618341914L;
	
	List<ScriptTraceElement> scriptTrace;
	
	public EvalFactoryException( String message, List<ScriptTraceElement> scriptTrace )
	{
		super( message );
		this.scriptTrace = scriptTrace;
	}
	
	public EvalFactoryException( String message, Throwable cause, List<ScriptTraceElement> scriptTrace )
	{
		super( message, cause );
		this.scriptTrace = scriptTrace;
	}
	
	public EvalFactoryException( Throwable cause, List<ScriptTraceElement> scriptTrace )
	{
		super( cause );
		this.scriptTrace = scriptTrace;
	}
	
	public EvalFactoryException( List<ScriptTraceElement> scriptTrace )
	{
		this.scriptTrace = scriptTrace;
	}
	
	public EvalFactoryException( String message, ShellFactory shellFactory )
	{
		super( message );
		scriptTrace = shellFactory.examineStackTrace( getStackTrace() );
	}
	
	public EvalFactoryException( String message, Throwable cause, ShellFactory shellFactory )
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
	public EvalFactoryException( Throwable cause, ShellFactory shellFactory, EvalMetaData metaData )
	{
		super( cause );
		scriptTrace = shellFactory.examineStackTrace( cause.getStackTrace() );
		
		if ( cause instanceof SandboxSecurityException )
		{
			SandboxSecurityException sse = ( SandboxSecurityException ) cause;
			scriptTrace.add( 0, new ScriptTraceElement( metaData, sse.getLineNumber(), sse.getLineColumnNumber(), sse.getMethodName(), sse.getClassName() ) );
		}
		else
			scriptTrace.add( 0, new ScriptTraceElement( metaData, cause.getMessage() ) );
	}
	
	public EvalFactoryException( Throwable cause, ShellFactory shellFactory )
	{
		super( cause );
		scriptTrace = shellFactory.examineStackTrace( cause.getStackTrace() );
	}
	
	public EvalFactoryException( ShellFactory shellFactory )
	{
		scriptTrace = shellFactory.examineStackTrace( getStackTrace() );
	}
	
	public ScriptTraceElement[] getScriptTrace()
	{
		return scriptTrace.toArray( new ScriptTraceElement[0] );
	}
}
