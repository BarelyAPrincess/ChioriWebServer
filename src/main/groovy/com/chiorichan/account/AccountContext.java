/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.chiorichan.account.auth.AccountCredentials;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountDescriptiveReason;
import com.google.common.collect.Maps;

/**
 * Provides context to an Accounts existence
 */
public class AccountContext
{
	/**
	 * Contains a list of keys that can be used to match logins
	 */
	protected final List<String> loginKeys;
	
	/**
	 * Used to remember the last instance of AccountCredentuals used
	 */
	AccountCredentials credentials = null;
	
	protected final Map<String, Object> rawMeta = Maps.newHashMap();
	protected final AccountCreator creator;
	protected final AccountType type;
	private AccountMeta acct;
	protected boolean keepLoaded = false;
	protected String acctId = null;
	protected String siteId = null;
	
	protected AccountContext( AccountCreator creator, AccountType type )
	{
		this.creator = creator;
		this.type = type;
		
		loginKeys = new ArrayList<String>( creator.getLoginKeys() );
		
		loginKeys.add( "acctId" );
	}
	
	protected AccountContext( AccountCreator creator, AccountType type, String acctId, String siteId )
	{
		this( creator, type );
		this.acctId = acctId;
		this.siteId = siteId;
	}
	
	protected AccountContext( AccountCreator creator, AccountType type, String acctId, String siteId, boolean keepLoaded )
	{
		this( creator, type, acctId, siteId );
		this.keepLoaded = keepLoaded;
	}
	
	public AccountCreator creator()
	{
		return creator;
	}
	
	public AccountCredentials credentials()
	{
		return credentials;
	}
	
	public String getAcctId()
	{
		if ( acctId == null || acctId.isEmpty() )
			return "<Not Set>";
		
		return acctId;
	}
	
	public String getAcctIdWithException() throws AccountException
	{
		if ( acctId == null || acctId.isEmpty() )
			throw new AccountException( AccountDescriptiveReason.EMPTY_ACCTID, AccountType.ACCOUNT_NONE );
		
		return acctId;
	}
	
	public String getSiteId()
	{
		if ( siteId == null || siteId.isEmpty() )
			return "%";
		
		return siteId;
	}
	
	public Object getValue( String key )
	{
		return rawMeta.get( key );
	}
	
	public Map<String, Object> getValues()
	{
		return Collections.unmodifiableMap( rawMeta );
	}
	
	public boolean keepLoaded()
	{
		return keepLoaded;
	}
	
	public AccountMeta meta()
	{
		return acct;
	}
	
	public void save() throws AccountException
	{
		creator.save( this );
	}
	
	void setAccount( AccountMeta acct )
	{
		this.acct = acct;
	}
	
	public void setKeepLoaded( boolean keepLoaded )
	{
		this.keepLoaded = keepLoaded;
	}
	
	public void setValue( String key, Object value )
	{
		rawMeta.put( key, value );
	}
	
	public AccountType type()
	{
		return type;
	}
}
