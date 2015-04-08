/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Maps;

/**
 * Loads InterpreterOverrides from InterpreterOverrides.properties file.
 * This file is used to override what Interpreter will handle a particular file extension.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class InterpreterOverrides
{
	static Map<String, String> interpreters = Maps.newLinkedHashMap();
	
	static
	{
		try
		{
			File contentTypes = new File( "InterpreterOverrides.properties" );
			
			if ( !contentTypes.exists() )
				contentTypes.createNewFile();
			
			InputStream isDefault = Loader.class.getClassLoader().getResourceAsStream( "com/chiorichan/InterpreterOverrides.properties" );
			InputStream is = new FileInputStream( contentTypes );
			try
			{
				Properties prop = new Properties();
				prop.load( isDefault );
				prop.load( is );
				for ( Object o : prop.keySet() )
					if ( o instanceof String )
						interpreters.put( ( String ) o, ( String ) prop.get( ( String ) o ) );
			}
			finally
			{
				if ( is != null )
					is.close();
			}
		}
		catch ( IOException e )
		{
			Loader.getLogger().severe( "Could not load the InterpreterOverride properties file, exact error was: " + e.getMessage() );
		}
	}
	
	public static String getFileExtension( File file )
	{
		return getFileExtension( file.getName() );
	}
	
	public static String getFileExtension( String file )
	{
		try
		{
			String[] exts = file.split( "\\." );
			return exts[exts.length - 1];
		}
		catch ( Throwable t )
		{
			return "";
		}
	}
	
	public static String getShellForExt( String ext )
	{
		if ( ext.isEmpty() )
			return null;
		
		if ( interpreters != null && interpreters.containsKey( ext.toLowerCase() ) )
		{
			return interpreters.get( ext.toLowerCase() );
		}
		else
		{
			return null;
		}
	}
	
	public static String getShellForFile( File file )
	{
		String ext = getFileExtension( file ).toLowerCase();
		
		if ( ext.isEmpty() )
			return null;
		
		if ( interpreters != null && interpreters.containsKey( ext ) )
		{
			return interpreters.get( ext );
		}
		else
		{
			return null;
		}
	}
}
