package com.chiorichan.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.chiorichan.Loader;

public class Versioning
{
	public static String getVersion()
	{
		if ( true )
			return "6.1.1013 (Sonic Dash)";
		
		String result = "Unknown-Version";
		
		InputStream stream = Loader.class.getClassLoader().getResourceAsStream( "META-INF/maven/org.bukkit/bukkit/pom.properties" );
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
	
	public static String getCopyright()
	{
		return "Copyright © 2013 Apple Bloom Company";
	}

	public static String getProduct()
	{
		return "Chiori Web Server (implementing Chiori Framework)";
	}
}
