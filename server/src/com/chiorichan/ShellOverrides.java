package com.chiorichan;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Maps;

/**
 * Loads ShellOverrides from ShellOverrides.properties file.
 * You can use that file to override what SeaShell will handle a purticular file extension.
 * 
 * @author Chiori-chan
 */
public class ShellOverrides
{
	public static Map<String, String> shells = Maps.newLinkedHashMap();
	
	// TODO Place a copy of the properties in the server root for user modification
	
	static
	{
		try
		{
			File contentTypes = new File( "ContentTypes.properties" );
			
			if ( !contentTypes.exists() )
				contentTypes.createNewFile();
			
			InputStream isDefault = Loader.class.getClassLoader().getResourceAsStream( "com/chiorichan/ShellOverrides.properties" );
			InputStream is = new FileInputStream( contentTypes );
			try
			{
				Properties prop = new Properties();
				prop.load( isDefault );
				prop.load( is );
				for ( Object o : prop.keySet() )
					if ( o instanceof String )
						shells.put( (String) o, (String) prop.get( (String) o ) );
			}
			finally
			{
				if ( is != null )
					is.close();
			}
		}
		catch ( IOException e )
		{
			Loader.getLogger().severe( "Could not load the ShellOverride properties file, exact error was: " + e.getMessage() );
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
		
		if ( shells != null && shells.containsKey( ext.toLowerCase() ) )
		{
			return shells.get( ext.toLowerCase() );
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
		
		if ( shells != null && shells.containsKey( ext ) )
		{
			return shells.get( ext );
		}
		else
		{
			return null;
		}
	}
}
