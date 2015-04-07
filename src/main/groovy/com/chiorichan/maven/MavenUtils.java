/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.maven;

import java.io.File;
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
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.util.FileUtil;
import com.google.common.collect.Lists;

/**
 * Used as a helper class for retrieving files from the central maven repository
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class MavenUtils
{
	public static final File LIBRARY_DIR = new File( "libraries" );
	public static final String BASE_MAVEN_URL = "http://search.maven.org/remotecontent?filepath=";
	public static List<String> loadedLibraries = Lists.newArrayList();
	
	static
	{
		FileUtil.directoryHealthCheck( LIBRARY_DIR );
		
		loadedLibraries.add( "org.codehaus.groovy:groovy" );
		loadedLibraries.add( "com.google.guava:guava" );
		loadedLibraries.add( "org.apache.commons:commons-lang3" );
		loadedLibraries.add( "commons-io:commons-io" );
		loadedLibraries.add( "commons-logging:commons-logging" );
		loadedLibraries.add( "commons-net:commons-net" );
		loadedLibraries.add( "commons-codec:commons-codec" );
		loadedLibraries.add( "org.yaml:snakeyaml" );
		loadedLibraries.add( "org.codehaus.groovy:groovy-all" );
		loadedLibraries.add( "org.slf4j:slf4j-api" );
		loadedLibraries.add( "org.codeartisans:org.json" );
		loadedLibraries.add( "javax:javaee-api" );
		loadedLibraries.add( "javax.xml:jaxr-api" );
		loadedLibraries.add( "javax.xml:jaxrpc" );
		loadedLibraries.add( "javax.validation:validation-api" );
		loadedLibraries.add( "javax.activation:activation" );
		loadedLibraries.add( "com.asual.lesscss:lesscss-engine" );
		loadedLibraries.add( "net.lingala.zip4j:zip4j" );
		loadedLibraries.add( "com.google.code.findbugs:jsr305" );
		loadedLibraries.add( "com.google.protobuf:protobuf-java" );
		loadedLibraries.add( "args4j:args4j" );
		loadedLibraries.add( "com.google.javascript:closure-compiler" );
		loadedLibraries.add( "tv.cntt:annovention" );
		loadedLibraries.add( "com.google.code.gson:gson" );
		loadedLibraries.add( "net.sf.jopt-simple:jopt-simple" );
		loadedLibraries.add( "joda-time:joda-time" );
		loadedLibraries.add( "org.apache.httpcomponents:fluent-hc" );
		loadedLibraries.add( "org.ow2.asm:asm-all" );
		loadedLibraries.add( "org.javassist:javassist" );
		loadedLibraries.add( "com.googlecode.libphonenumber:libphonenumber" );
		loadedLibraries.add( "org.mozilla:rhino" );
		loadedLibraries.add( "org.ocpsoft.prettytime:prettytime" );
		loadedLibraries.add( "net.java.dev.jna:jna" );
		loadedLibraries.add( "org.objenesis:objenesis" );
		loadedLibraries.add( "net.java.dev.jna:platform" );
		loadedLibraries.add( "com.sun.jersey.contribs:jersey-multipart" );
		loadedLibraries.add( "com.sun.jersey:jersey-bundle" );
		loadedLibraries.add( "org.jboss.logging:jboss-logging" );
		loadedLibraries.add( "com.google.zxing:core" );
		loadedLibraries.add( "com.google.zxing:javase" );
		loadedLibraries.add( "org.json:json" );
		loadedLibraries.add( "io.netty:netty-all" );
		loadedLibraries.add( "mysql:mysql-connector-java" );
		loadedLibraries.add( "org.xerial:sqlite-jdbc" );
		loadedLibraries.add( "org.fusesource.jansi:jansi" );
		loadedLibraries.add( "testjunit:junit" );
		loadedLibraries.add( "testorg.mockito:mockito-core" );
	}
	
	public static String resolveMavenUrl( String group, String name, String version, String ext )
	{
		return BASE_MAVEN_URL + group.replaceAll( "\\.", "/" ) + "/" + name + "/" + version + "/" + name + "-" + version + "." + ext;
	}
	
	public static File getLibraryDir()
	{
		return LIBRARY_DIR;
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
