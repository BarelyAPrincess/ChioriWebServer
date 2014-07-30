/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 */

package com.chiorichan.framework;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.ContentTypes;
import com.chiorichan.Loader;
import com.chiorichan.util.FileUtil;
import com.google.common.collect.Maps;

public class FileInterpreter
{
	protected Map<String, String> interpParams = Maps.newTreeMap();
	protected ByteArrayOutputStream bs = new ByteArrayOutputStream();
	protected File cachedFile = null;
	protected String encoding = "UTF-8";
	
	public String getEncoding()
	{
		return encoding;
	}
	
	public void setEncoding( String _encoding )
	{
		encoding = _encoding;
	}
	
	@Override
	public String toString()
	{
		String overrides = "";
		
		for ( Entry<String, String> o : interpParams.entrySet() )
		{
			overrides += "," + o.getKey() + "=" + o.getValue();
		}
		
		if ( overrides.length() > 1 )
			overrides = overrides.substring( 1 );
		
		String cachedFileStr = ( cachedFile == null ) ? "N/A" : cachedFile.getAbsolutePath();
		
		return "FileInterpreter{content=" + bs.size() + " bytes,encoding=" + encoding + ",file=" + cachedFileStr + ",overrides={" + overrides + "}}";
	}
	
	public File getFile()
	{
		return cachedFile;
	}
	
	public FileInterpreter()
	{
		interpParams.put( "title", null );
		interpParams.put( "reqlevel", "-1" );
		interpParams.put( "theme", null );
		interpParams.put( "view", null );
		
		interpParams.put( "html", null );
		interpParams.put( "file", null );
		
		// Shell Options (groovy,text,html)
		interpParams.put( "shell", null );
	}
	
	public FileInterpreter(File file) throws IOException
	{
		this();
		
		interpretParamsFromFile( file );
	}
	
	public final void interpretParamsFromFile( File file ) throws IOException
	{
		if ( file == null || !file.exists() )
			return;
		
		FileInputStream is = null;
		try
		{
			cachedFile = file;
			
			interpParams.put( "file", file.getAbsolutePath() );
			
			if ( !interpParams.containsKey( "shell" ) || interpParams.get( "shell" ) == null )
				if ( file.getName().toLowerCase().endsWith( ".groovy" ) )
					interpParams.put( "shell", "groovy" );
				else if ( file.getName().toLowerCase().endsWith( ".chi" ) || file.getName().toLowerCase().endsWith( ".gsp" ) )
					interpParams.put( "shell", "embedded" );
				else if ( file.getName().toLowerCase().endsWith( ".txt" ) )
					interpParams.put( "shell", "text" );
				else if ( ContentTypes.getContentType( cachedFile.getAbsoluteFile() ).toLowerCase().contains( "image" ) )
				{
					encoding = "ISO-8859-1";
					interpParams.put( "shell", "image" );
				}
			
			is = new FileInputStream( file );
			
			bs = FileUtil.inputStream2ByteArray( is );
			
			String[] scanner = new String( bs.toByteArray() ).split( "\\n" );
			
			int inx = 0;
			int ln = 0;
			for ( String l : scanner )
			{
				if ( l.trim().startsWith( "@" ) )
					try
					{
						String key = l.trim().substring( 1, l.trim().indexOf( " " ) );
						String val = l.trim().substring( l.trim().indexOf( " " ) + 1 );
						
						interpParams.put( key, val );
						Loader.getLogger().finer( "Setting param '" + key + "' to '" + val + "'" );
					}
					catch ( NullPointerException | ArrayIndexOutOfBoundsException e )
					{	
						
					}
				else if ( l.trim().isEmpty() )
					Loader.getLogger().finest( "Continue reading, this line is empty." );
				else
				{
					Loader.getLogger().finest( "We encountered the beginning of the file content. BREAK!" );
					break;
				}
				
				inx += l.length() + 1;
				ln++;
			}
			
			ByteArrayOutputStream finished = new ByteArrayOutputStream();
			
			int h = 0;
			for ( byte b : bs.toByteArray() )
			{
				h++;
				
				if ( h > inx )
					finished.write( b );
			}
			
			for ( int lnn = 0; lnn < ln; lnn++ )
			{
				finished.write( "\n".getBytes( encoding ) );
			}
			
			bs = finished;
		}
		finally
		{
			if ( is != null )
				is.close();
		}
	}
	
	public String getContentType()
	{
		if ( cachedFile == null )
			return "text/html";
		
		String type = ContentTypes.getContentType( cachedFile.getAbsoluteFile() );
		Loader.getLogger().info( "Detected '" + cachedFile.getAbsolutePath() + "' to be of '" + type + "' type." );
		return type;
	}
	
	public Map<String, String> getParams()
	{
		return interpParams;
	}
	
	public byte[] getContent()
	{
		return bs.toByteArray();
	}
	
	public String get( String key )
	{
		if ( !interpParams.containsKey( key ) )
			return null;
		
		return interpParams.get( key );
	}
	
	public void put( String key, String value )
	{
		interpParams.put( key, value );
	}
}
