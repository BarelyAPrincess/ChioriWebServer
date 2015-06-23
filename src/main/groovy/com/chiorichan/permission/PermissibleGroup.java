/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;

import com.chiorichan.permission.event.PermissibleEntityEvent;

public abstract class PermissibleGroup extends PermissibleEntity implements Comparable<PermissibleGroup>
{
	private int weight = 0;
	
	public PermissibleGroup( String groupName )
	{
		super( groupName );
	}
	
	@Override
	public final int compareTo( PermissibleGroup o )
	{
		return getWeight() - o.getWeight();
	}
	
	public final int getWeight()
	{
		return weight;
	}
	
	@Override
	boolean isGroup()
	{
		return true;
	}
	
	public final void setWeight( int weight )
	{
		this.weight = weight;
		PermissionManager.callEvent( new PermissibleEntityEvent( this, PermissibleEntityEvent.Action.WEIGHT_CHANGED ) );
	}
}
