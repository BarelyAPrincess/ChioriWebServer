/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

/**
 * Provides an easy to use Account
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
class AccountList implements Iterable<AccountMeta>
{
	private volatile Map<String, AccountMeta> accounts = Maps.newConcurrentMap();
	
	void put( AccountMeta meta )
	{
		accounts.put( meta.getAcctId(), meta );
	}
	
	AccountMeta get( String acctId )
	{
		return accounts.get( acctId );
	}
	
	AccountMeta remove( String acctId )
	{
		return accounts.remove( acctId );
	}
	
	@Override
	public Iterator<AccountMeta> iterator()
	{
		return accounts.values().iterator();
	}
	
	void clear()
	{
		accounts.clear();
	}
	
	public Set<AccountMeta> toSet()
	{
		return new HashSet<AccountMeta>( accounts.values() );
	}
	
}
