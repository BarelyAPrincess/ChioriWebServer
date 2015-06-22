/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.event;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;

import com.chiorichan.event.EventHandler;
import com.chiorichan.event.Listener;

public class PreCoffeeProcessor implements Listener
{
	@EventHandler( )
	public void onEvent( PreEvalEvent event )
	{
		if ( !event.context().contentType().endsWith( "coffee" ) && !event.context().contentType().endsWith( "litcoffee" ) && !event.context().contentType().endsWith( "coffee.md" ) )
			return;
		
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream( "com/chiorichan/coffee-script.js" );
		
		try
		{
			Reader reader = new InputStreamReader( inputStream, "UTF-8" );
			
			Context context = Context.enter();
			context.setOptimizationLevel( -1 ); // Without this, Rhino hits a 64K bytecode limit and fails
			
			try
			{
				Scriptable globalScope = context.initStandardObjects();
				context.evaluateReader( globalScope, reader, "coffee-script.js", 0, null );
				
				Scriptable compileScope = context.newObject( globalScope );
				compileScope.setParentScope( globalScope );
				compileScope.put( "coffeeScriptSource", compileScope, event.context().readString() );
				
				event.context().resetAndWrite( ( ( String ) context.evaluateString( compileScope, String.format( "CoffeeScript.compile(coffeeScriptSource, %s);", String.format( "{bare: %s, filename: '%s'}", true, event.context().filename() ) ), "CoffeeScriptCompiler-" + event.context().filename(), 0, null ) ).getBytes() );
			}
			finally
			{
				reader.close();
				Context.exit();
			}
		}
		catch ( JavaScriptException e )
		{
			return;
		}
		catch ( IOException e )
		{
			return;
		}
	}
}
