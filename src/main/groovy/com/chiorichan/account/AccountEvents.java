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
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.event.BuiltinEventCreator;
import com.chiorichan.event.EventBus;

/**
 * Handles the task and events between the {@link EventBus} and {@link AccountManager}
 */
public abstract class AccountEvents extends BuiltinEventCreator
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
		
		if ( event.getContext() == null )
			return null;
		
		if ( event.getResult() != AccountResult.LOGIN_SUCCESS )
		{
			AccountManager.getLogger().warning( event.getResult().getMessage() );
			return null;
		}
		
		AccountMeta acct = new AccountMeta( event.getContext() );
		
		return acct;
	}
	
	AccountMeta fireAccountLookupWithException( String acctId )
	{
		AccountLookupEvent event = new AccountLookupEvent( acctId );
		
		EventBus.INSTANCE.callEvent( event );
		
		if ( event.getContext() == null )
			throw AccountResult.INCORRECT_LOGIN.exception();
		
		if ( event.getResult() != AccountResult.LOGIN_SUCCESS )
			throw event.getResult().exception();
		
		AccountMeta acct = new AccountMeta( event.getContext() );
		
		return acct;
	}
}
