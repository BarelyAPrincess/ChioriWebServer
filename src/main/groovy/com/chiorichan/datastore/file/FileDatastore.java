/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.file;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.datastore.Datastore;
import com.chiorichan.util.FileFunc;
import com.google.common.collect.Lists;

/**
 * References multiple YAML files at once
 */
public class FileDatastore extends Datastore
{
	private String path;
	private final List<YamlConfiguration> yamls = Lists.newLinkedList();
	private int index;
	
	private FileDatastore( List<File> files )
	{
		path = "";
		for ( File file : files )
			if ( file != null )
				yamls.add( YamlConfiguration.loadConfiguration( file ) );
	}
	
	private FileDatastore( String path, List<YamlConfiguration> yamls, int index )
	{
		this.path = path;
		this.yamls.addAll( yamls );
		this.index = index;
	}
	
	public static FileDatastore loadDirectory( File dir, String regexPattern )
	{
		List<File> files = FileFunc.recursiveFiles( dir, StringUtils.countMatches( regexPattern, "/" ), regexPattern );
		return new FileDatastore( files );
	}
	
	public static FileDatastore loadFile( File file )
	{
		return new FileDatastore( Arrays.asList( file ) );
	}
	
	public Collection<YamlConfiguration> asList()
	{
		return Collections.unmodifiableCollection( yamls );
	}
	
	public void first()
	{
		index = 0;
	}
	
	public FileDatastore getChild( String child )
	{
		return new FileDatastore( path + "/" + child, yamls, index );
	}
	
	public String getString( String key )
	{
		ConfigurationSection section = section();
		return section == null ? null : section.getString( key );
	}
	
	public boolean hasNext()
	{
		return index < yamls.size();
	}
	
	public void last()
	{
		index = yamls.size() - 1;
	}
	
	public void next()
	{
		index++;
	}
	
	public void previous()
	{
		index--;
	}
	
	private ConfigurationSection section()
	{
		return yaml().getConfigurationSection( path );
	}
	
	private YamlConfiguration yaml()
	{
		if ( index < 0 || index > yamls.size() - 1 )
			throw new IndexOutOfBoundsException( "Index is out of bounds" );
		
		return yamls.get( index );
	}
}
