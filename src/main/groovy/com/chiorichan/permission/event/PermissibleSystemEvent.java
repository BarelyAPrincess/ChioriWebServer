/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission.event;

import com.chiorichan.event.HandlerList;

public class PermissibleSystemEvent extends PermissibleEvent
{
	protected Action action;
	private static final HandlerList handlers = new HandlerList();
	
	public PermissibleSystemEvent( Action action )
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
