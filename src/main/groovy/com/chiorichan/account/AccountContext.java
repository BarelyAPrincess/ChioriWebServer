/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.google.common.collect.Maps;

/**
 * Provides context to an Accounts existence
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class AccountContext
{
	private final Map<String, Object> rawMeta = Maps.newHashMap();
	private final AccountCreator creator;
	private final AccountType type;
	private boolean keepLoaded = false;
	private String acctId = null;
	private String siteId = null;
	
	public AccountContext( AccountCreator creator, AccountType type, String acctId, String siteId, boolean keepLoaded )
	{
		this( creator, type, acctId, siteId );
		this.keepLoaded = keepLoaded;
	}
	
	public AccountContext( AccountCreator creator, AccountType type, String acctId, String siteId )
	{
		this( creator, type );
		this.acctId = acctId;
		this.siteId = siteId;
	}
	
	public AccountContext( AccountCreator creator, AccountType type )
	{
		this.creator = creator;
		this.type = type;
	}
	
	public void setAcctId( String acctId )
	{
		this.acctId = acctId;
	}
	
	public void setSiteId( String siteId )
	{
		this.siteId = siteId;
	}
	
	public void setKeepLoaded( boolean keepLoaded )
	{
		this.keepLoaded = keepLoaded;
	}
	
	public AccountType type()
	{
		return type;
	}
	
	public AccountCreator creator()
	{
		return creator;
	}
	
	public String getAcctId()
	{
		if ( acctId == null || acctId.isEmpty() )
			throw new AccountException( AccountResult.EMPTY_ACCTID );
		
		return acctId;
	}
	
	public String getAcctIdWithoutException()
	{
		if ( acctId == null )
			return "<Not Set>";
		
		return acctId;
	}
	
	public String getSiteId()
	{
		if ( acctId == null || acctId.isEmpty() )
			return "%";
		
		return siteId;
	}
	
	public boolean keepLoaded()
	{
		return keepLoaded;
	}
	
	public void setValues( Map<String, Object> meta )
	{
		for ( Entry<String, Object> entry : meta.entrySet() )
			if ( !AccountMeta.IGNORED_KEYS.contains( entry.getKey() ) )
				rawMeta.put( entry.getKey(), entry.getValue() );
	}
	
	public Map<String, Object> getValues()
	{
		return Collections.unmodifiableMap( rawMeta );
	}
}
