package com.chiorichan.permission.backend;

import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissionBackend;

public abstract class PermissibleProxy extends PermissibleEntity
{
	PermissionBackend backend;
	
	public PermissibleProxy( String name, PermissionBackend permBackend )
	{
		super( name );
		backend = permBackend;
	}
}
