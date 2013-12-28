package com.chiorichan.event.server;

import com.chiorichan.event.HandlerList;
import com.chiorichan.plugin.Plugin;

/**
 * Called when a plugin is enabled.
 */
public class PluginEnableEvent extends PluginEvent
{
	private static final HandlerList handlers = new HandlerList();
	
	public PluginEnableEvent(final Plugin plugin)
	{
		super( plugin );
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
