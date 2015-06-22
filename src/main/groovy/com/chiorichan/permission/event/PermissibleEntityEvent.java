/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.event;

import com.chiorichan.event.HandlerList;
import com.chiorichan.permission.PermissibleBase;

public class PermissibleEntityEvent extends PermissibleEvent
{
	private static final HandlerList handlers = new HandlerList();
	protected PermissibleBase entity;
	protected Action action;
	
	public PermissibleEntityEvent( PermissibleBase entity, Action action )
	{
		super( action.toString() );
		
		this.entity = entity;
		this.action = action;
	}
	
	public Action getAction()
	{
		return this.action;
	}
	
	public PermissibleBase getEntity()
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
