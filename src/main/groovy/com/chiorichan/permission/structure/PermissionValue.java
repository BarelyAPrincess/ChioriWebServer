/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission.structure;

public abstract class PermissionValue<Type>
{
	private Type value;
	private final String name;
	
	protected PermissionValue( String permName, Type type )
	{
		value = type;
		name = permName;
	}
	
	public Type getValue()
	{
		return value;
	}
	
	public String getName()
	{
		return name;
	}
	
	@Override
	public String toString()
	{
		return "[type=" + getType() + ",value=" + getValue() + "]";
	}
	
	public PermissionType getType()
	{
		switch ( this.getClass().getSimpleName() )
		{
			case "PermissionValueBoolean":
				return PermissionType.BOOL;
			case "PermissionValueEnum":
				return PermissionType.ENUM;
			case "PermissionValueVar":
				return PermissionType.VAR;
			case "PermissionValueInt":
				return PermissionType.INT;
			default:
				return PermissionType.UNKNOWN;
		}
	}
	
	public abstract PermissionValue<Type> createChild( Object val );
	
	protected void setValue( Type val )
	{
		value = val;
	}
	
	public enum PermissionType
	{
		UNKNOWN, BOOL, ENUM, VAR, INT;
	}
}
