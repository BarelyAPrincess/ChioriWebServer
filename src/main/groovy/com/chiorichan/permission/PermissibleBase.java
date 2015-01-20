package com.chiorichan.permission;

import java.util.Set;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.plugin.loader.Plugin;

public abstract class PermissibleBase implements Permissible
{
	private PermissibleEntity perm = new PermissibleEntity( getId() );
	
	public String getName()
	{
		return getId();
	}
	
	public boolean isPermissionSet( String name )
	{
		return perm.isPermissionSet( name );
	}
	
	public boolean isPermissionSet( Permission perm )
	{
		return this.perm.isPermissionSet( perm );
	}
	
	public boolean hasPermission( String req )
	{
		Loader.getLogger().info( ChatColor.GREEN + "Checking `" + getAccountId() + "` for permission `" + req + "` with result `" + perm.hasPermission( req ) + "`" );
		// Everyone
		if ( req.equals( "-1" ) || req.isEmpty() )
			return true;
		// OP Only
		if ( req.equals( "0" ) || req.equalsIgnoreCase( "op" ) || req.equalsIgnoreCase( "admin" ) || req.equalsIgnoreCase( "root" ) )
			return isOp();
		return perm.hasPermission( req );
	}
	
	public boolean hasPermission( Permission perm )
	{
		return this.perm.hasPermission( perm );
	}
	
	public PermissionAttachment addAttachment( Plugin plugin, String name, boolean value )
	{
		return perm.addAttachment( plugin, name, value );
	}
	
	public PermissionAttachment addAttachment( Plugin plugin )
	{
		return perm.addAttachment( plugin );
	}
	
	public PermissionAttachment addAttachment( Plugin plugin, String name, boolean value, int ticks )
	{
		return perm.addAttachment( plugin, name, value, ticks );
	}
	
	public PermissionAttachment addAttachment( Plugin plugin, int ticks )
	{
		return perm.addAttachment( plugin, ticks );
	}
	
	public void removeAttachment( PermissionAttachment attachment )
	{
		perm.removeAttachment( attachment );
	}
	
	public void recalculatePermissions()
	{
		perm.recalculatePermissions();
	}
	
	public Set<PermissionAttachmentInfo> getEffectivePermissions()
	{
		return perm.getEffectivePermissions();
	}
	
	public void disconnect( String reason )
	{
		perm.clearPermissions();
	}
	
	@Override
	public boolean isOp()
	{
		return Loader.getAccountsManager().isOp( getId() );
	}
}
