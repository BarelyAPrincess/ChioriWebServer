/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventException;
import com.chiorichan.event.Listener;
import com.chiorichan.factory.event.PostEvalEvent;
import com.chiorichan.factory.event.PostImageProcessor;
import com.chiorichan.factory.event.PostJSMinProcessor;
import com.chiorichan.factory.event.PreCoffeeProcessor;
import com.chiorichan.factory.event.PreEvalEvent;
import com.chiorichan.factory.event.PreLessProcessor;
import com.chiorichan.factory.groovy.GroovyRegistry;
import com.chiorichan.factory.parsers.PreIncludesParserWrapper;
import com.chiorichan.factory.parsers.PreLinksParserWrapper;
import com.chiorichan.lang.ReportingLevel;
import com.chiorichan.lang.EvalException;
import com.chiorichan.util.WebFunc;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ScriptingFactory
{
	private static final List<ScriptingRegistry> scripting = Lists.newCopyOnWriteArrayList();
	static
	{
		new GroovyRegistry();
		
		/**
		 * Register Pre-Processors
		 */
		register( new PreLinksParserWrapper() );
		register( new PreIncludesParserWrapper() );
		if ( Loader.getConfig().getBoolean( "advanced.processors.coffeeProcessorEnabled", true ) )
			register( new PreCoffeeProcessor() );
		if ( Loader.getConfig().getBoolean( "advanced.processors.lessProcessorEnabled", true ) )
			register( new PreLessProcessor() );
		// register( new SassPreProcessor() );
		
		/**
		 * Register Post-Processors
		 */
		if ( Loader.getConfig().getBoolean( "advanced.processors.minifierJSProcessorEnabled", true ) )
			register( new PostJSMinProcessor() );
		if ( Loader.getConfig().getBoolean( "advanced.processors.imageProcessorEnabled", true ) )
			register( new PostImageProcessor() );
	}
	
	private final Map<ScriptingEngine, List<String>> engines = Maps.newLinkedHashMap();
	
	private final ScriptBinding binding;
	
	private final List<ByteBuf> bufferStack = Lists.newLinkedList();
	
	private Charset charset = Charsets.toCharset( Loader.getConfig().getString( "server.defaultEncoding", "UTF-8" ) );
	
	private final ByteBuf output = Unpooled.buffer();
	
	private final StackFactory stackFactory = new StackFactory();
	
	private ScriptingFactory( ScriptBinding binding )
	{
		Validate.notNull( binding, "The EvalBinding can't be null" );
		this.binding = binding;
	}
	
	// For Web Use
	public static ScriptingFactory create( BindingProvider provider )
	{
		return new ScriptingFactory( provider.getBinding() );
	}
	
	// For General Use
	public static ScriptingFactory create( ScriptBinding binding )
	{
		return new ScriptingFactory( binding );
	}
	
	public static void register( Listener listener )
	{
		EventBus.INSTANCE.registerEvents( listener, Loader.getInstance() );
	}
	
	/**
	 * Registers the provided ScriptingProcessing with the EvalFactory
	 *
	 * @param registry
	 *            The {@link ScriptingRegistry} instance to handle provided types
	 */
	public static void register( ScriptingRegistry registry )
	{
		if ( !scripting.contains( registry ) )
			scripting.add( registry );
	}
	
	public ScriptBinding binding()
	{
		return binding;
	}
	
	public Charset charset()
	{
		return charset;
	}
	
	private void compileEngines( ScriptingContext context )
	{
		for ( ScriptingRegistry registry : scripting )
			for ( ScriptingEngine engine : registry.makeEngines( context ) )
				if ( !contains( engine ) )
				{
					engine.setBinding( binding );
					engine.setOutput( output, charset );
					engines.put( engine, engine.getTypes() );
				}
	}
	
	private boolean contains( ScriptingEngine engine2 )
	{
		for ( ScriptingEngine engine1 : engines.keySet() )
			if ( engine1.getClass() == engine2.getClass() )
				return true;
		return false;
	}
	
	public ScriptingResult eval( ScriptingContext context )
	{
		ScriptingResult result = context.result();
		
		context.factory( this );
		context.charset( charset );
		context.baseSource( new String( context.readBytes(), charset ) );
		binding.setVariable( "__FILE__", context.filename() == null ? "<no file>" : context.filename() );
		
		if ( result.hasNotIgnorableExceptions() )
			return result;
		
		try
		{
			String name = "EvalScript" + WebFunc.randomNum( 8 ) + ".chi";
			context.name( name );
			stackFactory.stack( name, context );
			
			PreEvalEvent preEvent = new PreEvalEvent( context );
			try
			{
				EventBus.INSTANCE.callEventWithException( preEvent );
			}
			catch ( EventException e )
			{
				if ( EvalException.exceptionHandler( e.getCause() == null ? e : e.getCause(), context ) )
					return result;
			}
			
			if ( preEvent.isCancelled() )
			{
				EvalException.exceptionHandler( new EvalException( ReportingLevel.E_ERROR, "Evaluation was cancelled by an internal event" ), context );
				return result;
			}
			
			if ( engines.isEmpty() )
				compileEngines( context );
			
			if ( !engines.isEmpty() )
				for ( Entry<ScriptingEngine, List<String>> entry : engines.entrySet() )
					if ( entry.getValue() == null || entry.getValue().isEmpty() || entry.getValue().contains( context.shell().toLowerCase() ) )
						try
						{
							bufferStack.add( output.copy() );
							output.clear();
							String hash = context.bufferHash();
							entry.getKey().eval( context );
							if ( context.bufferHash().equals( hash ) )
								context.resetAndWrite( output );
							else
								context.write( output );
							output.clear();
							output.writeBytes( bufferStack.remove( bufferStack.size() - 1 ) );
							break;
						}
						catch ( Throwable cause )
						{
							if ( EvalException.exceptionHandler( cause, context ) )
								return result;
						}
			
			PostEvalEvent postEvent = new PostEvalEvent( context );
			try
			{
				EventBus.INSTANCE.callEventWithException( postEvent );
			}
			catch ( EventException e )
			{
				if ( EvalException.exceptionHandler( e.getCause() == null ? e : e.getCause(), context ) )
					return result;
			}
		}
		finally
		{
			stackFactory.unstack();
		}
		
		return result.success( true );
	}
	
	public Charset getCharset()
	{
		return charset;
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
	
	public ByteBuf getOutputStream()
	{
		return output;
	}
	
	public List<ScriptTraceElement> getScriptTrace()
	{
		return stackFactory.examineStackTrace( Thread.currentThread().getStackTrace() );
	}
	
	/**
	 * Gives externals subroutines access to the current output stream via print()
	 * 
	 * @param text
	 *            The text to output
	 */
	public void print( String text )
	{
		output.writeBytes( text.getBytes( charset ) );
	}
	
	/**
	 * Gives externals subroutines access to the current output stream via println()
	 * 
	 * @param text
	 *            The text to output
	 */
	public void println( String text )
	{
		output.writeBytes( ( text + "\n" ).getBytes( charset ) );
	}
	
	public void setEncoding( Charset charset )
	{
		this.charset = charset;
	}
	
	public void setVariable( String key, Object val )
	{
		binding.setVariable( key, val );
	}
	
	public StackFactory stack()
	{
		return stackFactory;
	}
}
