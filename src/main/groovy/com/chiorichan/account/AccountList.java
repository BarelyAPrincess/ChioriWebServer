/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

/**
 * Provides an easy to use Account
 */
class AccountList implements Iterable<AccountMeta>
{
	private volatile Map<String, AccountMeta> accounts = Maps.newConcurrentMap();
	
	void clear()
	{
		accounts.clear();
	}
	
	AccountMeta get( String acctId )
	{
		if ( "none".equals( acctId ) || "default".equals( acctId ) )
			return AccountType.ACCOUNT_NONE;
		
		if ( "root".equals( acctId ) )
			return AccountType.ACCOUNT_ROOT;
		
		AccountMeta meta = accounts.get( acctId );
		
		if ( meta == null )
			for ( AccountMeta am : accounts.values() )
				for ( String key : am.context().loginKeys )
					if ( acctId.equals( am.getString( key ) ) )
					{
						meta = am;
						break;
					}
		
		return meta;
	}
	
	@Override
	public Iterator<AccountMeta> iterator()
	{
		return accounts.values().iterator();
	}
	
	public Set<String> keySet()
	{
		return Collections.unmodifiableSet( accounts.keySet() );
	}
	
	void put( AccountMeta meta )
	{
		// Prevents the overriding of the builtin Accounts
		if ( ( "none".equals( meta.getId() ) || "default".equals( meta.getId() ) || "root".equals( meta.getId() ) ) && accounts.containsKey( meta.getId() ) )
			return;
		
		accounts.put( meta.getId(), meta );
	}
	
	AccountMeta remove( String acctId )
	{
		return accounts.remove( acctId );
	}
	
	public Set<AccountMeta> toSet()
	{
		return new HashSet<AccountMeta>( accounts.values() );
	}
	
}
