package com.chiorichan.permission.backend;

import java.io.IOException;
import java.io.OutputStreamWriter;

import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.PermissionBackend;
import com.chiorichan.permission.PermissionBackendException;
import com.chiorichan.permission.backend.memory.MemoryGroup;
import com.chiorichan.permission.backend.memory.MemoryEntity;

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
	public PermissibleEntity getUser( String name )
	{
		return new MemoryEntity( name, manager, this );
	}
	
	@Override
	public PermissibleGroup getGroup( String name )
	{
		return new MemoryGroup( name, manager, this );
	}
	
	@Override
	public PermissibleGroup getDefaultGroup( String siteName )
	{
		return this.manager.getGroup( "Default" );
	}
	
	@Override
	public void setDefaultGroup( PermissibleGroup group, String siteName )
	{
		
	}
	
	@Override
	public String[] getSiteInheritance( String site )
	{
		return new String[0];
	}
	
	@Override
	public void setSiteInheritance( String site, String[] parentSites )
	{
		// Do Nothing
	}
	
	@Override
	public PermissibleGroup[] getGroups()
	{
		return new PermissibleGroup[0];
	}
	
	@Override
	public PermissibleEntity[] getRegisteredUsers()
	{
		return new PermissibleEntity[0];
	}
	
	@Override
	public void reload() throws PermissionBackendException
	{
		// Do Nothing
		
	}
	
	@Override
	public void dumpData( OutputStreamWriter writer ) throws IOException
	{
		// Do Nothing
	}
	
}
