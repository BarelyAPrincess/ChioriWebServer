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
import com.chiorichan.account.lang.AccountDescriptiveReason;
import com.chiorichan.event.Conditional;
import com.chiorichan.event.EventException;
import com.chiorichan.event.RegisteredListener;

/**
 * Used to lookup accounts
 */
public class AccountLookupEvent extends AccountEvent implements Conditional
{
	private AccountContext context = null;
	private AccountDescriptiveReason reason = AccountDescriptiveReason.DEFAULT;
	private Throwable cause = null;
	private String acctId;
	
	public AccountLookupEvent( String acctId )
	{
		this.acctId = acctId;
	}
	
	@Override
	public boolean conditional( RegisteredListener context ) throws EventException
	{
		if ( cause != null )
			reason = AccountDescriptiveReason.INTERNAL_ERROR;
		
		return !reason.getReportingLevel().isSuccess() && reason.getReportingLevel().isIgnorable();
	}
	
	public String getAcctId()
	{
		return acctId;
	}
	
	public Throwable getCause()
	{
		return cause;
	}
	
	public AccountContext getContext()
	{
		return context;
	}
	
	public AccountDescriptiveReason getDescriptiveReason()
	{
		return reason;
	}
	
	public boolean hasCause()
	{
		return cause != null;
	}
	
	public AccountLookupEvent setCause( Throwable cause )
	{
		this.cause = cause;
		return this;
	}
	
	public AccountLookupEvent setResult( AccountContext context, AccountDescriptiveReason reason )
	{
		this.context = context;
		this.reason = reason;
		return this;
	}
}
