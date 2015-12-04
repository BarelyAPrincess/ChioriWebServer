/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account;

import com.chiorichan.account.event.AccountLoadEvent;
import com.chiorichan.account.event.AccountLookupEvent;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountDescriptiveReason;
import com.chiorichan.event.BuiltinRegistrar;
import com.chiorichan.event.EventBus;

/**
 * Handles the task and events between the {@link EventBus} and {@link AccountManager}
 */
public abstract class AccountEvents extends BuiltinRegistrar
{
	private final EventBus events = EventBus.INSTANCE;
	
	void fireAccountLoad( AccountMeta meta )
	{
		events.callEvent( new AccountLoadEvent( meta ) );
	}
	
	/**
	 * Tries to find the account from our cached account list.<br>
	 * Will also try and find the account within the Garbage Collection map to aid Memory Leaks.
	 * 
	 * @param id
	 *            The acctId to try and retrieve.
	 * @return
	 *         The account found. Will return NULL is not found.
	 */
	AccountMeta fireAccountLookup( String acctId )
	{
		AccountLookupEvent event = new AccountLookupEvent( acctId );
		
		EventBus.INSTANCE.callEvent( event );
		
		if ( !event.getDescriptiveReason().getReportingLevel().isSuccess() )
		{
			AccountManager.getLogger().warning( event.getDescriptiveReason().getMessage() );
			return null;
		}
		
		if ( event.getContext() == null )
			return null;
		
		AccountMeta acct = new AccountMeta( event.getContext() );
		
		return acct;
	}
	
	AccountMeta fireAccountLookupWithException( String acctId ) throws AccountException
	{
		AccountLookupEvent event = new AccountLookupEvent( acctId );
		
		EventBus.INSTANCE.callEvent( event );
		
		if ( !event.getDescriptiveReason().getReportingLevel().isSuccess() )
			throw new AccountException( event.getDescriptiveReason(), acctId );
		
		if ( event.getContext() == null )
			throw new AccountException( AccountDescriptiveReason.INCORRECT_LOGIN, acctId );
		
		AccountMeta acct = new AccountMeta( event.getContext() );
		
		return acct;
	}
}
