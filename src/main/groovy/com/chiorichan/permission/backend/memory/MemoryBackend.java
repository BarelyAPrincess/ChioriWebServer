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

import java.util.Set;

import com.chiorichan.Loader;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionBackend;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.lang.PermissionBackendException;
import com.chiorichan.permission.lang.PermissionException;
import com.google.common.collect.Sets;

/*
 * Memory Backend
 * Zero Persistence. Does not attempt to save any and all permissions.
 */
public class MemoryBackend extends PermissionBackend
{
	
	public MemoryBackend()
	{
		super();
	}
	
	@Override
	public void initialize() throws PermissionBackendException
	{
		
	}
	
	@Override
	public PermissibleEntity getEntity( String name )
	{
		return new MemoryEntity( name, this );
	}
	
	@Override
	public PermissibleGroup getGroup( String name )
	{
		return new MemoryGroup( name, this );
	}
	
	@Override
	public PermissibleGroup getDefaultGroup( String siteName )
	{
		return PermissionManager.INSTANCE.getGroup( "Default" );
	}
	
	@Override
	public void setDefaultGroup( String child, String... site )
	{
		
	}
	
	@Override
	public PermissibleGroup[] getGroups()
	{
		return new PermissibleGroup[0];
	}
	
	@Override
	public void reload() throws PermissionBackendException
	{
		// Nothing to do here!
	}
	
	@Override
	public PermissibleEntity[] getEntities()
	{
		return new PermissibleEntity[0];
	}
	
	@Override
	public Set<String> getEntityNames( int type )
	{
		return Sets.newHashSet();
	}
	
	@Override
	public void loadPermissionTree()
	{
		// Nothing to do here!
	}
	
	@Override
	public Permission createNode( String namespace ) throws PermissionException
	{
		return new MemoryPermission( namespace );
	}
	
	@Override
	public Permission createNode( String namespace, Permission parent ) throws PermissionException
	{
		return new MemoryPermission( namespace, parent );
	}
}
