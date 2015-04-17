/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

import com.chiorichan.Loader;
import com.chiorichan.site.Site;
import com.google.common.io.ByteStreams;

/**
 * Class containing file utilities
 */
public class FileUtil
{
	public static byte[] inputStream2Bytes( InputStream is ) throws IOException
	{
		return inputStream2ByteArray( is ).toByteArray();
	}
	
	public static ByteArrayOutputStream inputStream2ByteArray( InputStream is ) throws IOException
	{
		int nRead;
		byte[] data = new byte[16384];
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		
		while ( ( nRead = is.read( data, 0, data.length ) ) != -1 )
		{
			bs.write( data, 0, nRead );
		}
		
		bs.flush();
		
		return bs;
	}
	
	/**
	 * This method copies one file to another location
	 * 
	 * @param inFile
	 *            the source filename
	 * @param outFile
	 *            the target filename
	 * @return true on success
	 */
	@SuppressWarnings( "resource" )
	public static boolean copy( File inFile, File outFile )
	{
		if ( !inFile.exists() )
			return false;
		
		FileChannel in = null;
		FileChannel out = null;
		
		try
		{
			in = new FileInputStream( inFile ).getChannel();
			out = new FileOutputStream( outFile ).getChannel();
			
			long pos = 0;
			long size = in.size();
			
			while ( pos < size )
			{
				pos += in.transferTo( pos, 10 * 1024 * 1024, out );
			}
		}
		catch ( IOException ioe )
		{
			return false;
		}
		finally
		{
			try
			{
				if ( in != null )
					in.close();
				if ( out != null )
					out.close();
			}
			catch ( IOException ioe )
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Calculate a file location
	 * 
	 * @param path
	 *            Base file path
	 * @param site
	 *            Site that is used in relative
	 * @return A File object calculated
	 */
	public static File calculateFileBase( String path, Site site )
	{
		if ( path.startsWith( "[" ) )
		{
			path = path.replace( "[pwd]", new File( "" ).getAbsolutePath() );
			path = path.replace( "[web]", Loader.getWebRoot().getAbsolutePath() );
			
			if ( site != null )
				path = path.replace( "[site]", site.getAbsoluteRoot().getAbsolutePath() );
		}
		
		return new File( path );
	}
	
	public static File calculateFileBase( String path )
	{
		return calculateFileBase( path, null );
	}
	
	public static void directoryHealthCheck( File file )
	{
		if ( file.isFile() )
			file.delete();
		
		if ( !file.exists() )
			file.mkdirs();
	}
	
	public static File fileHealthCheck( File file ) throws IOException
	{
		if ( file.exists() && file.isDirectory() )
			file = new File( file, "default" );
		
		if ( !file.exists() )
			file.createNewFile();
		
		return file;
	}
	
	public static void putResource( String resource, File file ) throws IOException
	{
		try
		{
			InputStream is = Loader.class.getClassLoader().getResourceAsStream( resource );
			FileOutputStream os = new FileOutputStream( file );
			ByteStreams.copy( is, os );
			is.close();
			os.close();
		}
		catch ( FileNotFoundException e )
		{
			throw new IOException( e );
		}
	}
}
