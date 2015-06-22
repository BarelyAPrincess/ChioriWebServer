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
import com.google.common.collect.Lists;

/**
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class ChildPermission
{
	private boolean isInherited;
	private Permission parent;
	private List<String> refs;
	private PermissionValue value;
	
	/**
	 * References a permission state/value against an entity
	 * 
	 * @param parent
	 *            The permission this value ordains to
	 * @param refList
	 *            A list of references such as connection or ip this permission would apply
	 * @param childValue
	 *            The custom value assigned to this permission. Can be null to use default assigned value.
	 * @param isInherited
	 *            Was this value given to the entity because it was a member of a group?
	 */
	public ChildPermission( Permission parent, PermissionValue value, boolean isInherited, List<String> refs )
	{
		if ( refs == null )
			refs = Lists.newArrayList();
		
		refs = StringFunc.toLowerCase( refs );
		
		this.parent = parent;
		this.refs = refs;
		this.value = value;
		this.isInherited = isInherited;
	}
	
	public ChildPermission( Permission perm, PermissionValue value, boolean isInherited, String... refs )
	{
		this( perm, value, isInherited, new ArrayList<String>( Arrays.asList( refs ) ) );
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
		return parent;
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
		return parent.getType();
	}
	
	public PermissionValue getValue()
	{
		return value;
	}
	
	public boolean isInherited()
	{
		return isInherited;
	}
	
	/**
	 * Sets the custom value for the entity specified.
	 * Empty or null will set value to Permission default.
	 */
	public void setValue( Object val )
	{
		if ( val == null || val.equals( "" ) )
			value = parent.getModel().getModelValue(); // Use Parent Value
		else
			setValue( val );
		
		// TODO Save custom value to backend
	}
}
