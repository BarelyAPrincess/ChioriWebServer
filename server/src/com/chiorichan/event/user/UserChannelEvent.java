package com.chiorichan.event.user;

import com.chiorichan.event.HandlerList;
import com.chiorichan.user.User;

/**
 * This event is called after a User registers or unregisters a new plugin channel.
 */
public abstract class UserChannelEvent extends UserEvent
{
	private static final HandlerList handlers = new HandlerList();
	private final String channel;
	
	public UserChannelEvent(final User User, final String channel)
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
