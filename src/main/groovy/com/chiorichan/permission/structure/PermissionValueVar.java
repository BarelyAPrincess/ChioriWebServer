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

import com.chiorichan.util.ObjectUtil;

public class PermissionValueVar extends PermissionValue<String>
{
	private int maxLen = -1;
	
	public PermissionValueVar( String name, String val, String def, int len )
	{
		super( name, val, def );
		maxLen = len;
	}
	
	public int getMaxLen()
	{
		return maxLen;
	}
	
	@Override
	public String toString()
	{
		return "[type=" + getType() + ",value=" + getValue() + ",maxlen=" + maxLen + "]";
	}
	
	@Override
	public PermissionValue<String> createChild( Object val )
	{
		try
		{
			@SuppressWarnings( "unchecked" )
			PermissionValue<String> newVal = ( PermissionValue<String> ) clone();
			newVal.setValue( ObjectUtil.castToString( val ) );
			return newVal;
		}
		catch ( CloneNotSupportedException e )
		{
			throw new RuntimeException( e );
		}
	}
}
