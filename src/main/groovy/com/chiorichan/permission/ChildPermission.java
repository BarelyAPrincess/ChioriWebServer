/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.chiorichan.util.StringFunc;

public final class ChildPermission implements Comparable<ChildPermission>
{
	private final Permission perm;
	private final List<String> refs;
	private final PermissionValue value;
	private final int weight;
	
	/**
	 * References a permission state/value against an entity
	 * 
	 * @param parent
	 *            The permission this value ordains to
	 * @param childValue
	 *            The custom value assigned to this permission. Can be null to use default assigned value.
	 * @param weight
	 *            The sorting weight of this ChildPermission
	 * @param refs
	 *            Array so references that apply
	 */
	public ChildPermission( Permission perm, PermissionValue value, int weight, String... refs )
	{
		this.perm = perm;
		this.value = value;
		this.weight = weight;
		this.refs = new ArrayList<String>( Arrays.asList( StringFunc.toLowerCase( refs ) ) );
	}
	
	@Override
	public int compareTo( ChildPermission child )
	{
		if ( getWeight() == -1 && child.getWeight() == -1 )
			return 0;
		if ( getWeight() == -1 )
			return -1;
		if ( child.getWeight() == -1 )
			return 1;
		return getWeight() - child.getWeight();
	}
	
	public Boolean getBoolean()
	{
		if ( getType() == PermissionType.BOOL )
			return ( Boolean ) value.getValue();
		
		return null;
	}
	
	public Integer getInt()
	{
		if ( getType() == PermissionType.INT )
			return ( Integer ) value.getValue();
		
		return null;
	}
	
	public <T> T getObject()
	{
		return value.getValue();
	}
	
	public Permission getPermission()
	{
		return perm;
	}
	
	public List<String> getReferences()
	{
		return refs;
	}
	
	public String getString()
	{
		if ( getType() == PermissionType.ENUM || getType() == PermissionType.VAR )
			return ( String ) value.getValue();
		
		return null;
	}
	
	public PermissionType getType()
	{
		return perm.getType();
	}
	
	public PermissionValue getValue()
	{
		return value;
	}
	
	public int getWeight()
	{
		return weight;
	}
	
	public boolean isInherited()
	{
		return weight >= 0;
	}
}
