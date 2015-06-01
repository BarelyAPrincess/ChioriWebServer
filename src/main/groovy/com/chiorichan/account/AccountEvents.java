/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.chiorichan.account.event.AccountKickEvent;
import com.chiorichan.account.event.AccountLoadEvent;
import com.chiorichan.account.event.AccountLookupEvent;
import com.chiorichan.account.event.AccountMessageEvent;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.event.BuiltinEventCreator;
import com.chiorichan.event.EventBus;

/**
 * Handles the task and events between the {@link EventBus} and {@link AccountManager}
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public abstract class AccountEvents extends BuiltinEventCreator
{
	private final EventBus events = EventBus.INSTANCE;
	
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
	
	void fireAccountLoad( AccountMeta meta )
	{
		events.callEvent( new AccountLoadEvent( meta ) );
	}
	
	/**
	 * Attempts to kick all logins of account
	 * 
	 * @param acct
	 *            The Account to kick
	 * @param msg
	 *            The reason for kick
	 * @return Was the kick successful
	 */
	boolean fireKick( AccountInstance acct, String msg )
	{
		List<AccountPermissible> perms = Arrays.asList( acct.getPermissibles() );
		AccountKickEvent event = new AccountKickEvent( acct, perms, msg, acct.metadata().getLogoffMessage() );
		
		if ( event.isCancelled() )
			return false;
		
		for ( AccountPermissible ip : perms )
			if ( !ip.kick( event.getReason() ) )
				return false;
		
		if ( event.getLeaveMessage() != null )
			send( acct, event.getLeaveMessage() );
		
		return true;
	}
	
	/**
	 * Attempts to only kick the provided instance of login
	 * 
	 * @param acct
	 *            The instance to kick
	 * @param msg
	 *            The reason to kick
	 * @return Was the kick successful
	 */
	boolean fireKick( AccountPermissible acct, String msg )
	{
		AccountKickEvent event = new AccountKickEvent( acct.instance(), Arrays.asList( acct ), msg, acct.metadata().getLogoffMessage() );
		
		if ( event.isCancelled() )
			return false;
		
		if ( event.getLeaveMessage() != null )
			send( acct, acct, event.getLeaveMessage() );
		
		send( AccountType.ACCOUNT_NONE, acct, event.getReason() );
		return acct.logout() == AccountResult.LOGOUT_SUCCESS;
	}
	
	/**
	 * Attempts to send specified object to every initialized Account
	 * 
	 * @param excludedAcct
	 *            Ignore this account when sending
	 * @param objs
	 *            The objects to send
	 */
	public void send( Account sender, Account excluded, Object... objs )
	{
		Set<AccountPermissible> accts = AccountManager.INSTANCE.getAccountPermissibles();
		
		if ( excluded != null && excluded.metadata().isInitialized() && !Collections.disjoint( accts, Arrays.asList( excluded.instance().getPermissibles() ) ) )
			for ( AccountPermissible p : excluded.instance().getPermissibles() )
				accts.remove( p );
		
		AccountMessageEvent event = new AccountMessageEvent( sender, accts, objs );
		
		if ( event.isCancelled() )
			return;
		
		for ( AccountPermissible perm : event.getRecipients() )
			for ( Object obj : event.getMessages() )
				perm.send( sender, obj );
	}
	
	/**
	 * See {@link #send(Account, Account, Object...)}
	 */
	public void send( Account sender, Object... objs )
	{
		send( sender, null, objs );
	}
}
