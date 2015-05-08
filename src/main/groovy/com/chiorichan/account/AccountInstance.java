/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account;

import java.util.Set;

import com.chiorichan.account.lang.AccountException;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionResult;
import com.chiorichan.site.Site;
import com.chiorichan.util.WeakReferenceList;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public final class AccountInstance implements Account
{
	/**
	 * Tracks permissibles that are referencing this account and is self garbage collecting
	 */
	private final WeakReferenceList<AccountPermissible> permissibles = new WeakReferenceList<AccountPermissible>();
	
	/**
	 * Account MetaData
	 */
	private final AccountMeta metadata;
	
	AccountInstance( AccountMeta metadata ) throws AccountException
	{
		if ( metadata == null )
			throw new AccountException( "The metadata can't be null!" );
		
		this.metadata = metadata;
	}
	
	void registerPermissible( AccountPermissible permissible )
	{
		permissibles.add( permissible );
	}
	
	void unregisterPermissible( AccountPermissible permissible )
	{
		permissibles.remove( permissible );
	}
	
	public AccountPermissible[] getPermissibles()
	{
		return permissibles.toSet().toArray( new AccountPermissible[0] );
	}
	
	int countPermissibles()
	{
		return permissibles.size();
	}
	
	@Override
	public boolean isBanned()
	{
		return metadata.isBanned();
	}
	
	@Override
	public boolean isWhitelisted()
	{
		return metadata.isWhitelisted();
	}
	
	@Override
	public boolean isAdmin()
	{
		return metadata.isAdmin();
	}
	
	@Override
	public boolean isOp()
	{
		return metadata.isOp();
	}
	
	public PermissionResult checkPermission( String perm )
	{
		return metadata.getPermissibleEntity().checkPermission( perm );
	}
	
	public PermissionResult checkPermission( Permission perm )
	{
		return metadata.getPermissibleEntity().checkPermission( perm );
	}
	
	/**
	 * @deprecated
	 *             {@link #checkPermission(String)} is to replace this method since it provides more options to the requester
	 * @param perm
	 *            The permission node, e.g., com.chiorichan.permission.node
	 * @return
	 *         The result of said check. Will always return false if permission value is not of type boolean.
	 */
	@Deprecated
	public boolean hasPermission( String perm )
	{
		return checkPermission( perm ).isTrue();
	}
	
	@Override
	public String getHumanReadableName()
	{
		return metadata.getHumanReadableName();
	}
	
	public boolean kick( String msg )
	{
		return metadata.kick( msg );
	}
	
	@Override
	public String getAcctId()
	{
		return metadata.getAcctId();
	}
	
	@Override
	public String toString()
	{
		return "Account{" + metadata.toString() + ",Permissibles{" + Joiner.on( "," ).join( getPermissibles() ) + "}}";
	}
	
	/**
	 * 
	 * @param key
	 *            Metadata key.
	 * @return String
	 *         Returns an empty string if no result.
	 */
	public String getString( String key )
	{
		return getString( key, "" );
	}
	
	/**
	 * Get a string from the Metadata with a default value
	 * 
	 * @param key
	 *            Metadata key.
	 * @param def
	 *            Default value to return if no result.
	 * @return String
	 */
	public String getString( String key, String def )
	{
		if ( !metadata.containsKey( key ) )
			return def;
		
		return metadata.getString( key );
	}
	
	public Site getSite()
	{
		return metadata.getSite();
	}
	
	public String getSiteId()
	{
		return metadata.getSiteId();
	}
	
	@Override
	public Set<String> getIpAddresses()
	{
		Set<String> ips = Sets.newHashSet();
		for ( AccountPermissible perm : getPermissibles() )
			ips.addAll( perm.getIpAddresses() );
		return ips;
	}
	
	@Override
	public AccountMeta metadata()
	{
		return metadata;
	}
	
	@Override
	public AccountInstance instance()
	{
		return this;
	}
	
	@Override
	public void send( Object obj )
	{
		for ( AccountPermissible perm : permissibles )
			perm.send( obj );
	}
	
	@Override
	public void send( Account sender, Object obj )
	{
		for ( AccountPermissible perm : permissibles )
			perm.send( sender, obj );
	}
}
