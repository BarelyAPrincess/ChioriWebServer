/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * <p>
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.groovy;

import com.chiorichan.AppConfig;
import com.chiorichan.factory.ScriptBinding;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.factory.ScriptingEngine;
import com.chiorichan.lang.EnumColor;
import com.chiorichan.lang.ReportingLevel;
import com.chiorichan.lang.ScriptingException;
import com.chiorichan.logger.Log;
import com.chiorichan.util.Triplet;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ScriptingEngine for handling GSP files, i.e., Embedded Groovy File a.k.a. Groovy Server Pages.
 * <p>
 * <code>This is plain html<%= print ", with a twist of groovy. Today's date is: " + date("") %>.</code>
 */
public class EmbeddedGroovyEngine implements ScriptingEngine
{
	// private static final String MARKER_START = "<%";
	// private static final String MARKER_END = "%>";

	private static final String[] DO_NOT_PREPEND = new String[] {"println", "print", "echo", "def", "import", "if", "for", "do", "while", "{", "}", "else", "//", "/*", "\n", "\r"};
	private static final List<Triplet<String, String, String>> MARKERS = new ArrayList<>();

	static
	{
		MARKERS.add( new Triplet<>( "<%", null, "%>" ) );
		MARKERS.add( new Triplet<>( "<%=", "echo", "%>" ) );

		MARKERS.add( new Triplet<>( "{{", "echo", "}}" ) );
		MARKERS.add( new Triplet<>( "{!!", "print", "!!}" ) );

		if ( AppConfig.get().getBoolean( "advanced.scripting.gspAllowPhpTags" ) )
		{
			MARKERS.add( new Triplet<>( "<?", null, "?>" ) );
			MARKERS.add( new Triplet<>( "<?=", "echo", "?>" ) );
		}
	}

	private Binding binding = new Binding();
	private GroovyRegistry registry;

	public EmbeddedGroovyEngine( GroovyRegistry registry )
	{
		this.registry = registry;
	}

	public String escapeFragment( String fragment )
	{
		if ( fragment == null || fragment.length() == 0 )
			return "";

		String brackets = "\"\"\"";

		fragment = fragment.replace( "\\$", "$" );
		fragment = fragment.replace( "$", "\\$" );

		if ( fragment.endsWith( "\"" ) )
			brackets = "'''";

		return "print " + brackets + fragment + brackets + "; ";
	}

	@Override
	public boolean eval( ScriptingContext context ) throws Exception
	{
		String source = context.readString();

		int fullFileIndex = 0;

		StringBuilder output = new StringBuilder();

		while ( fullFileIndex < source.length() )
		{
			Triplet<String, String, String> activeMarker = null;
			int startIndex = -1;

			// Check which marker comes closest to the current index.
			for ( Triplet<String, String, String> marker : MARKERS )
			{
				int nextIndex = source.indexOf( marker.getStart(), fullFileIndex );

				if ( nextIndex > -1 && ( startIndex == -1 || nextIndex < startIndex || nextIndex == startIndex && marker.getStart().length() > activeMarker.getStart().length() ) )
				{
					startIndex = nextIndex;
					activeMarker = marker;
				}
			}

			if ( activeMarker != null && activeMarker.getStart().equals( "<%" ) )
				try
				{
					Log.get().warning( EnumColor.RED + "" + EnumColor.NEGATIVE + "DEPRECATED: Start Marker '<%' present on line " + ( StringUtils.countMatches( source.substring( 0, startIndex ), "\n" ) + 1 ) + ":" + ( startIndex - source.substring( 0, startIndex ).lastIndexOf( "\n" ) - 1 ) + " in file '" + context.filename() + "'" );
				}
				catch ( Exception e )
				{
					// Ignore
				}

			if ( startIndex > -1 )
			{
				// Append all the text until the marker
				String fragment = escapeFragment( source.substring( fullFileIndex, startIndex ) );

				if ( fragment.length() > 0 )
					output.append( fragment );

				int endIndex = source.indexOf( activeMarker.getEnd(), Math.max( startIndex, fullFileIndex ) );
				if ( endIndex == -1 )
					throw new ScriptingException( ReportingLevel.E_PARSE, String.format( "Found starting marker '%s' at line %s, expected close marker '%s' not found.", activeMarker.getStart(), StringUtils.countMatches( output.toString(), "\n" ) + 1, activeMarker.getEnd() ) );

				// Gets the entire fragment?
				fragment = source.substring( startIndex + activeMarker.getStart().length(), endIndex ).trim();

				boolean prependMiddle = activeMarker.getMiddle() != null && activeMarker.getMiddle().length() > 0;

				for ( String s : DO_NOT_PREPEND )
					if ( fragment.startsWith( s ) )
						prependMiddle = false;

				if ( prependMiddle )
				{
					StringBuilder builder = new StringBuilder();

					builder.append( activeMarker.getMiddle() );
					builder.append( "( " );
					builder.append( fragment.contains( ";" ) ? fragment.substring( 0, fragment.indexOf( ";" ) ) : fragment );
					builder.append( " ); " );

					if ( fragment.contains( ";" ) && fragment.length() - fragment.indexOf( ";" ) > 0 )
						builder.append( fragment.substring( fragment.indexOf( ";" ) + 1 ) );

					fragment = builder.toString().trim();
				}

				if ( fragment.length() > 0 )
					output.append( fragment + ( fragment.endsWith( ";" ) ? "" : ";" ) );

				// Position index after end marker
				fullFileIndex = endIndex + activeMarker.getEnd().length();
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
			Script script = GroovyRegistry.getCachedScript( context, binding );

			if ( script == null )
			{
				GroovyShell shell = registry.getNewShell( context, binding );
				script = registry.makeScript( shell, output.toString(), context );
			}

			context.result().setScript( script );
			context.result().setObject( script.run() );
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
	public List<String> getTypes()
	{
		return Arrays.asList( "embedded", "gsp", "jsp", "chi" );
	}

	@Override
	public void setBinding( ScriptBinding binding )
	{
		// Groovy Binding will keep the original EvalBinding map updated automatically. YAY!
		this.binding = new Binding( binding.getVariables() );
	}

	@Override
	public void setOutput( ByteBuf buffer, Charset charset )
	{
		try
		{
			binding.setProperty( "out", new PrintStream( new ByteBufOutputStream( buffer ), true, charset.name() ) );
		}
		catch ( UnsupportedEncodingException e )
		{
			e.printStackTrace();
		}
	}
}
