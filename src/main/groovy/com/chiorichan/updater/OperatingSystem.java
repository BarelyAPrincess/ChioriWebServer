/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.updater;

import java.io.File;
import java.util.Locale;

public enum OperatingSystem
{
	LINUX( "linux", new String[] { "linux", "unix" } ), WINDOWS( "windows", new String[] { "win" } ), OSX( "osx", new String[] { "mac" } ), UNKNOWN( "unknown", new String[0] );
	
	private static OperatingSystem operatingSystem;
	private final String name;
	private final String[] aliases;
	
	private OperatingSystem(String name, String[] aliases)
	{
		this.name = name;
		this.aliases = aliases;
	}
	
	public static String getJavaDir()
	{
		String separator = System.getProperty( "file.separator" );
		String path = System.getProperty( "java.home" ) + separator + "bin" + separator;
		
		if ( ( getOperatingSystem() == WINDOWS ) && ( new File( path + "javaw.exe" ).isFile() ) )
		{
			return path + "javaw.exe";
		}
		
		return path + "java";
	}
	
	public static OperatingSystem getOperatingSystem()
	{
		if ( OperatingSystem.operatingSystem != null )
		{
			return OperatingSystem.operatingSystem;
		}
		
		// Always specify english when tolowercase/touppercasing values for comparison against well-known values
		// Prevents an issue with turkish users
		String osName = System.getProperty( "os.name" ).toLowerCase( Locale.ENGLISH );
		
		for ( OperatingSystem operatingSystem : values() )
		{
			for ( String alias : operatingSystem.getAliases() )
			{
				if ( osName.contains( alias ) )
				{
					OperatingSystem.operatingSystem = operatingSystem;
					return operatingSystem;
				}
			}
		}
		
		return UNKNOWN;
	}
	
	public String[] getAliases()
	{
		return aliases;
	}
	
	public String getName()
	{
		return name;
	}
	
	public boolean isSupported()
	{
		return this != UNKNOWN;
	}
}
