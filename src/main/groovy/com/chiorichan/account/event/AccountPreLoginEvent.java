/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account.event;

import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.lang.AccountDescriptiveReason;
import com.chiorichan.event.Cancellable;
import com.chiorichan.event.Conditional;
import com.chiorichan.event.EventException;
import com.chiorichan.event.RegisteredListener;

/**
 * Stores details for Users attempting to log in
 */
public class AccountPreLoginEvent extends AccountEvent implements Conditional, Cancellable
{
	private AccountDescriptiveReason reason = AccountDescriptiveReason.DEFAULT;
	private final AccountPermissible via;
	private final Object[] creds;
	
	public AccountPreLoginEvent( AccountMeta meta, AccountPermissible accountPermissible, String acctId, Object[] creds )
	{
		super( meta, accountPermissible );
		via = accountPermissible;
		this.creds = creds;
	}
	
	@Override
	public boolean conditional( RegisteredListener context ) throws EventException
	{
		// If the result returned is an error then we skip the remaining EventListeners
		return reason.getReportingLevel().isSuccess();
	}
	
	/**
	 * Disallows the User from logging in, with the given reason
	 * 
	 * @param result
	 *            New result for disallowing the User
	 * @param message
	 *            Kick message to display to the user
	 */
	public void fail( final AccountDescriptiveReason reason )
	{
		this.reason = reason;
	}
	
	public AccountPermissible getAttachment()
	{
		return via;
	}
	
	public Object[] getCredentials()
	{
		return creds;
	}
	
	/**
	 * Gets the current result of the login, as an enum
	 * 
	 * @return Current AccountResult of the login
	 */
	public AccountDescriptiveReason getDescriptiveReason()
	{
		return reason;
	}
	
	@Override
	public boolean isCancelled()
	{
		return reason == AccountDescriptiveReason.CANCELLED_BY_EVENT;
	}
	
	@Override
	public void setCancelled( boolean cancel )
	{
		reason = AccountDescriptiveReason.CANCELLED_BY_EVENT;
	}
	
	/**
	 * Sets the new result of the login, as an enum
	 * 
	 * @param result
	 *            New result to set
	 */
	public void setDescriptiveReason( final AccountDescriptiveReason reason )
	{
		this.reason = reason;
	}
	
	/**
	 * Allows the User to log in
	 */
	public void success()
	{
		reason = AccountDescriptiveReason.DEFAULT;
	}
}
