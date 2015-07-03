/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.chiorichan.account.lang.AccountException;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.site.Site;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public final class AccountInstance implements Account
{
	/**
	 * Tracks permissibles that are referencing this account
	 */
	private final Set<AccountAttachment> permissibles = Collections.newSetFromMap( new WeakHashMap<AccountAttachment, Boolean>() );
	
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
	
	int countAttachments()
	{
		return permissibles.size();
	}
	
	public Collection<AccountAttachment> getAttachments()
	{
		return Collections.unmodifiableSet( permissibles );
	}
	
	@Override
	public String getDisplayName()
	{
		return metadata.getDisplayName();
	}
	
	@Override
	public PermissibleEntity getEntity()
	{
		return meta().getEntity();
	}
	
	@Override
	public String getId()
	{
		return metadata.getId();
	}
	
	public Collection<String> getIpAddresses()
	{
		Set<String> ips = Sets.newHashSet();
		for ( AccountAttachment perm : getAttachments() )
			ips.add( perm.getIpAddr() );
		return ips;
	}
	
	@Override
	public Site getSite()
	{
		return metadata.getSite();
	}
	
	@Override
	public String getSiteId()
	{
		return metadata.getSiteId();
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
	
	@Override
	public AccountInstance instance()
	{
		return this;
	}
	
	@Override
	public boolean isInitialized()
	{
		return true;
	}
	
	@Override
	public AccountMeta meta()
	{
		return metadata;
	}
	
	public void registerAttachment( AccountAttachment attachment )
	{
		if ( !permissibles.contains( attachment ) )
			permissibles.add( attachment );
	}
	
	@Override
	public String toString()
	{
		return "Account{" + metadata.toString() + ",Attachments{" + Joiner.on( "," ).join( getAttachments() ) + "}}";
	}
	
	public void unregisterAttachment( AccountAttachment attachment )
	{
		permissibles.remove( attachment );
	}
}
