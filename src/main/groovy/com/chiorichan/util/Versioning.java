/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.chiorichan.Loader;

public class Versioning
{
	private static Properties metadata = new Properties();
	
	public static void loadMetaData()
	{
		if ( metadata != null && !metadata.isEmpty() )
			return;
		
		InputStream is = null;
		try
		{
			is = Loader.class.getClassLoader().getResourceAsStream( "com/chiorichan/metadata.properties" );
			metadata.load( is );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if ( is != null )
					is.close();
			}
			catch ( IOException e )
			{
			}
		}
	}
	
	public static String getVersionNumber()
	{
		loadMetaData();
		return metadata.getProperty( "project.version", "Unknown-Version" );
	}
	
	public static String getBuildNumber()
	{
		loadMetaData();
		return metadata.getProperty( "project.build", "0" );
	}
	
	public static String getVersion()
	{
		loadMetaData();
		return metadata.getProperty( "project.version", "Unknown-Version" ) + " (" + metadata.getProperty( "project.codename" ) + ")";
	}
	
	public static String getCopyright()
	{
		loadMetaData();
		return metadata.getProperty( "project.copyright", "Copyright &copy; 2014 Chiori-chan" );
	}
	
	public static String getProduct()
	{
		loadMetaData();
		return metadata.getProperty( "project.name", "Chiori Web Server" );
	}
	
	public static String getProductSimple()
	{
		loadMetaData();
		return metadata.getProperty( "project.name", "ChioriWebServer" ).replaceAll( " ", "" );
	}
}
