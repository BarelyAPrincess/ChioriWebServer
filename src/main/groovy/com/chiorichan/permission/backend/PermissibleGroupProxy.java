package com.chiorichan.permission.backend;

import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.PermissionBackend;

public abstract class PermissibleGroupProxy extends PermissibleGroup
{
	PermissionBackend backend;
	
	public PermissibleGroupProxy( String name, PermissionBackend permBackend )
	{
		super( name );
		backend = permBackend;
	}
}
