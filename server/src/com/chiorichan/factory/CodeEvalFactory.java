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

public class CodeEvalFactory
{
	protected String encoding = Loader.getConfig().getString( "server.defaultEncoding", "UTF-8" );
	protected StringBuilder awaitingCode = new StringBuilder();
	protected CodeMetaData codeMeta = null;
	protected ByteArrayOutputStream bs = new ByteArrayOutputStream();
	
	protected static List<SeaShell> shells = Lists.newCopyOnWriteArrayList();
	
	protected GroovyShell shell;
	
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
		shell.setVariable( key, val );
	}
	
	public void setFileName( String fileName )
	{
		shell.setVariable( "__FILE__", new File( fileName ) );
	}
	
	public void setOutputStream( ByteArrayOutputStream _bs )
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
	
	public void resetOutputStream()
	{
		setOutputStream( bs );
	}
	
	protected CodeEvalFactory(Binding binding)
	{
		CompilerConfiguration configuration = new CompilerConfiguration();
		
		configuration.setScriptBaseClass( ScriptingBaseGroovy.class.getName() );
		configuration.setSourceEncoding( encoding );
		
		shell = new GroovyShell( Loader.class.getClassLoader(), binding, configuration );
		
		resetOutputStream();
	}
	
	public void setEncoding( String _encoding )
	{
		encoding = _encoding;
	}
	
	public static void registerShell( SeaShell shell )
	{
		shells.add( shell );
	}
	
	public boolean eval() throws ShellExecuteException
	{
		return eval( false );
	}
	
	public boolean eval( boolean dumpSourceOnFailure ) throws ShellExecuteException
	{
		String shell = ( codeMeta == null ) ? "html" : codeMeta.shell;
		shell = ( shell == null || shell.isEmpty() ) ? "html" : shell;
		
		if ( awaitingCode.toString().isEmpty() )
			return true;
		
		for ( SeaShell s : shells )
		{
			String[] handledShells = s.getHandledShells();
			
			for ( String she : handledShells )
			{
				if ( she.equalsIgnoreCase( shell ) || she.equalsIgnoreCase( "all" ) )
				{
					String result = s.eval( codeMeta, awaitingCode.toString(), this );
					
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
						awaitingCode = new StringBuilder();
						codeMeta = new CodeMetaData();
						return true;
					}
				}
			}
		}
		
		if ( dumpSourceOnFailure )
			try
			{
				bs.write( awaitingCode.toString().getBytes( encoding ) );
			}
			catch ( IOException e )
			{
				throw new ShellExecuteException( e );
			}
		
		return false;
	}
	
	public String getSource()
	{
		return awaitingCode.toString();
	}
	
	public void resetSource()
	{
		awaitingCode = new StringBuilder();
		codeMeta = new CodeMetaData();
	}
	
	public GroovyShell getShell()
	{
		return shell;
	}
	
	public String reset()
	{
		String result = "";
		
		try
		{
			result = new String( bs.toByteArray(), encoding );
		}
		catch ( UnsupportedEncodingException e )
		{	
			
		}
		
		bs = new ByteArrayOutputStream();
		resetOutputStream();
		
		return result;
	}
	
	public ByteArrayOutputStream getOutputStream()
	{
		return bs;
	}
	
	public void put( String code, String shell )
	{
		put( code );
		
		if ( codeMeta == null )
			codeMeta = new CodeMetaData();
		
		codeMeta.shell = shell;
	}
	
	public void put( String code )
	{
		awaitingCode.append( code );
	}
	
	public byte[] resetToBytes()
	{
		try
		{
			return reset().getBytes( encoding );
		}
		catch ( UnsupportedEncodingException e )
		{
			return new byte[0];
		}
	}
	
	public void put( File fi, String shell ) throws IOException
	{
		put( fi );
		
		codeMeta.shell = shell;
	}
	
	public void put( File fi ) throws IOException
	{
		put( FileUtils.readFileToString( fi, encoding ) );
		
		if ( codeMeta == null )
			codeMeta = new CodeMetaData();
		
		codeMeta.shell = FileInterpreter.determineShellFromName( fi.getName() );
		codeMeta.fileName = fi.getAbsolutePath();
	}
	
	public void put( FileInterpreter fi )
	{
		try
		{
			put( new String( fi.getContent(), fi.getEncoding() ) );
		}
		catch ( UnsupportedEncodingException e )
		{
			e.printStackTrace();
			return;
		}
		
		if ( codeMeta == null )
			codeMeta = new CodeMetaData();
		
		codeMeta.shell = fi.getParams().get( "shell" );
		codeMeta.fileName = fi.getFile().getAbsolutePath();
	}
	
	public void applyAliases( Map<String, String> aliases )
	{
		if ( aliases == null || aliases.size() < 1 )
			return;
		
		if ( awaitingCode.toString().isEmpty() )
			return;
		
		String source = awaitingCode.toString();
		
		for ( Entry<String, String> entry : aliases.entrySet() )
		{
			source = source.replace( "%" + entry.getKey() + "%", entry.getValue() );
		}
		
		awaitingCode = new StringBuilder( source );
	}
	
	public void parseForIncludes( Site site ) throws IOException, ShellExecuteException
	{
		if ( awaitingCode.toString().isEmpty() )
			return;
		
		String source = awaitingCode.toString();
		
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
				CodeEvalFactory iFactory = create( shell.getContext() );
				iFactory.put( res );
				iFactory.applyAliases( site.getAliases() );
				iFactory.parseForIncludes( site ); // XXX Prevent this from going into an infinite loop!
				iFactory.eval();
				result = iFactory.reset();
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
		
		awaitingCode = new StringBuilder( source );
	}
}
