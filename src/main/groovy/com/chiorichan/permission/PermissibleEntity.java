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
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.chiorichan.Loader;
import com.chiorichan.permission.event.PermissibleEntityEvent;
import com.chiorichan.permission.structure.ChildPermission;
import com.google.common.collect.Maps;

public abstract class PermissibleEntity implements PermissibleParent
{
	private String name;
	protected boolean virtual = true;
	protected boolean debugMode = false;
	protected Map<String, List<String>> timedPermissions = new ConcurrentHashMap<String, List<String>>();
	protected Map<String, Long> timedPermissionsTime = new ConcurrentHashMap<String, Long>();
	
	protected Map<String, ChildPermission> childPermissions = Maps.newConcurrentMap();
	
	public PermissibleEntity( String name )
	{
		this.name = name;
	}
	
	/**
	 * Return name of permission entity (User or Group)
	 * User should be equal to User's name on the server
	 * 
	 * @return name
	 */
	public String getName()
	{
		return this.name;
	}
	
	protected void setName( String name )
	{
		this.name = name;
	}
	
	public void attachPermission( ChildPermission perm )
	{
		childPermissions.put( perm.getPermission().getNamespace(), perm );
	}
	
	public void detachPermission( ChildPermission perm )
	{
		detachPermission( perm.getPermission().getNamespace() );
	}
	
	public void detachPermission( String perm )
	{
		childPermissions.remove( perm );
	}
	
	/**
	 * Returns entity prefix
	 * 
	 * @param siteName
	 * @return prefix
	 */
	public abstract String getPrefix( String siteName );
	
	public String getPrefix()
	{
		return this.getPrefix( null );
	}
	
	/**
	 * Returns entity prefix
	 * 
	 */
	/**
	 * Set prefix to value
	 * 
	 * @param prefix
	 *            new prefix
	 */
	public abstract void setPrefix( String prefix, String siteName );
	
	/**
	 * Return entity suffix
	 * 
	 * @return suffix
	 */
	public abstract String getSuffix( String siteName );
	
	public String getSuffix()
	{
		return getSuffix( null );
	}
	
	/**
	 * Set suffix to value
	 * 
	 * @param suffix
	 *            new suffix
	 */
	public abstract void setSuffix( String suffix, String siteName );
	
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
			PermissionManager.getLogger().info( "Entity " + getName() + " checked for \"" + permission + "\", " + ( perm == null ? "no permission found" : "\"" + perm + "\" found" ) );
		
		return ( perm == null ) ? false : true;
	}
	
	/**
	 * Return all entity permissions in specified site
	 * 
	 * @param site
	 *            Site name
	 * @return Array of permission expressions
	 */
	public abstract String[] getPermissions( String site );
	
	/**
	 * Return permissions for all sites
	 * Common permissions stored as "" (empty string) as site.
	 * 
	 * @return Map with site name as key and permissions array as value
	 */
	public abstract Map<String, String[]> getAllPermissions();
	
	/**
	 * Save in-memory data to storage backend
	 */
	public abstract void save();
	
	/**
	 * Remove entity data from backend
	 */
	public abstract void remove();
	
	/**
	 * Return state of entity
	 * 
	 * @return true if entity is only in-memory
	 */
	public boolean isVirtual()
	{
		return this.virtual;
	}
	
	/**
	 * Return site names where entity have permissions/options/etc
	 * 
	 * @return String array of site ids
	 */
	public abstract String[] getSites();
	
	/**
	 * Return entity timed (temporary) permission for site
	 * 
	 * @param site
	 * @return Array of timed permissions in that site
	 */
	public String[] getTimedPermissions( String site )
	{
		if ( site == null )
		{
			site = "";
		}
		
		if ( !this.timedPermissions.containsKey( site ) )
		{
			return new String[0];
		}
		
		return this.timedPermissions.get( site ).toArray( new String[0] );
	}
	
	/**
	 * Returns remaining lifetime of specified permission in site
	 * 
	 * @param permission
	 *            Name of permission
	 * @param site
	 * @return remaining lifetime in seconds of timed permission. 0 if permission is transient
	 */
	public int getTimedPermissionLifetime( String permission, String site )
	{
		if ( site == null )
		{
			site = "";
		}
		
		if ( !this.timedPermissionsTime.containsKey( site + ":" + permission ) )
		{
			return 0;
		}
		
		return ( int ) ( this.timedPermissionsTime.get( site + ":" + permission ).longValue() - ( System.currentTimeMillis() / 1000L ) );
	}
	
	/**
	 * Adds timed permission to specified site in seconds
	 * 
	 * @param permission
	 * @param site
	 * @param lifeTime
	 *            Lifetime of permission in seconds. 0 for transient permission (site disappear only after server reload)
	 */
	public void addTimedPermission( final String permission, String site, int lifeTime )
	{
		if ( site == null )
		{
			site = "";
		}
		
		if ( !this.timedPermissions.containsKey( site ) )
		{
			this.timedPermissions.put( site, new LinkedList<String>() );
		}
		
		this.timedPermissions.get( site ).add( permission );
		
		final String finalSite = site;
		
		if ( lifeTime > 0 )
		{
			TimerTask task = new TimerTask()
			{
				
				@Override
				public void run()
				{
					removeTimedPermission( permission, finalSite );
				}
			};
			
			Loader.getPermissionManager().registerTask( task, lifeTime );
			
			this.timedPermissionsTime.put( site + ":" + permission, ( System.currentTimeMillis() / 1000L ) + lifeTime );
		}
		
		Loader.getEventBus().callEvent( new PermissibleEntityEvent( this, PermissibleEntityEvent.Action.PERMISSIONS_CHANGED ) );
	}
	
	/**
	 * Removes specified timed permission for site
	 * 
	 * @param permission
	 * @param site
	 */
	public void removeTimedPermission( String permission, String site )
	{
		if ( site == null )
		{
			site = "";
		}
		
		if ( !this.timedPermissions.containsKey( site ) )
		{
			return;
		}
		
		this.timedPermissions.get( site ).remove( permission );
		this.timedPermissions.remove( site + ":" + permission );
		
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
		return this.name.equals( other.name );
	}
	
	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 89 * hash + ( this.name != null ? this.name.hashCode() : 0 );
		return hash;
	}
	
	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(" + this.getName() + ")";
	}
	
	public boolean explainExpression( String expression )
	{
		if ( expression == null || expression.isEmpty() )
		{
			return false;
		}
		
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
}
