package com.chiorichan.factory.preprocessors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.chiorichan.Loader;
import com.chiorichan.factory.CodeMetaData;

public class LessPreProcessor implements PreProcessor
{
	@Override
	public String[] getHandledTypes()
	{
		return new String[] { "less", "stylesheet/less" };
	}
	
	@Override
	public String process( CodeMetaData meta, String code )
	{
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream( "com/chiorichan/less.js" );
		InputStream inputStream2 = classLoader.getResourceAsStream( "com/chiorichan/env.rhino.js" );
		
		try
		{
			Reader reader = new InputStreamReader( inputStream, "UTF-8" );
			Reader reader2 = new InputStreamReader( inputStream2, "UTF-8" );
			
			Context context = Context.enter();
			context.setOptimizationLevel( -1 ); // Without this, Rhino hits a 64K bytecode limit and fails
			
			try
			{
				ScriptableObject globalScope = context.initStandardObjects();
				
				// context.setLanguageVersion( Context.VERSION_1_5 );
				
				globalScope.defineFunctionProperties( new String[] { "print" }, dummyJS.class, ScriptableObject.DONTENUM );
				
				context.evaluateReader( globalScope, reader2, "env.rhino.js", 0, null );
				context.evaluateReader( globalScope, reader, "less.js", 0, null );
				
				Scriptable compileScope = context.newObject( globalScope );
				compileScope.setParentScope( globalScope );
				compileScope.put( "lessSource", compileScope, code );
				
				String fileName = "dummyFile.less";
				
				if ( meta.fileName != null && !meta.fileName.isEmpty() )
					fileName = new File( meta.fileName ).getName();
				
				String script = "var parser = new(less.Parser)({ paths: ['.'], filename: '" + fileName + "' });";
				script += "parser.parse(lessSource, function (e, tree) { try{source = tree.toCSS({compress:true});}catch(ee){source = ee;}});";
				
				context.evaluateString( compileScope, script, "less2css.js", 0, null );
				
				if ( globalScope.get( "source" ) != null && globalScope.get( "source" ) instanceof String )
					code = (String) globalScope.get( "source" );
				else if ( globalScope.get( "source" ) != null )
					Loader.getLogger().debug( "" + globalScope.get( "source" ) );
				
				return code;
			}
			finally
			{
				reader.close();
				Context.exit();
			}
		}
		catch ( JavaScriptException e )
		{
			e.printStackTrace();
			return null;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static class dummyJS
	{
		public static void print()
		{
			
		}
	}
}
