package com.chiorichan.http;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MimeTypes;

public class ContentTypes
{
	public static Map<String, String> types = new LinkedHashMap<String, String>();
	private static final Detector DETECTOR = new DefaultDetector( MimeTypes.getDefaultMimeTypes() );
	
	static
	{
		types.put( "css", "text/css" );
		types.put( "htm", "text/html" );
		types.put( "html", "text/html" );
		types.put( "groovy", "text/html" );
		types.put( "chi", "text/html" );
		types.put( "gif", "image/gif" );
		types.put( "jpeg", "image/jpeg" );
		types.put( "jpg", "image/jpeg" );
	}
	
	public static String detectMimeType( final File file ) throws IOException
	{
		TikaInputStream tikaIS = null;
		try
		{
			tikaIS = TikaInputStream.get( file );
			
			final Metadata metadata = new Metadata();
			// metadata.set(Metadata.RESOURCE_NAME_KEY, file.getName());
			
			return DETECTOR.detect( tikaIS, metadata ).toString();
		}
		finally
		{
			if ( tikaIS != null )
			{
				tikaIS.close();
			}
		}
	}
	
	public static String getContentType( File file )
	{
		String ext = file.getName().split( "\\." )[1];
		
		if ( types.containsKey( ext ) )
		{
			return types.get( ext );
		}
		else
		{
			try
			{
				String mime = detectMimeType( file );
				types.put( ext, mime );
				return mime;
			}
			catch ( IOException e )
			{
				e.printStackTrace();
				return "text/plain";
			}
		}
	}
	
}
