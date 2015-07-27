/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.event;

import com.chiorichan.permission.PermissibleEntity;

public class PermissibleEntityEvent extends PermissibleEvent
{
	public enum Action
	{
		PERMISSIONS_CHANGED, OPTIONS_CHANGED, INHERITANCE_CHANGED, INFO_CHANGED, TIMEDPERMISSION_EXPIRED, RANK_CHANGED, DEFAULTGROUP_CHANGED, WEIGHT_CHANGED, SAVED, REMOVED, TIMEDGROUP_EXPIRED,
	}
	
	protected PermissibleEntity entity;
	protected Action action;
	
	public PermissibleEntityEvent( PermissibleEntity entity, Action action )
	{
		super( action.toString() );
		
		this.entity = entity;
		this.action = action;
	}
	
	public Action getAction()
	{
		return action;
	}
	
	public PermissibleEntity getEntity()
	{
		return entity;
	}
}
