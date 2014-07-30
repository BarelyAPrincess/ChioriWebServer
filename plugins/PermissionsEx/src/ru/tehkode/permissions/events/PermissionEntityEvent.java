package ru.tehkode.permissions.events;

import ru.tehkode.permissions.PermissionEntity;

import com.chiorichan.bus.events.HandlerList;

public class PermissionEntityEvent extends PermissionEvent
{
	
	private static final HandlerList handlers = new HandlerList();
	protected PermissionEntity entity;
	protected Action action;
	
	public PermissionEntityEvent(PermissionEntity entity, Action action)
	{
		super( action.toString() );
		
		this.entity = entity;
		this.action = action;
	}
	
	public Action getAction()
	{
		return this.action;
	}
	
	public PermissionEntity getEntity()
	{
		return entity;
	}
	
	public enum Action
	{
		
		PERMISSIONS_CHANGED, OPTIONS_CHANGED, INHERITANCE_CHANGED, INFO_CHANGED, TIMEDPERMISSION_EXPIRED, RANK_CHANGED, DEFAULTGROUP_CHANGED, WEIGHT_CHANGED, SAVED, REMOVED,
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
