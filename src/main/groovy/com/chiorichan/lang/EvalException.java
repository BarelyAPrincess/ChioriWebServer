/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.lang;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.Loader;
import com.chiorichan.factory.EvalExceptionCallback;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.factory.EvalFactoryResult;
import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.factory.ShellFactory;
import com.google.common.collect.Maps;

/**
 * Carries extra information for debugging when an {@link Exception} is thrown by the {@link EvalFactory}
 */
public class EvalException extends Exception
{
	private static final long serialVersionUID = -1611181613618341914L;
	private static final Map<Class<? extends Throwable>, EvalExceptionCallback> registered = Maps.newConcurrentMap();
	
	private final List<ScriptTraceElement> scriptTrace;
	private final ErrorReporting level;
	
	public EvalException( ErrorReporting level, ShellFactory factory )
	{
		scriptTrace = factory.examineStackTrace( getStackTrace() );
		this.level = level;
	}
	
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
		else if ( t instanceof NullPointerException || t instanceof ArrayIndexOutOfBoundsException )
			result.addException( message == null ? new EvalException( level, t, factory ) : new EvalException( level, message, t, factory ) );
		else
		{
			boolean handled = false;
			
			for ( Entry<Class<? extends Throwable>, EvalExceptionCallback> entry : registered.entrySet() )
				if ( entry.getKey().isAssignableFrom( t.getClass() ) )
				{
					handled = entry.getValue().callback( t, factory, result, level, message );
					if ( handled )
						break;
				}
			
			if ( !handled )
			{
				t.printStackTrace();
				Loader.getLogger().warning( "Uncaught exception in EvalFactory for exception " + t.getClass().getName() );
				result.addException( message == null ? new EvalException( level, t, factory ) : new EvalException( level, message, t, factory ) );
			}
		}
	}
	
	/**
	 * Registers an expected exception to be thrown by any subsystem of {@link EvalFactory}
	 * 
	 * @param callback
	 *            The Callback to call when such exception is thrown
	 * @param clzs
	 *            Classes to be registered
	 */
	@SafeVarargs
	public static void registerException( EvalExceptionCallback callback, Class<? extends Throwable>... clzs )
	{
		for ( Class<? extends Throwable> clz : clzs )
			registered.put( clz, callback );
	}
	
	public ErrorReporting errorLevel()
	{
		return level;
	}
	
	public ScriptTraceElement[] getScriptTrace()
	{
		return scriptTrace.toArray( new ScriptTraceElement[0] );
	}
	
	public boolean isIgnorable()
	{
		return level == ErrorReporting.E_IGNORABLE || level == ErrorReporting.E_DEPRECATED || level == ErrorReporting.E_USER_DEPRECATED || level == ErrorReporting.E_NOTICE || level == ErrorReporting.E_USER_NOTICE || level == ErrorReporting.E_WARNING || level == ErrorReporting.E_USER_WARNING;
	}
	
	public boolean isScriptingException()
	{
		return getCause() != null && getCause().getStackTrace().length > 0 && getCause().getStackTrace()[0].getClassName().startsWith( "org.codehaus.groovy.runtime" );
	}
}
