package com.chiorichan.bus.events.net;

import com.chiorichan.bus.events.Event;
import com.chiorichan.bus.events.HandlerList;

/**
 * Miscellaneous server events
 */
public abstract class NetEvent extends Event
{
	public static HandlerList getHandlerList()
	{
		return handlers;
	}
}
