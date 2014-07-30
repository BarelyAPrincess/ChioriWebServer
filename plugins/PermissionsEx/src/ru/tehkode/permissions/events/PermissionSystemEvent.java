package ru.tehkode.permissions.events;

import com.chiorichan.bus.events.HandlerList;

public class PermissionSystemEvent extends PermissionEvent
{
	
	protected Action action;
	private static final HandlerList handlers = new HandlerList();
	
	public PermissionSystemEvent(Action action)
	{
		super( action.toString() );
		
		this.action = action;
	}
	
	public Action getAction()
	{
		return this.action;
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
	
	public enum Action
	{
		
		BACKEND_CHANGED, RELOADED, WORLDINHERITANCE_CHANGED, DEFAULTGROUP_CHANGED, DEBUGMODE_TOGGLE, REINJECT_PERMISSIBLES,
	}
}
