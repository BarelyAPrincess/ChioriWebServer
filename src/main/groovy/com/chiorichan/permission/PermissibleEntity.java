/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimerTask;

import com.chiorichan.ConsoleColor;
import com.chiorichan.event.EventBus;
import com.chiorichan.permission.event.PermissibleEntityEvent;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.StringFunc;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
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
	
	private Map<String, PermissionResult> cachedResults = Maps.newConcurrentMap();
	
	protected List<ChildPermission> childPermissions = Lists.newArrayList();
	protected boolean debugMode = false;
	protected Map<String, PermissibleGroup> groups = Maps.newConcurrentMap();
	private String id;
	private boolean virtual = false;
	protected Map<String, LinkedList<TimedPermission>> timedPermissions = Maps.newConcurrentMap();
	
	public PermissibleEntity( String id )
	{
		this.id = id;
		reload();
	}
	
	public void addGroup( PermissibleGroup group, int lifetime, String... refs )
	{
		// TODO Timed Groups
		
		this.addGroup( group, refs );
	}
	
	public void addGroup( PermissibleGroup group, String... refs )
	{
		// TODO THIS!
		
		groups.put( group.getId(), group );
		recalculatePermissions();
	}
	
	public void addPermission( Permission perm, Object val, String ref )
	{
		attachPermission( new ChildPermission( perm, perm.getModel().createValue( val ), isGroup() ? ( ( PermissibleGroup ) this ).getWeight() : -1, ref ) );
	}
	
	public void addPermission( String node, Object val, String ref )
	{
		addPermission( PermissionManager.INSTANCE.getNode( node ), val, ref );
	}
	
	/**
	 * Adds timed permission to specified reference in seconds
	 * 
	 * @param permission
	 * @param ref
	 * @param lifeTime
	 *            Lifetime of permission in seconds. 0 for transient permission (reference disappear only after server reload)
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
		
		recalculatePermissions();
	}
	
	public void addTimedPermission( String perm, String ref, int lifeTime )
	{
		addTimedPermission( PermissionManager.INSTANCE.getNode( perm, true ), ref, lifeTime );
	}
	
	public final void attachPermission( ChildPermission perm )
	{
		if ( isDebug() )
			PermissionManager.getLogger().info( String.format( "%sThe permission `%s` with reference `%s` was attached to entity `%s`.", ConsoleColor.YELLOW, perm.getPermission().getNamespace(), Joiner.on( ", " ).join( perm.getReferences() ), getId() ) );
		childPermissions.add( perm );
		recalculatePermissions();
	}
	
	public PermissionResult checkPermission( Permission perm )
	{
		return checkPermission( perm, "" );
	}
	
	public PermissionResult checkPermission( Permission perm, String ref )
	{
		ref = StringFunc.formatReference( ref );
		
		/**
		 * We cache the results to reduce lag when a permission is checked multiple times over.
		 */
		PermissionResult result = cachedResults.get( perm.getNamespace() + "-" + ref );
		
		if ( result != null )
			if ( result.timecode > Timings.epoch() - 600 ) // 600 Seconds = 10 Minutes
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
		
		perm = PermissionManager.parseNode( perm );
		Permission permission = PermissionManager.INSTANCE.getNode( perm, true );
		result = checkPermission( permission, ref );
		
		return result;
	}
	
	public final void clearGroups()
	{
		groups.clear();
		recalculatePermissions();
	}
	
	public PermissibleGroup demote( PermissibleEntity demoter, String string )
	{
		return null;// TODO Auto-generated method stub
	}
	
	public final void detachAllPermissions()
	{
		childPermissions.clear();
		recalculatePermissions();
	}
	
	public final void detachPermission( ChildPermission perm, String... refs )
	{
		detachPermission( perm.getPermission().getNamespace(), refs );
	}
	
	public final void detachPermission( Permission perm, String... refs )
	{
		detachPermission( perm.getNamespace(), refs );
	}
	
	public final void detachPermission( String perm, String... refs )
	{
		refs = StringFunc.formatReference( refs );
		for ( ChildPermission child : childPermissions )
			if ( child.getPermission().getNamespace().equals( perm ) && ( ( refs.length == 0 && child.getReferences().size() == 0 ) || StringFunc.comparable( child.getReferences().toArray( new String[0] ), refs ) ) )
				childPermissions.remove( child );
		recalculatePermissions();
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
	
	private ChildPermission findChildPermission( List<PermissibleGroup> groups, Permission perm, String ref )
	{
		ref = StringFunc.formatReference( ref );
		
		// First we try checking this PermissibleEntity
		ChildPermission result = getChildPermission( perm, ref );
		
		if ( result != null )
			return result;
		
		// Next we check each group recursively
		for ( PermissibleGroup group : groups )
			if ( !groups.contains( group ) )
			{
				groups.add( group );
				result = findChildPermission( groups, perm, ref );
				if ( result != null )
					break;
			}
		
		return result;
	}
	
	/**
	 * Check it's self and each {@link PermissibleEntity} group until it finds the {@link ChildPermission} associated with {@link Permission}
	 * 
	 * @param perm
	 *            The {@link Permission} we associate with
	 * @return The resulting {@link ChildPermission}
	 */
	public ChildPermission findChildPermission( Permission perm )
	{
		return findChildPermission( perm, "" );
	}
	
	/**
	 * Check it's self and each {@link PermissibleEntity} group until it finds the {@link ChildPermission} associated with {@link Permission}
	 * 
	 * @param perm
	 *            The {@link Permission} we associate with
	 * @param ref
	 *            Reference string, e.g., site name
	 * @return The resulting {@link ChildPermission}
	 */
	public ChildPermission findChildPermission( Permission perm, String ref )
	{
		/**
		 * Used as a constant tracker for already checked groups, prevents infinite looping.
		 * e.g., User -> Group1 -> Group2 -> Group3 -> Group1
		 */
		return findChildPermission( new ArrayList<PermissibleGroup>(), perm, ref );
	}
	
	protected ChildPermission getChildPermission( Permission perm )
	{
		return getChildPermission( perm, "" );
	}
	
	protected ChildPermission getChildPermission( Permission perm, String ref )
	{
		ref = StringFunc.formatReference( ref );
		for ( ChildPermission child : childPermissions )
			if ( perm == child.getPermission() && ( ( child.getReferences().isEmpty() && ref.isEmpty() ) || child.getReferences().contains( ref ) ) )
				return child;
		return null;
	}
	
	public final Collection<ChildPermission> getChildPermissions( String... refs )
	{
		if ( refs.length == 0 )
		{
			Collections.sort( childPermissions );
			return childPermissions;
		}
		
		List<ChildPermission> children = Lists.newLinkedList();
		refs = StringFunc.formatReference( refs );
		for ( ChildPermission child : childPermissions )
			if ( ( child.getReferences().isEmpty() && refs.length == 0 ) || StringFunc.comparable( refs, child.getReferences().toArray( new String[0] ) ) )
				children.add( child );
		Collections.sort( children );
		return children;
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
	
	private String getMatchingExpression( Collection<Permission> permissions, String permission )
	{
		for ( Permission exp : permissions )
			if ( PermissionManager.INSTANCE.getMatcher().isMatches( exp, permission ) )
				return exp.getNamespace();
		return null;
	}
	
	public String getMatchingExpression( String permission, String ref )
	{
		return getMatchingExpression( getPermissions( ref ), permission );
	}
	
	public <T> T getOption( String key, String ref, T def )
	{
		return def;// TODO Auto-generated method stub
	}
	
	public Map<String, String> getOptions( String ref )
	{
		return Maps.newHashMap();// TODO Auto-generated method stub
	}
	
	public Collection<String> getParentGroupNames( String... refs )
	{
		List<String> result = Lists.newArrayList();
		for ( PermissibleGroup group : getParentGroups( refs ) )
			result.add( group.getId() );
		return result;
	}
	
	public final Collection<PermissibleGroup> getParentGroups( String... refs )
	{
		refs = StringFunc.formatReference( refs );
		if ( refs.length == 0 )
			return Collections.unmodifiableCollection( groups.values() );
		Set<PermissibleGroup> result = Sets.newHashSet();
		for ( Entry<String, PermissibleGroup> group : groups.entrySet() )
			for ( String ref : refs )
				if ( group.getKey().equalsIgnoreCase( ref ) )
				{
					result.add( group.getValue() );
					break;
				}
		return result;
	}
	
	public Collection<String> getPermissionNodes( String... refs )
	{
		List<String> result = Lists.newArrayList();
		for ( Permission p : getPermissions( refs ) )
			result.add( p.getNamespace() );
		return result;
	}
	
	public Collection<Permission> getPermissions( String... refs )
	{
		List<Permission> perms = Lists.newArrayList();
		for ( ChildPermission child : getChildPermissions( refs ) )
			perms.add( child.getPermission() );
		return perms;
	}
	
	public String getPrefix()
	{
		return null;
	}
	
	public String getPrefix( String ref )
	{
		return null;// TODO Auto-generated method stub
	}
	
	public Collection<String> getReferences()
	{
		return groups.keySet();
	}
	
	public String getSuffix()
	{
		return null;
	}
	
	public String getSuffix( String ref )
	{
		return null;// TODO Auto-generated method stub
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
	
	public boolean isCommitted()
	{
		// Future, was it committed to backend?
		return true;
	}
	
	public boolean isDebug()
	{
		return debugMode || PermissionManager.isDebug();
	}
	
	public final boolean isGroup()
	{
		return this instanceof PermissibleGroup;
	}
	
	public boolean isOp()
	{
		PermissionResult result = checkPermission( PermissionDefault.OP.getNode() );
		return result.isTrue();
	}
	
	public final boolean isVirtual()
	{
		return virtual;
	}
	
	public boolean isWhitelisted()
	{
		if ( !PermissionManager.INSTANCE.hasWhitelist() )
			return true;
		
		PermissionResult result = checkPermission( PermissionDefault.WHITELISTED.getNode() );
		return result.isTrue();
	}
	
	public PermissibleGroup promote( PermissibleEntity promoter, String ladder )
	{
		return null;// TODO Auto-generated method stub
	}
	
	public void recalculatePermissions()
	{
		for ( PermissionResult cache : cachedResults.values() )
			cache.recalculatePermissions();
		EventBus.INSTANCE.callEvent( new PermissibleEntityEvent( this, PermissibleEntityEvent.Action.PERMISSIONS_CHANGED ) );
	}
	
	public void reload()
	{
		reloadPermissions();
		reloadGroups();
	}
	
	public abstract void reloadGroups();
	
	public abstract void reloadPermissions();
	
	/**
	 * Remove entity data from backend
	 */
	public abstract void remove();
	
	public void removeGroup( String groupName, String ref )
	{
		// TODO THIS!
		
		groups.remove( groupName );
		recalculatePermissions();
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
			if ( tp.permission == perm )
				timedPermissions.get( ref ).remove( tp );
		
		recalculatePermissions();
	}
	
	public void removeTimedPermission( String perm, String ref )
	{
		removeTimedPermission( PermissionManager.INSTANCE.getNode( perm, true ), ref );
	}
	
	/**
	 * Save entity data to backend
	 */
	public abstract void save();
	
	public void setDebug( boolean debug )
	{
		debugMode = debug;
	}
	
	public void setOption( String key, String value, String ref )
	{
		// TODO Auto-generated method stub
	}
	
	// TODO Store groups by reference
	public void setParentGroups( Collection<PermissibleGroup> groups, String ref )
	{
		for ( PermissibleGroup group : groups )
			this.groups.put( group.getId(), group );
		recalculatePermissions();
	}
	
	public void setPrefix( String prefix )
	{
		// TODO Auto-generated method stub
	}
	
	public void setPrefix( String prefix, String ref )
	{
		// TODO Auto-generated method stub
	}
	
	public void setSuffix( String string, String ref )
	{
		// TODO Auto-generated method stub
	}
	
	void setVirtual( boolean virtual )
	{
		this.virtual = virtual;
	}
	
	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(" + getId() + ")";
	}
}
