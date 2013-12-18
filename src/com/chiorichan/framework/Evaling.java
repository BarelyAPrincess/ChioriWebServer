package com.chiorichan.framework;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;

public class Evaling
{
	ByteArrayOutputStream bs = new ByteArrayOutputStream();
	GroovyShell shell;
	
	public Evaling(Binding binding)
	{
		CompilerConfiguration configuration = new CompilerConfiguration();
		configuration.setScriptBaseClass( scriptingBaseGroovy.class.getName() );
		
		shell = new GroovyShell( Enviro.class.getClassLoader(), binding, configuration );
		
		shell.setProperty( "out", new PrintStream( bs ) );
	}
	
	public void setFileName( String fileName )
	{
		shell.setVariable( "__FILE__", new File( fileName ) );
	}
	
	public String flush() throws UnsupportedEncodingException
	{
		return new String( bs.toByteArray(), "UTF-8" );
	}
	
	public String reset() throws UnsupportedEncodingException
	{
		String bsOut = flush();
		bs.reset();
		
		// System.out.println( "Output Flush: " + bsOut.length() );
		
		return bsOut;
	}
	
	public void evalFile( String absolutePath ) throws IOException, CodeParsingException
	{
		if ( absolutePath == null || absolutePath.isEmpty() )
			return;
		
		evalFile( new File( absolutePath ) );
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
	
	public void evalCode( String code ) throws CompilationFailedException
	{
		if ( !code.isEmpty() )
			shell.evaluate( code );
	}
	
	public void evalCode( String code, boolean throwException ) throws CodeParsingException, UnsupportedEncodingException, IOException
	{
		try
		{
			if ( !code.isEmpty() )
				shell.evaluate( code );
		}
		catch ( CompilationFailedException e )
		{
			if ( throwException )
				throw new CodeParsingException( e, code );
			else
				bs.write( ( e.getMessage() ).getBytes( "UTF-8" ) );
		}
	}
}
