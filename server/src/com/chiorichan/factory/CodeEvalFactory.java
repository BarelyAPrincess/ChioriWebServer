package com.chiorichan.factory;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.control.CompilerConfiguration;

import com.chiorichan.Loader;
import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.factory.interpreters.GSPInterpreter;
import com.chiorichan.factory.interpreters.GroovyInterpreter;
import com.chiorichan.factory.interpreters.HTMLInterpreter;
import com.chiorichan.factory.interpreters.Interpreter;
import com.chiorichan.factory.parsers.IncludesParser;
import com.chiorichan.factory.parsers.LinksParser;
import com.chiorichan.factory.postprocessors.JSMinPostProcessor;
import com.chiorichan.factory.postprocessors.PostProcessor;
import com.chiorichan.factory.preprocessors.CoffeePreProcessor;
import com.chiorichan.factory.preprocessors.LessPreProcessor;
import com.chiorichan.factory.preprocessors.PreProcessor;
import com.chiorichan.framework.FileInterpreter;
import com.chiorichan.framework.ScriptingBaseGroovy;
import com.chiorichan.framework.Site;
import com.chiorichan.util.StringUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class CodeEvalFactory
{
	protected String encoding = Loader.getConfig().getString( "server.defaultEncoding", "UTF-8" );
	
	protected static List<PreProcessor> preProcessors = Lists.newCopyOnWriteArrayList();
	protected static List<Interpreter> interpreters = Lists.newCopyOnWriteArrayList();
	protected static List<PostProcessor> postProcessors = Lists.newCopyOnWriteArrayList();
	
	protected Map<GroovyShell, Boolean> groovyShells = Maps.newConcurrentMap();
	protected ByteArrayOutputStream bs = new ByteArrayOutputStream();
	protected Binding binding;
	
	static
	{
		// TODO Allow to override and/or extending of Pre-Processors, Interpreters and Post-Processors.
		
		// Register Pre Processors
		register( new CoffeePreProcessor() );
		register( new LessPreProcessor() );
		// register( new SassPreProcessor() );
		
		// Register Interpreters
		register( new GSPInterpreter() );
		register( new HTMLInterpreter() );
		register( new GroovyInterpreter() );
		
		// Register Post Processors
		register( new JSMinPostProcessor() );
	}
	
	public static CodeEvalFactory create( Binding binding )
	{
		return new CodeEvalFactory( binding );
	}
	
	public static CodeEvalFactory create( BindingProvider provider )
	{
		return provider.getCodeFactory();
	}
	
	public void setVariable( String key, Object val )
	{
		binding.setVariable( key, val );
	}
	
	protected GroovyShell getUnusedShell()
	{
		for ( Entry<GroovyShell, Boolean> eShell : groovyShells.entrySet() )
		{
			if ( eShell.getValue() == false )
				return eShell.getKey();
		}
		
		GroovyShell shell = getNewShell();
		groovyShells.put( shell, false );
		return shell;
	}
	
	protected GroovyShell getNewShell()
	{
		CompilerConfiguration configuration = new CompilerConfiguration();
		
		configuration.setScriptBaseClass( ScriptingBaseGroovy.class.getName() );
		configuration.setSourceEncoding( encoding );
		
		return new GroovyShell( Loader.class.getClassLoader(), binding, configuration );
	}
	
	protected void lock( GroovyShell shell )
	{
		groovyShells.put( shell, true );
	}
	
	protected void unlock( GroovyShell shell )
	{
		groovyShells.put( shell, false );
	}
	
	protected CodeEvalFactory(Binding _binding)
	{
		binding = _binding;
		setOutputStream( bs );
	}
	
	public void setOutputStream( ByteArrayOutputStream _bs )
	{
		try
		{
			binding.setProperty( "out", new PrintStream( _bs, true, encoding ) );
		}
		catch ( UnsupportedEncodingException e )
		{
			e.printStackTrace();
		}
	}
	
	public void setEncoding( String _encoding )
	{
		encoding = _encoding;
		setOutputStream( bs );
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
	
	public String eval( File fi, Site site ) throws ShellExecuteException
	{
		CodeMetaData codeMeta = new CodeMetaData();
		
		codeMeta.shell = FileInterpreter.determineShellFromName( fi.getName() );
		codeMeta.fileName = fi.getAbsolutePath();
		
		return eval( fi, codeMeta, site );
	}
	
	public String eval( File fi, CodeMetaData meta, Site site ) throws ShellExecuteException
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
	
	public String eval( FileInterpreter fi, Site site ) throws ShellExecuteException
	{
		CodeMetaData codeMeta = new CodeMetaData();
		
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
	
	public String eval( String code, Site site ) throws ShellExecuteException
	{
		CodeMetaData codeMeta = new CodeMetaData();
		
		codeMeta.shell = "html";
		
		return eval( code, codeMeta, site );
	}
	
	public String eval( String code, CodeMetaData meta, Site site ) throws ShellExecuteException
	{
		boolean success = false;
		
		if ( code == null || code.isEmpty() )
			return "";
		
		if ( meta.contentType == null )
			meta.contentType = meta.shell;
		
		meta.source = code;
		
		if ( site != null )
			code = runParsers( code, site );
		
		File cacheFile = null;
		
		// XXX Crude cache system, improve upon this and make it better.
		if ( meta.fileName != null && !meta.fileName.isEmpty() )
		{
			cacheFile = new File( site.getCacheDirectory(), StringUtil.md5( meta.fileName ) + ".cache" );
			
			if ( cacheFile.exists() )
				try
				{
					return FileUtils.readFileToString( cacheFile );
				}
				catch ( IOException e )
				{
					throw new ShellExecuteException( e, meta );
				}
		}
		
		for ( PreProcessor p : preProcessors )
		{
			String[] handledTypes = p.getHandledTypes();
			
			for ( String t : handledTypes )
				if ( t.equalsIgnoreCase( meta.shell ) || meta.contentType.toLowerCase().contains( t.toLowerCase() ) || t.equalsIgnoreCase( "all" ) )
				{
					String result = p.process( meta, code );
					if ( result != null )
					{
						code = result;
						break;
					}
				}
		}
		
		GroovyShell gShell = getUnusedShell();
		Loader.getLogger().fine( "Locking GroovyShell '" + gShell.toString() + "' for execution of '" + meta.fileName + ":" + code.length() + "'" );
		lock( gShell );
		
		byte[] saved = bs.toByteArray();
		bs.reset();
		
		for ( Interpreter s : interpreters )
		{
			String[] handledTypes = s.getHandledTypes();
			
			for ( String she : handledTypes )
			{
				if ( she.equalsIgnoreCase( meta.shell ) || she.equalsIgnoreCase( "all" ) )
				{
					String result = s.eval( meta, code, gShell, bs );
					
					try
					{
						bs.write( result.getBytes( encoding ) );
					}
					catch ( IOException e )
					{
						e.printStackTrace();
					}
					
					if ( result != null )
					{
						success = true;
						break;
					}
				}
			}
		}
		
		Loader.getLogger().fine( "Unlocking GroovyShell '" + gShell.toString() + "' for execution of '" + meta.fileName + ":" + code.length() + "'" );
		unlock( gShell );
		
		if ( success )
			try
			{
				code = new String( bs.toByteArray(), encoding );
				meta.source = code;
			}
			catch ( UnsupportedEncodingException e )
			{
				throw new ShellExecuteException( e, meta );
			}
		
		for ( PostProcessor p : postProcessors )
		{
			String[] handledTypes = p.getHandledTypes();
			
			// TODO Cache these results and only update on occasion
			
			for ( String t : handledTypes )
				if ( t.equalsIgnoreCase( meta.shell ) || meta.contentType.toLowerCase().contains( t.toLowerCase() ) || t.equalsIgnoreCase( "all" ) )
				{
					String result = p.process( meta, code );
					if ( result != null )
					{
						code = result;
						break;
					}
				}
		}
		
		bs.reset();
		
		try
		{
			bs.write( saved );
		}
		catch ( IOException e )
		{
			throw new ShellExecuteException( e, meta );
		}
		
		if ( cacheFile != null )
			try
			{
				List<String> cachePatterns = site.getCachePatterns();
				
				for ( String cache : cachePatterns )
				{
					if ( new File( meta.fileName ).getName().toLowerCase().contains( cache ) )
					{
						Loader.getLogger().info( "Wrote a cache file for requested file: " + meta.fileName );
						FileUtils.writeStringToFile( cacheFile, code );
						break;
					}
				}
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		
		return code;
	}
	
	private String runParsers( String source, Site site ) throws ShellExecuteException
	{
		source = new IncludesParser().runParser( source, site, this );
		source = new LinksParser().runParser( source, site );
		
		return source;
	}
}
