/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission.structure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.chiorichan.permission.PermissionManager;
import com.chiorichan.util.ObjectUtil;
import com.google.common.collect.Sets;

public class PermissionValueEnum extends PermissionValue<String>
{
	private Set<String> enumList = Sets.newHashSet();
	private int maxLen = -1;
	
	public PermissionValueEnum( String name, String val, int len, Set<String> enums )
	{
		super( name, val );
		maxLen = len;
		enumList = enums;
		
		if ( !enumList.contains( val ) )
			PermissionManager.getLogger().warning( "It would appear that permission '" + name + "' of type 'Enum' has a value of '" + val + "' which is not in the list of available enums '" + enumList.toString() + "', it would be recommended that this is fixed." );
	}
	
	public PermissionValueEnum( String name, String val, int len, List<String> enums )
	{
		this( name, val, len, new HashSet<String>( enums ) );
	}
	
	public PermissionValueEnum( String name, String val, int len, String... enums )
	{
		this( name, val, len, Arrays.asList( enums ) );
	}
	
	public Set<String> getEnumList()
	{
		return enumList;
	}
	
	public int getMaxLen()
	{
		return maxLen;
	}
	
	@Override
	public String toString()
	{
		return "[type=" + getType() + ",value=" + getValue() + ",enumList=" + enumList + ",maxlen=" + maxLen + "]";
	}
	
	@Override
	public PermissionValue<String> createChild( Object val )
	{
		try
		{
			@SuppressWarnings( "unchecked" )
			PermissionValue<String> newVal = (PermissionValue<String>) clone();
			newVal.setValue( ObjectUtil.castToString( val ) );
			return newVal;
		}
		catch( CloneNotSupportedException e )
		{
			throw new RuntimeException( e );
		}
	}
}
