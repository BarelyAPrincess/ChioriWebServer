/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.libraries;

import java.io.File;

import com.chiorichan.plugin.PluginManager;

/**
 * Used to parse for a new library
 * This is a very very crude class but it will be improved upon oneday soon
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class MavenReference
{
	private final String group;
	private final String name;
	private final String version;
	private final String source;
	
	public MavenReference( String sourceName, String group, String name, String version )
	{
		this.source = sourceName;
		this.group = group;
		this.name = name;
		this.version = version;
	}
	
	public MavenReference( String sourceName, String maven )
	{
		this.source = sourceName;
		
		String[] parts = maven.split( ":" );
		
		if ( parts.length > 3 || parts.length < 3 )
			throw new IllegalArgumentException( "Invalid length maven string, it must equal three parts when devided with delimiter ':', i.e., group:name:version." );
		
		group = parts[0];
		name = parts[1];
		version = parts[2];
	}
	
	public String getGroup()
	{
		return group;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getVersion()
	{
		return version;
	}
	
	@Override
	public String toString()
	{
		return group + ":" + name + ":" + version;
	}
	
	String getKey()
	{
		return group + ":" + name;
	}
	
	public LibrarySource getSource()
	{
		if ( "builtin".equals( source ) )
			return Libraries.SELF;
		
		return PluginManager.INSTANCE.getPluginByNameWithoutException( source );
	}
	
	public String mavenUrl( String ext )
	{
		return Libraries.BASE_MAVEN_URL + group.replaceAll( "\\.", "/" ) + "/" + name + "/" + version + "/" + name + "-" + version + "." + ext;
	}
	
	public File jarFile()
	{
		return new File( baseDir(), getName() + "-" + getVersion() + ".jar" );
	}
	
	public File pomFile()
	{
		return new File( baseDir(), getName() + "-" + getVersion() + ".pom" );
	}
	
	public File baseDir()
	{
		File dir = new File( Libraries.LIBRARY_DIR, getGroup().replaceAll( "\\.", "/" ) + "/" + getName() + "/" + getVersion() );
		
		if ( dir.isFile() )
			dir.delete();
		
		if ( !dir.exists() )
			dir.mkdirs();
		
		return dir;
	}
}
