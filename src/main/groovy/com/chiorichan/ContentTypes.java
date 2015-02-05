/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Loader Content-Types from ContentTypes.properties file.
 * FileInterpreter uses this class to find the correct Content-Type based on file extension.
 * 
 * @author Chiori-chan
 */
public class ContentTypes
{
	public static Map<String, String> types = Maps.newLinkedHashMap();
	
	// TODO Place a copy of the properties in the server root for user modification
	
	static
	{
		try
		{
			File contentTypes = new File( "ContentTypes.properties" );
			
			if ( !contentTypes.exists() )
				contentTypes.createNewFile();
			
			InputStream isDefault = Loader.class.getClassLoader().getResourceAsStream( "com/chiorichan/ContentTypes.properties" );
			InputStream is = new FileInputStream( contentTypes );
			try
			{
				Properties prop = new Properties();
				prop.load( isDefault );
				prop.load( is );
				for ( Object o : prop.keySet() )
					if ( o instanceof String )
						types.put( ( String ) o, ( String ) prop.get( ( String ) o ) );
			}
			finally
			{
				if ( is != null )
					is.close();
			}
		}
		catch ( IOException e )
		{
			Loader.getLogger().severe( "Could not load the Content-Type properties file, exact error was: " + e.getMessage() );
		}
	}
	
	public static String[] getAllTypes()
	{
		return types.values().toArray( new String[0] );
	}
	
	public static String[] getAllTypes( String search )
	{
		List<String> rtn = Lists.newArrayList();
		
		for ( Entry<String, String> e : types.entrySet() )
		{
			if ( e.getKey().toLowerCase().contains( search ) || e.getValue().toLowerCase().contains( search ) )
				rtn.add( e.getValue() );
		}
		
		return rtn.toArray( new String[0] );
	}
	
	public static String getContentType( File file )
	{
		if ( file.isDirectory() )
			return "folder";
		
		String[] exts = file.getName().split( "\\." );
		String ext = exts[exts.length - 1];
		
		if ( types != null && types.containsKey( ext ) )
		{
			return types.get( ext );
		}
		else
		{
			return "text/plain";
		}
	}
}
