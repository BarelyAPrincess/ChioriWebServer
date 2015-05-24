/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.lang;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

import com.chiorichan.http.ErrorDocument;
import com.google.common.base.Charsets;

/**
 * Used to parse Apache conf files, e.g., .htaccess.
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class ApacheParser extends ApacheConfContainer
{
	enum ElementType
	{
		DEFAULT, STARTTAG, ENDTAG;
	}
	
	class ApacheConfElement
	{
		String name;
		String[] args;
		ElementType type = ElementType.DEFAULT;
		
		ApacheConfElement( String line )
		{
			Validate.notNull( line );
			
			if ( line.startsWith( "</" ) && line.endsWith( ">" ) )
				type = ElementType.ENDTAG;
			else if ( line.startsWith( "<" ) && line.endsWith( ">" ) )
				type = ElementType.STARTTAG;
			
			String[] e = line.split( " " );
			
			if ( e.length < 1 )
				throw new RuntimeException( "Too Short!" );
			
			name = e[0];
			args = new String[e.length - 1];
			
			for ( int i = 1; i < e.length; i++ )
				args[i - 1] = e[i];
			
			switch ( name )
			{
				case "ErrorDocument":
					putErrorDocument( ErrorDocument.parseArgs( args ) );
					break;
			}
		}
	}
	
	public ApacheParser appendWithDir( File dir ) throws IOException
	{
		if ( dir.exists() && dir.isDirectory() )
		{
			File htaccessFile = new File( dir, ".htaccess" );
			if ( htaccessFile.exists() && htaccessFile.isFile() )
				appendWithFile( htaccessFile );
			
			htaccessFile = new File( dir, "htaccess" );
			if ( htaccessFile.exists() && htaccessFile.isFile() )
				appendWithFile( htaccessFile );
		}
		
		return this;
	}
	
	public ApacheParser appendWithFile( File conf ) throws IOException
	{
		if ( conf.exists() && conf.isFile() )
		{
			InputStream is = new FileInputStream( conf );
			List<String> contents = IOUtils.readLines( is, Charsets.US_ASCII );
			
			for ( String l : contents )
			{
				l = l.trim();
				
				// ErrorDocument 403 http://www.yahoo.com/
				// Order deny,allow
				// Deny from all
				// Allow from 208.113.134.190
				
				if ( l == null || l.isEmpty() || l.startsWith( "#" ) )
					continue;
				
				// ApacheConfElement e = new ApacheConfElement( l );
			}
		}
		
		return this;
	}
}
