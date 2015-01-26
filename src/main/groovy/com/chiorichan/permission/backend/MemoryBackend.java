package com.chiorichan.permission.backend;

import java.util.Set;

import org.gradle.jarjar.com.google.common.collect.Sets;

import com.chiorichan.Loader;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.PermissionBackend;
import com.chiorichan.permission.PermissionBackendException;
import com.chiorichan.permission.backend.memory.MemoryEntity;
import com.chiorichan.permission.backend.memory.MemoryGroup;

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
		return Loader.getPermissionManager().getGroup( "Default" );
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
		// Do Nothing
		
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
		
	}
}
