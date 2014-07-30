package com.chiorichan.framework;

import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;

import com.chiorichan.Loader;
import com.chiorichan.exceptions.ShellExecuteException;
import com.google.common.collect.Lists;

public class Evaling
{
	protected static List<SeaShell> shells = Lists.newCopyOnWriteArrayList();
	
	ByteArrayOutputStream bs = new ByteArrayOutputStream();
	GroovyShell shell;
	
	static
	{
		registerShell( new EmbeddedShell() );
	}
	
	public Evaling(Binding binding)
	{
		CompilerConfiguration configuration = new CompilerConfiguration();
		
		configuration.setScriptBaseClass( ScriptingBaseGroovy.class.getName() );
		
		shell = new GroovyShell( Loader.class.getClassLoader(), binding, configuration );
		
		shell.setProperty( "out", new PrintStream( bs ) );
	}
	
	public void setVariable( String key, Object val )
	{
		shell.setVariable( key, val );
	}
	
	public void setFileName( String fileName )
	{
		shell.setVariable( "__FILE__", new File( fileName ) );
	}
	
	public String parseForIncludes( String source, Site site ) throws IOException, CodeParsingException
	{
		return parseForIncludes( this, source, site );
	}
	
	public static String parseForIncludes( Evaling eval, String source, Site site ) throws IOException, CodeParsingException
	{
		Pattern p1 = Pattern.compile( "<!-- *include\\((.*)\\) *-->" );
		Pattern p2 = Pattern.compile( "(<!-- *include\\(.*\\) *-->)" );
		
		Matcher m1 = p1.matcher( source );
		Matcher m2 = p2.matcher( source );
		
		while ( m1.find() && m2.find() )
		{
			String result = WebUtils.evalPackage( eval, m1.group( 1 ), site );
			source = new StringBuilder( source ).replace( m2.start( 1 ), m2.end( 1 ), result ).toString();
			
			// We have to reset the matcher since the source changes with each loop
			m1 = p1.matcher( source );
			m2 = p2.matcher( source );
		}
		
		return source;
	}
	
	public String flush() throws UnsupportedEncodingException
	{
		return new String( bs.toByteArray(), "ISO-8859-1" );
	}
	
	public String reset() throws UnsupportedEncodingException
	{
		String bsOut = flush();
		bs.reset();
		
		return bsOut;
	}
	
	public byte[] flushToBytes()
	{
		return bs.toByteArray();
	}
	
	public void evalFile( String absolutePath ) throws IOException, CodeParsingException
	{
		if ( absolutePath == null || absolutePath.isEmpty() )
			return;
		
		evalFile( new File( absolutePath ) );
	}
	
	public void write( byte[] bytesToWrite ) throws IOException
	{
		bs.write( bytesToWrite );
	}
	
	public void evalFile( File file ) throws IOException, CodeParsingException
	{
		try
		{
			shell.setVariable( "__FILE__", file );
			shell.evaluate( file );
			
			// System.out.println( "BS Size: " + bs.size() );
		}
		catch ( CompilationFailedException e )
		{
			FileInputStream is = new FileInputStream( file );
			
			BufferedReader br = new BufferedReader( new InputStreamReader( is, "ISO-8859-1" ) );
			StringBuilder sb = new StringBuilder();
			
			String l;
			while ( ( l = br.readLine() ) != null )
			{
				sb.append( l + "\n" );
			}
			
			is.close();
			
			throw new CodeParsingException( e, sb.toString() );
		}
	}
	
	public void evalFileVirtual( byte[] code, String fileName ) throws CodeParsingException
	{
		evalFileVirtual( new String( code ), fileName );
	}
	
	public void evalFileVirtual( String code, String fileName ) throws CodeParsingException
	{
		try
		{
			shell.setVariable( "__FILE__", new File( fileName ) );
			shell.evaluate( code, fileName );
		}
		catch ( CompilationFailedException e )
		{
			throw new CodeParsingException( e, code );
		}
	}
	
	public void evalCode( String code )
	{
		try
		{
			evalCodeWithException( code );
		}
		catch ( CodeParsingException e )
		{
			try
			{
				bs.write( ( e.getMessage() ).getBytes( "UTF-8" ) );
			}
			catch ( IOException e1 )
			{
				e1.printStackTrace();
			}
		}
	}
	
	public void evalCodeWithException( String code ) throws CodeParsingException
	{
		try
		{
			if ( !code.isEmpty() )
				shell.evaluate( code );
		}
		catch ( CompilationFailedException e )
		{
			throw new CodeParsingException( e, code );
		}
	}
	
	public static void registerShell( SeaShell shell )
	{
		shells.add( shell );
	}
	
	public boolean shellExecute( String shellIdent, File file ) throws ShellExecuteException
	{
		if ( file == null )
			return false;
		
		if ( shellIdent == null )
			shellIdent = "html";
		
		if ( shellIdent.equalsIgnoreCase( "groovy" ) )
			try
			{
				evalFile( file.getAbsolutePath() );
				return true;
			}
			catch ( CodeParsingException | GroovyRuntimeException e )
			{
				throw new ShellExecuteException( e );
			}
			finally
			{
				return false;
			}
		
		for ( SeaShell s : shells )
		{
			if ( s.doYouHandle( shellIdent ) )
			{
				s.evalFile( file, this );
				return true;
			}
		}
		
		return false;
	}
	
	public boolean shellExecute( String shellIdent, String html ) throws ShellExecuteException
	{
		if ( html == null )
			return false;
		
		if ( shellIdent == null )
			shellIdent = "html";
		
		if ( shellIdent.equalsIgnoreCase( "groovy" ) )
			try
			{
				evalCodeWithException( html );
				return true;
			}
			catch ( CodeParsingException | GroovyRuntimeException e )
			{
				throw new ShellExecuteException( e );
			}
		
		for ( SeaShell s : shells )
		{
			if ( s.doYouHandle( shellIdent ) )
			{
				s.evalCode( html, this );
				return true;
			}
		}
		
		return false;
	}
	
	public boolean shellExecute( String shellIdent, FileInterpreter fi ) throws ShellExecuteException
	{
		if ( fi == null )
			return false;
		
		if ( shellIdent == null )
			shellIdent = "html";
		
		if ( shellIdent.equalsIgnoreCase( "groovy" ) )
			try
			{
				evalFileVirtual( fi.getContent(), fi.getFile().getAbsolutePath() );
				return true;
			}
			catch ( CodeParsingException | GroovyRuntimeException e )
			{
				throw new ShellExecuteException( e );
			}
		
		for ( SeaShell s : shells )
		{
			if ( s.doYouHandle( shellIdent ) )
			{
				s.eval( fi, this );
				return true;
			}
		}
		
		return false;
	}
	
	public Binding getBinding()
	{
		return shell.getContext();
	}

	public GroovyShell getShell()
	{
		return shell;
	}
}
