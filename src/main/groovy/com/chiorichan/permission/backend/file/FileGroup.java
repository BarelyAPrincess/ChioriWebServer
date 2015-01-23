package com.chiorichan.permission.backend.file;

import java.util.Arrays;
import java.util.List;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.ProxyPermissionGroup;
import ru.tehkode.permissions.backends.FileBackend;
import ru.tehkode.permissions.backends.file.FileEntity;

import com.chiorichan.configuration.ConfigurationSection;

public class FileGroup extends ProxyPermissionGroup
{
	protected ConfigurationSection node;
	
	public FileGroup(String name, PermissionManager manager, FileBackend backend)
	{
		super( new FileEntity( name, manager, backend, "groups" ) );
		
		this.node = ( (FileEntity) this.backendEntity ).getConfigNode();
	}
	
	@Override
	public String[] getParentGroupsNamesImpl( String siteName )
	{
		List<String> parents = this.node.getStringList( FileEntity.formatPath( siteName, "inheritance" ) );
		
		if ( parents.isEmpty() )
		{
			return new String[0];
		}
		
		return parents.toArray( new String[parents.size()] );
	}
	
	@Override
	public void setParentGroups( String[] parentGroups, String siteName )
	{
		if ( parentGroups == null )
		{
			return;
		}
		
		this.node.set( FileEntity.formatPath( siteName, "inheritance" ), Arrays.asList( parentGroups ) );
		
		this.save();
		
		// this.callEvent(PermissionEntityEvent.Action.INHERITANCE_CHANGED);
	}
}
