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
import com.chiorichan.event.EventBus;
import com.chiorichan.permission.event.PermissibleEntityEvent;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.PermissionFunc;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class PermissibleEntity
{
	protected class TimedPermission
	{
		Permission permission;
		
		int time;
		
		public TimedPermission( Permission perm, int lifeTime )
		{
			permission = perm;
			time = lifeTime;
		}
		
		public boolean isExpired()
		{
			return ( time - Timings.epoch() < 0 );
		}
	}
	
	protected PermissionBackend backend;
	protected Map<String, PermissionResult> cachedResults = Maps.newConcurrentMap();
	protected Set<ChildPermission> childPermissions = Sets.newConcurrentHashSet();
	
	protected boolean debugMode = false;
	protected Map<String, PermissibleGroup> groups = Maps.newConcurrentMap();
	private String id;
	protected Map<String, LinkedList<TimedPermission>> timedPermissions = Maps.newConcurrentMap();
	
	public PermissibleEntity( String id, PermissionBackend permBackend )
	{
		this.id = id;
		backend = permBackend;
		
		reload();
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
			timedPermissions.put( ref, new LinkedList<TimedPermission>() );
		
		timedPermissions.get( ref ).add( new TimedPermission( perm, Timings.epoch() + lifeTime ) );
		
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
			
			PermissionManager.INSTANCE.registerTask( task, lifeTime );
		}
		
		EventBus.INSTANCE.callEvent( new PermissibleEntityEvent( this, PermissibleEntityEvent.Action.PERMISSIONS_CHANGED ) );
	}
	
	public PermissionResult checkPermission( Permission perm )
	{
		return checkPermission( perm, "" );
	}
	
	public PermissionResult checkPermission( Permission perm, String ref )
	{
		if ( ref == null )
			ref = "";
		
		/**
		 * We cache the results to reduce lag when a permission is checked multiple times over.
		 */
		PermissionResult result = cachedResults.get( perm.getNamespace() + "-" + ref );
		
		if ( result != null )
			if ( result.timecode > Timings.epoch() - 150 ) // 150 Seconds = 2.5 Minutes
				return result;
			else
				cachedResults.remove( perm.getNamespace() + "-" + ref );
		
		result = new PermissionResult( this, perm, ref );
		
		cachedResults.put( perm.getNamespace() + "-" + ref, result );
		
		if ( !perm.getNamespace().equalsIgnoreCase( PermissionDefault.OP.getNameSpace() ) && isDebug() )
			PermissionManager.getLogger().info( ConsoleColor.YELLOW + "Entity `" + getId() + "` checked for permission `" + perm.getNamespace() + "`" + ( ( ref.isEmpty() ) ? "" : " with reference `" + ref + "`" ) + " with result `" + result + "`" );
		
		return result;
	}
	
	public PermissionResult checkPermission( String perm )
	{
		return checkPermission( perm, "" );
	}
	
	public PermissionResult checkPermission( String perm, String ref )
	{
		PermissionResult result = null;
		
		if ( ref == null )
			ref = "";
		
		perm = Permission.parseNode( perm );
		
		Permission permission = Permission.getNode( perm, true );
		result = checkPermission( permission, ref );
		
		return result;
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if ( obj == null )
			return false;
		if ( !getClass().equals( obj.getClass() ) )
			return false;
		if ( this == obj )
			return true;
		
		final PermissibleEntity other = ( PermissibleEntity ) obj;
		return id.equals( other.id );
	}
	
	public boolean explainExpression( String expression )
	{
		if ( expression == null || expression.isEmpty() )
			return false;
		
		return !expression.startsWith( "-" ); // If expression have - (minus) before then that mean expression are negative
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
		
		if ( !PermissionFunc.containsValidChars( ref ) )
			ref = PermissionFunc.removeInvalidChars( ref );
		
		for ( ChildPermission child : childPermissions )
			if ( ns.matches( child.getPermission() ) && ( ( child.getReferences().isEmpty() && ref.isEmpty() ) || child.getReferences().contains( ref ) ) )
				return child;
		
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
		
		return getTimedPermission( perm, ref ).time - Timings.epoch();
	}
	
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
	
	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 89 * hash + ( id != null ? id.hashCode() : 0 );
		return hash;
	}
	
	public boolean isAdmin()
	{
		PermissionResult result = checkPermission( PermissionDefault.ADMIN.getNode() );
		return result.isTrue();
	}
	
	public boolean isBanned()
	{
		PermissionResult result = checkPermission( PermissionDefault.BANNED.getNode() );
		return result.isTrue();
	}
	
	public boolean isDebug()
	{
		return debugMode || PermissionManager.INSTANCE.isDebug();
	}
	
	public boolean isOp()
	{
		PermissionResult result = checkPermission( PermissionDefault.OP.getNode() );
		return result.isTrue();
	}
	
	public boolean isWhitelisted()
	{
		if ( !PermissionManager.INSTANCE.hasWhitelist() )
			return true;
		
		PermissionResult result = checkPermission( PermissionDefault.WHITELISTED.getNode() );
		return result.isTrue();
	}
	
	public void recalculateChildPermissions()
	{
		
	}
	
	public void reload()
	{
		reloadPermissions();
		reloadGroups();
		recalculateChildPermissions();
	}
	
	public abstract void reloadGroups();
	
	public abstract void reloadPermissions();
	
	/**
	 * Remove entity data from backend
	 */
	public abstract void remove();
	
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
			if ( tp.permission == perm )
				timedPermissions.get( ref ).remove( tp );
		
		EventBus.INSTANCE.callEvent( new PermissibleEntityEvent( this, PermissibleEntityEvent.Action.PERMISSIONS_CHANGED ) );
	}
	
	/**
	 * Save entity data to backend
	 */
	public abstract void save();
	
	public void setDebug( boolean debug )
	{
		debugMode = debug;
	}
	
	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(" + getId() + ")";
	}
}
