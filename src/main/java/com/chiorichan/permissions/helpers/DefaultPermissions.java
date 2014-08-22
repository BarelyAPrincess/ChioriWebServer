package com.chiorichan.permissions.helpers;

import java.util.Map;

import com.chiorichan.Loader;
import com.chiorichan.permissions.Permission;
import com.chiorichan.permissions.PermissionDefault;

public final class DefaultPermissions
{
	private static final String ROOT = "chiori";
	private static final String LEGACY_PREFIX = "chiori";
	
	private DefaultPermissions()
	{
	}
	
	public static Permission registerPermission( Permission perm )
	{
		return registerPermission( perm, true );
	}
	
	public static Permission registerPermission( Permission perm, boolean withLegacy )
	{
		Permission result = perm;
		
		try
		{
			Loader.getPermissionsManager().addPermission( perm );
		}
		catch ( IllegalArgumentException ex )
		{
			result = Loader.getPermissionsManager().getPermission( perm.getName() );
		}
		
		if ( withLegacy )
		{
			Permission legacy = new Permission( LEGACY_PREFIX + result.getName(), result.getDescription(), PermissionDefault.FALSE );
			legacy.getChildren().put( result.getName(), true );
			registerPermission( perm, false );
		}
		
		return result;
	}
	
	public static Permission registerPermission( Permission perm, Permission parent )
	{
		parent.getChildren().put( perm.getName(), true );
		return registerPermission( perm );
	}
	
	public static Permission registerPermission( String name, String desc )
	{
		Permission perm = registerPermission( new Permission( name, desc ) );
		return perm;
	}
	
	public static Permission registerPermission( String name, String desc, Permission parent )
	{
		Permission perm = registerPermission( name, desc );
		parent.getChildren().put( perm.getName(), true );
		return perm;
	}
	
	public static Permission registerPermission( String name, String desc, PermissionDefault def )
	{
		Permission perm = registerPermission( new Permission( name, desc, def ) );
		return perm;
	}
	
	public static Permission registerPermission( String name, String desc, PermissionDefault def, Permission parent )
	{
		Permission perm = registerPermission( name, desc, def );
		parent.getChildren().put( perm.getName(), true );
		return perm;
	}
	
	public static Permission registerPermission( String name, String desc, PermissionDefault def, Map<String, Boolean> children )
	{
		Permission perm = registerPermission( new Permission( name, desc, def, children ) );
		return perm;
	}
	
	public static Permission registerPermission( String name, String desc, PermissionDefault def, Map<String, Boolean> children, Permission parent )
	{
		Permission perm = registerPermission( name, desc, def, children );
		parent.getChildren().put( perm.getName(), true );
		return perm;
	}
	
	public static void registerCorePermissions()
	{
		Permission parent = registerPermission( ROOT, "Gives the user the ability to use all Chiori-chan's utilities and commands" );
		
		CommandPermissions.registerPermissions( parent );
		BroadcastPermissions.registerPermissions( parent );
		
		parent.recalculatePermissibles();
	}
}
