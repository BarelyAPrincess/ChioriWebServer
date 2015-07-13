/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory;

import groovy.lang.Binding;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.Validate;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

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
import com.chiorichan.factory.processors.ScriptingProcessor;
import com.chiorichan.lang.ErrorReporting;
import com.chiorichan.lang.EvalException;
import com.chiorichan.util.WebFunc;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class EvalFactory
{
	private static final List<ScriptingProcessor> processors = Lists.newCopyOnWriteArrayList();
	
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
	
	private final EvalBinding binding;
	
	private final List<ByteBuf> bufferStack = Lists.newLinkedList();
	
	private Charset charset = Charsets.toCharset( Loader.getConfig().getString( "server.defaultEncoding", "UTF-8" ) );
	
	private final ByteBuf output = Unpooled.buffer();
	
	private final StackFactory stackFactory = new StackFactory();
	
	private EvalFactory( EvalBinding binding )
	{
		Validate.notNull( binding, "The EvalBinding can't be null" );
		
		this.binding = binding;
		setOutputStream( output );
	}
	
	// For Web Use
	public static EvalFactory create( BindingProvider provider )
	{
		return new EvalFactory( provider.getBinding() );
	}
	
	// For General Use
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
	
	public Binding binding()
	{
		return binding;
	}
	
	public Charset charset()
	{
		return charset;
	}
	
	public EvalResult eval( EvalContext context )
	{
		EvalResult result = context.result();
		
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
				EvalException.exceptionHandler( new EvalException( ErrorReporting.E_ERROR, "Evaluation was cancelled by an internal event" ), context );
				return result;
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
							String hash = context.bufferHash();
							s.eval( context );
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
							{
								Loader.getLogger().debug( Joiner.on( ", " ).join( result.getExceptions() ) );
								return result;
							}
						}
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
		DefaultGroovyMethods.print( ( PrintStream ) binding.getProperty( "out" ), text );
	}
	
	/**
	 * Gives externals subroutines access to the current output stream via println()
	 * 
	 * @param text
	 *            The text to output
	 */
	public void println( String text )
	{
		DefaultGroovyMethods.println( ( PrintStream ) binding.getProperty( "out" ), text );
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
	
	public StackFactory stack()
	{
		return stackFactory;
	}
}
