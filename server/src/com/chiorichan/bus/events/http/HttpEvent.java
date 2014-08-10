package com.chiorichan.bus.events.http;

import com.chiorichan.bus.events.Event;
import com.chiorichan.bus.events.HandlerList;

/**
 * Miscellaneous server events
 */
public abstract class HttpEvent extends Event
{
	public static HandlerList getHandlerList()
	{
		return handlers;
	}
}
