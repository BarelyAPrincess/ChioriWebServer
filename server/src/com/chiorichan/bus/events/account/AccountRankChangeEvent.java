package com.chiorichan.bus.events.account;

import com.chiorichan.account.bases.Account;
import com.chiorichan.bus.events.HandlerList;

/**
 * Called when a Users level changes
 */
public class AccountRankChangeEvent extends AccountEvent
{
	private static final HandlerList handlers = new HandlerList();
	private final int oldLevel;
	private final int newLevel;
	
	public AccountRankChangeEvent(final Account User, final int oldLevel, final int newLevel)
	{
		super( User );
		this.oldLevel = oldLevel;
		this.newLevel = newLevel;
	}
	
	/**
	 * Gets the old level of the User
	 * 
	 * @return The old level of the User
	 */
	public int getOldLevel()
	{
		return oldLevel;
	}
	
	/**
	 * Gets the new level of the User
	 * 
	 * @return The new (current) level of the User
	 */
	public int getNewLevel()
	{
		return newLevel;
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
