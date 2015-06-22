/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.backend.memory;

import java.util.Set;

import com.chiorichan.permission.PermissibleBase;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionBackend;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.lang.PermissionBackendException;
import com.google.common.collect.Sets;

/*
 * Memory Backend
 * Zero Persistence. Does not attempt to save any changes.
 */
public class MemoryBackend extends PermissionBackend
{
	private static MemoryBackend backend;
	
	public MemoryBackend()
	{
		super();
		backend = this;
	}
	
	public static MemoryBackend getBackend()
	{
		return backend;
	}
	
	@Override
	public PermissibleGroup getDefaultGroup( String siteName )
	{
		return PermissionManager.INSTANCE.getGroup( "Default" );
	}
	
	@Override
	public PermissibleBase[] getEntities()
	{
		return new PermissibleBase[0];
	}
	
	@Override
	public PermissibleBase getEntity( String name )
	{
		return new MemoryEntity( name );
	}
	
	@Override
	public Set<String> getEntityNames( int type )
	{
		return Sets.newHashSet();
	}
	
	@Override
	public PermissibleGroup getGroup( String name )
	{
		return new MemoryGroup( name );
	}
	
	@Override
	public PermissibleGroup[] getGroups()
	{
		return new PermissibleGroup[0];
	}
	
	@Override
	public void initialize() throws PermissionBackendException
	{
		// Nothing to do here!
	}
	
	@Override
	public void loadData()
	{
		// Nothing to do here!
	}
	
	@Override
	public void nodeCommit( Permission perm )
	{
		PermissionManager.getLogger().fine( "MemoryPermission nodes can not be saved. Sorry for the inconvinence. Might you consider changing permission backends. :(" );
	}
	
	@Override
	public void nodeDestroy( Permission perm )
	{
		// Nothing to do here!
	}
	
	@Override
	public void nodeReload( Permission perm )
	{
		// Nothing to do here!
	}
	
	@Override
	public void reload() throws PermissionBackendException
	{
		// Nothing to do here!
	}
	
	@Override
	public void setDefaultGroup( String child, String... site )
	{
		// Nothing to do here!
	}
}
