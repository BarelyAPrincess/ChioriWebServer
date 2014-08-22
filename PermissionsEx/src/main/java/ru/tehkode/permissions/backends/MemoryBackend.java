package ru.tehkode.permissions.backends;

import java.io.IOException;
import java.io.OutputStreamWriter;

import ru.tehkode.permissions.PermissionBackend;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.backends.memory.MemoryGroup;
import ru.tehkode.permissions.backends.memory.MemoryUser;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import com.chiorichan.configuration.Configuration;

/*
 * Memory Backend
 * Zero Persistence. Does not attempt to save any and all permissions.
 */
public class MemoryBackend extends PermissionBackend
{
	
	public MemoryBackend(PermissionManager manager, Configuration config)
	{
		super( manager, config );
	}
	
	@Override
	public void initialize() throws PermissionBackendException
	{
		
	}
	
	@Override
	public PermissionUser getUser( String name )
	{
		return new MemoryUser( name, manager, this );
	}
	
	@Override
	public PermissionGroup getGroup( String name )
	{
		return new MemoryGroup( name, manager, this );
	}
	
	@Override
	public PermissionGroup getDefaultGroup( String siteName )
	{
		return this.manager.getGroup( "Default" );
	}
	
	@Override
	public void setDefaultGroup( PermissionGroup group, String siteName )
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
	public PermissionGroup[] getGroups()
	{
		return new PermissionGroup[0];
	}
	
	@Override
	public PermissionUser[] getRegisteredUsers()
	{
		return new PermissionUser[0];
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
