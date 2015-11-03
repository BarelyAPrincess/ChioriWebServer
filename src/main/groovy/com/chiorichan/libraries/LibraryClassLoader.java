/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.libraries;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Acts as the classloader for downloaded Maven Libraries
 */

@SuppressWarnings( {"unchecked", "rawtypes"} )
public class LibraryClassLoader
{
	private static final Class[] parameters = new Class[] {URL.class};
	
	public static void addFile( String s ) throws IOException
	{
		File f = new File( s );
		addFile( f );
	}
	
	public static void addFile( File f ) throws IOException
	{
		addURL( f.toURI().toURL() );
	}
	
	public static void addURL( URL u ) throws IOException
	{
		URLClassLoader sysloader = ( URLClassLoader ) ClassLoader.getSystemClassLoader();
		Class sysclass = URLClassLoader.class;
		
		try
		{
			Method method = sysclass.getDeclaredMethod( "addURL", parameters );
			method.setAccessible( true );
			method.invoke( sysloader, new Object[] {u} );
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
			throw new IOException( "Error, could not add URL to system classloader" );
		}
		
	}
}
