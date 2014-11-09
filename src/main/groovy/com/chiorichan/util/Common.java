/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import com.chiorichan.Loader;

public class Common
{
	/**
	 * @return Epoch based on the current Timezone
	 */
	public static int getEpoch()
	{
		return (int) ( System.currentTimeMillis() / 1000 );
	}
	
	public static void logMessage( String level, String msg )
	{
		try
		{
			Method meth = Class.forName( "com.chiorichan.Loader" ).getMethod( "getLogger" );
			Object o = meth.invoke( null );
			
			meth = o.getClass().getMethod( level.toLowerCase(), String.class );
			
			meth.invoke( o, msg );
		}
		catch ( ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e )
		{
			System.out.println( "[" + level.toUpperCase() + "] " + msg );
		}
	}
	
	public static byte[] createChecksum( String filename ) throws Exception
	{
		InputStream fis = new FileInputStream( filename );
		
		byte[] buffer = new byte[1024];
		MessageDigest complete = MessageDigest.getInstance( "MD5" );
		int numRead;
		
		do
		{
			numRead = fis.read( buffer );
			if ( numRead > 0 )
			{
				complete.update( buffer, 0, numRead );
			}
		}
		while ( numRead != -1 );
		
		fis.close();
		return complete.digest();
	}
	
	public static String getMD5Checksum( String filename ) throws Exception
	{
		byte[] b = createChecksum( filename );
		String result = "";
		
		for ( int i = 0; i < b.length; i++ )
		{
			result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16 ).substring( 1 );
		}
		return result;
	}
	
	public static String md5( String data )
	{
		try
		{
			byte[] bytesOfMessage = data.getBytes( "UTF-8" );
			MessageDigest complete = MessageDigest.getInstance( "MD5" );
			
			byte[] b = complete.digest( bytesOfMessage );
			String result = "";
			
			for ( int i = 0; i < b.length; i++ )
			{
				result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16 ).substring( 1 );
			}
			
			return result;
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static boolean isValidMD5( String s )
	{
		return s.matches( "[a-fA-F0-9]{32}" );
	}
	
	public static boolean isRoot()
	{
		return System.getProperty( "user.name" ).equalsIgnoreCase( "root" );
	}
}
