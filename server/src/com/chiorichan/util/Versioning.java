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
		if ( metadata == null )
			loadMetaData();
		
		return metadata.getProperty( "project.version", "Unknown-Version" );
	}
	
	public static String getBuildNumber()
	{
		if ( metadata == null )
			loadMetaData();
		
		return metadata.getProperty( "project.build", "0" );
	}
	
	public static String getVersion()
	{
		if ( metadata == null )
			loadMetaData();
		
		return metadata.getProperty( "project.version", "Unknown-Version" ) + " (" + metadata.getProperty( "project.codename" + ")" );
	}
	
	public static String getCopyright()
	{
		if ( metadata == null )
			loadMetaData();
		
		return metadata.getProperty( "project.copyright", "Copyright &copy; 2014 Chiori-chan" );
	}
	
	public static String getProduct()
	{
		if ( metadata == null )
			loadMetaData();
		
		return metadata.getProperty( "project.name", "Chiori Web Server" );
	}
}
