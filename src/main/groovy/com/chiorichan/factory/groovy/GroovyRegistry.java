/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.groovy;

import groovy.lang.GroovyRuntimeException;
import groovy.lang.MissingMethodException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeoutException;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;

import com.chiorichan.Loader;
import com.chiorichan.factory.EvalExceptionCallback;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.factory.EvalFactoryResult;
import com.chiorichan.factory.ShellFactory;
import com.chiorichan.factory.processors.EmbeddedGroovyScriptProcessor;
import com.chiorichan.factory.processors.GroovyScriptProcessor;
import com.chiorichan.lang.ErrorReporting;
import com.chiorichan.lang.EvalException;
import com.chiorichan.lang.SandboxSecurityException;

/**
 * Handles the registry of the Groovy related scripting language
 */
public class GroovyRegistry
{
	public GroovyRegistry()
	{
		/**
		 * Register Script-Processors
		 */
		if ( Loader.getConfig().getBoolean( "advanced.scripting.gspEnabled", true ) )
			EvalFactory.register( new EmbeddedGroovyScriptProcessor() );
		if ( Loader.getConfig().getBoolean( "advanced.scripting.groovyEnabled", true ) )
			EvalFactory.register( new GroovyScriptProcessor() );
		
		EvalException.registerException( new EvalExceptionCallback()
		{
			@Override
			public boolean callback( Throwable t, ShellFactory factory, EvalFactoryResult result, ErrorReporting level, String message )
			{
				MultipleCompilationErrorsException exp = ( MultipleCompilationErrorsException ) t;
				ErrorCollector e = exp.getErrorCollector();
				
				for ( Object err : e.getErrors() )
					if ( err instanceof Throwable )
						EvalException.exceptionHandler( ( Throwable ) err, factory, result, level, message );
					else if ( err instanceof SyntaxErrorMessage )
						EvalException.exceptionHandler( ( ( SyntaxErrorMessage ) err ).getCause(), factory, result, level, message );
					else if ( err instanceof Message )
					{
						StringWriter writer = new StringWriter();
						( ( Message ) err ).write( new PrintWriter( writer, true ) );
						Loader.getLogger().warning( "Received this error while trying to eval: " + writer.toString() );
					}
				return true;
			}
		}, MultipleCompilationErrorsException.class );
		
		EvalException.registerException( new EvalExceptionCallback()
		{
			@Override
			public boolean callback( Throwable t, ShellFactory factory, EvalFactoryResult result, ErrorReporting level, String message )
			{
				result.addException( message == null ? new EvalException( level, t, factory ) : new EvalException( level, message, t, factory ) );
				return true;
			}
		}, TimeoutException.class, MissingMethodException.class, SyntaxException.class, CompilationFailedException.class, SandboxSecurityException.class, GroovyRuntimeException.class );
		
		/**
		 * {@link TimeoutException} is thrown when a script does not exit within an alloted amount of time.<br>
		 * {@link MissingMethodException} is thrown when a groovy script tries to call a non-existent method<br>
		 * {@link SyntaxException} is for when the user makes a syntax coding error<br>
		 * {@link CompilationFailedException} is for when compilation fails from source errors<br>
		 * {@link SandboxSecurityException} thrown when script attempts to access a blacklisted API<br>
		 * {@link GroovyRuntimeException} thrown for basically all remaining Groovy exceptions not caught above
		 */
	}
}
