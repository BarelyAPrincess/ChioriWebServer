package com.chiorichan.framework;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import bsh.EvalError;
import bsh.Interpreter;

public class Enviro
{
	Interpreter bsh = new Interpreter();
	ByteArrayOutputStream results = new ByteArrayOutputStream();
	ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
	Boolean outputBufferEnabled = false;
	
	public Enviro(Framework fw)
	{
		this();
		setFramework( fw );
	}
	
	public void setFramework( Framework fw )
	{
		try
		{
			bsh.set( "chiori", fw );
		}
		catch ( EvalError e )
		{
			e.printStackTrace();
		}
	}
	
	public Enviro()
	{
		bsh.getNameSpace().loadDefaultImports();
		bsh.getNameSpace().importCommands( "com.chiorichan.framework.bsh" );
		bsh.getNameSpace().importClass( "com.chiorichan.*" );
		bsh.getNameSpace().importClass( "java.text.SimpleDateFormat" );
		bsh.getNameSpace().importClass( "org.eclipse.jetty.util.security.Credential.MD5" );
		bsh.setOut( new PrintStream( results ) );
		
		try
		{
			bsh.set( "yes", Boolean.TRUE );
			bsh.set( "no", Boolean.FALSE );
			bsh.set( "__FILE__", "" );
		}
		catch ( EvalError e )
		{
			e.printStackTrace();
		}
	}
	
	public void startOutputBuffer()
	{
		bsh.setOut( new PrintStream( outputBuffer ) );
		outputBufferEnabled = true;
	}
	
	public void stopOutputBuffer()
	{
		bsh.setOut( new PrintStream( results ) );
		outputBufferEnabled = false;
	}
	
	public String flushOutputBuffer() throws UnsupportedEncodingException
	{
		stopOutputBuffer();
		String rtn = new String( outputBuffer.toByteArray(), "UTF-8" );
		outputBuffer.reset();
		return rtn;
	}
	
	public void evalFile( String absolutePath ) throws EvalError, CodeParsingException
	{
		if ( absolutePath == null || absolutePath.isEmpty() )
			return;
		
		evalFile( new File( absolutePath ) );
	}
	
	public void evalFile( File file ) throws EvalError, CodeParsingException
	{
		FileInputStream is;
		try
		{
			is = new FileInputStream( file );
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		
		try
		{
			BufferedReader br = new BufferedReader( new InputStreamReader( is, "ISO-8859-1" ) );
			
			String l;
			while ( ( l = br.readLine() ) != null )
			{
				sb.append( l + "\n" );
			}
			
			is.close();
			
			try
			{
				bsh.set( "__FILE__", file.getAbsoluteFile() );
				evalCode( sb.toString(), true );
				bsh.set( "__FILE__", "" );
			}
			catch ( EvalError e )
			{
				e.setMessage( e.getMessage() + " in " + file.getAbsolutePath() );
				throw e;
			}
			catch ( CodeParsingException e )
			{
				e.setMessage( e.getMessage() + " in " + file.getAbsolutePath() );
				throw e;
			}
		}
		catch ( IOException e )
		{
			// e.printStackTrace();
			return;
		}
	}
	
	public void evalFileVirtual( String code, String filename ) throws IOException, EvalError, CodeParsingException
	{
		try
		{
			evalCode( code, true );
		}
		catch ( EvalError e )
		{
			e.setMessage( e.getMessage() + " in " + filename );
			throw e;
		}
	}
	
	public void evalCode( String code ) throws EvalError, IOException, CodeParsingException
	{
		evalCode( code, false );
	}
	
	public void evalCode( String code, boolean throwException ) throws EvalError, IOException, CodeParsingException
	{
		boolean htmlMode = true;
		StringBuilder codeBlock = new StringBuilder();
		String[] rawBlock = code.split( "<\\?php|<\\?PHP|<\\?|\\?>" );
		
		int x = 1;
		for ( String c : rawBlock )
		{
			if ( htmlMode )
			{
				for ( String l : c.trim().split( "\n" ) )
				{
					// Prevent already escapes (") from being escaped again.
					l = StringUtils.replace( l, "\\\"", "" );
					l = StringUtils.replace( l, "\"", "\\\"" );
					//l = StringUtils.replace( l, "&ep;", "\\\"" );
					
					codeBlock.append( "x(\"" + l + "\");\n" );
					
					//System.out.println( x + " x(\"" + l.replace( "$", "\\$" ) + "\");" );
					x++;
				}
			}
			else
			{
				codeBlock.append( parseHacks( c ).trim() + "\n" );
				
				/*
				for ( String l : c.trim().replaceAll( "\"", "\\\\\"" ).split( "\\n" ) )
				{
					System.out.println( x + " " + parseHacks( l ).trim() );
					x++;
				}
				*/
			}
			
			htmlMode = !htmlMode;
		}
		
		try
		{
			bsh.eval( codeBlock.toString() );
		}
		catch ( EvalError e )
		{
			if ( throwException )
				throw new CodeParsingException( e, codeBlock.toString() );
			else
				( ( outputBufferEnabled ) ? outputBuffer : results ).write( ( e.getMessage() ).getBytes( "UTF-8" ) );
		}
	}
	
	public Map<String, Object> compileMap( String[] keys, Object[] vals )
	{
		Map<String, Object> array = new LinkedHashMap<String, Object>();
		
		int i = 0;
		for ( String k : keys )
		{
			array.put( k, vals[i] );
			i++;
		}
		
		return array;
	}
	
	public String parseHacks( String code )
	{
		if ( code.startsWith( "=" ) )
		{
			code = code.trim().replaceAll( "^=|;$", "" );
			code = "echo (" + code + ");";
		}
		
		// Start - new Array
		Pattern p = Pattern.compile( "array\\((.*?)\\)" );
		StringBuffer myStringBuffer = new StringBuffer();
		Matcher m = p.matcher( code );
		while ( m.find() )
		{
			String arr = m.group( 1 );
			
			arr = arr.trim().replaceAll( "^array\\(|\\)$", "" );
			
			StringBuilder line1 = new StringBuilder();
			StringBuilder line2 = new StringBuilder();
			
			int i = 0;
			for ( String s : arr.split( "," ) )
			{
				String[] v = s.trim().split( "=>" );
				
				if ( v.length == 1 )
				{
					line1.append( i + ", " );
					line2.append( v[0].trim() + ", " );
					i++;
				}
				else if ( v.length == 2 )
				{
					line1.append( v[0].trim() + ", " );
					line2.append( v[1].trim() + ", " );
				}
				// else IGNORE ERROR
			}
			
			String l1 = line1.toString().replace( "$", "\\$" );
			String l2 = line2.toString().replace( "$", "\\$" );
			
			m.appendReplacement( myStringBuffer, "getFramework().getEnv().compileMap( new String[] {" + l1.toString().substring( 0, l1.toString().length() - 2 ) + "}, new Object[] {" + l2.toString().substring( 0, l2.toString().length() - 2 ) + "} )" );
		}
		m.appendTail( myStringBuffer );
		
		if ( myStringBuffer.toString() != null && !myStringBuffer.toString().isEmpty() )
			code = myStringBuffer.toString();
		// End
		
	// Start - Array select
			Pattern p1 = Pattern.compile( "(\\$\\w*\\[.*?\\])" );
			StringBuffer myStringBuffer1 = new StringBuffer();
			Matcher m1 = p1.matcher( code );
			while ( m1.find() )
			{
				String arr1 = m1.group( 1 );
				
				arr1 = arr1.replaceAll("\\[", ".get(").replaceAll("\\]", ")").replace( "$", "\\$" );
				
				m1.appendReplacement( myStringBuffer1, arr1 );
			}
			m1.appendTail( myStringBuffer1 );
			
			if ( myStringBuffer1.toString() != null && !myStringBuffer1.toString().isEmpty() )
				code = myStringBuffer1.toString();
			// End
		
		return code;
	}
	
	public String flush() throws IOException
	{
		results.flush();
		byte[] arr = results.toByteArray();
		results.reset();
		return new String( arr, "UTF-8" );
	}
	
	public void set( String name, Object value ) throws EvalError
	{
		bsh.set( name, value );
	}
	
	public ByteArrayOutputStream getOutputBuffer()
	{
		return outputBuffer;
	}
	
	public ByteArrayOutputStream getOutputStream()
	{
		return results;
	}
}
