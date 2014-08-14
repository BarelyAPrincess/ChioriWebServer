package com.chiorichan.factory.shells;

import groovy.lang.GroovyShell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.codehaus.groovy.syntax.SyntaxException;

import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.factory.CodeMetaData;

/**
 * SeaShell for handling GSP files.
 * ie. Embedded Groovy Files
 * 
 * sample:
 * <p>
 * This is plain html<% print ", with a twist of groovy. Today's date: " + date("") %>.
 * </p>
 * 
 * @author Chiori Greene
 */
public class GSPSeaShell implements SeaShell
{
	private static final String MARKER_START = "<%";
	private static final String MARKER_END = "%>";
	
	@Override
	public String[] getHandledShells()
	{
		return new String[] { "embedded", "gsp", "jsp", "chi" };
	}
	
	@Override
	public String eval( CodeMetaData meta, String fullFile, GroovyShell shell, ByteArrayOutputStream bs ) throws ShellExecuteException
	{
		try
		{
			shell.setVariable( "__FILE__", meta.fileName );
			
			int fullFileIndex = 0;
			String[] dontStartWith = new String[] { "println", "print", "echo", "def", "import", "if", "for", "do", "}", "else" };
			
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
					
					int endIndex = fullFile.indexOf( MARKER_END, Math.max( startIndex, fullFileIndex ) );
					assert -1 != endIndex : "MARKER NOT CLOSED";
					fragment = fullFile.substring( startIndex + MARKER_START.length(), endIndex ).trim();
					
					boolean appendPrint = true;
					
					for ( String s : dontStartWith )
						if ( fragment.trim().startsWith( s ) )
							appendPrint = false;
					
					if ( appendPrint )
						fragment = "print " + fragment;
					
					if ( !fragment.isEmpty() )
						output.append( fragment + "; " );
					
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
			
			meta.source = output.toString();
			bs.write( interpret( shell, output.toString() ).getBytes( "UTF-8" ) );
			
			return "";
		}
		catch ( Throwable e )
		{
			if ( e instanceof ShellExecuteException )
				throw (ShellExecuteException) e;
			else
				throw new ShellExecuteException( e, meta );
		}
	}
	
	public String escapeFragment( String fragment )
	{
		String brackets = "\"\"\"";
		
		fragment = fragment.replace( "\\$", "$" );
		fragment = fragment.replace( "$", "\\$" );
		
		if( fragment.endsWith( "\"" ) )
			brackets = "'''";
		
		return "print " + brackets + fragment + brackets + "; ";
	}
	
	private String interpret( GroovyShell shell, String source ) throws SyntaxException, ClassNotFoundException, IOException
	{
		Object result = shell.evaluate( source );
		if ( result != null )
			return result.toString();
		else
			return "";
	}
}
