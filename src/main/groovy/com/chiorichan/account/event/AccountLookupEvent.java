/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account.event;

import com.chiorichan.account.AccountContext;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.event.Conditional;
import com.chiorichan.event.EventException;
import com.chiorichan.event.RegisteredListener;

/**
 * Used to lookup accounts
 */
public class AccountLookupEvent extends AccountEvent implements Conditional
{
	private AccountContext context = null;
	private AccountResult result = AccountResult.DEFAULT;
	private String acctId;
	
	public AccountLookupEvent( String acctId )
	{
		this.acctId = acctId;
	}
	
	public AccountContext getContext()
	{
		return context;
	}
	
	public AccountResult getResult()
	{
		return result;
	}
	
	public String getAcctId()
	{
		return acctId;
	}
	
	public void setResult( AccountContext context, AccountResult result )
	{
		this.context = context;
		this.result = result;
	}
	
	@Override
	public boolean conditional( RegisteredListener context ) throws EventException
	{
		if ( result == AccountResult.LOGIN_SUCCESS || result == AccountResult.CANCELLED_BY_EVENT || result == AccountResult.EMPTY_ACCTID || result == AccountResult.EMPTY_USERNAME || result == AccountResult.UNDER_ATTACK )
			return false;
		
		return true;
	}
}
