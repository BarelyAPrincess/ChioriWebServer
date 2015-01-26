package com.chiorichan.permission;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.permission.structure.Permission;

public abstract class PermissibleBase implements Permissible
{
	private PermissibleEntity perm;
	
	protected PermissibleBase( PermissibleEntity entity )
	{
		perm = entity;
	}
	
	public PermissibleBase()
	{
		
	}
	
	public void init()
	{
		perm = Loader.getPermissionManager().getBackend().getEntity( getId() );
	}
	
	@Override
	public String getName()
	{
		return getId();
	}
	
	@Override
	public final boolean isPermissionSet( String req )
	{
		return perm.isPermissionSet( req );
	}
	
	@Override
	public final boolean isPermissionSet( Permission req )
	{
		return perm.isPermissionSet( req );
	}
	
	@Override
	public final boolean hasPermission( String req )
	{
		Loader.getLogger().info( ChatColor.GREEN + "Checking `" + getId() + "` for permission `" + req + "` with result `" + perm.hasPermission( req ) + "`" );
		// Everyone
		if ( req.equals( "-1" ) || req.isEmpty() )
			return true;
		// OP Only
		if ( req.equals( "0" ) || req.equalsIgnoreCase( "op" ) || req.equalsIgnoreCase( "admin" ) || req.equalsIgnoreCase( "root" ) )
			return isOp();
		return perm.hasPermission( req );
	}
	
	@Override
	public final boolean hasPermission( Permission perm )
	{
		return this.perm.hasPermission( perm );
	}
	
	@Override
	public final boolean isOp()
	{
		return perm.isOp();
	}
}
