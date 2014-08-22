/*
 * PermissionsEx - Permissions plugin for Loader
 * Copyright (C) 2011 t3hk0d3 http://www.tehkode.ru
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package ru.tehkode.permissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.permissions.exceptions.RankingException;

import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.permissions.Permission;

/**
 * @author code
 */
public abstract class PermissionUser extends PermissionEntity
{
	
	private final static String PERMISSION_NOT_FOUND = "<not found>"; // used replace null for ConcurrentHashMap
	
	protected Map<String, List<PermissionGroup>> cachedGroups = new HashMap<String, List<PermissionGroup>>();
	protected Map<String, String[]> cachedPermissions = new HashMap<String, String[]>();
	protected Map<String, String> cachedPrefix = new HashMap<String, String>();
	protected Map<String, String> cachedSuffix = new HashMap<String, String>();
	protected Map<String, String> cachedAnwsers = new ConcurrentHashMap<String, String>();
	protected Map<String, String> cachedOptions = new HashMap<String, String>();
	
	public PermissionUser(String userName, PermissionManager manager)
	{
		super( userName, manager );
	}
	
	@Override
	public void initialize()
	{
		super.initialize();
		
		if ( this.manager.getBackend().isCreateUserRecords() && this.isVirtual() )
		{
			this.setGroups( this.getGroups( null ), null );
			
			this.save();
		}
		
		if ( this.isDebug() )
		{
			Logger.getLogger( "" ).info( "[PermissionsEx] User " + this.getName() + " initialized" );
		}
	}
	
	/**
	 * Return non-inherited user prefix.
	 * This means if a user don't have has own prefix
	 * then empty string or null would be returned
	 * 
	 * @return prefix as string
	 */
	public String getOwnPrefix()
	{
		return this.getOwnPrefix( null );
	}
	
	public abstract String getOwnPrefix( String siteName );
	
	/**
	 * Return non-inherited suffix prefix.
	 * This means if a user don't has own suffix
	 * then empty string or null would be returned
	 * 
	 * @return suffix as string
	 */
	public final String getOwnSuffix()
	{
		return this.getOwnSuffix( null );
	}
	
	public abstract String getOwnSuffix( String siteName );
	
	/**
	 * Return non-inherited permissions of a user in site
	 * 
	 * @param site site's name
	 * @return String array of owned Permissions
	 */
	public abstract String[] getOwnPermissions( String site );
	
	@Override
	public String getOption( String optionName, String siteName, String defaultValue )
	{
		String cacheIndex = siteName + "|" + optionName;
		
		if ( this.cachedOptions.containsKey( cacheIndex ) )
		{
			return this.cachedOptions.get( cacheIndex );
		}
		
		String value = this.getOwnOption( optionName, siteName, null );
		if ( value != null )
		{
			this.cachedOptions.put( cacheIndex, value );
			return value;
		}
		
		if ( siteName != null )
		{ // site inheritance
			for ( String site : manager.getSiteInheritance( siteName ) )
			{
				value = this.getOption( optionName, site, null );
				if ( value != null )
				{
					this.cachedOptions.put( cacheIndex, value );
					return value;
				}
			}
			
			// Check common space
			value = this.getOption( optionName, null, null );
			if ( value != null )
			{
				this.cachedOptions.put( cacheIndex, value );
				return value;
			}
		}
		
		// Inheritance
		for ( PermissionGroup group : this.getGroups( siteName ) )
		{
			value = group.getOption( optionName, siteName, null );
			if ( value != null )
			{
				this.cachedOptions.put( cacheIndex, value ); // put into cache inherited value
				return value;
			}
		}
		
		// Nothing found
		return defaultValue;
	}
	
	/**
	 * Return non-inherited value of specified option for user in site
	 * 
	 * @param option option string
	 * @param site site's name
	 * @param defaultValue default value
	 * @return option value or defaultValue if option is not set
	 */
	public abstract String getOwnOption( String option, String site, String defaultValue );
	
	/**
	 * Return non-inherited value of specified option in common space (all sites).
	 * 
	 * @param option
	 * @return option value or empty string if option is not set
	 */
	public String getOwnOption( String option )
	{
		return this.getOwnOption( option, null, null );
	}
	
	public String getOwnOption( String option, String site )
	{
		return this.getOwnOption( option, site, null );
	}
	
	public int getOwnOptionInteger( String optionName, String site, int defaultValue )
	{
		String option = this.getOwnOption( optionName, site, Integer.toString( defaultValue ) );
		
		try
		{
			return Integer.parseInt( option );
		}
		catch ( NumberFormatException e )
		{}
		
		return defaultValue;
	}
	
	public boolean getOwnOptionBoolean( String optionName, String site, boolean defaultValue )
	{
		String option = this.getOwnOption( optionName, site, Boolean.toString( defaultValue ) );
		
		if ( "false".equalsIgnoreCase( option ) )
		{
			return false;
		}
		else if ( "true".equalsIgnoreCase( option ) )
		{
			return true;
		}
		
		return defaultValue;
	}
	
	public double getOwnOptionDouble( String optionName, String site, double defaultValue )
	{
		String option = this.getOwnOption( optionName, site, Double.toString( defaultValue ) );
		
		try
		{
			return Double.parseDouble( option );
		}
		catch ( NumberFormatException e )
		{}
		
		return defaultValue;
	}
	
	protected abstract String[] getGroupsNamesImpl( String siteName );
	
	/**
	 * Get group for this user, global inheritance only
	 * 
	 * @return
	 */
	public PermissionGroup[] getGroups()
	{
		return this.getGroups( null );
	}
	
	/**
	 * Get groups for this user for specified site
	 * 
	 * @param siteName Name of site
	 * @return PermissionGroup groups
	 */
	public PermissionGroup[] getGroups( String siteName )
	{
		if ( !this.cachedGroups.containsKey( siteName ) )
		{
			this.cachedGroups.put( siteName, this.getGroups( siteName, this.manager.getDefaultGroup( siteName ) ) );
		}
		
		return this.cachedGroups.get( siteName ).toArray( new PermissionGroup[0] );
	}
	
	private List<PermissionGroup> getGroups( String siteName, PermissionGroup fallback )
	{
		List<PermissionGroup> groups = new LinkedList<PermissionGroup>();
		
		for ( String groupName : this.getGroupsNamesImpl( siteName ) )
		{
			if ( groupName == null || groupName.isEmpty() )
			{
				continue;
			}
			
			PermissionGroup group = this.manager.getGroup( groupName );
			
			if ( !this.checkMembership( group, siteName ) )
			{
				continue;
			}
			
			if ( !groups.contains( group ) )
			{
				groups.add( group );
			}
		}
		
		if ( siteName != null )
		{ // also check site-inheritance
			// site inheritance
			for ( String site : this.manager.getSiteInheritance( siteName ) )
			{
				groups.addAll( this.getGroups( site, null ) );
			}
			
			// common groups
			groups.addAll( this.getGroups( null, null ) );
		}
		
		if ( groups.isEmpty() && fallback != null )
		{
			groups.add( fallback );
		}
		
		if ( groups.size() > 1 )
		{
			Collections.sort( groups );
		}
		
		return groups;
	}
	
	public Map<String, PermissionGroup[]> getAllGroups()
	{
		Map<String, PermissionGroup[]> allGroups = new HashMap<String, PermissionGroup[]>();
		
		for ( String siteName : this.getSites() )
		{
			allGroups.put( siteName, this.getSiteGroups( siteName ) );
		}
		
		allGroups.put( null, this.getSiteGroups( null ) );
		
		return allGroups;
	}
	
	protected PermissionGroup[] getSiteGroups( String siteName )
	{
		List<PermissionGroup> groups = new LinkedList<PermissionGroup>();
		
		for ( String groupName : this.getGroupsNamesImpl( siteName ) )
		{
			if ( groupName == null || groupName.isEmpty() )
			{
				continue;
			}
			
			PermissionGroup group = this.manager.getGroup( groupName );
			
			if ( !groups.contains( group ) )
			{
				groups.add( group );
			}
		}
		
		Collections.sort( groups );
		
		return groups.toArray( new PermissionGroup[0] );
	}
	
	/**
	 * Get group names, common space only
	 * 
	 * @return
	 */
	public String[] getGroupsNames()
	{
		return this.getGroupsNames( null );
	}
	
	/**
	 * Get group names in specified site
	 * 
	 * @return String array of user's group names
	 */
	public String[] getGroupsNames( String siteName )
	{
		List<String> groups = new LinkedList<String>();
		for ( PermissionGroup group : this.getGroups( siteName ) )
		{
			if ( group != null )
			{
				groups.add( group.getName() );
			}
		}
		
		return groups.toArray( new String[0] );
	}
	
	/**
	 * Set parent groups for user
	 * 
	 * @param groups array of parent group names
	 */
	public abstract void setGroups( String[] groups, String siteName );
	
	public void setGroups( String[] groups )
	{
		this.setGroups( groups, null );
	}
	
	/**
	 * Set parent groups for user
	 * 
	 * @param groups array of parent group objects
	 */
	public void setGroups( PermissionGroup[] parentGroups, String siteName )
	{
		List<String> groups = new LinkedList<String>();
		
		for ( PermissionGroup group : parentGroups )
		{
			groups.add( group.getName() );
		}
		
		this.setGroups( groups.toArray( new String[0] ), siteName );
	}
	
	public void setGroups( PermissionGroup[] parentGroups )
	{
		this.setGroups( parentGroups, null );
	}
	
	/**
	 * Add user to group
	 * 
	 * @param groupName group's name as String
	 */
	public void addGroup( String groupName, String siteName )
	{
		if ( groupName == null || groupName.isEmpty() )
		{
			return;
		}
		
		List<String> groups = new ArrayList<String>( Arrays.asList( this.getGroupsNamesImpl( siteName ) ) );
		
		if ( groups.contains( groupName ) )
		{
			return;
		}
		
		if ( this.manager.userAddGroupsLast )
		{
			groups.add( groupName );
		}
		else
		{
			groups.add( 0, groupName ); // add group to start of list
		}
		
		this.setGroups( groups.toArray( new String[0] ), siteName );
	}
	
	public void addGroup( String groupName )
	{
		this.addGroup( groupName, null );
	}
	
	/**
	 * Add user to group
	 * 
	 * @param group as PermissionGroup object
	 */
	public void addGroup( PermissionGroup group, String siteName )
	{
		if ( group == null )
		{
			return;
		}
		
		this.addGroup( group.getName(), siteName );
	}
	
	public void addGroup( PermissionGroup group )
	{
		this.addGroup( group, null );
	}
	
	public void addGroup( String groupName, String siteName, long lifetime )
	{
		this.addGroup( groupName, siteName );
		
		if ( lifetime > 0 )
		{
			this.setOption( "group-" + groupName + "-until", Long.toString( System.currentTimeMillis() / 1000 + lifetime ), siteName );
		}
	}
	
	/**
	 * Remove user from group
	 * 
	 * @param groupName group's name as String
	 */
	public void removeGroup( String groupName, String siteName )
	{
		if ( groupName == null || groupName.isEmpty() )
		{
			return;
		}
		
		List<String> groups = new ArrayList<String>( Arrays.asList( this.getGroupsNamesImpl( siteName ) ) );
		
		if ( !groups.contains( groupName ) )
		{
			return;
		}
		
		groups.remove( groupName );
		
		this.setGroups( groups.toArray( new String[0] ), siteName );
	}
	
	public void removeGroup( String groupName )
	{
		this.removeGroup( this.manager.getGroup( groupName ) );
	}
	
	/**
	 * Remove user from group
	 * 
	 * @param group group as PermissionGroup object
	 */
	public void removeGroup( PermissionGroup group, String siteName )
	{
		if ( group == null )
		{
			return;
		}
		
		this.removeGroup( group.getName(), siteName );
	}
	
	public void removeGroup( PermissionGroup group )
	{
		for ( String siteName : this.getSites() )
		{
			this.removeGroup( group, siteName );
		}
		
		this.removeGroup( group, null );
	}
	
	/**
	 * Check if this user is member of group or one of its descendant groups (optionally)
	 * 
	 * @param group group as PermissionGroup object
	 * @param siteName
	 * @param checkInheritance if true then descendant groups of the given group would be checked too
	 * @return true on success, false otherwise
	 */
	public boolean inGroup( PermissionGroup group, String siteName, boolean checkInheritance )
	{
		for ( PermissionGroup parentGroup : this.getGroups( siteName ) )
		{
			if ( parentGroup.equals( group ) )
			{
				return true;
			}
			
			if ( checkInheritance && parentGroup.isChildOf( group, siteName, true ) )
			{
				return true;
			}
		}
		
		return false;
	}
	
	public boolean inGroup( PermissionGroup group, boolean checkInheritance )
	{
		for ( String siteName : this.getSites() )
		{
			if ( this.inGroup( group, siteName, checkInheritance ) )
			{
				return true;
			}
		}
		
		return this.inGroup( group, null, checkInheritance );
	}
	
	/**
	 * Check if this user is member of group or one of its descendant groups (optionally)
	 * 
	 * @param groupName group's name to check
	 * @param siteName
	 * @param checkInheritance if true than descendant groups of specified group would be checked too
	 * @return true on success, false otherwise
	 */
	public boolean inGroup( String groupName, String siteName, boolean checkInheritance )
	{
		return this.inGroup( this.manager.getGroup( groupName ), siteName, checkInheritance );
	}
	
	public boolean inGroup( String groupName, boolean checkInheritance )
	{
		return this.inGroup( this.manager.getGroup( groupName ), checkInheritance );
	}
	
	/**
	 * Check if this user is member of group or one of its descendant groups
	 * 
	 * @param group
	 * @param siteName
	 * @return true on success, false otherwise
	 */
	public boolean inGroup( PermissionGroup group, String siteName )
	{
		return this.inGroup( group, siteName, true );
	}
	
	public boolean inGroup( PermissionGroup group )
	{
		return this.inGroup( group, true );
	}
	
	/**
	 * Checks if this user is member of specified group or one of its descendant groups
	 * 
	 * @param group group's name
	 * @return true on success, false otherwise
	 */
	public boolean inGroup( String groupName, String siteName )
	{
		return this.inGroup( this.manager.getGroup( groupName ), siteName, true );
	}
	
	public boolean inGroup( String groupName )
	{
		return this.inGroup( groupName, true );
	}
	
	/**
	 * Promotes user in specified ladder.
	 * If user is not member of the ladder RankingException will be thrown
	 * If promoter is not null and he is member of the ladder and
	 * his rank is lower then user's RankingException will be thrown too.
	 * If there is no group to promote the user to RankingException would be thrown
	 * 
	 * @param promoter null if action is performed from console or by a plugin
	 * @param ladderName Ladder name
	 * @throws RankingException
	 */
	public PermissionGroup promote( PermissionUser promoter, String ladderName ) throws RankingException
	{
		if ( ladderName == null || ladderName.isEmpty() )
		{
			ladderName = "default";
		}
		
		int promoterRank = getPromoterRankAndCheck( promoter, ladderName );
		int rank = this.getRank( ladderName );
		
		PermissionGroup sourceGroup = this.getRankLadders().get( ladderName );
		PermissionGroup targetGroup = null;
		
		for ( Map.Entry<Integer, PermissionGroup> entry : this.manager.getRankLadder( ladderName ).entrySet() )
		{
			int groupRank = entry.getValue().getRank();
			if ( groupRank >= rank )
			{ // group have equal or lower than current rank
				continue;
			}
			
			if ( groupRank <= promoterRank )
			{ // group have higher rank than promoter
				continue;
			}
			
			if ( targetGroup != null && groupRank <= targetGroup.getRank() )
			{ // group have higher rank than target group
				continue;
			}
			
			targetGroup = entry.getValue();
		}
		
		if ( targetGroup == null )
		{
			throw new RankingException( "User are not promoteable", this, promoter );
		}
		
		this.swapGroups( sourceGroup, targetGroup );
		
		this.callEvent( PermissionEntityEvent.Action.RANK_CHANGED );
		
		return targetGroup;
	}
	
	/**
	 * Demotes user in specified ladder.
	 * If user is not member of the ladder RankingException will be thrown
	 * If demoter is not null and he is member of the ladder and
	 * his rank is lower then user's RankingException will be thrown too.
	 * If there is no group to demote the user to RankingException would be thrown
	 * 
	 * @param promoter Specify null if action performed from console or by plugin
	 * @param ladderName
	 * @throws RankingException
	 */
	public PermissionGroup demote( PermissionUser demoter, String ladderName ) throws RankingException
	{
		if ( ladderName == null || ladderName.isEmpty() )
		{
			ladderName = "default";
		}
		
		int promoterRank = getPromoterRankAndCheck( demoter, ladderName );
		int rank = this.getRank( ladderName );
		
		PermissionGroup sourceGroup = this.getRankLadders().get( ladderName );
		PermissionGroup targetGroup = null;
		
		for ( Map.Entry<Integer, PermissionGroup> entry : this.manager.getRankLadder( ladderName ).entrySet() )
		{
			int groupRank = entry.getValue().getRank();
			if ( groupRank <= rank )
			{ // group have equal or higher than current rank
				continue;
			}
			
			if ( groupRank <= promoterRank )
			{ // group have higher rank than promoter
				continue;
			}
			
			if ( targetGroup != null && groupRank >= targetGroup.getRank() )
			{ // group have lower rank than target group
				continue;
			}
			
			targetGroup = entry.getValue();
		}
		
		if ( targetGroup == null )
		{
			throw new RankingException( "User are not demoteable", this, demoter );
		}
		
		this.swapGroups( sourceGroup, targetGroup );
		
		this.callEvent( PermissionEntityEvent.Action.RANK_CHANGED );
		
		return targetGroup;
	}
	
	/**
	 * Check if the user is in the specified ladder
	 * 
	 * @param ladder Ladder name
	 * @return true on success, false otherwise
	 */
	public boolean isRanked( String ladder )
	{
		return ( this.getRank( ladder ) > 0 );
	}
	
	/**
	 * Return user rank in specified ladder
	 * 
	 * @param ladder Ladder name
	 * @return rank as int
	 */
	public int getRank( String ladder )
	{
		Map<String, PermissionGroup> ladders = this.getRankLadders();
		
		if ( ladders.containsKey( ladder ) )
		{
			return ladders.get( ladder ).getRank();
		}
		
		return 0;
	}
	
	/**
	 * Return user's group in specified ladder
	 * 
	 * @param ladder Ladder name
	 * @return PermissionGroup object of ranked ladder group
	 */
	public PermissionGroup getRankLadderGroup( String ladder )
	{
		if ( ladder == null || ladder.isEmpty() )
		{
			ladder = "default";
		}
		
		return this.getRankLadders().get( ladder );
	}
	
	/**
	 * Return all ladders the user is participating in
	 * 
	 * @return Map, key - name of ladder, group - corresponding group of that ladder
	 */
	public Map<String, PermissionGroup> getRankLadders()
	{
		Map<String, PermissionGroup> ladders = new HashMap<String, PermissionGroup>();
		
		for ( PermissionGroup group : this.getGroups() )
		{
			if ( !group.isRanked() )
			{
				continue;
			}
			
			ladders.put( group.getRankLadder(), group );
		}
		
		return ladders;
	}
	
	@Override
	public String[] getPermissions( String siteName )
	{
		if ( !this.cachedPermissions.containsKey( siteName ) )
		{
			List<String> permissions = new LinkedList<String>();
			this.getInheritedPermissions( siteName, permissions, true, false );
			
			this.cachedPermissions.put( siteName, permissions.toArray( new String[0] ) );
		}
		
		return this.cachedPermissions.get( siteName );
	}
	
	@Override
	public void addPermission( String permission, String siteName )
	{
		List<String> permissions = new LinkedList<String>( Arrays.asList( this.getOwnPermissions( siteName ) ) );
		
		if ( permissions.contains( permission ) )
		{ // remove old permission
			permissions.remove( permission );
		}
		
		// add permission on the top of list
		permissions.add( 0, permission );
		
		this.setPermissions( permissions.toArray( new String[0] ), siteName );
	}
	
	@Override
	public void removePermission( String permission, String siteName )
	{
		List<String> permissions = new LinkedList<String>( Arrays.asList( this.getOwnPermissions( siteName ) ) );
		
		permissions.remove( permission );
		
		this.setPermissions( permissions.toArray( new String[0] ), siteName );
	}
	
	protected void getInheritedPermissions( String siteName, List<String> permissions, boolean groupInheritance, boolean siteInheritance )
	{
		permissions.addAll( Arrays.asList( this.getTimedPermissions( siteName ) ) );
		permissions.addAll( Arrays.asList( this.getOwnPermissions( siteName ) ) );
		
		if ( siteName != null )
		{
			// Site inheritance
			for ( String parentSite : this.manager.getSiteInheritance( siteName ) )
			{
				getInheritedPermissions( parentSite, permissions, false, true );
			}
			
			// Common permissions
			if ( !siteInheritance )
			{ // skip common site permissions if we are inside site-inheritance tree
				getInheritedPermissions( null, permissions, false, true );
			}
		}
		
		// Group inhertance
		if ( groupInheritance )
		{
			for ( PermissionGroup parentGroup : this.getGroups( siteName ) )
			{
				parentGroup.getInheritedPermissions( siteName, permissions, true, false, new HashSet<PermissionGroup>() );
			}
		}
		
		// Add all child nodes
		for ( String node : permissions.toArray( new String[0] ) )
		{
			this.getInheritedChildPermissions( node, permissions );
		}
	}
	
	protected void getInheritedChildPermissions( String perm, List<String> list )
	{
		getInheritedChildPermissions( perm, list, false );
	}
	
	protected void getInheritedChildPermissions( String perm, List<String> list, boolean invert )
	{
		
		if ( perm.startsWith( "-" ) )
		{
			invert = !invert;
			perm = perm.substring( 1 );
		}
		getInheritedChildPermissions( Loader.getPermissionsManager().getPermission( perm ), list, invert );
	}
	
	protected void getInheritedChildPermissions( Permission perm, List<String> list, boolean invert )
	{
		if ( perm == null )
		{
			return;
		}
		for ( Map.Entry<String, Boolean> entry : perm.getChildren().entrySet() )
		{
			boolean has = entry.getValue().booleanValue() ^ invert;
			String node = ( has ? "" : "-" ) + entry.getKey();
			if ( !list.contains( node ) )
			{
				list.add( node );
				getInheritedChildPermissions( node, list, !has );
			}
		}
	}
	
	@Override
	public void addTimedPermission( String permission, String site, int lifeTime )
	{
		super.addTimedPermission( permission, site, lifeTime );
		
		this.clearCache();
	}
	
	@Override
	public void removeTimedPermission( String permission, String site )
	{
		super.removeTimedPermission( permission, site );
		
		this.clearCache();
	}
	
	protected int getPromoterRankAndCheck( PermissionUser promoter, String ladderName ) throws RankingException
	{
		if ( !this.isRanked( ladderName ) )
		{ // not ranked
			throw new RankingException( "User are not in this ladder", this, promoter );
		}
		
		int rank = this.getRank( ladderName );
		int promoterRank = 0;
		
		if ( promoter != null && promoter.isRanked( ladderName ) )
		{
			promoterRank = promoter.getRank( ladderName );
			
			if ( promoterRank >= rank )
			{
				throw new RankingException( "Promoter don't have high enough rank to change " + this.getName() + "'s rank", this, promoter );
			}
		}
		
		return promoterRank;
	}
	
	protected void swapGroups( PermissionGroup src, PermissionGroup dst )
	{
		List<PermissionGroup> groups = new ArrayList<PermissionGroup>( Arrays.asList( this.getGroups() ) );
		
		groups.remove( src );
		groups.add( dst );
		
		this.setGroups( groups.toArray( new PermissionGroup[0] ) );
	}
	
	@Override
	public String getPrefix( String siteName )
	{
		// @TODO This method should be refactored
		
		if ( !this.cachedPrefix.containsKey( siteName ) )
		{
			String localPrefix = this.getOwnPrefix( siteName );
			
			if ( siteName != null && ( localPrefix == null || localPrefix.isEmpty() ) )
			{
				// Site-inheritance
				for ( String parentSite : this.manager.getSiteInheritance( siteName ) )
				{
					String prefix = this.getOwnPrefix( parentSite );
					if ( prefix != null && !prefix.isEmpty() )
					{
						localPrefix = prefix;
						break;
					}
				}
				
				// Common space
				if ( localPrefix == null || localPrefix.isEmpty() )
				{
					localPrefix = this.getOwnPrefix( null );
				}
			}
			
			if ( localPrefix == null || localPrefix.isEmpty() )
			{
				for ( PermissionGroup group : this.getGroups( siteName ) )
				{
					localPrefix = group.getPrefix( siteName );
					if ( localPrefix != null && !localPrefix.isEmpty() )
					{
						break;
					}
				}
			}
			
			if ( localPrefix == null )
			{ // just for NPE safety
				localPrefix = "";
			}
			
			this.cachedPrefix.put( siteName, localPrefix );
		}
		
		return this.cachedPrefix.get( siteName );
	}
	
	@Override
	public boolean has( String permission )
	{
		Account user = Loader.getAccountsManager().getAccount( this.getName() );
		if ( user != null )
		{
			return this.has( permission, user.getSite().getName() );
		}
		
		return super.has( permission );
	}
	
	@Override
	public String getSuffix( String siteName )
	{
		// @TODO This method should be refactored
		if ( !this.cachedSuffix.containsKey( siteName ) )
		{
			String localSuffix = this.getOwnSuffix( siteName );
			
			if ( siteName != null && ( localSuffix == null || localSuffix.isEmpty() ) )
			{
				// Site-inheritance
				for ( String parentSite : this.manager.getSiteInheritance( siteName ) )
				{
					String suffix = this.getOwnSuffix( parentSite );
					if ( suffix != null && !suffix.isEmpty() )
					{
						localSuffix = suffix;
						break;
					}
				}
				
				// Common space
				if ( localSuffix == null || localSuffix.isEmpty() )
				{
					localSuffix = this.getOwnSuffix( null );
				}
			}
			
			if ( localSuffix == null || localSuffix.isEmpty() )
			{
				for ( PermissionGroup group : this.getGroups( siteName ) )
				{
					localSuffix = group.getSuffix( siteName );
					if ( localSuffix != null && !localSuffix.isEmpty() )
					{
						break;
					}
				}
			}
			
			if ( localSuffix == null )
			{ // just for NPE safety
				localSuffix = "";
			}
			this.cachedSuffix.put( siteName, localSuffix );
		}
		
		return this.cachedSuffix.get( siteName );
	}
	
	@Override
	public String getMatchingExpression( String permission, String site )
	{
		String cacheId = site + ":" + permission;
		if ( !this.cachedAnwsers.containsKey( cacheId ) )
		{
			String result = super.getMatchingExpression( permission, site );
			
			if ( result == null )
			{ // this is actually kinda dirty clutch
				result = PERMISSION_NOT_FOUND; // ConcurrentHashMap deny storage of null values
			}
			
			this.cachedAnwsers.put( cacheId, result );
		}
		
		String result = this.cachedAnwsers.get( cacheId );
		
		if ( PERMISSION_NOT_FOUND.equals( result ) )
		{
			result = null;
		}
		
		return result;
	}
	
	protected void clearCache()
	{
		this.cachedPrefix.clear();
		this.cachedSuffix.clear();
		
		this.cachedGroups.clear();
		this.cachedPermissions.clear();
		this.cachedAnwsers.clear();
		this.cachedOptions.clear();
	}
	
	@Override
	public void setPrefix( String prefix, String siteName )
	{
		this.clearCache();
	}
	
	@Override
	public void setSuffix( String postfix, String siteName )
	{
		this.clearCache();
	}
	
	@Override
	public void remove()
	{
		this.clearCache();
		
		this.callEvent( PermissionEntityEvent.Action.REMOVED );
	}
	
	@Override
	public void save()
	{
		this.clearCache();
		
		this.callEvent( PermissionEntityEvent.Action.SAVED );
	}
	
	@Override
	public boolean explainExpression( String expression )
	{
		if ( expression == null && this.manager.allowOps )
		{
			Account user = Loader.getAccountsManager().getAccount( this.getName() );
			if ( user != null && user.isOp() )
			{
				return true;
			}
		}
		
		return super.explainExpression( expression );
	}
	
	protected boolean checkMembership( PermissionGroup group, String siteName )
	{
		int groupLifetime = this.getOwnOptionInteger( "group-" + group.getName() + "-until", siteName, 0 );
		
		if ( groupLifetime > 0 && groupLifetime < System.currentTimeMillis() / 1000 )
		{ // check for expiration
			this.setOption( "group-" + group.getName() + "-until", null, siteName ); // remove option
			this.removeGroup( group, siteName ); // remove membership
			// @TODO Make notification of user about expired memebership
			return false;
		}
		
		return true;
	}
}
