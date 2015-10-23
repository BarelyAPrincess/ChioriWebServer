/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.LogColor;
import com.chiorichan.event.EventBus;
import com.chiorichan.permission.event.PermissibleEntityEvent;
import com.chiorichan.permission.lang.PermissionException;
import com.chiorichan.tasks.Timings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class PermissibleEntity
{
	private Map<String, PermissionResult> cachedResults = Maps.newConcurrentMap();
	
	private Map<ChildPermission, References> permissions = Maps.newConcurrentMap();
	private Map<ChildPermission, TimedReferences> timedPermissions = Maps.newConcurrentMap();
	private Map<PermissibleGroup, References> groups = Maps.newConcurrentMap();
	private Map<PermissibleGroup, TimedReferences> timedGroups = Maps.newConcurrentMap();
	
	protected boolean debugMode = false;
	private String id;
	private boolean virtual = false;
	
	public PermissibleEntity( String id )
	{
		if ( PermissionManager.isDebug() )
			PermissionManager.getLogger().info( String.format( "%sThe %s `%s` has been created.", LogColor.YELLOW, isGroup() ? "group" : "entity", id ) );
		
		this.id = id;
		reload();
	}
	
	public void addGroup( PermissibleGroup group, References refs )
	{
		addGroup0( group, refs );
		recalculatePermissions();
	}
	
	protected void addGroup0( PermissibleGroup group, References refs )
	{
		References ref = groups.get( group );
		if ( ref == null )
			ref = refs;
		else
			ref.add( refs );
		groups.put( group, ref );
		removeTimedGroup( group, ref );
		
		if ( isDebug() )
			PermissionManager.getLogger().info( String.format( "%sThe group `%s` with reference `%s` was attached to entity `%s`.", LogColor.YELLOW, group.getId(), refs.join(), getId() ) );
	}
	
	protected final void addPermission( ChildPermission perm, References refs )
	{
		Validate.notNull( perm );
		
		if ( refs == null )
			refs = References.format();
		References oldRefs = getPermissionReferences( perm.getPermission() );
		
		if ( oldRefs != null )
			refs.add( oldRefs );
		permissions.put( perm, refs );
		
		if ( isDebug() )
			PermissionManager.getLogger().info( String.format( "%sThe permission `%s` with reference `%s` was attached to entity `%s`.", LogColor.YELLOW, perm.getPermission().getNamespace(), refs.join(), getId() ) );
		
		recalculatePermissions();
	}
	
	public void addPermission( Permission perm, Object val, References refs )
	{
		addPermission( new ChildPermission( this, perm, perm.getModel().createValue( val ), isGroup() ? ( ( PermissibleGroup ) this ).getWeight() : -1 ), refs );
	}
	
	public void addPermission( String node, Object val, References refs )
	{
		Permission perm = PermissionManager.INSTANCE.getNode( node );
		if ( perm == null )
			throw new PermissionException( String.format( "The permission node %s is non-existent, you must create it first.", node ) );
		addPermission( perm, val, refs );
	}
	
	public void addTimedGroup( PermissibleGroup group, int lifetime, References refs )
	{
		if ( refs == null )
			refs = References.format();
		timedGroups.put( group, new TimedReferences( lifetime ).add( refs ) );
	}
	
	protected final void addTimedPermission( ChildPermission perm, TimedReferences refs )
	{
		permissions.put( perm, refs );
		if ( isDebug() )
			PermissionManager.getLogger().info( String.format( "%sThe permission `%s` with reference `%s` was attached to entity `%s`.", LogColor.YELLOW, perm.getPermission().getNamespace(), refs.toString(), getId() ) );
		recalculatePermissions();
	}
	
	/**
	 * Adds timed permission with specified references and a lifetime to live
	 * 
	 * @param permission
	 *            The Permission Node
	 * @param refs
	 *            The References
	 * @param lifeTime
	 *            Lifetime of permission in seconds. 0 for transient permission (reference disappear only after server reload)
	 */
	public void addTimedPermission( final Permission perm, Object val, References refs, int lifeTime )
	{
		addTimedPermission( new ChildPermission( this, perm, perm.getModel().createValue( val ), isGroup() ? ( ( PermissibleGroup ) this ).getWeight() : -1 ), new TimedReferences( lifeTime ).add( refs ) );
	}
	
	public void addTimedPermission( String perm, Object val, References refs, int lifeTime )
	{
		addTimedPermission( PermissionManager.INSTANCE.createNode( perm ), val, refs, lifeTime );
	}
	
	public PermissionResult checkPermission( Permission perm )
	{
		return checkPermission( perm, References.format( "" ) );
	}
	
	public PermissionResult checkPermission( Permission perm, References refs )
	{
		/**
		 * We cache the results to reduce lag when a permission is checked multiple times over.
		 */
		PermissionResult result = cachedResults.get( perm.getNamespace() + "-" + refs.hash() );
		
		if ( result != null )
			if ( result.timecode > Timings.epoch() - 600 ) // 600 Seconds = 10 Minutes
				return result;
			else
				cachedResults.remove( perm.getNamespace() + "-" + refs.hash() );
		
		result = new PermissionResult( this, perm, refs );
		
		cachedResults.put( perm.getNamespace() + "-" + refs.hash(), result );
		
		if ( isDebug() && !perm.getNamespace().equalsIgnoreCase( PermissionDefault.OP.getNameSpace() ) )
			PermissionManager.getLogger().info( LogColor.YELLOW + "Entity `" + getId() + "` checked for permission `" + perm.getNamespace() + "`" + ( ( refs.isEmpty() ) ? "" : " with reference `" + refs.toString() + "`" ) + " with result `" + result + "`" );
		
		return result;
	}
	
	public PermissionResult checkPermission( String perm )
	{
		return checkPermission( perm, References.format( "" ) );
	}
	
	public PermissionResult checkPermission( String perm, References ref )
	{
		perm = PermissionManager.parseNode( perm );
		Permission permission = PermissionManager.INSTANCE.createNode( perm );
		PermissionResult result = checkPermission( permission, ref );
		
		return result;
	}
	
	protected void clearGroups()
	{
		groups.clear();
		recalculatePermissions();
	}
	
	protected void clearPermissions()
	{
		permissions.clear();
		recalculatePermissions();
	}
	
	protected void clearTimedGroups()
	{
		timedGroups.clear();
		recalculatePermissions();
	}
	
	protected void clearTimedPermissions()
	{
		timedPermissions.clear();
		recalculatePermissions();
	}
	
	public PermissibleGroup demote( PermissibleEntity demoter, String string )
	{
		return null;// TODO Auto-generated method stub
	}
	
	public boolean explainExpression( String expression )
	{
		if ( expression == null || expression.isEmpty() )
			return false;
		
		return !expression.startsWith( "-" ); // If expression have - (minus) before then that mean expression are negative
	}
	
	protected ChildPermission getChildPermission( Permission perm, References refs )
	{
		ChildPermission result = null;
		for ( ChildPermission child : getChildPermissions( refs ) )
			if ( child.getPermission() == perm )
			{
				result = child;
				break;
			}
		// Loader.getLogger().debug( "Get PermissionChild on " + ( isGroup() ? "group" : "entity" ) + " " + getId() + " with result " + ( result != null ) );
		return result;
	}
	
	protected Entry<ChildPermission, References> getChildPermissionEntry( Permission perm, References refs )
	{
		for ( Entry<ChildPermission, References> entry : permissions.entrySet() )
			if ( entry.getKey().getPermission() == perm && entry.getValue().match( refs ) )
				return entry;
		return null;
	}
	
	protected Entry<ChildPermission, References> getChildPermissionEntry( References refs )
	{
		for ( Entry<ChildPermission, References> entry : permissions.entrySet() )
			if ( entry.getValue().match( refs ) )
				return entry;
		return null;
	}
	
	/**
	 * Check it's self and each {@link PermissibleEntity} group until it finds the {@link ChildPermission} associated with {@link Permission}
	 * 
	 * @param perm
	 *            The {@link Permission} we associate with
	 * @param refs
	 *            Reference to be looking for
	 * @return The resulting {@link ChildPermission}
	 */
	protected ChildPermission getChildPermissionRecursive( Permission perm, References refs )
	{
		/**
		 * Used as a constant tracker for already checked groups, prevents infinite looping.
		 * e.g., User -> Group1 -> Group2 -> Group3 -> Group1
		 */
		return getChildPermissionRecursive( new HashSet<PermissibleGroup>(), perm, refs );
	}
	
	protected ChildPermission getChildPermissionRecursive( Set<PermissibleGroup> stacker, Permission perm, References refs )
	{
		// First we try checking this PermissibleEntity
		ChildPermission result = getChildPermission( perm, refs );
		
		if ( result != null )
			return result;
		
		// Next we check each group recursively
		for ( PermissibleGroup group : getGroups( refs ) )
			if ( !stacker.contains( group ) )
			{
				stacker.add( group );
				result = group.getChildPermissionRecursive( stacker, perm, refs );
				if ( result != null )
					break;
			}
		
		return result;
	}
	
	protected Collection<ChildPermission> getChildPermissions( References refs )
	{
		Set<ChildPermission> result = Sets.newHashSet();
		for ( Entry<ChildPermission, References> entry : permissions.entrySet() )
			if ( entry.getValue().match( refs ) )
				result.add( entry.getKey() );
		return result;
	}
	
	public Collection<Entry<PermissibleGroup, References>> getGroupEntrys( References refs )
	{
		Set<Entry<PermissibleGroup, References>> result = Sets.newHashSet();
		for ( Entry<PermissibleGroup, References> entry : groups.entrySet() )
			if ( entry.getValue().match( refs ) )
				result.add( entry );
		return result;
	}
	
	public Collection<String> getGroupNames( References refs )
	{
		List<String> result = Lists.newArrayList();
		for ( PermissibleGroup group : getGroups( refs ) )
			result.add( group.getId() );
		return result;
	}
	
	public References getGroupReferences()
	{
		References refs = new References();
		for ( References ref : timedGroups.values() )
			refs.add( ref );
		for ( References ref : groups.values() )
			refs.add( ref );
		return refs;
	}
	
	public References getGroupReferences( PermissibleGroup group )
	{
		References refs = new References();
		for ( Entry<PermissibleGroup, References> entry : groups.entrySet() )
			if ( entry.getKey() == group )
				refs.add( entry.getValue() );
		for ( Entry<PermissibleGroup, TimedReferences> entry : timedGroups.entrySet() )
			if ( entry.getKey() == group )
				refs.add( entry.getValue() );
		return refs;
	}
	
	public final Collection<PermissibleGroup> getGroups( References refs )
	{
		Set<PermissibleGroup> result = Sets.newHashSet();
		for ( Entry<PermissibleGroup, References> entry : groups.entrySet() )
			if ( entry.getValue().match( refs ) )
				result.add( entry.getKey() );
		for ( Entry<PermissibleGroup, TimedReferences> entry : timedGroups.entrySet() )
			if ( entry.getValue().match( refs ) && !entry.getValue().isExpired() )
				result.add( entry.getKey() );
		return result;
	}
	
	/**
	 * Return id of permission entity (Entity or Group)
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
	
	public String getMatchingExpression( String permission, References refs )
	{
		return getMatchingExpression( getPermissions( refs ), permission );
	}
	
	public <T> T getOption( String key, References refs, T def )
	{
		return def;// TODO Auto-generated method stub
	}
	
	public Map<String, String> getOptions( References refs )
	{
		return Maps.newHashMap();// TODO Auto-generated method stub
	}
	
	public Collection<Entry<Permission, References>> getPermissionEntrys( References refs )
	{
		Set<Entry<Permission, References>> result = Sets.newHashSet();
		for ( Entry<ChildPermission, References> entry : permissions.entrySet() )
			if ( entry.getValue().match( refs ) )
				result.add( new SimpleEntry<Permission, References>( entry.getKey().getPermission(), entry.getValue() ) );
		return result;
	}
	
	public Collection<String> getPermissionNames( References refs )
	{
		List<String> result = Lists.newArrayList();
		for ( Permission perm : getPermissions( refs ) )
			result.add( perm.getNamespace() );
		return result;
	}
	
	public References getPermissionReferences()
	{
		References refs = new References();
		for ( References ref : timedPermissions.values() )
			refs.add( ref );
		for ( References ref : permissions.values() )
			refs.add( ref );
		return refs;
	}
	
	public References getPermissionReferences( Permission perm )
	{
		References refs = new References();
		for ( Entry<ChildPermission, References> entry : permissions.entrySet() )
			if ( entry.getKey().getPermission() == perm )
				refs.add( entry.getValue() );
		for ( Entry<ChildPermission, TimedReferences> entry : timedPermissions.entrySet() )
			if ( entry.getKey().getPermission() == perm )
				refs.add( entry.getValue() );
		return refs;
	}
	
	public Collection<Permission> getPermissions( References refs )
	{
		Set<Permission> result = Sets.newHashSet();
		for ( Entry<ChildPermission, References> entry : permissions.entrySet() )
			if ( entry.getValue().match( refs ) )
				result.add( entry.getKey().getPermission() );
		return result;
	}
	
	public Entry<Permission, PermissionValue> getPermissionValue( Permission perm, References refs )
	{
		for ( Entry<ChildPermission, References> entry : permissions.entrySet() )
			if ( entry.getKey().getPermission() == perm && entry.getValue().match( refs ) )
				return new SimpleEntry<Permission, PermissionValue>( entry.getKey().getPermission(), entry.getKey().getValue() );
		for ( Entry<ChildPermission, TimedReferences> entry : timedPermissions.entrySet() )
			if ( !entry.getValue().isExpired() && entry.getKey().getPermission() == perm && entry.getValue().match( refs ) )
				return new SimpleEntry<Permission, PermissionValue>( entry.getKey().getPermission(), entry.getKey().getValue() );
		return null;
	}
	
	public Collection<Entry<Permission, PermissionValue>> getPermissionValues( References refs )
	{
		Set<Entry<Permission, PermissionValue>> result = Sets.newHashSet();
		for ( Entry<ChildPermission, References> entry : permissions.entrySet() )
			if ( entry.getValue().match( refs ) )
				result.add( new SimpleEntry<Permission, PermissionValue>( entry.getKey().getPermission(), entry.getKey().getValue() ) );
		for ( Entry<ChildPermission, TimedReferences> entry : timedPermissions.entrySet() )
			if ( !entry.getValue().isExpired() && entry.getValue().match( refs ) )
				result.add( new SimpleEntry<Permission, PermissionValue>( entry.getKey().getPermission(), entry.getKey().getValue() ) );
		return result;
	}
	
	public String getPrefix()
	{
		return null;
	}
	
	public String getPrefix( References refs )
	{
		return null;// TODO Auto-generated method stub
	}
	
	public String getSuffix()
	{
		return null;
	}
	
	public String getSuffix( References refs )
	{
		return null;// TODO Auto-generated method stub
	}
	
	/**
	 * Returns remaining lifetime of specified permission in ref
	 * 
	 * @param permission
	 *            Name of permission
	 * @param ref
	 * @return remaining lifetime in seconds of timed permission. 0 if permission is transient
	 */
	public int getTimedPermissionLifetime( Permission perm, References refs )
	{
		for ( Entry<ChildPermission, TimedReferences> entry : timedPermissions.entrySet() )
			if ( entry.getValue().match( refs ) && entry.getKey().getPermission() == perm )
				return Timings.epoch() - entry.getValue().lifeTime;
		return -1;
	}
	
	public Collection<Permission> getTimedPermissions()
	{
		return getTimedPermissions( null );
	}
	
	/**
	 * Return entity timed (temporary) permission
	 * 
	 * @param ref
	 *            The Reference to check
	 * @return Collection of timed permissions
	 */
	public Collection<Permission> getTimedPermissions( References refs )
	{
		Set<Permission> result = Sets.newHashSet();
		for ( Entry<ChildPermission, TimedReferences> entry : timedPermissions.entrySet() )
			if ( entry.getValue().match( refs ) && !entry.getValue().isExpired() )
				result.add( entry.getKey().getPermission() );
		return result;
	}
	
	public boolean hasGroup( PermissibleGroup group )
	{
		return groups.containsKey( group );
	}
	
	public boolean hasTimedGroup( PermissibleGroup group )
	{
		return timedGroups.containsKey( group );
	}
	
	public boolean isAdmin()
	{
		PermissionResult result = checkPermission( PermissionDefault.ADMIN.getNode() );
		return result.isTrue();
	}
	
	public boolean isAdminOnly()
	{
		PermissionResult result = checkPermission( PermissionDefault.ADMIN.getNode() );
		return result.isTrue( false );
	}
	
	public boolean isBanned()
	{
		// You can't ban an OP entity
		if ( PermissionManager.allowOps && isOp() )
			return false;
		
		PermissionResult result = checkPermission( PermissionDefault.BANNED.getNode() );
		return result.isTrue();
	}
	
	public boolean isCommitted()
	{
		// XXX Future, was it committed to backend?
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
		return result.isTrue( false );
	}
	
	public final boolean isVirtual()
	{
		return virtual;
	}
	
	public boolean isWhitelisted()
	{
		if ( !PermissionManager.INSTANCE.hasWhitelist() || ( PermissionManager.allowOps && isOp() ) )
			return true;
		
		PermissionResult result = checkPermission( PermissionDefault.WHITELISTED.getNode() );
		return result.isTrue();
	}
	
	public PermissibleGroup promote( PermissibleEntity promoter, String ladder )
	{
		return null;
	}
	
	public void recalculatePermissions()
	{
		for ( Entry<PermissibleGroup, TimedReferences> entry : timedGroups.entrySet() )
			if ( entry.getValue().isExpired() )
			{
				timedGroups.remove( entry.getKey() );
				EventBus.INSTANCE.callEvent( new PermissibleEntityEvent( this, PermissibleEntityEvent.Action.TIMEDGROUP_EXPIRED ) );
			}
		for ( Entry<ChildPermission, TimedReferences> entry : timedPermissions.entrySet() )
			if ( entry.getValue().isExpired() )
			{
				timedPermissions.remove( entry.getKey() );
				EventBus.INSTANCE.callEvent( new PermissibleEntityEvent( this, PermissibleEntityEvent.Action.TIMEDPERMISSION_EXPIRED ) );
			}
		for ( PermissionResult cache : cachedResults.values() )
			cache.recalculatePermissions();
		EventBus.INSTANCE.callEvent( new PermissibleEntityEvent( this, PermissibleEntityEvent.Action.PERMISSIONS_CHANGED ) );
	}
	
	public void reload()
	{
		reloadGroups();
		reloadPermissions();
	}
	
	/**
	 * Reload entity group references from backend
	 */
	public abstract void reloadGroups();
	
	/**
	 * Reload entity permissions from backend
	 */
	public abstract void reloadPermissions();
	
	/**
	 * Remove entity permission and group references from backend
	 */
	public abstract void remove();
	
	public final void removeAllPermissions()
	{
		permissions.clear();
		recalculatePermissions();
	}
	
	public void removeGroup( PermissibleGroup group, References refs )
	{
		References current = groups.get( group );
		if ( current == null )
			return;
		current.remove( refs );
		if ( current.isEmpty() )
			groups.remove( group );
		recalculatePermissions();
	}
	
	public void removeGroup( String group, References refs )
	{
		removeGroup( PermissionManager.INSTANCE.getGroup( group ), refs );
	}
	
	public final void removePermission( Permission perm, References refs )
	{
		for ( Entry<ChildPermission, References> entry : permissions.entrySet() )
			if ( entry.getKey().getPermission() == perm && entry.getValue().match( refs ) )
				permissions.remove( perm );
		recalculatePermissions();
	}
	
	public void removePermission( String permission, References refs )
	{
		removePermission( PermissionManager.INSTANCE.getNode( permission ), refs );
	}
	
	/**
	 * Removes specified timed permission for ref
	 * 
	 * @param permission
	 * @param ref
	 */
	public void removeTimedGroup( PermissibleGroup group, References refs )
	{
		References current = timedGroups.get( group );
		if ( current == null )
			return;
		current.remove( refs );
		if ( current.isEmpty() )
			timedGroups.remove( group );
		recalculatePermissions();
	}
	
	public void removeTimedPermission( Permission perm, References refs )
	{
		for ( Entry<ChildPermission, TimedReferences> entry : timedPermissions.entrySet() )
			if ( entry.getKey().getPermission() == perm && entry.getValue().match( refs ) )
				timedPermissions.remove( entry.getKey() );
	}
	
	public void removeTimedPermission( String perm, References refs )
	{
		removeTimedPermission( PermissionManager.INSTANCE.getNode( perm ), refs );
	}
	
	/**
	 * Save entity data to backend
	 */
	public abstract void save();
	
	public void setDebug( boolean debug )
	{
		debugMode = debug;
	}
	
	public void setGroups( Collection<PermissibleGroup> groups, References refs )
	{
		setGroups0( groups, refs );
		recalculatePermissions();
	}
	
	protected void setGroups0( Collection<PermissibleGroup> groups, References refs )
	{
		clearGroups();
		for ( PermissibleGroup group : groups )
			addGroup0( group, refs );
	}
	
	public void setOption( String key, String value, References ref )
	{
		// TODO Auto-generated method stub
	}
	
	public void setPrefix( String prefix )
	{
		// TODO Auto-generated method stub
	}
	
	public void setPrefix( String prefix, References ref )
	{
		// TODO Auto-generated method stub
	}
	
	public void setSuffix( String string, References refs )
	{
		// TODO Auto-generated method stub
	}
	
	public void setVirtual( boolean virtual )
	{
		this.virtual = virtual;
	}
	
	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "{" + getId() + "}";
	}
}
