package com.chiorichan.bus.events.account;

import com.chiorichan.account.bases.Account;
import com.chiorichan.bus.events.HandlerList;

/**
 * This event is called after a User registers or unregisters a new plugin channel.
 */
public abstract class AccountChannelEvent extends AccountEvent
{
	private static final HandlerList handlers = new HandlerList();
	private final String channel;
	
	public AccountChannelEvent(final Account User, final String channel)
	{
		super( User );
		this.channel = channel;
	}
	
	public final String getChannel()
	{
		return channel;
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
