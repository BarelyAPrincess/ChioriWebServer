package com.chiorichan;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

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
						types.put( (String) o, (String) prop.get( (String) o ) );
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
	
	public static String getContentType( File file )
	{
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
