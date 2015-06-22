/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.backend.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.permission.PermissionManager;

public class FileConfig extends YamlConfiguration
{
	protected File file;
	
	public FileConfig( File file )
	{
		super();
		
		this.file = file;
		
		reload();
	}
	
	public File getFile()
	{
		return file;
	}
	
	public void reload()
	{
		
		try
		{
			this.load( file );
		}
		catch ( FileNotFoundException e )
		{
			// do nothing
		}
		catch ( Throwable e )
		{
			throw new IllegalStateException( "Error loading permissions file", e );
		}
	}
	
	public void save()
	{
		try
		{
			this.save( file );
		}
		catch ( IOException e )
		{
			PermissionManager.getLogger().severe( "Error during saving permissions file: " + e.getMessage() );
		}
	}
}
