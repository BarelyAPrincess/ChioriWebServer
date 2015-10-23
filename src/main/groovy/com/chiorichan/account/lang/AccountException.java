/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account.lang;

import org.apache.commons.lang3.Validate;

import com.chiorichan.account.AccountMeta;
import com.chiorichan.lang.ReportingLevel;

/**
 * Used to pass login errors to the requester.
 */
public class AccountException extends Exception
{
	private static final long serialVersionUID = 5522301956671473324L;
	
	private AccountDescriptiveReason reason;
	private AccountResult result;
	
	public AccountException( AccountDescriptiveReason reason, AccountMeta meta )
	{
		this( reason, meta.getId() );
	}
	
	public AccountException( AccountDescriptiveReason reason, AccountResult result )
	{
		super( reason.getMessage() );
		
		Validate.notNull( reason );
		Validate.notNull( result );
		
		result.setReason( reason );
		this.reason = reason;
		this.result = result;
	}
	
	public AccountException( AccountDescriptiveReason reason, String acctId )
	{
		super( reason.getMessage() );
		this.reason = reason;
		result = new AccountResult( acctId, reason );
	}
	
	public AccountException( AccountDescriptiveReason reason, Throwable cause, AccountMeta meta )
	{
		this( reason, cause, meta.getId() );
	}
	
	public AccountException( AccountDescriptiveReason reason, Throwable cause, AccountResult result )
	{
		super( cause );
		
		if ( cause instanceof AccountException )
			throw new IllegalStateException( "Stacking AccountException is not recommended!" );
		
		Validate.notNull( reason );
		Validate.notNull( cause );
		Validate.notNull( result );
		
		this.reason = reason;
		this.result = result;
	}
	
	public AccountException( AccountDescriptiveReason reason, Throwable cause, String acctId )
	{
		this( reason, cause, new AccountResult( acctId ) );
	}
	
	public AccountException( Throwable cause, AccountMeta meta )
	{
		this( cause, meta.getId() );
	}
	
	public AccountException( Throwable cause, AccountResult result )
	{
		super( cause );
		
		if ( cause instanceof AccountException )
			throw new IllegalStateException( "Stacking AccountException is not recommended!" );
		
		Validate.notNull( cause );
		Validate.notNull( result );
		
		reason = new AccountDescriptiveReason( cause.getMessage(), ReportingLevel.L_ERROR );
		this.result = result;
	}
	
	public AccountException( Throwable cause, String acctId )
	{
		this( cause, new AccountResult( acctId ) );
	}
	
	public AccountMeta getAccount()
	{
		return result.getAccount();
	}
	
	public String getAcctId()
	{
		return result.getAcctId();
	}
	
	public AccountDescriptiveReason getReason()
	{
		return reason;
	}
	
	public AccountResult getResult()
	{
		if ( !result.isIgnorable() )
			result = result.setCause( this );
		
		return result;
	}
	
	public boolean hasCause()
	{
		return getCause() != null;
	}
}
