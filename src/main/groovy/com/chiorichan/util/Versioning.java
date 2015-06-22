/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Properties;

import org.apache.commons.lang3.SystemUtils;

import com.chiorichan.Loader;

/**
 * Provides easy access to the server metadata plus operating system and jvm information
 */
public class Versioning
{
	private static Properties metadata;
	
	static
	{
		loadMetaData( false );
	}
	
	/**
	 * Get the server build number
	 * The build number is only set when the server is built on our Jenkins Build Server or by Travis,
	 * meaning this will be 0 for all development builds
	 * 
	 * @return The server build number
	 */
	public static String getBuildNumber()
	{
		return metadata.getProperty( "project.build", "0" );
	}
	
	/*
	 * Server and Software Methods
	 */
	
	/**
	 * Get the server copyright, e.g., Copyright (c) 2015 Chiori-chan
	 * 
	 * @return The server copyright
	 */
	public static String getCopyright()
	{
		return metadata.getProperty( "project.copyright", "Copyright &copy; 2015 Chiori-chan" );
	}
	
	/**
	 * Get the GitHub Branch this was built from, e.g., master
	 * Set by the Gradle build script
	 * 
	 * @return The GitHub branch
	 */
	public static String getGitHubBranch()
	{
		return metadata.getProperty( "project.branch", "master" );
	}
	
	/**
	 * Get the Java version, e.g., 1.7.0_80
	 * 
	 * @return The Java version number
	 */
	public static String getJavaVersion()
	{
		return System.getProperty( "java.version" );
	}
	
	/**
	 * Get the JVM name
	 * 
	 * @return The JVM name
	 */
	public static String getJVMName()
	{
		// System.getProperty("java.vm.name");
		return ManagementFactory.getRuntimeMXBean().getVmName();
	}
	
	/**
	 * Get the server product name, e.g., Chiori-chan's Web Server
	 * 
	 * @return The Product Name
	 */
	public static String getProduct()
	{
		return metadata.getProperty( "project.name", "Chiori-chan's Web Server" );
	}
	
	/**
	 * Get the server product name without spaces or special characters, e.g., ChioriWebServer
	 * 
	 * @return The Product Name Simple
	 */
	public static String getProductSimple()
	{
		return metadata.getProperty( "project.name", "ChioriWebServer" ).replaceAll( " ", "" );
	}
	
	/**
	 * Get the system username
	 * 
	 * @return The username
	 */
	public static String getUser()
	{
		return System.getProperty( "user.name" );
	}
	
	/**
	 * Get the server version, e.g., 9.2.1 (Milky Berry)
	 * 
	 * @return The server version with code name
	 */
	public static String getVersion()
	{
		return metadata.getProperty( "project.version", "Unknown-Version" ) + " (" + metadata.getProperty( "project.codename" ) + ")";
	}
	
	/*
	 * Java and JVM Methods
	 */
	
	/**
	 * Get the server version number, e.g., 9.2.1
	 * 
	 * @return The server version number
	 */
	public static String getVersionNumber()
	{
		return metadata.getProperty( "project.version", "Unknown-Version" );
	}
	
	/**
	 * Indicates if we are running as either the root user for Unix-like or Administrator user for Windows
	 * 
	 * @return True if Administrator or root
	 */
	public static boolean isAdminUser()
	{
		return "root".equalsIgnoreCase( System.getProperty( "user.name" ) ) || "administrator".equalsIgnoreCase( System.getProperty( "user.name" ) );
	}
	
	/*
	 * Operating System Methods
	 */
	
	/**
	 * Indicates if we are running a development build of the server
	 * 
	 * @return True is we are running in development mode
	 */
	public static boolean isDevelopment()
	{
		return "0".equals( getBuildNumber() ) || Loader.getConfig().getBoolean( "server.developmentMode" );
	}
	
	/**
	 * Only effects Unix-like OS'es (Linux and Mac OS X)
	 * It's possible to give non-root users access to privileged ports but it's very complicated
	 * for java and a technically a security risk if malicious code was ran
	 * but it would be in our interest to find a way to detect such workaround
	 *
	 * @param port
	 *            The port run we would like to check
	 * @return True if the port is under 1024 and we are not running on the root account
	 */
	public static boolean isPrivilegedPort( int port )
	{
		// Privileged Ports only exist on Linux, Unix, and Mac OS X (I know I'm missing some)
		if ( !isUnixLikeOS() )
			return false;
		
		// Privileged Port range from 0 to 1024
		if ( port > 1024 )
			return false;
		
		// If we are trying to use a privileged port, We need to be running as root
		return isAdminUser();
	}
	
	/**
	 * Indicates if we are running on an Unix-like Operating System, e.g., Linux or Max OS X
	 * 
	 * @return True we are running on an Unix-like OS.
	 */
	public static boolean isUnixLikeOS()
	{
		// String os = System.getProperty( "os.name" );
		// return "linux".equalsIgnoreCase( os ) || "unix".equalsIgnoreCase( os ) || "mac os x".equalsIgnoreCase( os );
		
		return SystemUtils.IS_OS_UNIX;
	}
	
	/**
	 * Loads the server metadata from the file {@value "com/chiorichan/build.properties"},
	 * which is usually updated by our Gradle build script
	 * 
	 * @param force
	 *            Force a metadata reload
	 */
	private static void loadMetaData( boolean force )
	{
		if ( metadata != null && !metadata.isEmpty() && !force )
			return;
		
		metadata = new Properties();
		
		InputStream is = null;
		try
		{
			is = Loader.class.getClassLoader().getResourceAsStream( "com/chiorichan/build.properties" );
			metadata.load( is );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if ( is != null )
					is.close();
			}
			catch ( IOException e )
			{
			}
		}
	}
}
