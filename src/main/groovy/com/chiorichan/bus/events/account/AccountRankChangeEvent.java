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

import com.chiorichan.account.bases.Account;
import com.chiorichan.event.HandlerList;

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
