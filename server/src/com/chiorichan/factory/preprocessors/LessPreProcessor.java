package com.chiorichan.factory.preprocessors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.chiorichan.Loader;
import com.chiorichan.factory.CodeMetaData;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
		InputStream inputStream = classLoader.getResourceAsStream( "com/chiorichan/less-rhino-1.7.4.js" );
		
		try
		{
			Reader reader = new InputStreamReader( inputStream, "UTF-8" );
			
			Context context = Context.enter();
			context.setOptimizationLevel( -1 ); // Without this, Rhino hits a 64K bytecode limit and fails
			
			try
			{
				ScriptableObject globalScope = context.initStandardObjects();
				
				context.evaluateReader( globalScope, reader, "less-rhino-1.7.4.js", 0, null );
				
				Scriptable compileScope = context.newObject( globalScope );
				compileScope.setParentScope( globalScope );
				compileScope.put( "lessSource", compileScope, code );
				
				String fileName = "dummyFile.less";
				
				if ( meta.fileName != null && !meta.fileName.isEmpty() )
					fileName = new File( meta.fileName ).getName();
				
				Gson gson = new GsonBuilder().create();
				
				Map<String, Object> compilerOptions = Maps.newHashMap();
				
				compilerOptions.put( "filename", fileName );
				compilerOptions.put( "compress", true );
				
				String script = "var parser = new(less.Parser)(" + gson.toJson( compilerOptions ) + ");";
				script += "parser.parse(lessSource, function (e, tree) { try{source = tree.toCSS(" + gson.toJson( compilerOptions ) + ");}catch(ee){source = ee;}});";
				
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
