/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * <p>
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.groovy;

import com.chiorichan.logger.Log;
import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;
import groovy.transform.TimedInterrupt;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;

import com.chiorichan.AppConfig;
import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountType;
import com.chiorichan.account.auth.AccountAuthenticator;
import com.chiorichan.event.EventBus;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.factory.ScriptingEngine;
import com.chiorichan.factory.ScriptingFactory;
import com.chiorichan.factory.ScriptingRegistry;
import com.chiorichan.lang.ExceptionCallback;
import com.chiorichan.lang.ExceptionContext;
import com.chiorichan.lang.ExceptionReport;
import com.chiorichan.lang.ReportingLevel;
import com.chiorichan.lang.SandboxSecurityException;
import com.chiorichan.lang.ScriptingException;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.References;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.session.SessionManager;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Ticks;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.Looper;
import com.google.common.collect.Maps;

/**
 * Handles the registry of the Groovy related scripting language
 */
public class GroovyRegistry implements ScriptingRegistry
{
	/*
	 * Groovy Imports :P
	 */
	private static final GroovyImportCustomizer imports = new GroovyImportCustomizer();

	private static final Class<?>[] classImports = new Class<?>[] {References.class, NestedScript.class, Loader.class, AccountManager.class, AccountType.class, Account.class, AccountAuthenticator.class, EventBus.class, PermissionManager.class, PluginManager.class, TaskManager.class, Ticks.class, Timings.class, SessionManager.class, SiteManager.class, Site.class, ScriptingContext.class};
	private static final String[] starImports = new String[] {"com.chiorichan.lang", "com.chiorichan.factory.api", "com.chiorichan.util", "com.chiorichan.logger", "org.apache.commons.lang3.text", "org.ocpsoft.prettytime", "java.util", "java.net", "com.google.common.base"};
	private static final Class<?>[] staticImports = new Class<?>[] {Looper.class, ReportingLevel.class, HttpResponseStatus.class};
	private static final GroovySandbox secure = new GroovySandbox();

	/*
	 * Groovy Sandbox Customization
	 */
	private static final ASTTransformationCustomizer timedInterrupt = new ASTTransformationCustomizer( TimedInterrupt.class );

	static
	{
		imports.addImports( classImports );
		imports.addStarImports( starImports );
		imports.addStaticStars( staticImports );

		// Transforms scripts to limit their execution to 30 seconds.
		long timeout = AppConfig.get().getLong( "advanced.security.defaultScriptTimeout", 30L );
		if ( timeout > 0 )
		{
			Map<String, Object> timedInterruptParams = Maps.newHashMap();
			timedInterruptParams.put( "value", timeout );
			timedInterrupt.setAnnotationParameters( timedInterruptParams );
		}
	}

	private static Map<String, String> scriptCacheMd5 = new HashMap<>();

	public static Script getCachedScript( ScriptingContext context, Binding binding )
	{
		try
		{
			// File classFile = FileFunc.buildFile( context.cache(), context.scriptPackage().replace( '.', File.separatorChar ), context.scriptSimpleName() + ".class" );
			if ( scriptCacheMd5.containsKey( context.scriptClassName() ) )
			{
				if ( scriptCacheMd5.get( context.site().getId() + "//" + context.scriptClassName() ).equals( context.md5() ) )
				{
					Class<?> scriptClass = Class.forName( context.scriptClassName() );
					Constructor<?> con = scriptClass.getConstructor( Binding.class );
					Script script = ( Script ) con.newInstance( binding );
					return script;
				}
			}
			else
				scriptCacheMd5.put( context.site().getId() + "//" + context.scriptClassName(), context.md5() );
		}
		catch ( Throwable t )
		{

		}
		return null;
	}

	public GroovyRegistry()
	{
		// if ( Loader.getConfig().getBoolean( "advanced.scripting.gspEnabled", true ) )
		// EvalFactory.register( new EmbeddedGroovyScriptProcessor() );
		// if ( Loader.getConfig().getBoolean( "advanced.scripting.groovyEnabled", true ) )
		// EvalFactory.register( new GroovyScriptProcessor() );

		ScriptingFactory.register( this );

		ExceptionReport.registerException( new ExceptionCallback()
		{
			@Override
			public ReportingLevel callback( Throwable cause, ExceptionReport report, ExceptionContext context )
			{
				MultipleCompilationErrorsException exp = ( MultipleCompilationErrorsException ) cause;
				ErrorCollector e = exp.getErrorCollector();
				boolean abort = false;

				for ( Object err : e.getErrors() )
					if ( err instanceof Throwable )
					{
						if ( report.handleException( ( Throwable ) err, context ) )
							abort = true;
					}
					else if ( err instanceof ExceptionMessage )
					{
						if ( report.handleException( ( ( ExceptionMessage ) err ).getCause(), context ) )
							abort = true;
					}
					else if ( err instanceof SyntaxErrorMessage )
					{
						report.handleException( ( ( SyntaxErrorMessage ) err ).getCause(), context );
						abort = true;
					}
					else if ( err instanceof Message )
					{
						StringWriter writer = new StringWriter();
						( ( Message ) err ).write( new PrintWriter( writer, true ) );
						report.handleException( new ScriptingException( ReportingLevel.E_NOTICE, writer.toString() ), context );
					}
				return abort ? ReportingLevel.E_ERROR : ReportingLevel.E_IGNORABLE;
			}
		}, MultipleCompilationErrorsException.class );

		ExceptionReport.registerException( new ExceptionCallback()
		{
			@Override
			public ReportingLevel callback( Throwable cause, ExceptionReport report, ExceptionContext context )
			{
				report.addException( ReportingLevel.E_ERROR, cause );
				return ReportingLevel.E_ERROR;
			}
		}, TimeoutException.class, MissingMethodException.class, CompilationFailedException.class, SandboxSecurityException.class, GroovyRuntimeException.class );

		ExceptionReport.registerException( new ExceptionCallback()
		{
			@Override
			public ReportingLevel callback( Throwable cause, ExceptionReport report, ExceptionContext context )
			{
				report.addException( ReportingLevel.E_PARSE, cause );
				return ReportingLevel.E_PARSE;
			}
		}, SyntaxException.class );

		/**
		 * {@link TimeoutException} is thrown when a script does not exit within an alloted amount of time.<br>
		 * {@link MissingMethodException} is thrown when a groovy script tries to call a non-existent method<br>
		 * {@link SyntaxException} is for when the user makes a syntax coding error<br>
		 * {@link CompilationFailedException} is for when compilation fails from source errors<br>
		 * {@link SandboxSecurityException} thrown when script attempts to access a blacklisted API<br>
		 * {@link GroovyRuntimeException} thrown for basically all remaining Groovy exceptions not caught above
		 */
	}

	@SuppressWarnings( "deprecation" )
	public GroovyShell getNewShell( ScriptingContext context, Binding binding )
	{
		CompilerConfiguration configuration = new CompilerConfiguration();

		/*
		 * Finalize Imports and implement Sandbox
		 */
		configuration.addCompilationCustomizers( imports, timedInterrupt, secure );

		if ( context.getScriptBaseClass() == null || context.getScriptBaseClass().length() == 0 )
			configuration.setScriptBaseClass( ScriptingBaseGroovy.class.getName() );
		else
			configuration.setScriptBaseClass( context.getScriptBaseClass() );

		/*
		 * Set default encoding
		 */
		configuration.setSourceEncoding( context.factory().charset().name() );

		configuration.setTargetDirectory( context.cache() );

		return new GroovyShell( Loader.class.getClassLoader(), binding, configuration );
	}

	@Override
	public ScriptingEngine[] makeEngines( ScriptingContext context )
	{
		return new ScriptingEngine[] {new GroovyEngine( this ), new EmbeddedGroovyEngine( this )};
	}

	public Script makeScript( GroovyShell shell, ScriptingContext context ) throws ScriptingException
	{
		return makeScript( shell, context.readString(), context );
	}

	public Script makeScript( GroovyShell shell, String source, ScriptingContext context ) throws ScriptingException
	{
		// TODO Determine if a package node is prohibited and replace with an alternative, e.g., public, private, etc.

		if ( source.contains( "package " ) )
			throw new ScriptingException( ReportingLevel.E_ERROR, "Package path is predefined by Groovy Engine, remove `package` from source." );

		if ( context.scriptPackage() != null )
		{
			source = "package " + context.scriptPackage() + "; " + source;
			context.baseSource( source );
		}

		return shell.parse( source, context.scriptName() );
	}
}
