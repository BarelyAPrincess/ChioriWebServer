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
}
