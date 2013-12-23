package com.chiorichan.event.server;

import com.chiorichan.event.Event;
import com.chiorichan.event.HandlerList;

/**
 * Miscellaneous server events
 */
public abstract class ServerEvent extends Event
{
	public static HandlerList getHandlerList()
	{
		return handlers;
	}
}
