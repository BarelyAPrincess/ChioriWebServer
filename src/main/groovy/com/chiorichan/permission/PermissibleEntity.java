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
import java.util.TimerTask;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.permission.event.PermissibleEntityEvent;
import com.chiorichan.permission.structure.ChildPermission;
import com.chiorichan.permission.structure.Permission;
import com.chiorichan.permission.structure.PermissionValueBoolean;
import com.chiorichan.util.Common;
import com.google.common.collect.Maps;

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
	protected Map<String, ChildPermission> childPermissions = Maps.newConcurrentMap();
	protected Map<String, PermissibleGroup> groups = Maps.newConcurrentMap();
	
	public PermissibleEntity( String id, PermissionBackend permBackend )
	{
		this.id = id;
		backend = permBackend;
		
		recalculateChildPermissions();
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
	
	/**
	 * Checks if entity has specified permission in default site
	 * 
	 * @param permission
	 *            Permission to check
	 * @return true if entity has this permission otherwise false
	 */
	public boolean has( String permission )
	{
		return has( permission, Loader.getSiteManager().getSites().get( 0 ).getName() );
	}
	
	/**
	 * Check if entity has specified permission in site
	 * 
	 * @param permission
	 *            Permission to check
	 * @param site
	 *            Site to check permission in
	 * @return true if entity has this permission otherwise false
	 */
	public boolean has( String permission, String site )
	{
		if ( permission != null && permission.isEmpty() )
		{ // empty permission for public access :)
			return true;
		}
		
		ChildPermission perm = childPermissions.get( permission );
		
		if ( isDebug() )
			PermissionManager.getLogger().info( "Entity " + getId() + " checked for \"" + permission + "\", " + ( perm == null ? "no permission found" : "\"" + perm + "\" found" ) );
		
		return ( perm == null ) ? false : true;
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
	 * @param site
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
			if ( tp.permission.getName().equals( perm ) || tp.permission.getNamespace().equals( perm ) )
				return tp;
		
		return null;
	}
	
	/**
	 * Returns remaining lifetime of specified permission in site
	 * 
	 * @param permission
	 *            Name of permission
	 * @param site
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
	 * Adds timed permission to specified site in seconds
	 * 
	 * @param permission
	 * @param ref
	 * @param lifeTime
	 *            Lifetime of permission in seconds. 0 for transient permission (site disappear only after server reload)
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
	 * Removes specified timed permission for site
	 * 
	 * @param permission
	 * @param site
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
	
	public boolean isOp()
	{
		PermissionResult result = checkPermission( Permission.OP );
		return result.isTrue();
	}
	
	public PermissionResult checkPermission( String perm )
	{
		PermissionResult result = null;
		
		// Everyone
		if ( perm == null || perm.equals( "-1" ) || perm.isEmpty() )
			result = new PermissionResult( this, new Permission( "all", new PermissionValueBoolean( "all", true, true ), "The dummy node used for the 'everyone' permission check." ) );
		
		// OP Only
		if ( perm.equals( "0" ) || perm.equalsIgnoreCase( "op" ) || perm.equalsIgnoreCase( "root" ) )
			perm = Permission.OP;
		
		if ( perm.equalsIgnoreCase( "admin" ) )
			perm = Permission.ADMIN;
		
		if ( perm == null )
		{
			Permission permission = Permission.getPermissionNode( perm );
			result = checkPermission( permission );
		}
		
		Loader.getLogger().info( ConsoleColor.GREEN + "Is `" + getId() + "` true for permission `" + perm + "` with result `" + result + "`" );
		return result;
	}
	
	public PermissionResult checkPermission( Permission perm )
	{
		return new PermissionResult( this, perm );
	}
}
