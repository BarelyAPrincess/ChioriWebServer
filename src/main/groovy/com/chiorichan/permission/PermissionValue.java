/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;

/**
 * References child values of assigned permissions
 */
public class PermissionValue
{
	private final PermissionModelValue model;
	private final Object value;
	
	public PermissionValue( final PermissionModelValue model, final Object value )
	{
		this.model = model;
		this.value = value;
	}
	
	public PermissionModelValue getModelValue()
	{
		return model;
	}
	
	@SuppressWarnings( "unchecked" )
	public <T> T getValue()
	{
		return ( T ) value;
	}
	
	@Override
	public String toString()
	{
		return String.format( "PermissionValue{model=%s,value=%s}", model, value );
	}
}
