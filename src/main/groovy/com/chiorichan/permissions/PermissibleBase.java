/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permissions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.chiorichan.Loader;
import com.chiorichan.plugin.Plugin;

public class PermissibleBase extends Permissible
{
	private Permissible parent = this;
	private final List<PermissionAttachment> attachments = new LinkedList<PermissionAttachment>();
	private final Map<String, PermissionAttachmentInfo> permissions = new HashMap<String, PermissionAttachmentInfo>();
	
	public PermissibleBase( Permissible p )
	{
		parent = p;
		recalculatePermissions();
	}
	
	public boolean isOp()
	{
		return Loader.getAccountsManager().isOp( parent.getId() );
	}
	
	public boolean isPermissionSet( String name )
	{
		if ( name == null )
		{
			throw new IllegalArgumentException( "Permission name cannot be null" );
		}
		
		return permissions.containsKey( name.toLowerCase() );
	}
	
	public boolean isPermissionSet( Permission perm )
	{
		if ( perm == null )
		{
			throw new IllegalArgumentException( "Permission cannot be null" );
		}
		
		return isPermissionSet( perm.getName() );
	}
	
	public boolean hasPermission( String inName )
	{
		if ( inName == null )
		{
			throw new IllegalArgumentException( "Permission name cannot be null" );
		}
		
		String name = inName.toLowerCase();
		
		if ( isPermissionSet( name ) )
		{
			return permissions.get( name ).getValue();
		}
		else
		{
			Permission perm = Loader.getPermissionsManager().getPermission( name );
			
			if ( perm != null )
			{
				return perm.getDefault().getValue( isOp() );
			}
			else
			{
				return Permission.DEFAULT_PERMISSION.getValue( isOp() );
			}
		}
	}
	
	public boolean hasPermission( Permission perm )
	{
		if ( perm == null )
		{
			throw new IllegalArgumentException( "Permission cannot be null" );
		}
		
		String name = perm.getName().toLowerCase();
		
		if ( isPermissionSet( name ) )
		{
			return permissions.get( name ).getValue();
		}
		return perm.getDefault().getValue( isOp() );
	}
	
	public PermissionAttachment addAttachment( Plugin plugin, String name, boolean value )
	{
		if ( name == null )
		{
			throw new IllegalArgumentException( "Permission name cannot be null" );
		}
		else if ( plugin == null )
		{
			throw new IllegalArgumentException( "Plugin cannot be null" );
		}
		else if ( !plugin.isEnabled() )
		{
			throw new IllegalArgumentException( "Plugin " + plugin.getDescription().getFullName() + " is disabled" );
		}
		
		PermissionAttachment result = addAttachment( plugin );
		result.setPermission( name, value );
		
		recalculatePermissions();
		
		return result;
	}
	
	public PermissionAttachment addAttachment( Plugin plugin )
	{
		if ( plugin == null )
		{
			throw new IllegalArgumentException( "Plugin cannot be null" );
		}
		else if ( !plugin.isEnabled() )
		{
			throw new IllegalArgumentException( "Plugin " + plugin.getDescription().getFullName() + " is disabled" );
		}
		
		PermissionAttachment result = new PermissionAttachment( plugin, parent );
		
		attachments.add( result );
		recalculatePermissions();
		
		return result;
	}
	
	public void removeAttachment( PermissionAttachment attachment )
	{
		if ( attachment == null )
		{
			throw new IllegalArgumentException( "Attachment cannot be null" );
		}
		
		if ( attachments.contains( attachment ) )
		{
			attachments.remove( attachment );
			PermissionRemovedExecutor ex = attachment.getRemovalCallback();
			
			if ( ex != null )
			{
				ex.attachmentRemoved( attachment );
			}
			
			recalculatePermissions();
		}
		else
		{
			throw new IllegalArgumentException( "Given attachment is not part of Permissible object " + parent );
		}
	}
	
	public void recalculatePermissions()
	{
		clearPermissions();
		Set<Permission> defaults = Loader.getPermissionsManager().getDefaultPermissions( isOp() );
		Loader.getPermissionsManager().subscribeToDefaultPerms( isOp(), parent );
		
		for ( Permission perm : defaults )
		{
			String name = perm.getName().toLowerCase();
			permissions.put( name, new PermissionAttachmentInfo( parent, name, null, true ) );
			Loader.getPermissionsManager().subscribeToPermission( name, parent );
			calculateChildPermissions( perm.getChildren(), false, null );
		}
		
		for ( PermissionAttachment attachment : attachments )
		{
			calculateChildPermissions( attachment.getPermissions(), false, attachment );
		}
	}
	
	public synchronized void clearPermissions()
	{
		Set<String> perms = permissions.keySet();
		
		for ( String name : perms )
		{
			Loader.getPermissionsManager().unsubscribeFromPermission( name, parent );
		}
		
		Loader.getPermissionsManager().unsubscribeFromDefaultPerms( false, parent );
		Loader.getPermissionsManager().unsubscribeFromDefaultPerms( true, parent );
		
		permissions.clear();
	}
	
	private void calculateChildPermissions( Map<String, Boolean> children, boolean invert, PermissionAttachment attachment )
	{
		Set<String> keys = children.keySet();
		
		for ( String name : keys )
		{
			Permission perm = Loader.getPermissionsManager().getPermission( name );
			boolean value = children.get( name ) ^ invert;
			String lname = name.toLowerCase();
			
			permissions.put( lname, new PermissionAttachmentInfo( parent, lname, attachment, value ) );
			Loader.getPermissionsManager().subscribeToPermission( name, parent );
			
			if ( perm != null )
			{
				calculateChildPermissions( perm.getChildren(), !value, attachment );
			}
		}
	}
	
	public PermissionAttachment addAttachment( Plugin plugin, String name, boolean value, int ticks )
	{
		if ( name == null )
		{
			throw new IllegalArgumentException( "Permission name cannot be null" );
		}
		else if ( plugin == null )
		{
			throw new IllegalArgumentException( "Plugin cannot be null" );
		}
		else if ( !plugin.isEnabled() )
		{
			throw new IllegalArgumentException( "Plugin " + plugin.getDescription().getFullName() + " is disabled" );
		}
		
		PermissionAttachment result = addAttachment( plugin, ticks );
		
		if ( result != null )
		{
			result.setPermission( name, value );
		}
		
		return result;
	}
	
	public PermissionAttachment addAttachment( Plugin plugin, int ticks )
	{
		if ( plugin == null )
		{
			throw new IllegalArgumentException( "Plugin cannot be null" );
		}
		else if ( !plugin.isEnabled() )
		{
			throw new IllegalArgumentException( "Plugin " + plugin.getDescription().getFullName() + " is disabled" );
		}
		
		PermissionAttachment result = addAttachment( plugin );
		
		if ( Loader.getScheduler().scheduleSyncDelayedTask( plugin, new RemoveAttachmentRunnable( result ), ticks ) == -1 )
		{
			Loader.getLogger().log( Level.WARNING, "Could not add PermissionAttachment to " + parent + " for plugin " + plugin.getDescription().getFullName() + ": Scheduler returned -1" );
			result.remove();
			return null;
		}
		else
		{
			return result;
		}
	}
	
	public Set<PermissionAttachmentInfo> getEffectivePermissions()
	{
		return new HashSet<PermissionAttachmentInfo>( permissions.values() );
	}
	
	private class RemoveAttachmentRunnable implements Runnable
	{
		private PermissionAttachment attachment;
		
		public RemoveAttachmentRunnable(PermissionAttachment attachment)
		{
			this.attachment = attachment;
		}
		
		public void run()
		{
			attachment.remove();
		}
	}

	@Override
	public String getId()
	{
		return parent.getId();
	}
}
