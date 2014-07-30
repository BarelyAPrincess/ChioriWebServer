package com.chiorichan.bus.events.server;

import com.chiorichan.bus.events.Event;
import com.chiorichan.bus.events.HandlerList;

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
