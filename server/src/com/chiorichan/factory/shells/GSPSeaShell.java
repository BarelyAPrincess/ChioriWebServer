package com.chiorichan.factory.shells;

import groovy.lang.GroovyShell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.codehaus.groovy.syntax.SyntaxException;

import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.factory.CodeEvalFactory;
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
	private int m_scriptCount = 0;
	
	@Override
	public String[] getHandledShells()
	{
		return new String[] { "embedded", "gsp", "jsp", "chi" };
	}
	
	@Override
	public String eval( CodeMetaData meta, String fullFile, CodeEvalFactory factory ) throws ShellExecuteException
	{
		try
		{
			//Loader.getLogger().debug( fullFile.substring( 0, 1000 ) );
			
			ByteArrayOutputStream bs = factory.getOutputStream();
			GroovyShell shell = factory.getShell();
			shell.setVariable( "__FILE__", meta.fileName );
			
			int fullFileIndex = 0;
			String[] dontStartWith = new String[] { "println", "print", "echo", "def", "import" };
			
			StringBuilder output = new StringBuilder();
			
			while ( fullFileIndex < fullFile.length() )
			{
				int startIndex = fullFile.indexOf( MARKER_START, fullFileIndex );
				if ( -1 != startIndex )
				{
					// Append all the simple text until the marker
					
					String fragment = escapeFragment( fullFile.substring( fullFileIndex, startIndex ) );
					if ( !fragment.isEmpty() )
						output.append( fragment + "\n" );
					
					int endIndex = fullFile.indexOf( MARKER_END, Math.max( startIndex, fullFileIndex ) );
					assert -1 != endIndex : "MARKER NOT CLOSED";
					fragment = fullFile.substring( startIndex + MARKER_START.length(), endIndex ).trim();
					
					boolean appendPrint = true;
					
					for ( String s : dontStartWith )
						if ( fragment.startsWith( s ) )
							appendPrint = false;
					
					if ( appendPrint )
						fragment = "print " + fragment;
					
					if ( !fragment.isEmpty() )
						output.append( fragment + ";\n" );
					
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
			
			//Loader.getLogger().debug( output.toString().substring( 0, 1000 ) );
			
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
		fragment = fragment.replace( "\\$", "$" );
		fragment = fragment.replace( "$", "\\$" );
		return "println \"\"\"" + fragment + "\"\"\";";
	}
	
	private String interpret( GroovyShell shell, String source ) throws SyntaxException, ClassNotFoundException, IOException
	{
		Object result = shell.evaluate( source, "EmbeddedShellScript" + m_scriptCount );
		m_scriptCount++;
		if ( result != null )
			return result.toString();
		else
			return "";
	}
}
