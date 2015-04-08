/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission.backend.file;

import com.chiorichan.permission.Permission;

public final class FilePermission extends Permission
{
	public FilePermission( String localName )
	{
		super( localName );
	}
	
	public FilePermission( String localName, Permission parent )
	{
		super( localName, parent );
	}
	
	@Override
	public void saveNode()
	{
		
	}
	
	@Override
	public void reloadNode()
	{
		
	}
	
	@Override
	public void destroyNode()
	{
		
	}
}
