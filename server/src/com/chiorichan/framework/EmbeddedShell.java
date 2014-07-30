package com.chiorichan.framework;

import groovy.lang.GroovyShell;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.codehaus.groovy.syntax.SyntaxException;

import com.chiorichan.exceptions.ShellExecuteException;

public class EmbeddedShell implements SeaShell
{
	private static final String MARKER_START = "<%";
	private static final String MARKER_END = "%>";
	
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
			evaluate( new String( fi.getContent() ), eval.getShell(), eval.bs );
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
	
	public void evaluate( String fullFile, GroovyShell shell, ByteArrayOutputStream bs ) throws IOException
	{
		int fullFileIndex = 0;
		
		try
		{
			while ( fullFileIndex < fullFile.length() )
			{
				int startIndex = fullFile.indexOf( MARKER_START, fullFileIndex );
				if ( -1 != startIndex )
				{
					// Append all the simple text until the marker
					bs.write( fullFile.substring( fullFileIndex, startIndex ).getBytes() );
					int endIndex = fullFile.indexOf( MARKER_END, fullFileIndex );
					assert -1 != endIndex : "MARKER NOT CLOSED";
					
					String fragment = fullFile.substring( startIndex + MARKER_START.length(), endIndex );
					// Append all the substituted text
					String interpretedText = interpret( shell, fragment );
					bs.write( interpretedText.getBytes() );
					
					// Position index after end marker
					fullFileIndex = endIndex + MARKER_END.length();
				}
				else
				{
					bs.write( fullFile.substring( fullFileIndex ).getBytes() );
					
					// Position index after the end of the file
					fullFileIndex = fullFile.length() + 1;
				}
			}
		}
		catch ( SyntaxException e1 )
		{
			e1.printStackTrace();
			bs.write( fullFile.substring( fullFileIndex ).getBytes() );
			
			// Position index after the end of the file
			fullFileIndex = fullFile.length() + 1;
		}
		catch ( ClassNotFoundException e1 )
		{
			e1.printStackTrace();
			bs.write( fullFile.substring( fullFileIndex ).getBytes() );
			
			// Position index after the end of the file
			fullFileIndex = fullFile.length() + 1;
		}
		catch ( IOException e1 )
		{
			e1.printStackTrace();
			bs.write( fullFile.substring( fullFileIndex ).getBytes() );
			
			// Position index after the end of the file
			fullFileIndex = fullFile.length() + 1;
		}
	}
	
	private int m_scriptCount = 0;
	
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
