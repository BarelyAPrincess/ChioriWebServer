package com.chiorichan.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.chiorichan.Loader;
import com.chiorichan.file.YamlConfiguration;

public class Versioning
{
	private static YamlConfiguration metadata = null;
	
	public static void loadMetaData()
	{
		metadata = YamlConfiguration.loadConfiguration( Loader.class.getClassLoader().getResourceAsStream( "com/chiorichan/metadata.yml" ) );
	}
	
	public static String getVersion()
	{
		if ( metadata == null )
			loadMetaData();
		
		return metadata.getString( "meta.version", "Unknown-Version" );
	}
	
	public static String getCopyright()
	{
		if ( metadata == null )
			loadMetaData();
		
		return metadata.getString( "meta.copyright", "Copyright © 2014 Chiori-chan" );
	}

	public static String getProduct()
	{
		if ( metadata == null )
			loadMetaData();
		
		return metadata.getString( "meta.product", "Chiori Web Server" );
	}
}
