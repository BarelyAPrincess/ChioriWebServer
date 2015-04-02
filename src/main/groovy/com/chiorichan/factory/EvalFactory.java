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

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.control.CompilerConfiguration;

import com.chiorichan.ContentTypes;
import com.chiorichan.Loader;
import com.chiorichan.exception.ShellExecuteException;
import com.chiorichan.factory.interpreters.GSPInterpreter;
import com.chiorichan.factory.interpreters.GroovyInterpreter;
import com.chiorichan.factory.interpreters.HTMLInterpreter;
import com.chiorichan.factory.interpreters.Interpreter;
import com.chiorichan.factory.parsers.IncludesParser;
import com.chiorichan.factory.parsers.LinksParser;
import com.chiorichan.factory.postprocessors.ImagePostProcessor;
import com.chiorichan.factory.postprocessors.JSMinPostProcessor;
import com.chiorichan.factory.postprocessors.PostProcessor;
import com.chiorichan.factory.preprocessors.CoffeePreProcessor;
import com.chiorichan.factory.preprocessors.LessPreProcessor;
import com.chiorichan.factory.preprocessors.PreProcessor;
import com.chiorichan.framework.FileInterpreter;
import com.chiorichan.framework.Site;
import com.chiorichan.http.WebInterpreter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class EvalFactory
{
	public class GroovyShellTracker
	{
		private GroovyShell shell = null;
		private boolean inUse = false;
		private int lineNumber = -1;
		private EvalMetaData meta = null;
		
		public GroovyShellTracker( GroovyShell shell )
		{
			this.shell = shell;
		}
		
		public GroovyShell getShell()
		{
			return shell;
		}
		
		public void setInUse( boolean inUse )
		{
			this.inUse = inUse;
		}
		
		public boolean isInUse()
		{
			return inUse;
		}
		
		public int getLineNumber()
		{
			return lineNumber;
		}
		
		public void setLineNumber( int lineNumber )
		{
			this.lineNumber = lineNumber;
		}
		
		public EvalMetaData getMetaData()
		{
			return meta;
		}
		
		public void setMetaData( EvalMetaData meta )
		{
			this.meta = meta;
		}
		
		@Override
		public String toString()
		{
			return "GroovyShellTracker(shell=" + shell + ",inUse=" + inUse + ",lineNumber=" + lineNumber + ")";
		}
	}
	
	protected String encoding = Loader.getConfig().getString( "server.defaultEncoding", "UTF-8" );
	
	protected static List<PreProcessor> preProcessors = Lists.newCopyOnWriteArrayList();
	protected static List<Interpreter> interpreters = Lists.newCopyOnWriteArrayList();
	protected static List<PostProcessor> postProcessors = Lists.newCopyOnWriteArrayList();
	
	protected Set<GroovyShellTracker> groovyShells = Sets.newLinkedHashSet();
	protected ByteArrayOutputStream bs = new ByteArrayOutputStream();
	protected Binding binding;
	
	static
	{
		// TODO Allow to override and/or extending of Pre-Processors, Interpreters and Post-Processors.
		
		/**
		 * Register Pre-Processors
		 */
		if ( Loader.getConfig().getBoolean( "advanced.processors.coffeeProcessorEnabled", true ) )
			register( new CoffeePreProcessor() );
		if ( Loader.getConfig().getBoolean( "advanced.processors.lessProcessorEnabled", true ) )
			register( new LessPreProcessor() );
		// register( new SassPreProcessor() );
		
		/**
		 * Register Interpreters
		 */
		if ( Loader.getConfig().getBoolean( "advanced.interpreters.gspEnabled", true ) )
			register( new GSPInterpreter() );
		if ( Loader.getConfig().getBoolean( "advanced.interpreters.groovyEnabled", true ) )
			register( new GroovyInterpreter() );
		register( new HTMLInterpreter() );
		
		/**
		 * Register Post-Processors
		 */
		if ( Loader.getConfig().getBoolean( "advanced.processors.minifierJSProcessorEnabled", true ) )
			register( new JSMinPostProcessor() );
		if ( Loader.getConfig().getBoolean( "advanced.processors.imageProcessorEnabled", true ) )
			register( new ImagePostProcessor() );
	}
	
	protected EvalFactory( Binding binding )
	{
		this.binding = binding;
		setOutputStream( bs );
	}
	
	public static EvalFactory create( Binding binding )
	{
		return new EvalFactory( binding );
	}
	
	public static EvalFactory create( BindingProvider provider )
	{
		return provider.getCodeFactory();
	}
	
	public void setVariable( String key, Object val )
	{
		binding.setVariable( key, val );
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
	
	protected GroovyShell getUnusedShell()
	{
		for ( GroovyShellTracker tracker : groovyShells )
			if ( !tracker.isInUse() )
				return tracker.getShell();
		
		GroovyShell shell = getNewShell();
		groovyShells.add( new GroovyShellTracker( shell ) );
		return shell;
	}
	
	protected GroovyShell getNewShell()
	{
		CompilerConfiguration configuration = new CompilerConfiguration();
		
		configuration.setScriptBaseClass( ScriptingBaseGroovy.class.getName() );
		configuration.setSourceEncoding( encoding );
		
		// TODO Extend class loader as to create a type of security protection
		return new GroovyShell( Loader.class.getClassLoader(), binding, configuration );
	}
	
	protected GroovyShellTracker getTracker( GroovyShell shell )
	{
		for ( GroovyShellTracker t : groovyShells )
			if ( t.getShell() == shell )
				return t;
		
		return null;
	}
	
	/**
	 * Attempts to find the current line number for the current groovy script.
	 * 
	 * @return The current line number. Returns -1 if no there was a problem getting the current line number.
	 */
	public int getLineNumber()
	{
		for ( StackTraceElement l : Thread.currentThread().getStackTrace() )
			if ( l.getFileName() != null && l.getFileName().matches( "Script\\d*.groovy" ) )
				return l.getLineNumber();
		
		return -1;
	}
	
	public String getFileName()
	{
		if ( getCurrentTracker() == null || getCurrentTracker().getMetaData() == null )
			return "<codeEval>";
		else
			return getCurrentTracker().getMetaData().fileName;
	}
	
	private GroovyShellTracker getCurrentTracker()
	{
		try
		{
			return groovyShells.toArray( new GroovyShellTracker[0] )[groovyShells.size() - 1];
		}
		catch ( ArrayIndexOutOfBoundsException e )
		{
			return null;
		}
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
	
	protected void unlock( GroovyShell shell )
	{
		GroovyShellTracker tracker = getTracker( shell );
		
		if ( tracker != null )
			tracker.setInUse( false );
	}
	
	public void setOutputStream( ByteArrayOutputStream bs )
	{
		try
		{
			binding.setProperty( "out", new PrintStream( bs, true, encoding ) );
		}
		catch ( UnsupportedEncodingException e )
		{
			e.printStackTrace();
		}
	}
	
	public void setEncoding( String encoding )
	{
		this.encoding = encoding;
		setOutputStream( bs );
	}
	
	/**
	 * 
	 * @param orig
	 *            , The original class you would like to override.
	 * @param replace
	 *            , An instance of the class you are overriding with. Must extend the original class.
	 */
	public static boolean overrideProcessor( Class<? extends PreProcessor> orig, PreProcessor replace )
	{
		if ( !orig.isAssignableFrom( replace.getClass() ) )
			return false;
		
		for ( PreProcessor p : preProcessors )
			if ( p.getClass().equals( orig ) )
				preProcessors.remove( p );
		register( replace );
		
		return true;
	}
	
	/**
	 * 
	 * @param orig
	 *            , The original class you would like to override.
	 * @param replace
	 *            , An instance of the class you are overriding with. Must extend the original class.
	 */
	public static boolean overrideInterpreter( Class<? extends Interpreter> orig, Interpreter replace )
	{
		if ( !orig.isAssignableFrom( replace.getClass() ) )
			return false;
		
		for ( Interpreter p : interpreters )
			if ( p.getClass().equals( orig ) )
				interpreters.remove( p );
		register( replace );
		
		return true;
	}
	
	/**
	 * 
	 * @param orig
	 *            The original class you would like to override.
	 * @param replace
	 *            An instance of the class you are overriding with. Must extend the original class.
	 */
	public static boolean overrideProcessor( Class<? extends PostProcessor> orig, PostProcessor replace )
	{
		if ( !orig.isAssignableFrom( replace.getClass() ) )
			return false;
		
		for ( PostProcessor p : postProcessors )
			if ( p.getClass().equals( orig ) )
				postProcessors.remove( p );
		register( replace );
		
		return true;
	}
	
	public static void register( PreProcessor preProcessor )
	{
		preProcessors.add( preProcessor );
	}
	
	public static void register( Interpreter interpreter )
	{
		interpreters.add( interpreter );
	}
	
	public static void register( PostProcessor postProcessor )
	{
		postProcessors.add( postProcessor );
	}
	
	public EvalFactoryResult eval( File fi, Site site ) throws ShellExecuteException
	{
		EvalMetaData codeMeta = new EvalMetaData();
		
		codeMeta.shell = FileInterpreter.determineShellFromName( fi.getName() );
		codeMeta.fileName = fi.getAbsolutePath();
		
		return eval( fi, codeMeta, site );
	}
	
	public EvalFactoryResult eval( File fi, EvalMetaData meta, Site site ) throws ShellExecuteException
	{
		try
		{
			return eval( FileUtils.readFileToString( fi, encoding ), meta, site );
		}
		catch ( IOException e )
		{
			throw new ShellExecuteException( e, meta );
		}
	}
	
	public EvalFactoryResult eval( FileInterpreter fi, Site site ) throws ShellExecuteException
	{
		return eval( fi, null, site );
	}
	
	public EvalFactoryResult eval( FileInterpreter fi, EvalMetaData codeMeta, Site site ) throws ShellExecuteException
	{
		if ( codeMeta == null )
			codeMeta = new EvalMetaData();
		
		if ( fi instanceof WebInterpreter )
			codeMeta.params = ( ( WebInterpreter ) fi ).getRewriteParams();
		else
			codeMeta.params = fi.getParams();
		
		codeMeta.contentType = fi.getContentType();
		codeMeta.shell = fi.getParams().get( "shell" );
		codeMeta.fileName = ( fi.getFile() != null ) ? fi.getFile().getAbsolutePath() : fi.getParams().get( "file" );
		
		try
		{
			return eval( new String( fi.getContent(), fi.getEncoding() ), codeMeta, site );
		}
		catch ( UnsupportedEncodingException e )
		{
			throw new ShellExecuteException( e, codeMeta );
		}
	}
	
	public EvalFactoryResult eval( String code, Site site ) throws ShellExecuteException
	{
		EvalMetaData codeMeta = new EvalMetaData();
		
		codeMeta.shell = "html";
		
		return eval( code, codeMeta, site );
	}
	
	public EvalFactoryResult eval( String code, EvalMetaData meta, Site site ) throws ShellExecuteException
	{
		EvalFactoryResult result = new EvalFactoryResult( meta, site );
		
		if ( code == null || code.isEmpty() )
			return result.setReason( "Code Block was null or empty!" );
		
		if ( meta.contentType == null )
			if ( meta.fileName == null )
				meta.contentType = meta.shell;
			else
				meta.contentType = ContentTypes.getContentType( meta.fileName );
		
		meta.source = code;
		meta.site = site;
		
		if ( site != null )
			code = runParsers( code, site );
		
		for ( PreProcessor p : preProcessors )
		{
			Set<String> handledTypes = new HashSet<String>( Arrays.asList( p.getHandledTypes() ) );
			
			for ( String t : handledTypes )
				if ( t.equalsIgnoreCase( meta.shell ) || meta.contentType.toLowerCase().contains( t.toLowerCase() ) || t.equalsIgnoreCase( "all" ) )
				{
					String evaled = p.process( meta, code );
					if ( evaled != null )
					{
						code = evaled;
						break;
					}
				}
		}
		
		GroovyShellTracker tracker = getUnusedShellTracker();
		GroovyShell shell = tracker.getShell();
		
		for ( StackTraceElement l : Thread.currentThread().getStackTrace() )
			if ( l.getFileName() != null && l.getFileName().matches( "Script\\d*.groovy" ) )
				tracker.setLineNumber( l.getLineNumber() );
		
		ByteBuf output = Unpooled.buffer();
		boolean success = false;
		
		Loader.getLogger().fine( "Locking GroovyShell '" + shell.toString() + "' for execution of '" + meta.fileName + "', length '" + code.length() + "', line '" + tracker.getLineNumber() + "'" );
		tracker.setInUse( true );
		tracker.setMetaData( meta );
		
		byte[] saved = bs.toByteArray();
		bs.reset();
		
		for ( Interpreter s : interpreters )
		{
			Set<String> handledTypes = new HashSet<String>( Arrays.asList( s.getHandledTypes() ) );
			
			for ( String she : handledTypes )
			{
				if ( she.equalsIgnoreCase( meta.shell ) || she.equalsIgnoreCase( "all" ) )
				{
					result.obj = s.eval( meta, code, shell, bs );
					success = true;
					break;
				}
			}
		}
		
		output.writeBytes( ( success ) ? bs.toByteArray() : code.getBytes() );
		try
		{
			bs.reset();
			bs.write( saved );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		
		Loader.getLogger().fine( "Unlocking GroovyShell '" + shell.toString() + "' for execution of '" + meta.fileName + "', length '" + code.length() + "', line '" + tracker.getLineNumber() + "'" );
		tracker.setInUse( false );
		tracker.setMetaData( null );
		
		for ( PostProcessor p : postProcessors )
		{
			Set<String> handledTypes = new HashSet<String>( Arrays.asList( p.getHandledTypes() ) );
			
			for ( String t : handledTypes )
				if ( t.equalsIgnoreCase( meta.shell ) || meta.contentType.toLowerCase().contains( t.toLowerCase() ) || t.equalsIgnoreCase( "all" ) )
				{
					ByteBuf finished = p.process( meta, output );
					if ( finished != null )
					{
						output = finished;
						break;
					}
				}
		}
		
		return result.setResult( output, true );
	}
	
	private String runParsers( String source, Site site ) throws ShellExecuteException
	{
		source = new IncludesParser().runParser( source, site, this );
		source = new LinksParser().runParser( source, site );
		
		return source;
	}
}
