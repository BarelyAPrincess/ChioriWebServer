package com.chiorichan.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.chiorichan.Main;

public class Versioning
{
	public static String getVersion()
	{
		if ( true )
			return "1.0";
		
		String result = "Unknown-Version";
		
		InputStream stream = Main.class.getClassLoader().getResourceAsStream( "META-INF/maven/org.bukkit/bukkit/pom.properties" );
		Properties properties = new Properties();
		
		if ( stream != null )
		{
			try
			{
				properties.load( stream );
				
				result = properties.getProperty( "version" );
			}
			catch ( IOException ex )
			{
				Logger.getLogger( Versioning.class.getName() ).log( Level.SEVERE, "Could not get Bukkit version!", ex );
			}
		}
		
		return result;
	}

	public static String getFrameworkVersion()
	{
		return "5.2.0826 (Lunar Dream)";
	}
	
	public static String getFrameworkCopyright()
	{
		return "Copyright © 2013 Apple Bloom Company";
	}
}
