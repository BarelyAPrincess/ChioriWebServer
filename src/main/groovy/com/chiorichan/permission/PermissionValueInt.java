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

import com.chiorichan.util.ObjectFunc;

public class PermissionValueInt extends PermissionValue<Integer>
{
	public PermissionValueInt( String name, Integer val, Integer def )
	{
		super( name, val, def );
	}
	
	@Override
	public PermissionValue<Integer> createChild( Object val )
	{
		try
		{
			@SuppressWarnings( "unchecked" )
			PermissionValue<Integer> newVal = ( PermissionValue<Integer> ) clone();
			newVal.setValue( ObjectFunc.castToLong( val ).intValue() );
			return newVal;
		}
		catch ( CloneNotSupportedException e )
		{
			throw new RuntimeException( e );
		}
	}
}
