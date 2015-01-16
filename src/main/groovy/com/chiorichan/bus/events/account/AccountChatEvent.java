/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.bus.events.account;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.Warning;
import com.chiorichan.account.bases.Account;
import com.chiorichan.event.Cancellable;
import com.chiorichan.event.HandlerList;

/**
 * Holds information for Account chat and commands
 * 
 * @deprecated This event will fire from the main thread and allows the use of all of the Bukkit API, unlike the
 *             {@link AsyncAccountChatEvent}.<br>
 * <br>
 *             Listening to this event forces chat to wait for the main thread which causes delays for chat.<br>
 *             {@link AsyncAccountChatEvent} is the encouraged alternative for thread safe implementations.
 */
@Deprecated
@Warning( reason = "Listening to this event forces chat to wait for the main thread, delaying chat messages." )
public class AccountChatEvent extends AccountEvent implements Cancellable
{
	private static final HandlerList handlers = new HandlerList();
	private boolean cancel = false;
	private String message;
	private String format;
	private final Set<Account> recipients;
	
	public AccountChatEvent(final Account Account, final String message)
	{
		super( Account );
		this.message = message;
		this.format = "<%1$s> %2$s";
		this.recipients = new HashSet<Account>(  Loader.getAccountsManager().getOnlineAccounts() );
	}
	
	public AccountChatEvent(final Account Account, final String message, final String format, final Set<Account> recipients)
	{
		super( Account );
		this.message = message;
		this.format = format;
		this.recipients = recipients;
	}
	
	public boolean isCancelled()
	{
		return cancel;
	}
	
	public void setCancelled( boolean cancel )
	{
		this.cancel = cancel;
	}
	
	/**
	 * Gets the message that the Account is attempting to send
	 * 
	 * @return Message the Account is attempting to send
	 */
	public String getMessage()
	{
		return message;
	}
	
	/**
	 * Sets the message that the Account will send
	 * 
	 * @param message
	 *           New message that the Account will send
	 */
	public void setMessage( String message )
	{
		this.message = message;
	}
	
	/**
	 * Sets the Account that this message will display as, or command will be executed as
	 * 
	 * @param Account
	 *           New Account which this event will execute as
	 */
	public void setAcount( final Account acct )
	{
		Validate.notNull( acct, "Account cannot be null" );
		this.acct = acct;
	}
	
	/**
	 * Gets the format to use to display this chat message
	 * 
	 * @return String.Format compatible format string
	 */
	public String getFormat()
	{
		return format;
	}
	
	/**
	 * Sets the format to use to display this chat message
	 * 
	 * @param format
	 *           String.Format compatible format string
	 */
	public void setFormat( final String format )
	{
		// Oh for a better way to do this!
		try
		{
			String.format( format, acct, message );
		}
		catch ( RuntimeException ex )
		{
			ex.fillInStackTrace();
			throw ex;
		}
		
		this.format = format;
	}
	
	/**
	 * Gets a set of recipients that this chat message will be displayed to
	 * 
	 * @return All Accounts who will see this chat message
	 */
	public Set<Account> getRecipients()
	{
		return recipients;
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
