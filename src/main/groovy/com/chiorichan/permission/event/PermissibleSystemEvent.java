/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.event;

public class PermissibleSystemEvent extends PermissibleEvent
{
	public enum Action
	{
		BACKEND_CHANGED, RELOADED, WORLDINHERITANCE_CHANGED, DEFAULTGROUP_CHANGED, DEBUGMODE_TOGGLE, REINJECT_PERMISSIBLES,
	}
	
	protected Action action;
	
	public PermissibleSystemEvent( Action action )
	{
		super( action.toString() );
		
		this.action = action;
	}
	
	public Action getAction()
	{
		return action;
	}
}
