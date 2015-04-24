/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.maven;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.util.FileFunc;
import com.google.common.collect.Lists;

/**
 * Used as a helper class for retrieving files from the central maven repository
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class MavenUtils
{
	public static final File LIBRARY_DIR = new File( Loader.getConfig().getString( "advanced.libraries.libPath", "libraries" ) );
	public static final File INCLUDES_DIR = new File( LIBRARY_DIR, "local" );
	public static final String BASE_MAVEN_URL = "http://search.maven.org/remotecontent?filepath=";
	public static List<String> loadedLibraries = Lists.newArrayList();
	
	static
	{
		FileFunc.directoryHealthCheck( LIBRARY_DIR );
		FileFunc.directoryHealthCheck( INCLUDES_DIR );
		
		loadedLibraries.add( "org.fusesource.jansi:jansi:1.11" );
		loadedLibraries.add( "net.sf.jopt-simple:jopt-simple:4.7" );
		loadedLibraries.add( "org.codehaus.groovy:groovy-all:2.3.6" );
		loadedLibraries.add( "io.netty:netty-all:5.0.0.Alpha1" );
		loadedLibraries.add( "mysql:mysql-connector-java:5.1.32" );
		loadedLibraries.add( "org.xerial:sqlite-jdbc:3.6.16" );
		loadedLibraries.add( "com.google.guava:guava:17.0" );
		loadedLibraries.add( "org.apache.commons:commons-lang3:3.3.2" );
		loadedLibraries.add( "commons-io:commons-io:2.4" );
		loadedLibraries.add( "commons-net:commons-net:3.3" );
		loadedLibraries.add( "commons-codec:commons-codec:1.9" );
		loadedLibraries.add( "org.yaml:snakeyaml:1.13" );
		loadedLibraries.add( "com.google.javascript:closure-compiler:r2388" );
		loadedLibraries.add( "org.mozilla:rhino:1.7R4" );
		loadedLibraries.add( "com.asual.lesscss:lesscss-engine:1.3.0" );
		loadedLibraries.add( "joda-time:joda-time:2.7" );
		loadedLibraries.add( "com.googlecode.libphonenumber:libphonenumber:7.0.4" );
		loadedLibraries.add( "com.google.code.gson:gson:2.3" );
		loadedLibraries.add( "org.apache.httpcomponents:fluent-hc:4.3.5" );
		
		// Scans the 'libraries/local' folder for jar files that need to loaded into the classpath
		for ( File f : INCLUDES_DIR.listFiles( new FilenameFilter()
		{
			@Override
			public boolean accept( File dir, String name )
			{
				return name.toLowerCase().endsWith( "jar" );
			}
		} ) )
		{
			loadLibrary( f );
		}
	}
	
	public static String resolveMavenUrl( String group, String name, String version, String ext )
	{
		return BASE_MAVEN_URL + group.replaceAll( "\\.", "/" ) + "/" + name + "/" + version + "/" + name + "-" + version + "." + ext;
	}
	
	public static File getLibraryDir()
	{
		return LIBRARY_DIR;
	}
	
	public static boolean loadLibrary( File lib )
	{
		if ( lib == null || !lib.exists() )
			return false;
		
		PluginManager.getLogger().info( ConsoleColor.GOLD + "Loading the library `" + lib.getName() + "` from `" + lib.getParent() + "`..." );
		
		try
		{
			MavenClassLoader.addFile( lib );
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public static boolean loadLibrary( MavenLibrary lib )
	{
		String urlJar = MavenUtils.resolveMavenUrl( lib.group, lib.name, lib.version, "jar" );
		String urlPom = MavenUtils.resolveMavenUrl( lib.group, lib.name, lib.version, "pom" );
		File mavenBaseFile = new File( LIBRARY_DIR, lib.group.replaceAll( "\\.", "/" ) + "/" + lib.name + "/" + lib.version + "/" + lib.name + "-" + lib.version );
		File mavenLocalJarFile = new File( mavenBaseFile + ".jar" );
		File mavenLocalPomFile = new File( mavenBaseFile + ".pom" );
		
		if ( urlJar == null || urlJar.isEmpty() || urlPom == null || urlPom.isEmpty() )
			return false;
		
		try
		{
			if ( !mavenLocalPomFile.exists() || !mavenLocalJarFile.exists() )
			{
				PluginManager.getLogger().info( ConsoleColor.GOLD + "Downloading the library `" + lib.toString() + "` from url `" + urlJar + "`... Please Wait!" );
				
				if ( !downloadFile( urlPom, mavenLocalPomFile ) )
					return false;
				
				if ( !downloadFile( urlJar, mavenLocalJarFile ) )
					return false;
			}
			
			PluginManager.getLogger().info( ConsoleColor.GOLD + "Loading the library `" + lib.toString() + "` from file `" + mavenLocalJarFile + "`..." );
			
			MavenClassLoader.addFile( mavenLocalJarFile );
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
			return false;
		}
		
		loadedLibraries.add( lib.group + ":" + lib.name );
		
		return true;
	}
	
	public static boolean downloadFile( String url, File dest ) throws ClientProtocolException, IOException
	{
		HttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet( url );
		
		HttpResponse response = httpclient.execute( httpget );
		HttpEntity entity = response.getEntity();
		
		if ( response.getStatusLine().getStatusCode() != 200 )
		{
			PluginManager.getLogger().severe( "Could not download the file `" + url + "`, webserver returned `" + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase() + "`" );
			return false;
		}
		
		InputStream instream = entity.getContent();
		
		FileUtils.copyInputStreamToFile( instream, dest );
		
		return true;
	}
}
