/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission.backend.memory;

import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.structure.Permission;
import com.chiorichan.permission.structure.PermissionValue;

public final class MemoryPermission extends Permission
{
	public MemoryPermission( String localName )
	{
		super( localName );
	}
	
	public MemoryPermission( String localName, Permission parent )
	{
		super( localName, parent );
	}
	
	public MemoryPermission( String localName, PermissionValue<?> value, String desc )
	{
		super( localName, value, desc );
	}
	
	// TODO Make it so node can be changed from one backend to another with ease and without restarting.
	
	@Override
	public void saveNode()
	{
		PermissionManager.getLogger().fine( "MemoryPermission nodes can not be saved. Sorry for the inconvinence. :(" );
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
