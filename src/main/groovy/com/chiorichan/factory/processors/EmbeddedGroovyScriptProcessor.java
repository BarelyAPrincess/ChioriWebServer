/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.processors;

import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.chiorichan.factory.EvalContext;
import com.chiorichan.factory.groovy.GroovyRegistry;
import com.chiorichan.lang.ErrorReporting;
import com.chiorichan.lang.EvalException;

/**
 * SeaShell for handling GSP files.
 * ie. Embedded Groovy Files
 * 
 * sample:
 * <p>
 * This is plain html<% print ", with a twist of groovy. Today's date: " + date("") %>.
 * </p>
 */
public class EmbeddedGroovyScriptProcessor implements ScriptingProcessor
{
	private static final String MARKER_END = "%>";
	private static final String MARKER_START = "<%";
	
	public String escapeFragment( String fragment )
	{
		String brackets = "\"\"\"";
		
		fragment = fragment.replace( "\\$", "$" );
		fragment = fragment.replace( "$", "\\$" );
		
		if ( fragment.endsWith( "\"" ) )
			brackets = "'''";
		
		return "print " + brackets + fragment + brackets + "; ";
	}
	
	@Override
	public boolean eval( EvalContext context ) throws Exception
	{
		String source = context.readString();
		
		int fullFileIndex = 0;
		String[] dontStartWith = new String[] {"println", "print", "echo", "def", "import", "if", "for", "do", "}", "else", "//", "/*", "\n", "\r"};
		
		StringBuilder output = new StringBuilder();
		
		while ( fullFileIndex < source.length() )
		{
			int startIndex = source.indexOf( MARKER_START, fullFileIndex );
			if ( -1 != startIndex )
			{
				// Append all the simple text until the marker
				
				String fragment = escapeFragment( source.substring( fullFileIndex, startIndex ) );
				if ( !fragment.isEmpty() )
					output.append( fragment );
				
				int endIndex = source.indexOf( MARKER_END, Math.max( startIndex, fullFileIndex ) );
				
				if ( endIndex == -1 )
					throw new EvalException( ErrorReporting.E_PARSE, new IOException( "Marker `<%` was not closed after line " + ( StringUtils.countMatches( output.toString(), "\n" ) + 1 ) + ", please check your source file and try again." ) );
				
				fragment = source.substring( startIndex + MARKER_START.length(), endIndex );
				
				boolean appendPrint = true;
				
				for ( String s : dontStartWith )
					if ( fragment.trim().startsWith( s ) || fragment.startsWith( s ) || fragment.trim().isEmpty() )
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
				String fragment = escapeFragment( source.substring( fullFileIndex ) );
				
				if ( !fragment.isEmpty() )
					output.append( fragment );
				
				// Position index after the end of the file
				fullFileIndex = source.length() + 1;
			}
		}
		
		context.baseSource( output.toString() );
		try
		{
			GroovyShell shell = GroovyRegistry.getNewShell( context );
			Script script = GroovyRegistry.makeScript( shell, output.toString(), context );
			
			context.result().object( script.run() );
		}
		catch ( Throwable t )
		{
			// Clear the input source code and replace it with the exception stack trace
			// context.resetAndWrite( ExceptionUtils.getStackTrace( t ) );
			context.reset();
			throw t;
		}
		return true;
	}
	
	@Override
	public String[] getHandledTypes()
	{
		return new String[] {"embedded", "gsp", "jsp", "chi"};
	}
}
