/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;

public final class ChildPermission implements Comparable<ChildPermission>
{
	private final PermissibleEntity entity;
	private final Permission perm;
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
	 *            Instance of References
	 */
	public ChildPermission( PermissibleEntity entity, Permission perm, PermissionValue value, int weight )
	{
		this.entity = entity;
		this.perm = perm;
		this.value = value;
		this.weight = weight;
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
	
	public References getReferences()
	{
		return entity.getPermissionReferences( perm );
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
	
	@Override
	public String toString()
	{
		return String.format( "ChildPermission{entity=%s,node=%s,value=%s,weight=%s}", entity.getId(), perm.getNamespace(), value.getValue(), weight );
	}
}
