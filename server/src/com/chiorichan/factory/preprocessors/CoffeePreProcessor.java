package com.chiorichan.factory.preprocessors;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import sun.org.mozilla.javascript.Context;
import sun.org.mozilla.javascript.JavaScriptException;
import sun.org.mozilla.javascript.Scriptable;

import com.chiorichan.factory.CodeMetaData;

public class CoffeePreProcessor implements PreProcessor
{
	@Override
	public String[] getHandledTypes()
	{
		return new String[] { "coffee", "litcoffee", "coffee.md" };
		// XXX Is coffee.md a markdown type?
	}
	
	@Override
	public String process( CodeMetaData meta, String code )
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
				
				return (String) context.evaluateString( compileScope, String.format( "CoffeeScript.compile(coffeeScriptSource, %s);", String.format( "{bare: %s, filename: '%s'}", true, meta.fileName ) ), "CoffeeScriptCompiler-" + meta.fileName, 0, null );
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
