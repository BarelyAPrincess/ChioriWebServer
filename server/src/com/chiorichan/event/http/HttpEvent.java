package com.chiorichan.event.http;

import com.chiorichan.event.Event;
import com.chiorichan.event.HandlerList;

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
