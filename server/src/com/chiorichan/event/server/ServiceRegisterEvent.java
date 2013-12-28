package com.chiorichan.event.server;

import com.chiorichan.event.HandlerList;
import com.chiorichan.plugin.RegisteredServiceProvider;

/**
 * This event is called when a service is registered.<br>
 * <b>Warning:</b> The order in which register and unregister events are called should not be relied upon.
 */
public class ServiceRegisterEvent extends ServiceEvent
{
	private static final HandlerList handlers = new HandlerList();
	
	public ServiceRegisterEvent(RegisteredServiceProvider<?> registeredProvider)
	{
		super( registeredProvider );
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
