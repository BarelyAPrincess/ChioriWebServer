/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission.structure;

public abstract class PermissionValue<T>
{
	private T value;
	private T def;
	private final String name;
	
	protected PermissionValue( String name, T value, T def )
	{
		this.name = name;
		this.value = value;
		this.def = def;
	}
	
	public T getValue()
	{
		return value;
	}
	
	public T getDefault()
	{
		return def;
	}
	
	public String getName()
	{
		return name;
	}
	
	@Override
	public String toString()
	{
		return "[type=" + getType() + ",value=" + getValue() + ",default=" + getDefault() + "]";
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
	
	public abstract PermissionValue<T> createChild( Object val );
	
	protected void setValue( T value )
	{
		this.value = value;
	}
	
	protected void setDefault( T val )
	{
		def = val;
	}
	
	public enum PermissionType
	{
		UNKNOWN, BOOL, ENUM, VAR, INT;
		
		public String toString()
		{
			return name();
		}
		
		public boolean hasMaxLen()
		{
			return this == ENUM || this == VAR;
		}
	}
}
