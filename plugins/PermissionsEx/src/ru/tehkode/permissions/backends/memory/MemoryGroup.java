package ru.tehkode.permissions.backends.memory;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.ProxyPermissionGroup;
import ru.tehkode.permissions.backends.MemoryBackend;
import ru.tehkode.permissions.events.PermissionEntityEvent;

public class MemoryGroup extends ProxyPermissionGroup
{
	MemoryEntity backend;
	
	public MemoryGroup(String name, PermissionManager manager, MemoryBackend backend)
	{
		super( new MemoryEntity( name, manager, backend ) );
		
		this.backend = (MemoryEntity) this.backendEntity;
	}
	
	@Override
	protected String[] getParentGroupsNamesImpl( String siteName )
	{
		return this.backend.getParentNames( siteName );
	}
	
	@Override
	public void setParentGroups( String[] parentGroups, String siteName )
	{
		if ( parentGroups == null )
		{
			return;
		}
		
		this.backend.setParents( parentGroups, siteName );
		
		this.callEvent( PermissionEntityEvent.Action.INHERITANCE_CHANGED );
	}
	
}
