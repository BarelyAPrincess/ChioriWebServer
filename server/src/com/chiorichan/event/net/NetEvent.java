package com.chiorichan.event.net;

import com.chiorichan.event.Event;
import com.chiorichan.event.HandlerList;

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
