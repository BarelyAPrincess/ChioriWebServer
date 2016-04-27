/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.event;

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

import com.chiorichan.event.EventHandler;
import com.chiorichan.event.Listener;
import com.chiorichan.logger.Log;
import com.google.common.collect.Maps;
import com.google.gson.GsonBuilder;

public class PreLessProcessor implements Listener
{
	@EventHandler( )
	public void onEvent( PreEvalEvent event )
	{
		if ( !event.context().contentType().equals( "stylesheet/less" ) || !event.context().shell().equals( "less" ) )
			return;

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
				compileScope.put( "lessSource", compileScope, event.context().readString() );

				String filename = "dummyFile.less";

				if ( event.context().filename() != null && !event.context().filename().isEmpty() )
					filename = new File( event.context().filename() ).getName();

				/*
				 * try
				 * {
				 * code = new LessImportParser().runParser( code, new File( meta.fileName ).getParentFile().getAbsoluteFile() );
				 * }
				 * catch ( ShellExecuteException e )
				 * {
				 * e.printStackTrace();
				 * }
				 */

				Map<String, Object> compilerOptions = Maps.newHashMap();

				compilerOptions.put( "filename", filename );
				compilerOptions.put( "compress", true );

				String json = new GsonBuilder().create().toJson( compilerOptions );

				context.evaluateString( compileScope, "var parser = new less.Parser(" + json + ");", "less2css.js", 0, null );

				// String script = "parser.parse(lessSource, function (e, tree) { source = 'Hello World'; } );";

				// Loader.getLogger().debug( "" + context.evaluateString( compileScope, script, "less2css.js", 0, null ) );

				// Loader.getLogger().debug( "" + globalScope.get( "source" ) );

				if ( globalScope.get( "source" ) != null && globalScope.get( "source" ) instanceof String )
					event.context().resetAndWrite( ( String ) globalScope.get( "source" ) );
				else if ( globalScope.get( "source" ) != null )
					Log.get().warning( "We did not get what we expected back from Less.js: " + globalScope.get( "source" ) );
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
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}
