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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.lang.StartupException;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.util.FileFunc;
import com.chiorichan.util.FileFunc.DirectoryInfo;
import com.chiorichan.util.NetworkFunc;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Used as a helper class for retrieving files from the central maven repository
 */
public class Libraries implements LibrarySource
{
	public static final String BASE_MAVEN_URL = "http://search.maven.org/remotecontent?filepath=";
	public static final File INCLUDES_DIR;
	public static final File LIBRARY_DIR;
	public static Map<String, MavenReference> loadedLibraries = Maps.newHashMap();
	
	public static final Libraries SELF = new Libraries();
	
	static
	{
		LIBRARY_DIR = new File( Loader.getConfig().getString( "advanced.libraries.libPath", "libraries" ) );
		INCLUDES_DIR = new File( LIBRARY_DIR, "local" );
		
		DirectoryInfo result = FileFunc.directoryHealthCheck( LIBRARY_DIR );
		if ( result != DirectoryInfo.DIRECTORY_HEALTHY )
			throw new StartupException( result.getDescription( LIBRARY_DIR ) );
		result = FileFunc.directoryHealthCheck( INCLUDES_DIR );
		if ( result != DirectoryInfo.DIRECTORY_HEALTHY )
			throw new StartupException( result.getDescription( INCLUDES_DIR ) );
		
		addLoaded( "org.fusesource.jansi:jansi:1.11" );
		addLoaded( "net.sf.jopt-simple:jopt-simple:4.7" );
		addLoaded( "org.codehaus.groovy:groovy-all:2.3.6" );
		addLoaded( "io.netty:netty-all:5.0.0.Alpha1" );
		addLoaded( "mysql:mysql-connector-java:5.1.32" );
		addLoaded( "org.xerial:sqlite-jdbc:3.6.16" );
		addLoaded( "com.google.guava:guava:17.0" );
		addLoaded( "org.apache.commons:commons-lang3:3.3.2" );
		addLoaded( "commons-io:commons-io:2.4" );
		addLoaded( "commons-net:commons-net:3.3" );
		addLoaded( "commons-codec:commons-codec:1.9" );
		addLoaded( "org.yaml:snakeyaml:1.13" );
		addLoaded( "com.google.javascript:closure-compiler:r2388" );
		addLoaded( "org.mozilla:rhino:1.7R4" );
		addLoaded( "com.asual.lesscss:lesscss-engine:1.3.0" );
		addLoaded( "joda-time:joda-time:2.7" );
		addLoaded( "com.googlecode.libphonenumber:libphonenumber:7.0.4" );
		addLoaded( "com.google.code.gson:gson:2.3" );
		addLoaded( "org.apache.httpcomponents:fluent-hc:4.3.5" );
		
		// Scans the 'libraries/local' folder for jar files that need to loaded into the classpath
		for ( File f : INCLUDES_DIR.listFiles( new FilenameFilter()
		{
			@Override
			public boolean accept( File dir, String name )
			{
				return name.toLowerCase().endsWith( "jar" );
			}
		} ) )
			loadLibrary( f );
	}
	
	private Libraries()
	{
		
	}
	
	private static void addLoaded( String library )
	{
		try
		{
			MavenReference ref = new MavenReference( "builtin", library );
			loadedLibraries.put( ref.getKey(), ref );
		}
		catch ( IllegalArgumentException e )
		{
			// Do Nothing
		}
	}
	
	public static File getLibraryDir()
	{
		return LIBRARY_DIR;
	}
	
	public static List<MavenReference> getLoadedLibraries()
	{
		return new ArrayList<MavenReference>( loadedLibraries.values() );
	}
	
	public static List<MavenReference> getLoadedLibrariesBySource( LibrarySource source )
	{
		List<MavenReference> references = Lists.newArrayList();
		
		for ( MavenReference ref : loadedLibraries.values() )
			if ( ref.getSource() == source )
				references.add( ref );
		
		return references;
	}
	
	public static MavenReference getReferenceByGroup( String group )
	{
		Validate.notNull( group );
		for ( MavenReference ref : loadedLibraries.values() )
			if ( group.equalsIgnoreCase( ref.getGroup() ) )
				return ref;
		return null;
	}
	
	public static MavenReference getReferenceByName( String name )
	{
		Validate.notNull( name );
		for ( MavenReference ref : loadedLibraries.values() )
			if ( name.equalsIgnoreCase( ref.getName() ) )
				return ref;
		return null;
	}
	
	public static boolean isLoaded( MavenReference lib )
	{
		return loadedLibraries.containsKey( lib.getKey() );
	}
	
	public static boolean loadLibrary( File lib )
	{
		if ( lib == null || !lib.exists() )
			return false;
		
		PluginManager.getLogger().info( ConsoleColor.GRAY + "Loading the library `" + lib.getName() + "`" );
		
		try
		{
			LibraryClassLoader.addFile( lib );
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public static boolean loadLibrary( MavenReference lib )
	{
		String urlJar = lib.mavenUrl( "jar" );
		String urlPom = lib.mavenUrl( "pom" );
		
		File mavenLocalJar = lib.jarFile();
		File mavenLocalPom = lib.pomFile();
		
		if ( urlJar == null || urlJar.isEmpty() || urlPom == null || urlPom.isEmpty() )
			return false;
		
		try
		{
			if ( !mavenLocalPom.exists() || !mavenLocalJar.exists() )
			{
				PluginManager.getLogger().info( ConsoleColor.GOLD + "Downloading the library `" + lib.toString() + "` from url `" + urlJar + "`... Please Wait!" );
				
				if ( !NetworkFunc.downloadFile( urlPom, mavenLocalPom ) )
					return false;
				
				if ( !NetworkFunc.downloadFile( urlJar, mavenLocalJar ) )
					return false;
			}
			
			PluginManager.getLogger().info( ConsoleColor.DARK_GRAY + "Loading the library `" + lib.toString() + "` from file `" + mavenLocalJar + "`..." );
			
			LibraryClassLoader.addFile( mavenLocalJar );
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
			return false;
		}
		
		loadedLibraries.put( lib.getKey(), lib );
		try
		{
			FileFunc.extractNatives( lib.jarFile(), lib.baseDir() );
		}
		catch ( IOException e )
		{
			PluginManager.getLogger().severe( "We had a problem trying to extract native libraries from jar file '" + lib.jarFile() + "', regardless if they existed or not:", e );
		}
		
		return true;
	}
	
	@Override
	public String getName()
	{
		return "builtin";
	}
}
