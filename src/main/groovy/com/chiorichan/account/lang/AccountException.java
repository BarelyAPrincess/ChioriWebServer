/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account.lang;

import com.chiorichan.account.AccountContext;
import com.chiorichan.account.AccountMeta;

/**
 * Used to pass login errors to the requester.
 */
public class AccountException extends RuntimeException
{
	private static final long serialVersionUID = 5522301956671473324L;
	
	private AccountResult result = AccountResult.UNKNOWN_ERROR;
	private AccountMeta acct = null;
	private AccountContext context = null;
	
	public AccountException()
	{
		
	}
	
	public AccountException( AccountResult result )
	{
		super( result.getMessage() );
		this.result = result;
	}
	
	public AccountException( AccountResult result, AccountContext context )
	{
		this( result );
		this.context = context;
	}
	
	public AccountException( AccountResult result, AccountMeta acct )
	{
		this( result );
		this.acct = acct;
	}
	
	public AccountException( String msg )
	{
		super( msg );
	}
	
	public AccountException( String msg, Throwable cause )
	{
		super( cause );
	}
	
	public AccountException( Throwable cause )
	{
		super( cause );
	}
	
	public AccountException( Throwable cause, AccountContext context )
	{
		this( cause );
		this.context = context;
	}
	
	public AccountException( Throwable cause, AccountMeta acct )
	{
		this( cause );
		this.acct = acct;
	}
	
	public AccountMeta getAccount()
	{
		return acct;
	}
	
	public AccountContext getContext()
	{
		return context == null && acct != null ? acct.context() : context;
	}
	
	public AccountResult getResult()
	{
		if ( result == null )
			result = AccountResult.UNKNOWN_ERROR;
		
		if ( !result.isIgnorable() )
			result.setThrowable( this );
		
		return result;
	}
	
	public AccountException setAccount( AccountMeta acct )
	{
		this.acct = acct;
		return this;
	}
	
	public AccountException setContext( AccountContext context )
	{
		this.context = context;
		return this;
	}
}
