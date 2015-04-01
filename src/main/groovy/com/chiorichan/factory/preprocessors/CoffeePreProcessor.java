/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory.preprocessors;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;

import com.chiorichan.factory.EvalMetaData;

public class CoffeePreProcessor implements PreProcessor
{
	@Override
	public String[] getHandledTypes()
	{
		return new String[] {"coffee", "litcoffee", "coffee.md"};
		// XXX Is coffee.md a markdown type?
	}
	
	@Override
	public String process( EvalMetaData meta, String code )
	{
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
				compileScope.put( "coffeeScriptSource", compileScope, code );
				
				return ( String ) context.evaluateString( compileScope, String.format( "CoffeeScript.compile(coffeeScriptSource, %s);", String.format( "{bare: %s, filename: '%s'}", true, meta.fileName ) ), "CoffeeScriptCompiler-" + meta.fileName, 0, null );
			}
			finally
			{
				reader.close();
				Context.exit();
			}
		}
		catch ( JavaScriptException e )
		{
			return null;
		}
		catch ( IOException e )
		{
			return null;
		}
	}
}
