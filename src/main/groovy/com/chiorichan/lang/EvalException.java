/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.lang;

import groovy.lang.GroovyRuntimeException;
import groovy.lang.MissingMethodException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;

import com.chiorichan.Loader;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.factory.EvalFactoryResult;
import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.factory.ShellFactory;

/**
 * Carries extra information for debugging when an {@link Exception} are thrown by the {@link EvalFactory}
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class EvalException extends Exception
{
	private static final long serialVersionUID = -1611181613618341914L;
	
	private final List<ScriptTraceElement> scriptTrace;
	private final ErrorReporting level;
	
	public EvalException( ErrorReporting level, String message, ShellFactory factory )
	{
		super( message );
		this.level = level;
		scriptTrace = factory.examineStackTrace( getStackTrace() );
	}
	
	public EvalException( ErrorReporting level, String message, Throwable cause, ShellFactory factory )
	{
		super( message, cause );
		this.level = level;
		scriptTrace = factory.examineStackTrace( cause.getStackTrace() );
	}
	
	public EvalException( ErrorReporting level, Throwable cause, ShellFactory factory )
	{
		super( cause );
		this.level = level;
		scriptTrace = factory.examineStackTrace( cause.getStackTrace() );
	}
	
	public EvalException( ErrorReporting level, ShellFactory factory )
	{
		scriptTrace = factory.examineStackTrace( getStackTrace() );
		this.level = level;
	}
	
	public ScriptTraceElement[] getScriptTrace()
	{
		return scriptTrace.toArray( new ScriptTraceElement[0] );
	}
	
	public boolean isScriptingException()
	{
		return getCause() != null && getCause().getStackTrace().length > 0 && getCause().getStackTrace()[0].getClassName().startsWith( "org.codehaus.groovy.runtime" );
	}
	
	public boolean isIgnorable()
	{
		return level == ErrorReporting.E_IGNORABLE || level == ErrorReporting.E_DEPRECATED || level == ErrorReporting.E_USER_DEPRECATED || level == ErrorReporting.E_NOTICE || level == ErrorReporting.E_USER_NOTICE || level == ErrorReporting.E_WARNING || level == ErrorReporting.E_USER_WARNING;
	}
	
	public static void exceptionHandler( Throwable t, ShellFactory factory, EvalFactoryResult result )
	{
		exceptionHandler( t, factory, result, ErrorReporting.E_ERROR );
	}
	
	public static void exceptionHandler( Throwable t, ShellFactory factory, EvalFactoryResult result, ErrorReporting level )
	{
		exceptionHandler( t, factory, result, level, null );
	}
	
	public static void exceptionHandler( Throwable t, ShellFactory factory, EvalFactoryResult result, ErrorReporting level, String message )
	{
		if ( t == null )
			return;
		/**
		 * We just forward {@link IgnorableEvalException} and {@link EvalFactoryException}
		 */
		if ( t instanceof EvalException )
			result.addException( ( EvalException ) t );
		/**
		 * {@link MultipleCompilationErrorsException} need to be iterated to find the true exceptions
		 */
		else if ( t instanceof MultipleCompilationErrorsException )
		{
			MultipleCompilationErrorsException exp = ( MultipleCompilationErrorsException ) t;
			ErrorCollector e = exp.getErrorCollector();
			
			for ( Object err : e.getErrors() )
			{
				if ( err instanceof Throwable )
				{
					exceptionHandler( ( Throwable ) err, factory, result, level, message );
				}
				else if ( err instanceof SyntaxErrorMessage )
				{
					exceptionHandler( ( ( SyntaxErrorMessage ) err ).getCause(), factory, result, level, message );
				}
				else if ( err instanceof Message )
				{
					StringWriter writer = new StringWriter();
					( ( Message ) err ).write( new PrintWriter( writer, true ) );
					Loader.getLogger().warning( "Received this error while trying to eval: " + writer.toString() );
				}
			}
		}
		/**
		 * {@link TimeoutException} is thrown when a script does not exit within an alloted amount of time
		 * This might need to be handled differently
		 */
		else if ( t instanceof TimeoutException )
		{
			TimeoutException e = ( TimeoutException ) t;
			result.addException( message == null ? new EvalException( level, e, factory ) : new EvalException( level, message, e, factory ) );
		}
		/**
		 * {@link MissingMethodException} is for missing methods
		 * Only thrown for Groovy Scripts
		 */
		else if ( t instanceof MissingMethodException )
		{
			MissingMethodException e = ( MissingMethodException ) t;
			result.addException( message == null ? new EvalException( level, e, factory ) : new EvalException( level, message, e, factory ) );
		}
		/**
		 * {@link SyntaxException} is for when the user makes a syntax coding error
		 * Only thrown for Groovy Scripts
		 */
		else if ( t instanceof SyntaxException )
		{
			SyntaxException e = ( SyntaxException ) t;
			result.addException( message == null ? new EvalException( level, e, factory ) : new EvalException( level, message, e, factory ) );
		}
		/**
		 * {@link CompilationFailedException} is for when compilation fails from source errors
		 * Only thrown for Groovy Scripts
		 */
		else if ( t instanceof CompilationFailedException )
		{
			CompilationFailedException e = ( CompilationFailedException ) t;
			result.addException( message == null ? new EvalException( level, e, factory ) : new EvalException( level, message, e, factory ) );
		}
		/**
		 * {@link SandboxSecurityException} thrown when script attempts to access a blacklisted API
		 * Only thrown for Groovy Scripts
		 */
		else if ( t instanceof SandboxSecurityException )
		{
			SandboxSecurityException e = ( SandboxSecurityException ) t;
			result.addException( message == null ? new EvalException( level, e, factory ) : new EvalException( level, message, e, factory ) );
		}
		/**
		 * {@link GroovyRuntimeException} thrown for basically all remaining Groovy exceptions not caught above
		 * Only thrown for Groovy Scripts
		 */
		else if ( t instanceof GroovyRuntimeException )
		{
			GroovyRuntimeException e = ( GroovyRuntimeException ) t;
			result.addException( message == null ? new EvalException( level, e, factory ) : new EvalException( level, message, e, factory ) );
		}
		/**
		 * General Exception
		 */
		else if ( t instanceof NullPointerException )
		{
			NullPointerException e = ( NullPointerException ) t;
			result.addException( message == null ? new EvalException( level, e, factory ) : new EvalException( level, message, e, factory ) );
		}
		/**
		 * General Exception
		 */
		else if ( t instanceof ArrayIndexOutOfBoundsException )
		{
			ArrayIndexOutOfBoundsException e = ( ArrayIndexOutOfBoundsException ) t;
			result.addException( message == null ? new EvalException( level, e, factory ) : new EvalException( level, message, e, factory ) );
		}
		else
		{
			t.printStackTrace();
			Loader.getLogger().warning( "Uncaught exception in EvalFactory for exception " + t.getClass().getName() );
			result.addException( message == null ? new EvalException( level, t, factory ) : new EvalException( level, message, t, factory ) );
		}
	}
	
	public ErrorReporting errorLevel()
	{
		return level;
	}
}
