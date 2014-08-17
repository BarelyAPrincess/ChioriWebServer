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
import com.chiorichan.factory.parsers.IncludesParser;
import com.chiorichan.factory.parsers.LinksParser;
import com.chiorichan.factory.shells.GSPSeaShell;
import com.chiorichan.factory.shells.GroovySeaShell;
import com.chiorichan.factory.shells.HTMLSeaShell;
import com.chiorichan.factory.shells.SeaShell;
import com.chiorichan.framework.FileInterpreter;
import com.chiorichan.framework.ScriptingBaseGroovy;
import com.chiorichan.framework.Site;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class CodeEvalFactory
{
	protected String encoding = Loader.getConfig().getString( "server.defaultEncoding", "UTF-8" );
	
	protected static List<SeaShell> shells = Lists.newCopyOnWriteArrayList();
	protected Map<GroovyShell, Boolean> groovyShells = Maps.newConcurrentMap();
	protected ByteArrayOutputStream bs = new ByteArrayOutputStream();
	protected Binding binding;
	
	static
	{
		// TODO Allow to override and/or extending of shells.
		registerShell( new GSPSeaShell() );
		registerShell( new HTMLSeaShell() );
		registerShell( new GroovySeaShell() );
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
	
	public static void registerShell( SeaShell shell )
	{
		shells.add( shell );
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
		
		codeMeta.shell = fi.getParams().get( "shell" );
		codeMeta.fileName = (fi.getFile() != null) ? fi.getFile().getAbsolutePath() : fi.getParams().get( "file" );
		
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
		
		GroovyShell gShell = getUnusedShell();
		Loader.getLogger().fine( "Locking GroovyShell '" + gShell.toString() + "' for execution of '" + meta.fileName + ":" + code.length() + "'" );
		lock( gShell );
		
		if ( site != null )
		{
			code = runParsers( code, site );
		}
		
		byte[] saved = bs.toByteArray();
		bs.reset();
		
		for ( SeaShell s : shells )
		{
			String[] handledShells = s.getHandledShells();
			
			for ( String she : handledShells )
			{
				if ( she.equalsIgnoreCase( meta.shell ) || she.equalsIgnoreCase( "all" ) )
				{
					meta.source = code;
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
		
		bs.reset();
		
		try
		{
			bs.write( saved );
		}
		catch ( IOException e )
		{
			throw new ShellExecuteException( e, meta );
		}
		
		return code;
	}
	
	private String runParsers( String source, Site site ) throws ShellExecuteException
	{
		source = new IncludesParser().runParser( source, site, this );
		source = new LinksParser().runParser( source );
		
		return source;
	}
}
