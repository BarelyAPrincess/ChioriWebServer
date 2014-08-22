package com.chiorichan.bus.events.account;

import com.chiorichan.account.bases.Account;
import com.chiorichan.bus.events.Event;
import com.chiorichan.bus.events.HandlerList;

/**
 * Represents a User related event
 */
public abstract class AccountEvent extends Event
{
	private static final HandlerList handlers = new HandlerList();
	protected Account acct;
	
	public AccountEvent(final Account who)
	{
		acct = who;
	}
	
	AccountEvent(final Account who, boolean async)
	{
		super( async );
		acct = who;
		
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
