package com.chiorichan.permission.backend.file;

import java.util.Arrays;
import java.util.List;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.ProxyPermissionUser;
import ru.tehkode.permissions.backends.FileBackend;
import ru.tehkode.permissions.backends.file.FileEntity;
import ru.tehkode.permissions.events.PermissionEntityEvent;

import com.chiorichan.configuration.ConfigurationSection;

public class FileUser extends ProxyPermissionUser
{
	
	protected ConfigurationSection node;
	protected FileBackend backend;
	
	public FileUser(String userName, PermissionManager manager, FileBackend backend)
	{
		super( new FileEntity( userName, manager, backend, "users" ) );
		
		this.backend = backend;
		
		this.node = ( (FileEntity) this.backendEntity ).getConfigNode();
	}
	
	@Override
	protected String[] getGroupsNamesImpl( String siteName )
	{
		Object groups = this.node.get( FileEntity.formatPath( siteName, "group" ) );
		
		if ( groups instanceof String )
		{ // old style
			String[] groupsArray;
			String groupsString = ( (String) groups );
			if ( groupsString.contains( "," ) )
			{
				groupsArray = ( (String) groups ).split( "," );
			}
			else
			{
				groupsArray = new String[] { groupsString };
			}
			
			// Now migrate to new system
			this.node.set( "group", Arrays.asList( groupsArray ) );
			this.save();
			
			return groupsArray;
		}
		else if ( groups instanceof List )
		{
			return ( (List<String>) groups ).toArray( new String[0] );
		}
		else
		{
			return new String[0];
		}
	}
	
	@Override
	public void setGroups( String[] groups, String siteName )
	{
		if ( groups == null )
		{
			return;
		}
		
		this.node.set( FileEntity.formatPath( siteName, "group" ), Arrays.asList( groups ) );
		
		this.save();
		this.callEvent( PermissionEntityEvent.Action.INHERITANCE_CHANGED );
	}
}
