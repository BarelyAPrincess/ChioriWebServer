/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.commands;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.console.InteractiveConsole;
import com.chiorichan.console.commands.AdvancedCommand;
import com.chiorichan.console.commands.advanced.AutoCompleteChoicesException;
import com.chiorichan.console.commands.advanced.CommandListener;
import com.chiorichan.permission.ChildPermission;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.util.StringFunc;
import com.google.common.collect.Sets;

public abstract class PermissionsCommand implements CommandListener
{
	protected AdvancedCommand command;
	
	protected String autoCompleteAccount( String account )
	{
		return autoCompleteAccount( account, "user" );
	}
	
	protected String autoCompleteAccount( String query, String argName )
	{
		if ( query == null )
			return null;
		
		if ( query.startsWith( "#" ) )
			return query.substring( 1 );
		
		Set<String> accounts = Sets.newHashSet();
		
		for ( AccountMeta account : AccountManager.INSTANCE.getAccounts() )
		{
			if ( account.getAcctId().toLowerCase().startsWith( query.toLowerCase() ) )
				accounts.add( account.getAcctId() );
			
			if ( account.getDisplayName().toLowerCase().startsWith( query.toLowerCase() ) )
				accounts.add( account.getDisplayName() );
		}
		
		if ( accounts.size() > 1 )
			throw new AutoCompleteChoicesException( accounts.toArray( new String[0] ), argName );
		else if ( accounts.size() == 1 )
			return accounts.toArray( new String[0] )[0];
		
		return query;
	}
	
	protected String autoCompleteGroupName( String groupName )
	{
		return this.autoCompleteGroupName( groupName, "group" );
	}
	
	protected String autoCompleteGroupName( String groupName, String argName )
	{
		if ( groupName.startsWith( "#" ) )
			return groupName.substring( 1 );
		
		List<String> groups = new LinkedList<String>();
		
		for ( String group : PermissionManager.INSTANCE.getBackend().getGroupNames() )
		{
			if ( group.equalsIgnoreCase( groupName ) )
				return group;
			
			if ( group.toLowerCase().startsWith( groupName.toLowerCase() ) && !groups.contains( group ) )
				groups.add( group );
		}
		
		if ( groups.size() > 1 )
			throw new AutoCompleteChoicesException( groups.toArray( new String[0] ), argName );
		else if ( groups.size() == 1 )
			return groups.get( 0 );
		
		// Nothing found
		return groupName;
	}
	
	protected String autoCompletePermission( PermissibleEntity entity, String permission, String ref )
	{
		return this.autoCompletePermission( entity, permission, ref, "permission" );
	}
	
	protected String autoCompletePermission( PermissibleEntity entity, String permission, String ref, String argName )
	{
		if ( permission == null )
			return permission;
		
		Set<String> permissions = Sets.newHashSet();
		for ( Permission perm : entity.getPermissions( ref ) )
		{
			if ( perm.getNamespace().equalsIgnoreCase( permission ) )
				return perm.getLocalName();
			
			if ( perm.getNamespace().startsWith( permission.toLowerCase() ) )
				permissions.add( perm.getNamespace() );
		}
		
		if ( permissions.size() > 0 )
		{
			String[] permissionArray = permissions.toArray( new String[0] );
			
			if ( permissionArray.length == 1 )
				return permissionArray[0];
			
			throw new AutoCompleteChoicesException( permissionArray, argName );
		}
		
		return permission;
	}
	
	protected String autoCompleteRef( String ref )
	{
		return autoCompleteRef( ref, "default" );
	}
	
	protected String autoCompleteRef( String ref, String argName )
	{
		if ( ref == null || ref.isEmpty() || "*".equals( ref ) )
			return null;
		
		Set<String> refs = Sets.newHashSet();
		
		for ( String r : PermissionManager.INSTANCE.getReferences() )
			if ( r.toLowerCase().startsWith( ref.toLowerCase() ) )
				refs.add( r );
		
		for ( Site site : SiteManager.INSTANCE.getSites() )
		{
			if ( site.getSiteId().toLowerCase().startsWith( ref.toLowerCase() ) )
				refs.add( site.getName() );
			
			if ( site.getName().toLowerCase().startsWith( ref.toLowerCase() ) )
				refs.add( site.getName() );
		}
		
		if ( refs.size() > 1 )
			throw new AutoCompleteChoicesException( refs.toArray( new String[0] ), argName );
		else if ( refs.size() == 1 )
			return refs.toArray( new String[0] )[0];
		
		return ref;
	}
	
	protected List<String> getPermissionsTree( PermissibleEntity entity, String ref, int level )
	{
		// Thing might need some help!
		
		List<String> permissions = new LinkedList<String>();
		Set<String> refPermissions = Sets.newHashSet();
		Set<String> commonPermissions = Sets.newHashSet();
		ref = StringFunc.formatReference( ref );
		
		for ( ChildPermission child : entity.getChildPermissions() )
			if ( ref == null || ref.isEmpty() || child.getReferences().contains( ref ) )
				refPermissions.add( child.getPermission().getNamespace() );
			else if ( child.getReferences().contains( "" ) )
				commonPermissions.add( child.getPermission().getNamespace() );
		
		permissions.addAll( sprintPermissions( ref, refPermissions.toArray( new String[0] ) ) );
		
		// for ( String parentSite : Permissions.getPermissionManager().getSiteInheritance( ref ) )
		// if ( parentSite != null && !parentSite.isEmpty() )
		// permissions.addAll( getPermissionsTree( entity, parentSite, level + 1 ) );
		
		if ( level == 0 && commonPermissions.size() > 0 )
			permissions.addAll( sprintPermissions( "common", commonPermissions.toArray( new String[0] ) ) );
		
		return permissions;
	}
	
	protected int getPosition( String permission, Permission[] permissions )
	{
		try
		{
			// permission is permission index
			int position = Integer.parseInt( permission ) - 1;
			
			if ( position < 0 || position >= permissions.length )
				throw new RuntimeException( "Wrong permission index specified!" );
			
			return position;
		}
		catch ( NumberFormatException e )
		{
			for ( int i = 0; i < permissions.length; i++ )
				if ( permission.equalsIgnoreCase( permissions[i].getNamespace() ) )
					return i;
		}
		
		throw new RuntimeException( "Specified permission not found" );
	}
	
	protected String getSafeSite( String ref, String acctId )
	{
		if ( ref == null )
		{
			AccountMeta meta = AccountManager.INSTANCE.getAccount( acctId );
			
			if ( meta == null )
				ref = SiteManager.INSTANCE.getDefaultSite().getSiteId();
			else
				ref = meta.getSite().getSiteId();
		}
		
		return ref;
	}
	
	protected String getSenderName( InteractiveConsole sender )
	{
		return sender.getPersistence().getSession().getDisplayName();
	}
	
	protected void informEntity( String entityId, String message )
	{
		if ( !Loader.getConfig().getBoolean( "permissions.informEntities.changes", false ) )
			return; // User informing is disabled
			
		AccountMeta meta = AccountManager.INSTANCE.getAccount( entityId );
		if ( meta == null )
			return;
		
		meta.send( ConsoleColor.BLUE + "[Permissions] " + ConsoleColor.WHITE + message );
	}
	
	protected void informGroup( PermissibleGroup group, String message )
	{
		for ( PermissibleEntity entity : group.getChildEntities() )
			informEntity( entity.getId(), message );
	}
	
	protected String mapPermissions( String ref, PermissibleEntity entity, int level )
	{
		StringBuilder builder = new StringBuilder();
		
		int index = 1;
		for ( String permission : getPermissionsTree( entity, ref, 0 ) )
		{
			if ( level > 0 )
				builder.append( "   " );
			else
				builder.append( index++ ).append( ") " );
			
			builder.append( permission );
			if ( level > 0 )
				builder.append( " (from " ).append( entity.getId() ).append( ")" );
			else
				builder.append( " (own)" );
			builder.append( "\n" );
		}
		
		entity.getParentGroups( ref );
		
		level++; // Just increment level once
		return builder.toString();
	}
	
	@Override
	public void onRegistered( AdvancedCommand command )
	{
		this.command = command;
	}
	
	protected Object parseValue( String value )
	{
		if ( value == null )
			return null;
		
		if ( value.equalsIgnoreCase( "true" ) || value.equalsIgnoreCase( "false" ) )
			return Boolean.parseBoolean( value );
		
		try
		{
			return Integer.parseInt( value );
		}
		catch ( NumberFormatException e )
		{
		}
		
		try
		{
			return Double.parseDouble( value );
		}
		catch ( NumberFormatException e )
		{
		}
		
		return value;
	}
	
	protected void printEntityInheritance( InteractiveConsole sender, Collection<PermissibleGroup> collection )
	{
		for ( PermissibleGroup group : collection )
		{
			String rank = "not ranked";
			if ( group.isRanked() )
				rank = "rank " + group.getRank() + " @ "; // XXX + group.getRankLadder();
				
			sender.sendMessage( "   " + group.getId() + " (" + rank + ")" );
		}
	}
	
	protected String printHierarchy( PermissibleGroup parent, String ref, int level )
	{
		StringBuilder buffer = new StringBuilder();
		
		Collection<PermissibleGroup> groups = parent == null ? PermissionManager.INSTANCE.getGroups() : parent.getChildGroups( ref );
		
		for ( PermissibleGroup group : groups )
		{
			if ( parent == null && group.getParentGroups( ref ).size() > 0 )
				continue;
			
			buffer.append( StringUtils.repeat( "  ", level ) ).append( " - " ).append( group.getId() ).append( "\n" );
			
			// Groups
			buffer.append( printHierarchy( group, ref, level + 1 ) );
			
			for ( PermissibleEntity user : group.getChildEntities( ref ) )
				buffer.append( StringUtils.repeat( "  ", level + 1 ) ).append( " + " ).append( user.getId() ).append( "\n" );
		}
		
		return buffer.toString();
	}
	
	protected void sendMessage( InteractiveConsole sender, String message )
	{
		for ( String messagePart : message.split( "\n" ) )
			sender.sendMessage( messagePart );
	}
	
	protected List<String> sprintPermissions( String ref, String[] permissions )
	{
		List<String> permissionList = new LinkedList<String>();
		
		if ( permissions == null )
			return permissionList;
		
		for ( String permission : permissions )
			permissionList.add( permission + ( ref != null ? " @" + ref : "" ) );
		
		return permissionList;
	}
}
