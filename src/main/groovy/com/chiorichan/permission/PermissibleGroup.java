/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission;

import com.chiorichan.Loader;
import com.chiorichan.permission.event.PermissibleEntityEvent;

public abstract class PermissibleGroup extends PermissibleEntity implements Comparable<PermissibleGroup>
{
	protected int weight = 0;
	
	public PermissibleGroup( String groupName )
	{
		super( groupName );
	}
	
	public int getWeight()
	{
		return weight;
	}
	
	public void setWeight( int weight )
	{
		this.weight = weight;
		Loader.getPermissionManager().callEvent( new PermissibleEntityEvent( this, PermissibleEntityEvent.Action.WEIGHT_CHANGED ) );
	}
	
	@Override
	public int compareTo( PermissibleGroup o )
	{
		return this.getWeight() - o.getWeight();
	}
}
