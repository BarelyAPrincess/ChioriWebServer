package com.chiorichan.framework;

import groovy.lang.GroovyShell;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.codehaus.groovy.syntax.SyntaxException;

import com.chiorichan.Loader;
import com.chiorichan.exceptions.ShellExecuteException;

public class EmbeddedShell implements SeaShell
{
	private static final String MARKER_START = "<%";
	private static final String MARKER_END = "%>";
	private int m_scriptCount = 0;
	
	@Override
	public boolean doYouHandle( String shellIdent )
	{
		return shellIdent.equalsIgnoreCase( "embedded" );
	}
	
	@Override
	public void eval( FileInterpreter fi, Evaling eval ) throws ShellExecuteException
	{
		try
		{
			eval.getShell().setVariable( "__FILE__", fi.getFile() );
			evaluate( new String( fi.getContent(), fi.getEncoding() ), eval.getShell(), eval.bs );
		}
		catch ( Throwable e )
		{
			throw new ShellExecuteException( e );
		}
	}
	
	@Override
	public void evalFile( File file, Evaling eval ) throws ShellExecuteException
	{
		try
		{
			eval.getShell().setVariable( "__FILE__", file );
			evaluate( readAll( file ), eval.getShell(), eval.bs );
		}
		catch ( Throwable e )
		{
			throw new ShellExecuteException( e );
		}
	}
	
	@Override
	public void evalCode( String source, Evaling eval ) throws ShellExecuteException
	{
		try
		{
			eval.getShell().setVariable( "__FILE__", null );
			evaluate( source, eval.getShell(), eval.bs );
		}
		catch ( Throwable e )
		{
			throw new ShellExecuteException( e );
		}
	}
	
	public String escapeFragment( String fragment )
	{
		fragment = fragment.replace( "$", "\\$" );
		Loader.getLogger().debug( "println \"\"\"" + fragment + "\"\"\";" );
		return "println \"\"\"" + fragment + "\"\"\";";
	}
	
	public void evaluate( String fullFile, GroovyShell shell, ByteArrayOutputStream bs ) throws IOException, ShellExecuteException
	{
		int fullFileIndex = 0;
		
		try
		{
			StringBuilder output = new StringBuilder();
			
			while ( fullFileIndex < fullFile.length() )
			{
				int startIndex = fullFile.indexOf( MARKER_START, fullFileIndex );
				if ( -1 != startIndex )
				{
					// Append all the simple text until the marker
					
					String fragment = escapeFragment( fullFile.substring( fullFileIndex, startIndex ) );
					if ( !fragment.isEmpty() )
						output.append( fragment );
					
					int endIndex = fullFile.indexOf( MARKER_END, fullFileIndex );
					assert -1 != endIndex : "MARKER NOT CLOSED";
					fragment = fullFile.substring( startIndex + MARKER_START.length(), endIndex ).trim();
					
					if ( !fragment.startsWith( "print" ) && !fragment.startsWith( "echo" ) && !fragment.startsWith( "def" ) )
						fragment = "print " + fragment;
					
					if ( !fragment.isEmpty() )
						output.append( fragment + ";" );
					
					// Position index after end marker
					fullFileIndex = endIndex + MARKER_END.length();
				}
				else
				{
					String fragment = escapeFragment( fullFile.substring( fullFileIndex ) );
					
					if ( !fragment.isEmpty() )
						output.append( fragment );
					
					// Position index after the end of the file
					fullFileIndex = fullFile.length() + 1;
				}
			}
			
			String embeddedCode = interpret( shell, output.toString() );
			
			if ( !embeddedCode.isEmpty() )
				bs.write( embeddedCode.getBytes( "UTF-8" ) );
		}
		catch ( Exception e1 )
		{
			fullFileIndex = fullFile.length() + 1;
			throw new ShellExecuteException( e1 );
		}
	}
	
	private String interpretFragment( GroovyShell shell, String source ) throws SyntaxException, ClassNotFoundException, IOException
	{
		Object result = shell.evaluate( source, "EmbeddedShellScript" + m_scriptCount );
		m_scriptCount++;
		if ( result != null )
			return result.toString();
		else
			return "";
	}
	
	private String interpret( GroovyShell shell, String source ) throws SyntaxException, ClassNotFoundException, IOException
	{
		return interpretFragment( shell, source );
	}
	
	private String readAll( File path ) throws IOException
	{
		StringBuffer sb = new StringBuffer();
		BufferedReader m_templateFile = new BufferedReader( new FileReader( path ) );
		
		try
		{
			String line = null;
			line = m_templateFile.readLine();
			while ( null != line )
			{
				sb.append( line ).append( "\n" );
				line = m_templateFile.readLine();
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		
		String result = sb.toString();
		m_templateFile.close();
		return result;
	}
}
