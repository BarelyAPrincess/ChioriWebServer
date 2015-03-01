/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.permission.event.PermissibleEntityEvent;
import com.chiorichan.util.Common;
import com.chiorichan.util.PermissionUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class PermissibleEntity
{
	protected class TimedPermission
	{
		public TimedPermission( Permission perm, int lifeTime )
		{
			permission = perm;
			time = lifeTime;
		}
		
		Permission permission;
		int time;
		
		public boolean isExpired()
		{
			return ( time - Common.getEpoch() < 0 );
		}
	}
	
	private String id;
	protected boolean debugMode = false;
	protected PermissionBackend backend;
	
	protected Map<String, LinkedList<TimedPermission>> timedPermissions = Maps.newConcurrentMap();
	protected Set<ChildPermission> childPermissions = Sets.newConcurrentHashSet();
	protected Map<String, PermissibleGroup> groups = Maps.newConcurrentMap();
	
	public PermissibleEntity( String id, PermissionBackend permBackend )
	{
		this.id = id;
		backend = permBackend;
		
		reload();
	}
	
	protected ChildPermission getChildPermission( String namespace )
	{
		return getChildPermission( namespace, "" );
	}
	
	protected ChildPermission getChildPermission( String namespace, String ref )
	{
		PermissionNamespace ns = new PermissionNamespace( namespace ).fixInvalidChars();
		
		if ( ref == null )
			ref = "";
		
		ref = ref.toLowerCase();
		
		if ( !PermissionUtil.containsValidChars( ref ) )
			ref = PermissionUtil.removeInvalidChars( ref );
		
		for ( ChildPermission child : childPermissions )
		{
			if ( ns.matches( child.getPermission() ) && ( ( child.getReferences().isEmpty() && ref.isEmpty() ) || child.getReferences().contains( ref ) ) )
				return child;
		}
		
		return null;
	}
	
	/**
	 * Return id of permission entity (User or Group)
	 * User should be equal to User's id on the server
	 * 
	 * @return id
	 */
	public String getId()
	{
		return id;
	}
	
	public void reload()
	{
		reloadPermissions();
		reloadGroups();
		recalculateChildPermissions();
	}
	
	public abstract void reloadPermissions();
	
	public abstract void reloadGroups();
	
	public void recalculateChildPermissions()
	{
		
	}
	
	/**
	 * Save in-memory data to storage backend
	 */
	public abstract void save();
	
	/**
	 * Remove entity data from backend
	 */
	public abstract void remove();
	
	/**
	 * Return entity timed (temporary) permission for ref
	 * 
	 * @param ref
	 * @return Array of timed permissions in that ref
	 */
	public TimedPermission[] getTimedPermissions( String ref )
	{
		if ( ref == null )
			ref = "";
		
		if ( !timedPermissions.containsKey( ref ) )
			return new TimedPermission[0];
		
		return timedPermissions.get( ref ).toArray( new TimedPermission[0] );
	}
	
	public TimedPermission getTimedPermission( Permission perm, String ref )
	{
		if ( ref == null )
			ref = "";
		
		if ( !timedPermissions.containsKey( ref ) )
			return null;
		
		for ( TimedPermission tp : timedPermissions.get( ref ) )
			if ( tp.permission == perm )
				return tp;
		
		return null;
	}
	
	public TimedPermission getTimedPermission( String perm, String ref )
	{
		if ( ref == null )
			ref = "";
		
		if ( !timedPermissions.containsKey( ref ) )
			return null;
		
		for ( TimedPermission tp : timedPermissions.get( ref ) )
			if ( tp.permission.getLocalName().equals( perm ) || tp.permission.getNamespace().equals( perm ) )
				return tp;
		
		return null;
	}
	
	/**
	 * Returns remaining lifetime of specified permission in ref
	 * 
	 * @param permission
	 *            Name of permission
	 * @param ref
	 * @return remaining lifetime in seconds of timed permission. 0 if permission is transient
	 */
	public int getTimedPermissionLifetime( String perm, String ref )
	{
		if ( ref == null )
			ref = "";
		
		if ( !timedPermissions.containsKey( ref ) )
			return 0;
		
		return getTimedPermission( perm, ref ).time - Common.getEpoch();
	}
	
	/**
	 * Adds timed permission to specified ref in seconds
	 * 
	 * @param permission
	 * @param ref
	 * @param lifeTime
	 *            Lifetime of permission in seconds. 0 for transient permission (ref disappear only after server reload)
	 */
	public void addTimedPermission( final Permission perm, String ref, int lifeTime )
	{
		if ( ref == null )
			ref = "";
		
		if ( !timedPermissions.containsKey( ref ) )
			this.timedPermissions.put( ref, new LinkedList<TimedPermission>() );
		
		timedPermissions.get( ref ).add( new TimedPermission( perm, Common.getEpoch() + lifeTime ) );
		
		final String finalRef = ref;
		
		if ( lifeTime > 0 )
		{
			TimerTask task = new TimerTask()
			{
				@Override
				public void run()
				{
					removeTimedPermission( perm, finalRef );
				}
			};
			
			Loader.getPermissionManager().registerTask( task, lifeTime );
		}
		
		Loader.getEventBus().callEvent( new PermissibleEntityEvent( this, PermissibleEntityEvent.Action.PERMISSIONS_CHANGED ) );
	}
	
	/**
	 * Removes specified timed permission for ref
	 * 
	 * @param permission
	 * @param ref
	 */
	public void removeTimedPermission( Permission perm, String ref )
	{
		if ( ref == null )
			ref = "";
		
		if ( !timedPermissions.containsKey( ref ) )
			return;
		
		for ( TimedPermission tp : timedPermissions.get( ref ) )
		{
			if ( tp.permission == perm )
				timedPermissions.get( ref ).remove( tp );
		}
		
		Loader.getEventBus().callEvent( new PermissibleEntityEvent( this, PermissibleEntityEvent.Action.PERMISSIONS_CHANGED ) );
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if ( obj == null )
		{
			return false;
		}
		if ( !getClass().equals( obj.getClass() ) )
		{
			return false;
		}
		if ( this == obj )
		{
			return true;
		}
		
		final PermissibleEntity other = ( PermissibleEntity ) obj;
		return this.id.equals( other.id );
	}
	
	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 89 * hash + ( this.id != null ? this.id.hashCode() : 0 );
		return hash;
	}
	
	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(" + this.getId() + ")";
	}
	
	public boolean explainExpression( String expression )
	{
		if ( expression == null || expression.isEmpty() )
			return false;
		
		return !expression.startsWith( "-" ); // If expression have - (minus) before then that mean expression are negative
	}
	
	public boolean isDebug()
	{
		return debugMode || Loader.getPermissionManager().isDebug();
	}
	
	public void setDebug( boolean debug )
	{
		debugMode = debug;
	}
	
	public boolean isBanned()
	{
		PermissionResult result = checkPermission( PermissionDefault.BANNED.getNode() );
		return result.isTrue();
	}
	
	public boolean isWhitelisted()
	{
		if ( !Loader.getPermissionManager().hasWhitelist )
			return true;
		
		PermissionResult result = checkPermission( PermissionDefault.WHITELISTED.getNode() );
		return result.isTrue();
	}
	
	public boolean isAdmin()
	{
		PermissionResult result = checkPermission( PermissionDefault.ADMIN.getNode() );
		return result.isTrue();
	}
	
	public boolean isOp()
	{
		PermissionResult result = checkPermission( PermissionDefault.OP.getNode() );
		return result.isTrue();
	}
	
	public PermissionResult checkPermission( String perm )
	{
		return checkPermission( perm, "" );
	}
	
	public PermissionResult checkPermission( Permission perm )
	{
		return checkPermission( perm, "" );
	}
	
	public PermissionResult checkPermission( String perm, String ref )
	{
		PermissionResult result = null;
		
		if ( ref == null )
			ref = "";
		
		// Everyone
		if ( perm == null || perm.equals( "-1" ) || perm.isEmpty() )
			perm = PermissionDefault.EVERYBODY.getNameSpace();
		
		// OP Only
		if ( perm.equals( "0" ) || perm.equalsIgnoreCase( "op" ) || perm.equalsIgnoreCase( "root" ) )
			perm = PermissionDefault.OP.getNameSpace();
		
		if ( perm.equalsIgnoreCase( "admin" ) )
			perm = PermissionDefault.ADMIN.getNameSpace();
		
		Permission permission = Permission.getNode( perm, true );
		result = checkPermission( permission, ref );
		
		return result;
	}
	
	public PermissionResult checkPermission( Permission perm, String ref )
	{
		if ( ref == null )
			ref = "";
		
		PermissionResult result = new PermissionResult( this, perm, ref );
		
		if ( !perm.getNamespace().equalsIgnoreCase( PermissionDefault.OP.getNameSpace() ) && isDebug() )
			PermissionManager.getLogger().info( ConsoleColor.YELLOW + "Entity `" + getId() + "` checked for permission `" + perm.getNamespace() + "`" + ( ( ref.isEmpty() ) ? "" : " with reference `" + ref + "`" ) + " with result `" + result + "`" );
		
		return result;
	}
}
