package com.chiorichan.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.chiorichan.Loader;

class FileInterpreter
{
	Map<String, String> fwOverrides = new HashMap<String, String>();
	ByteArrayOutputStream bs = new ByteArrayOutputStream();
	File cachedFile;
	
	public FileInterpreter(File file) throws IOException
	{
		cachedFile = file;
		
		fwOverrides.put( "title", null );
		fwOverrides.put( "reqlevel", null );
		fwOverrides.put( "theme", null );
		fwOverrides.put( "view", null );
		
		// Shell Options (groovy,text,html)
		fwOverrides.put( "shell", "html" );
		
		if ( file == null || !file.exists() )
			return;
		
		if ( file.getName().toLowerCase().endsWith( ".chi" ) || file.getName().toLowerCase().endsWith( ".groovy" ) )
			fwOverrides.put( "shell", "groovy" );
		else if ( file.getName().toLowerCase().endsWith( ".txt" ) )
			fwOverrides.put( "shell", "text" );
		else if ( ContentTypes.getContentType( cachedFile.getAbsoluteFile() ).toLowerCase().contains( "image" ) )
			fwOverrides.put( "shell", "image" );
		
		FileInputStream is = new FileInputStream( file );
		
		int nRead;
		byte[] data = new byte[16384];
		
		while ( ( nRead = is.read( data, 0, data.length ) ) != -1 )
		{
			bs.write( data, 0, nRead );
		}
		
		bs.flush();
		
		String[] scanner = new String( bs.toByteArray() ).split( "\\n" );
		
		int inx = 0;
		for ( String l : scanner )
		{
			if ( l.trim().startsWith( "@" ) )
			{
				try
				{
					String key = l.trim().substring( 1, l.trim().indexOf( " " ) );
					String val = l.trim().substring( l.trim().indexOf( " " ) + 1 );
					
					fwOverrides.put( key, val );
					Loader.getLogger().finer( "Setting fwOverride '" + key + "' to '" + val + "'" );
				}
				catch ( NullPointerException | ArrayIndexOutOfBoundsException e )
				{
					e.printStackTrace();
				}
			}
			else if ( l.trim().isEmpty() )
			{
				Loader.getLogger().finest( "Continue reading, this line is empty." );
			}
			else
			{
				Loader.getLogger().finest( "We encountered the beginning of the file content. BREAK!" );
				break;
			}
			
			inx += l.length() + 1;
		}
		
		ByteArrayOutputStream finished = new ByteArrayOutputStream();
		
		int h = 0;
		for ( byte b : bs.toByteArray() )
		{
			h++;
			
			if ( h > inx )
				finished.write( b );
		}
		
		bs = finished;
		
		is.close();
	}
	
	public String getContentType()
	{
		String type = ContentTypes.getContentType( cachedFile.getAbsoluteFile() );
		Loader.getLogger().info( "Detected '" + cachedFile.getAbsolutePath() + "' to be of '" + type + "' type." );
		return type;
	}
	
	public Map<String, String> getOverrides()
	{
		return fwOverrides;
	}
	
	public byte[] getContent()
	{
		return bs.toByteArray();
	}

	public String get( String key )
	{
		return fwOverrides.get( key );
	}
}
