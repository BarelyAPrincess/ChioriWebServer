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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.chiorichan.util.StringUtil;
import com.google.common.collect.Lists;

public class ChildPermission<T>
{
	public Permission perm;
	public List<String> refs;
	public PermissionValue<T> value;
	public boolean isInherited;
	
	public ChildPermission( Permission perm, PermissionValue<T> value, boolean isInherited, String... refs )
	{
		this( perm, value, isInherited, new ArrayList<String>( Arrays.asList( refs ) ) );
	}
	
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
	public ChildPermission( Permission perm, PermissionValue<T> value, boolean isInherited, List<String> refs )
	{
		if ( refs == null )
			refs = Lists.newArrayList();
		
		refs = StringUtil.toLowerCase( refs );
		
		this.perm = perm;
		this.refs = refs;
		this.value = value;
		this.isInherited = isInherited;
	}
	
	public Permission getPermission()
	{
		return perm;
	}
	
	public List<String> getReferences()
	{
		return refs;
	}
	
	public PermissionValue<?> getValue()
	{
		return value;
	}
	
	/**
	 * Sets the custom value for the entity specified.
	 * Empty or null will set value to Permission default.
	 */
	public void setValue( T val )
	{
		if ( val == null || val.equals( "" ) )
			value = null;
		else
			value.setValue( val );
		
		// TODO Save custom value to backend
	}
	
	public T getObject()
	{
		return value.getValue();
	}
	
	public String getString()
	{
		if ( value.getType() == PermissionValue.PermissionType.ENUM || value.getType() == PermissionValue.PermissionType.VAR )
			return ( String ) value.getValue();
		
		return null;
	}
	
	public Integer getInt()
	{
		if ( value.getType() == PermissionValue.PermissionType.INT )
			return ( Integer ) value.getValue();
		
		return null;
	}
	
	public Boolean getBoolean()
	{
		if ( value.getType() == PermissionValue.PermissionType.BOOL )
			return ( Boolean ) value.getValue();
		
		return null;
	}
}
