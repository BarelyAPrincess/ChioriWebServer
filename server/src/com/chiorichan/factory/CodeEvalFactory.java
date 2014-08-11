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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.control.CompilerConfiguration;

import com.chiorichan.Loader;
import com.chiorichan.exceptions.ShellExecuteException;
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
		for ( GroovyShell shell : groovyShells.keySet() )
			shell.setVariable( key, val );
	}
	
	public void setFileName( String fileName )
	{
		for ( GroovyShell shell : groovyShells.keySet() )
			shell.setVariable( "__FILE__", new File( fileName ) );
	}
	
	private void setOutputStream( GroovyShell shell, ByteArrayOutputStream _bs )
	{
		try
		{
			shell.setProperty( "out", new PrintStream( _bs, true, encoding ) );
		}
		catch ( UnsupportedEncodingException e )
		{
			e.printStackTrace();
		}
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
	}
	
	public void setEncoding( String _encoding )
	{
		encoding = _encoding;
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
		codeMeta.fileName = fi.getFile().getAbsolutePath();
		
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
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		boolean success = false;
		
		if ( code == null || code.isEmpty() )
			return "";
		
		GroovyShell gShell = getUnusedShell();
		lock( gShell );
		setOutputStream( gShell, bs );
		
		if ( site != null )
		{
			code = applyAliases( code, site.getAliases() );
			try
			{
				code = parseForIncludes( code, site );
			}
			catch ( IOException e)
			{
				throw new ShellExecuteException( e, meta );
			}
		}
		
		for ( SeaShell s : shells )
		{
			String[] handledShells = s.getHandledShells();
			
			for ( String she : handledShells )
			{
				if ( she.equalsIgnoreCase( meta.shell ) || she.equalsIgnoreCase( "all" ) )
				{
					// TODO Add HTML to CodeMeta
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
		
		unlock( gShell );
		
		if ( success )
			try
			{
				return new String( bs.toByteArray(), encoding );
			}
			catch ( UnsupportedEncodingException e )
			{
				throw new ShellExecuteException( e, meta );
			}
		
		return code;
	}
	
	private String applyAliases( String source, Map<String, String> aliases )
	{
		if ( source.isEmpty() )
			return "";
		
		if ( aliases == null || aliases.size() < 1 )
			return source;
		
		for ( Entry<String, String> entry : aliases.entrySet() )
		{
			source = source.replace( "%" + entry.getKey() + "%", entry.getValue() );
		}
		
		return source;
	}
	
	private String parseForIncludes( String source, Site site ) throws IOException, ShellExecuteException
	{
		if ( source.isEmpty() )
			return source;
		
		Pattern p1 = Pattern.compile( "<!-- *include\\((.*)\\) *-->" );
		Pattern p2 = Pattern.compile( "(<!-- *include\\(.*\\) *-->)" );
		
		Matcher m1 = p1.matcher( source );
		Matcher m2 = p2.matcher( source );
		
		while ( m1.find() && m2.find() )
		{
			File res = site.getResource( m1.group( 1 ) );
			
			if ( res == null )
				res = Loader.getSiteManager().getFrameworkSite().getResource( m1.group( 1 ) );
			
			String result = "";
			
			if ( res != null && res.exists() )
			{
				// TODO Prevent this from going into an infinite loop!
				result = eval( res, site );
			}
			else if ( !res.exists() )
			{
				Loader.getLogger().warning( "We had a problem finding the include file `" + res.getAbsolutePath() + "`" );
			}
			
			if ( result == null )
				result = "";
			
			source = new StringBuilder( source ).replace( m2.start( 1 ), m2.end( 1 ), result ).toString();
			
			// We have to reset the matcher since the source changes with each loop
			m1 = p1.matcher( source );
			m2 = p2.matcher( source );
		}
		
		return source;
	}
}
