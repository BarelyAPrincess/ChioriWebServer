/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory;

import groovy.lang.GroovyShell;
import groovy.transform.TimedInterrupt;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.Validate;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;

import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountType;
import com.chiorichan.account.auth.AccountAuthenticator;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventException;
import com.chiorichan.event.Listener;
import com.chiorichan.factory.event.PreCoffeeProcessor;
import com.chiorichan.factory.event.PostEvalEvent;
import com.chiorichan.factory.event.PreEvalEvent;
import com.chiorichan.factory.event.PostImageProcessor;
import com.chiorichan.factory.event.PostJSMinProcessor;
import com.chiorichan.factory.event.PreLessProcessor;
import com.chiorichan.factory.event.PreParseWrapper;
import com.chiorichan.factory.parsers.IncludesParser;
import com.chiorichan.factory.parsers.LinksParser;
import com.chiorichan.factory.processors.EmbeddedGroovyScriptProcessor;
import com.chiorichan.factory.processors.GroovyScriptProcessor;
import com.chiorichan.factory.processors.ScriptingProcessor;
import com.chiorichan.lang.ErrorReporting;
import com.chiorichan.lang.EvalException;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.session.SessionManager;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Timings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class EvalFactory
{
	/*
	 * Groovy Imports :P
	 */
	private static final Class<?>[] classImports = new Class<?>[] {Loader.class, AccountManager.class, AccountType.class, Account.class, AccountAuthenticator.class, EventBus.class, PermissionManager.class, PluginManager.class, TaskManager.class, Timings.class, SessionManager.class, SiteManager.class, Site.class};
	
	private static final GroovyImportCustomizer imports = new GroovyImportCustomizer();
	private static final List<ScriptingProcessor> processors = Lists.newCopyOnWriteArrayList();
	private static final GroovySandbox secure = new GroovySandbox();
	private static final String[] starImports = new String[] {"com.chiorichan.lang", "com.chiorichan.util", "org.apache.commons.lang3.text", "org.ocpsoft.prettytime", "java.util", "java.net", "com.google.common.base"};
	
	private static final String[] staticImports = new String[] {"com.chiorichan.util.Looper"};
	/*
	 * Groovy Sandbox Customization
	 */
	private static final ASTTransformationCustomizer timedInterrupt = new ASTTransformationCustomizer( TimedInterrupt.class );
	
	static
	{
		/**
		 * Register Pre-Processors
		 */
		register( new PreParseWrapper( new LinksParser(), new IncludesParser() ) );
		if ( Loader.getConfig().getBoolean( "advanced.processors.coffeeProcessorEnabled", true ) )
			register( new PreCoffeeProcessor() );
		if ( Loader.getConfig().getBoolean( "advanced.processors.lessProcessorEnabled", true ) )
			register( new PreLessProcessor() );
		// register( new SassPreProcessor() );
		
		/**
		 * Register Script-Processors
		 */
		if ( Loader.getConfig().getBoolean( "advanced.scripting.gspEnabled", true ) )
			register( new EmbeddedGroovyScriptProcessor() );
		if ( Loader.getConfig().getBoolean( "advanced.scripting.groovyEnabled", true ) )
			register( new GroovyScriptProcessor() );
		
		/**
		 * Register Post-Processors
		 */
		if ( Loader.getConfig().getBoolean( "advanced.processors.minifierJSProcessorEnabled", true ) )
			register( new PostJSMinProcessor() );
		if ( Loader.getConfig().getBoolean( "advanced.processors.imageProcessorEnabled", true ) )
			register( new PostImageProcessor() );
		
		imports.addImports( classImports );
		imports.addStarImports( starImports );
		imports.addStaticStars( staticImports );
		
		// Transforms scripts to limit their execution to 30 seconds.
		long timeout = Loader.getConfig().getLong( "advanced.security.defaultScriptTimeout", 30L );
		if ( timeout > 0 )
		{
			Map<String, Object> timedInterruptParams = Maps.newHashMap();
			timedInterruptParams.put( "value", timeout );
			timedInterrupt.setAnnotationParameters( timedInterruptParams );
		}
	}
	
	private final EvalBinding binding;
	
	private final List<ByteBuf> bufferStack = Lists.newLinkedList();
	
	private Charset charset = Charsets.toCharset( Loader.getConfig().getString( "server.defaultEncoding", "UTF-8" ) );
	
	private final Set<GroovyShellTracker> groovyShells = Sets.newLinkedHashSet();
	
	private final ByteBuf output = Unpooled.buffer();
	
	private final ShellFactory shellFactory = new ShellFactory();
	
	private EvalFactory( EvalBinding binding )
	{
		Validate.notNull( binding, "The EvalBinding can't be null" );
		
		this.binding = binding;
		setOutputStream( output );
	}
	
	public static EvalFactory create( BindingProvider provider )
	{
		return new EvalFactory( provider.getBinding() );
	}
	
	public static EvalFactory create( EvalBinding binding )
	{
		return new EvalFactory( binding );
	}
	
	public static void register( Listener listener )
	{
		EventBus.INSTANCE.registerEvents( listener, Loader.getInstance() );
	}
	
	public static void register( ScriptingProcessor processor )
	{
		processors.add( processor );
	}
	
	public Charset charset()
	{
		return charset;
	}
	
	public EvalFactoryResult eval( EvalExecutionContext context )
	{
		EvalFactoryResult result = context.result();
		GroovyShellTracker tracker = getUnusedShellTracker();
		
		context.charset( charset );
		context.baseSource( new String( context.readBytes(), charset ) );
		
		synchronized ( tracker )
		{
			try
			{
				tracker.setInUse( true );
				GroovyShell shell = tracker.getShell();
				Loader.getLogger().fine( "Locking GroovyShell '" + shell.toString() + "' for execution of '" + context.filename() + "'" );
				
				if ( !context.prepare( shell ) )
					return result;
				
				// Loader.getLogger().debug( Hex.encodeHexString( context.baseSource().getBytes( charset ) ).substring( 0, 255 ) + " <--> " + Hex.encodeHexString( context.readBytes() ).substring( 0, 255 ) );
				
				PreEvalEvent preEvent = new PreEvalEvent( context );
				try
				{
					EventBus.INSTANCE.callEventWithException( preEvent );
				}
				catch ( EventException e )
				{
					EvalException.exceptionHandler( e.getCause() == null ? e : e.getCause(), shellFactory, result, ErrorReporting.E_WARNING, "Exception caught while running PreEvent" );
				}
				
				for ( ScriptingProcessor s : processors )
				{
					List<String> handledTypes = Arrays.asList( s.getHandledTypes() );
					
					for ( String she : handledTypes )
						if ( she.equalsIgnoreCase( context.shell() ) || she.equalsIgnoreCase( "all" ) )
							try
							{
								bufferStack.add( output.copy() );
								output.clear();
								s.eval( context, shellFactory.setShell( shell ) );
								context.resetAndWrite( output );
								output.clear();
								output.writeBytes( bufferStack.remove( bufferStack.size() - 1 ) );
								break;
							}
							catch ( Throwable t )
							{
								EvalException.exceptionHandler( t, shellFactory, result );
								return result;
							}
				}
				
				PostEvalEvent postEvent = new PostEvalEvent( context );
				try
				{
					EventBus.INSTANCE.callEventWithException( postEvent );
				}
				catch ( EventException e )
				{
					EvalException.exceptionHandler( e.getCause() == null ? e : e.getCause(), shellFactory, result, ErrorReporting.E_WARNING, "Exception caught while running PostEvent" );
				}
				
				return result.success( true );
			}
			finally
			{
				Loader.getLogger().fine( "Unlocking GroovyShell '" + context.toString() + "' for execution of '" + context.filename() + "'" );
				tracker.setInUse( false );
			}
		}
	}
	
	public String getFileName()
	{
		List<ScriptTraceElement> scriptTrace = getScriptTrace();
		
		if ( scriptTrace.size() < 1 )
			return "<unknown>";
		
		String fileName = scriptTrace.get( scriptTrace.size() - 1 ).context().filename();
		
		if ( fileName == null || fileName.isEmpty() )
			return "<unknown>";
		
		return fileName;
	}
	
	/**
	 * Attempts to find the current line number for the current groovy script.
	 * 
	 * @return The current line number. Returns -1 if no there was a problem getting the current line number.
	 */
	public int getLineNumber()
	{
		List<ScriptTraceElement> scriptTrace = getScriptTrace();
		
		if ( scriptTrace.size() < 1 )
			return -1;
		
		return scriptTrace.get( scriptTrace.size() - 1 ).getLineNumber();
	}
	
	/**
	 * Attempts to create a new GroovyShell instance using our own CompilerConfigurations
	 * 
	 * @return
	 *         new instance of GroovyShell
	 */
	protected GroovyShell getNewShell()
	{
		CompilerConfiguration configuration = new CompilerConfiguration();
		
		/*
		 * Finalize Imports and implement Sandbox
		 */
		configuration.addCompilationCustomizers( imports, timedInterrupt, secure );
		
		/*
		 * Set Groovy Base Script Class
		 */
		configuration.setScriptBaseClass( ScriptingBaseGroovy.class.getName() );
		
		/*
		 * Set default encoding
		 */
		configuration.setSourceEncoding( charset.name() );
		
		return new GroovyShell( Loader.class.getClassLoader(), binding, configuration );
	}
	
	public List<ScriptTraceElement> getScriptTrace()
	{
		return shellFactory.examineStackTrace( Thread.currentThread().getStackTrace() );
	}
	
	public ShellFactory getShellFactory()
	{
		return shellFactory;
	}
	
	protected GroovyShellTracker getTracker( GroovyShell shell )
	{
		for ( GroovyShellTracker t : groovyShells )
			if ( t.getShell() == shell )
				return t;
		
		return null;
	}
	
	protected GroovyShell getUnusedShell()
	{
		for ( GroovyShellTracker tracker : groovyShells )
			if ( !tracker.isInUse() )
				return tracker.getShell();
		
		GroovyShell shell = getNewShell();
		groovyShells.add( new GroovyShellTracker( shell ) );
		return shell;
	}
	
	protected GroovyShellTracker getUnusedShellTracker()
	{
		for ( GroovyShellTracker tracker : groovyShells )
			if ( !tracker.isInUse() )
				return tracker;
		
		GroovyShell shell = getNewShell();
		GroovyShellTracker tracker = new GroovyShellTracker( shell );
		groovyShells.add( tracker );
		return tracker;
	}
	
	protected void lock( GroovyShell shell )
	{
		GroovyShellTracker tracker = getTracker( shell );
		
		if ( tracker == null )
		{
			tracker = new GroovyShellTracker( shell );
			groovyShells.add( tracker );
		}
		
		tracker.setInUse( true );
	}
	
	/**
	 * Called when each request is finished
	 * This method is mostly used to clear cache from the request
	 */
	public void onFinished()
	{
		shellFactory.onFinished();
	}
	
	public void setEncoding( Charset charset )
	{
		this.charset = charset;
	}
	
	public void setOutputStream( ByteBuf buffer )
	{
		try
		{
			binding.setProperty( "out", new PrintStream( new ByteBufOutputStream( buffer ), true, charset.name() ) );
		}
		catch ( UnsupportedEncodingException e )
		{
			e.printStackTrace();
		}
	}
	
	public void setVariable( String key, Object val )
	{
		binding.setVariable( key, val );
	}
	
	protected void unlock( GroovyShell shell )
	{
		GroovyShellTracker tracker = getTracker( shell );
		
		if ( tracker != null )
			tracker.setInUse( false );
	}
}
