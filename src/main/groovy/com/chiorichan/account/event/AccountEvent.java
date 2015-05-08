/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.chiorichan.account.Account;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.event.Event;
import com.chiorichan.event.HandlerList;

/**
 * Represents a account related event
 */
public abstract class AccountEvent extends Event
{
	private static final HandlerList handlers = new HandlerList();
	private Account acct;
	private List<AccountPermissible> permissibles;
	
	public AccountEvent()
	{
		// New Sub Class?
	}
	
	public AccountEvent( Account acct )
	{
		this( acct, new ArrayList<AccountPermissible>() );
	}
	
	public AccountEvent( Account acct, boolean async )
	{
		this( acct, new ArrayList<AccountPermissible>(), async );
	}
	
	public AccountEvent( Account acct, AccountPermissible permissible )
	{
		this( acct, Arrays.asList( permissible ) );
	}
	
	public AccountEvent( Account acct, List<AccountPermissible> permissibles )
	{
		this.acct = acct;
		this.permissibles = permissibles;
	}
	
	AccountEvent( Account acct, List<AccountPermissible> permissibles, boolean async )
	{
		super( async );
		this.acct = acct;
		this.permissibles = permissibles;
	}
	
	/**
	 * Returns the User involved in this event
	 * 
	 * @return User who is involved in this event
	 */
	public final Account getAccount()
	{
		return acct;
	}
	
	public final List<AccountPermissible> getPermissibles()
	{
		return Collections.unmodifiableList( permissibles );
	}
	
	@Override
	public HandlerList getHandlers()
	{
		return handlers;
	}
	
	public static HandlerList getHandlerList()
	{
		return handlers;
	}
}
