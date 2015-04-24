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
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.syntax.SyntaxException;
import org.ocpsoft.prettytime.PrettyTime;

import com.chiorichan.ContentTypes;
import com.chiorichan.Loader;
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
import com.chiorichan.http.WebInterpreter;
import com.chiorichan.lang.EvalFactoryException;
import com.chiorichan.lang.IgnorableEvalException;
import com.chiorichan.lang.SandboxSecurityException;
import com.chiorichan.site.Site;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

@SuppressWarnings( "rawtypes" )
public class EvalFactory
{
	protected static Charset encoding = Charsets.toCharset( Loader.getConfig().getString( "server.defaultEncoding", "UTF-8" ) );
	
	protected static List<PreProcessor> preProcessors = Lists.newCopyOnWriteArrayList();
	protected static List<Interpreter> interpreters = Lists.newCopyOnWriteArrayList();
	protected static List<PostProcessor> postProcessors = Lists.newCopyOnWriteArrayList();
	
	protected ShellFactory shellFactory = new ShellFactory();
	protected Set<GroovyShellTracker> groovyShells = Sets.newLinkedHashSet();
	protected ByteArrayOutputStream bs = new ByteArrayOutputStream();
	protected Binding binding;
	
	/*
	 * Groovy Sandbox Customization
	 */
	private static final ImportCustomizer imports = new ImportCustomizer();
	private static final GroovySandbox secure = new GroovySandbox();
	
	/*
	 * Groovy Imports :P
	 */
	private static final String[] dynImports = new String[] {"com.chiorichan.Loader"};
	private static final String[] starImports = new String[] {"com.chiorichan.lang", "com.chiorichan.util", "org.apache.commons.lang3.text", "org.ocpsoft.prettytime", "java.util", "java.net"};
	private static final String[] staticImports = new String[] {};
	
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
		
		imports.addImports( dynImports );
		imports.addStarImports( starImports );
		imports.addStaticStars( staticImports );
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
		return provider.getEvalFactory();
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
		configuration.addCompilationCustomizers( imports, secure );
		
		/*
		 * Set Groovy Base Script Class
		 */
		configuration.setScriptBaseClass( ScriptingBaseGroovy.class.getName() );
		
		/*
		 * Set default encoding
		 */
		configuration.setSourceEncoding( encoding.name() );
		
		return new GroovyShell( Loader.class.getClassLoader(), binding, configuration );
	}
	
	protected GroovyShellTracker getTracker( GroovyShell shell )
	{
		for ( GroovyShellTracker t : groovyShells )
			if ( t.getShell() == shell )
				return t;
		
		return null;
	}
	
	public List<ScriptTraceElement> getScriptTrace()
	{
		return shellFactory.examineStackTrace( Thread.currentThread().getStackTrace() );
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
		
		return scriptTrace.get( scriptTrace.size() - 1 ).lineNum;
	}
	
	public String getFileName()
	{
		List<ScriptTraceElement> scriptTrace = getScriptTrace();
		
		if ( scriptTrace.size() < 1 )
			return "<unknown>";
		
		String fileName = scriptTrace.get( scriptTrace.size() - 1 ).metaData.fileName;
		
		if ( fileName == null || fileName.isEmpty() )
			return "<unknown>";
		
		return fileName;
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
			binding.setProperty( "out", new PrintStream( bs, true, encoding.name() ) );
		}
		catch ( UnsupportedEncodingException e )
		{
			e.printStackTrace();
		}
	}
	
	public void setEncoding( Charset encoding )
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
	
	public EvalFactoryResult eval( File fi, Site site ) throws EvalFactoryException
	{
		EvalMetaData codeMeta = new EvalMetaData();
		
		codeMeta.shell = FileInterpreter.determineShellFromName( fi.getName() );
		codeMeta.fileName = fi.getAbsolutePath();
		
		return eval( fi, codeMeta, site );
	}
	
	public EvalFactoryResult eval( File fi, EvalMetaData meta, Site site ) throws EvalFactoryException
	{
		try
		{
			return eval( FileUtils.readFileToString( fi, encoding ), meta, site );
		}
		catch ( IOException e )
		{
			throw new EvalFactoryException( e, shellFactory );
		}
	}
	
	public EvalFactoryResult eval( FileInterpreter fi, Site site ) throws EvalFactoryException
	{
		return eval( fi, null, site );
	}
	
	public EvalFactoryResult eval( FileInterpreter fi, EvalMetaData meta, Site site ) throws EvalFactoryException
	{
		if ( meta == null )
			meta = new EvalMetaData();
		
		if ( fi instanceof WebInterpreter )
			meta.params = ( ( WebInterpreter ) fi ).getRewriteParams();
		else
			meta.params = fi.getParams();
		
		meta.contentType = fi.getContentType();
		meta.shell = fi.getParams().get( "shell" );
		meta.fileName = ( fi.getFile() != null ) ? fi.getFile().getAbsolutePath() : fi.getParams().get( "file" );
		
		return eval( fi.consumeString(), meta, site );
	}
	
	public EvalFactoryResult eval( String code, Site site ) throws EvalFactoryException
	{
		EvalMetaData codeMeta = new EvalMetaData();
		
		codeMeta.shell = "html";
		
		return eval( code, codeMeta, site );
	}
	
	public EvalFactoryResult eval( String code, EvalMetaData meta, Site site ) throws EvalFactoryException
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
		
		try
		{
			if ( site != null )
				code = runParsers( code, site, meta );
		}
		catch ( Exception e )
		{
			result.addException( ignorableExceptionHandler( e, meta, "Exception caught while executing runParsers" ) );
		}
		
		for ( PreProcessor p : preProcessors )
		{
			Set<String> handledTypes = new HashSet<String>( Arrays.asList( p.getHandledTypes() ) );
			
			for ( String t : handledTypes )
				if ( t.equalsIgnoreCase( meta.shell ) || meta.contentType.toLowerCase().contains( t.toLowerCase() ) || t.equalsIgnoreCase( "all" ) )
				{
					try
					{
						String evaled = p.process( meta, code );
						if ( evaled != null )
						{
							code = evaled;
							break;
						}
					}
					catch ( Exception e )
					{
						result.addException( ignorableExceptionHandler( e, meta, "Exception caught while running PreProcessor `" + p.getClass().getSimpleName() + "`" ) );
					}
				}
		}
		
		GroovyShellTracker tracker = getUnusedShellTracker();
		GroovyShell shell = tracker.getShell();
		
		shell.setVariable( "__FILE__", meta.fileName );
		
		ByteBuf output = Unpooled.buffer();
		boolean success = false;
		
		synchronized ( tracker )
		{
			Loader.getLogger().fine( "Locking GroovyShell '" + shell.toString() + "' for execution of '" + meta.fileName + "', length '" + code.length() + "'" );
			tracker.setInUse( true );
			
			byte[] saved = bs.toByteArray();
			bs.reset();
			
			for ( Interpreter s : interpreters )
			{
				Set<String> handledTypes = new HashSet<String>( Arrays.asList( s.getHandledTypes() ) );
				
				for ( String she : handledTypes )
				{
					if ( she.equalsIgnoreCase( meta.shell ) || she.equalsIgnoreCase( "all" ) )
					{
						try
						{
							result.obj = s.eval( meta, code, shellFactory.setShell( shell ), bs );
						}
						catch ( Throwable t )
						{
							exceptionHandler( t, meta );
						}
						
						success = true;
						break;
					}
				}
			}
			
			try
			{
				output.writeBytes( ( success ) ? bs.toByteArray() : code.getBytes( encoding ) );
				
				bs.reset();
				bs.write( saved );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
			
			Loader.getLogger().fine( "Unlocking GroovyShell '" + shell.toString() + "' for execution of '" + meta.fileName + "', length '" + code.length() + "'" );
			tracker.setInUse( false );
		}
		
		for ( PostProcessor p : postProcessors )
		{
			Set<String> handledTypes = new HashSet<String>( Arrays.asList( p.getHandledTypes() ) );
			
			for ( String t : handledTypes )
				if ( t.equalsIgnoreCase( meta.shell ) || meta.contentType.toLowerCase().contains( t.toLowerCase() ) || t.equalsIgnoreCase( "all" ) )
				{
					try
					{
						ByteBuf finished = p.process( meta, output );
						if ( finished != null )
						{
							output = finished;
							break;
						}
					}
					catch ( Exception e )
					{
						result.addException( ignorableExceptionHandler( e, meta, "Exception caught while running PreProcessor `" + p.getClass().getSimpleName() + "`" ) );
					}
				}
		}
		
		return result.setResult( output, true );
	}
	
	private IgnorableEvalException ignorableExceptionHandler( Throwable t, EvalMetaData meta, String msg ) throws EvalFactoryException
	{
		if ( t instanceof IgnorableEvalException )
		{
			return ( IgnorableEvalException ) t;
		}
		else if ( t instanceof EvalFactoryException )
		{
			return ignorableExceptionHandler( t.getCause(), meta, msg );
		}
		else if ( t instanceof MultipleCompilationErrorsException )
		{
			MultipleCompilationErrorsException e = ( MultipleCompilationErrorsException ) t;
			
			if ( e.getErrorCollector().getErrorCount() > 0 )
			{
				if ( e.getErrorCollector().getErrorCount() == 1 )
					return ignorableExceptionHandler( e.getErrorCollector().getException( 0 ), meta, msg );
				
				// TODO What should be do when there are more than one exceptions
				Loader.getLogger().debug( "Got Exception: " + e.getErrorCollector().getErrors() );
				return ignorableExceptionHandler( e.getErrorCollector().getException( 0 ), meta, msg );
			}
			
			return null;
		}
		else if ( t instanceof CompilationFailedException ) // This is usually a parsing exception
		{
			CompilationFailedException e = ( CompilationFailedException ) t;
			throw new EvalFactoryException( e, shellFactory );
		}
		else if ( t instanceof SandboxSecurityException ) // This usually happens when the script tries to use a blacklisted feature
		{
			SandboxSecurityException e = ( SandboxSecurityException ) t;
			throw new EvalFactoryException( e, shellFactory, meta );
		}
		else if ( t instanceof GroovyRuntimeException )
		{
			GroovyRuntimeException e = ( GroovyRuntimeException ) t;
			throw new EvalFactoryException( e, shellFactory );
		}
		else
		{
			return new IgnorableEvalException( msg, t );
		}
	}
	
	private void exceptionHandler( Throwable t, EvalMetaData meta ) throws EvalFactoryException
	{
		if ( t instanceof EvalFactoryException )
		{
			throw ( EvalFactoryException ) t;
		}
		else if ( t instanceof MultipleCompilationErrorsException )
		{
			MultipleCompilationErrorsException e = ( MultipleCompilationErrorsException ) t;
			
			if ( e.getErrorCollector().getErrorCount() > 0 )
			{
				if ( e.getErrorCollector().getErrorCount() == 1 )
					exceptionHandler( e.getErrorCollector().getException( 0 ), meta );
				
				// TODO What should be do when there are more than one exceptions
				Loader.getLogger().debug( "Got Exception: " + e.getErrorCollector().getErrors() );
				exceptionHandler( e.getErrorCollector().getException( 0 ), meta );
			}
		}
		else if ( t instanceof SyntaxException ) // Parsing exception
		{
			SyntaxException e = ( SyntaxException ) t;
			throw new EvalFactoryException( e, shellFactory, meta );
		}
		else if ( t instanceof CompilationFailedException ) // This is usually a parsing exception
		{
			CompilationFailedException e = ( CompilationFailedException ) t;
			throw new EvalFactoryException( e, shellFactory, meta );
		}
		else if ( t instanceof SandboxSecurityException ) // This usually happens when the script tries to use a blacklisted feature
		{
			SandboxSecurityException e = ( SandboxSecurityException ) t;
			throw new EvalFactoryException( e, shellFactory, meta );
		}
		else if ( t instanceof GroovyRuntimeException )
		{
			GroovyRuntimeException e = ( GroovyRuntimeException ) t;
			throw new EvalFactoryException( e, shellFactory );
		}
		else
		{
			Loader.getLogger().warning( "Uncaught exception in EvalFactory for exception " + t.getClass().getName() );
			throw new EvalFactoryException( t, shellFactory );
		}
	}
	
	private String runParsers( String source, Site site, EvalMetaData meta ) throws Exception
	{
		source = new IncludesParser().runParser( source, site, meta, this );
		source = new LinksParser().runParser( source, site );
		
		return source;
	}
	
	/**
	 * Called when each request is finished
	 * This method is mostly used to clear cache from the request
	 */
	public void onFinished()
	{
		shellFactory.onFinished();
	}
	
	public ShellFactory getShellFactory()
	{
		return shellFactory;
	}
}
