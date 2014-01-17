package ru.tehkode.permissions.backends.memory;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.ProxyPermissionUser;
import ru.tehkode.permissions.backends.MemoryBackend;
import ru.tehkode.permissions.events.PermissionEntityEvent;

public class MemoryUser extends ProxyPermissionUser
{
	MemoryEntity backend;
	
	public MemoryUser(String userName, PermissionManager manager, MemoryBackend backend)
	{
		super( new MemoryEntity( userName, manager, backend ) );
		
		this.backend = (MemoryEntity) this.backendEntity;
	}
	
	@Override
	protected String[] getGroupsNamesImpl( String siteName )
	{
		return backend.getParentNames( siteName );
	}
	
	@Override
	public void setGroups( String[] groups, String siteName )
	{
		backend.setParents( groups, siteName );
		
		this.clearCache();
		
		this.callEvent( PermissionEntityEvent.Action.INHERITANCE_CHANGED );
	}
	
}
