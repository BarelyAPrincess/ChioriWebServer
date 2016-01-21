/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
	 * Get the developer e-mail address
	 * Suggested use is to report problems
	 *
	 * @return The developer e-mail address
	 */
	public static String getDeveloperContact()
	{
		return metadata.getProperty( "project.email", "me@chiorichan.com" );
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
	 * Generates a HTML suitable footer for general server info and exception pages
	 *
	 * @return
	 *         HTML footer string
	 */
	public static String getHTMLFooter()
	{
		return "<small>Running <a href=\"https://github.com/ChioriGreene/ChioriWebServer\">" + getProduct() + "</a> Version " + getVersion() + " (Build #" + getBuildNumber() + ")<br />" + getCopyright() + "</small>";
	}

	/**
	 * Get the Java Binary
	 *
	 * @return
	 */
	public static String getJavaBinary()
	{
		String path = System.getProperty( "java.home" ) + File.pathSeparator + "bin" + File.pathSeparator;

		if ( Versioning.isWindows() )
			if ( new File( path + "javaw.exe" ).isFile() )
				return path + "javaw.exe";
			else if ( new File( path + "java.exe" ).isFile() )
				return path + "java.exe";

		return path + "java";
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

	public static String getProcessID()
	{
		// Confirmed working on Debian Linux, Windows?

		String pid = ManagementFactory.getRuntimeMXBean().getName();

		if ( pid != null && pid.contains( "@" ) )
			pid = pid.substring( 0, pid.indexOf( "@" ) );

		return pid;
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

	/*
	 * Java and JVM Methods
	 */

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
	 * Operating System Methods
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

	/**
	 * Indicates if we are running a development build of the server
	 *
	 * @return True is we are running in development mode
	 */
	public static boolean isDevelopment()
	{
		return "0".equals( getBuildNumber() ) || Loader.getConfig() != null && Loader.getConfig().getBoolean( "server.developmentMode", false );
	}

	/**
	 * Indicates if the provided PID is still running, this method is setup to work with both Windows and Linux, might need tuning for other OS's
	 *
	 * @param pid
	 * @return
	 */
	public static boolean isPIDRunning( int pid ) throws IOException
	{
		String[] cmds;
		if ( isUnixLikeOS() )
			cmds = new String[] {"sh", "-c", "ps -ef | grep " + pid + " | grep -v grep"};
		else
			cmds = new String[] {"cmd", "/c", "tasklist /FI \"PID eq " + pid + "\""};

		Runtime runtime = Runtime.getRuntime();
		Process proc = runtime.exec( cmds );

		InputStream inputstream = proc.getInputStream();
		InputStreamReader inputstreamreader = new InputStreamReader( inputstream );
		BufferedReader bufferedreader = new BufferedReader( inputstreamreader );
		String line;
		while ( ( line = bufferedreader.readLine() ) != null )
			if ( line.contains( " " + pid + " " ) )
				return true;

		return false;
	}

	/**
	 * Only effects Unix-like OS'es (Linux and Mac OS X)
	 * It's possible to give non-root users access to privileged ports but it's very complicated
	 * for java and a technically a security risk if malicious code was ran
	 * but it would be in our interest to find a way to detect such workaround
	 *
	 * @param port
	 *             The port run we would like to check
	 * @return True if the port is under 1024 and we are not running on the root account
	 */
	public static boolean isPrivilegedPort( int port )
	{
		// Privileged Ports only exist on Linux, Unix, and Mac OS X (I might be missing some)
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
	 * @return True if we are running on an Unix-like OS.
	 */
	public static boolean isUnixLikeOS()
	{
		// String os = System.getProperty( "os.name" );
		// return "linux".equalsIgnoreCase( os ) || "unix".equalsIgnoreCase( os ) || "mac os x".equalsIgnoreCase( os );

		return SystemUtils.IS_OS_UNIX;
	}

	/**
	 * Indicates if we are running on a Windows Operating System
	 *
	 * @return True if we are running on Windows OS
	 */
	public static boolean isWindows()
	{
		return SystemUtils.IS_OS_WINDOWS;
	}

	/**
	 * Loads the server metadata from the file {@value "com/chiorichan/build.properties"},
	 * which is usually updated by our Gradle build script
	 *
	 * @param force
	 *             Force a metadata reload
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
