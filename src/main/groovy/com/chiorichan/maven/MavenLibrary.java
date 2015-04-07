/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.maven;


/**
 * Used to parse for a new library
 * This is a very very crude class but it will be improved upon oneday soon
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class MavenLibrary
{
	String group;
	String name;
	String version;
	
	public MavenLibrary( String group, String name, String version )
	{
		this.group = group;
		this.name = name;
		this.version = version;
	}
	
	public MavenLibrary( String maven ) throws IllegalAccessException
	{
		String[] parts = maven.split( ":" );
		
		if ( parts.length > 3 || parts.length < 3 )
			throw new IllegalAccessException( "Invalid length maven string, it must equal three parts." );
		
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
}
