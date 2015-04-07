/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan;

import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import com.google.common.io.CharStreams;

public class ResourceLoader
{
	private File resourcePath = null;
	private ZipFile zipLib = null;
	private boolean isZipFile = false;
	
	public ResourceLoader( File path ) throws IOException
	{
		resourcePath = path;
		isZipFile = path.getAbsolutePath().endsWith( ".zip" );
		
		if ( isZipFile )
			zipLib = new ZipFile( resourcePath );
	}
	
	/*
	 * Provide a folder or zip file that contains the resources.
	 */
	public static ResourceLoader buildLoader( String resource )
	{
		File workingWith = new File( resource );
		
		if ( !workingWith.exists() )
			return null;
		
		try
		{
			return new ResourceLoader( workingWith );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public InputStream getInputStream( String relPath ) throws ZipException, IOException
	{
		if ( isZipFile )
		{
			ZipEntry entry = zipLib.getEntry( relPath );
			if ( entry == null )
				throw new IOException( "No idea what went wrong but the Zip Library returned a null file header." );
			
			if ( entry.isDirectory() )
				throw new IOException( "Can not get an InputStream on a folder." );
			
			return zipLib.getInputStream( entry );
		}
		else
		{
			File file = new File( resourcePath.getAbsolutePath() + System.getProperty( "file.separator", "/" ) + relPath );
			
			if ( !file.exists() )
				throw new FileNotFoundException();
			
			if ( file.isDirectory() )
				throw new IOException( "Can not get an InputStream on a folder." );
			
			return new FileInputStream( file );
		}
	}
	
	public Image getImage( String relPath )
	{
		try
		{
			InputStream is = getInputStream( relPath );
			
			BufferedInputStream in = new BufferedInputStream( is );
			
			return ImageIO.read( in );
		}
		catch ( IOException e )
		{
			return null;
		}
	}
	
	public String getText( String relPath )
	{
		try
		{
			InputStream is = getInputStream( relPath );
			
			return CharStreams.toString( new InputStreamReader( is, "UTF-8" ) );
		}
		catch ( IOException e )
		{
			return null;
		}
	}
}
