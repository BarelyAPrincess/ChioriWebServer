/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission.structure;

import com.chiorichan.util.ObjectUtil;

public class PermissionValueBoolean extends PermissionValue<Boolean>
{
	public PermissionValueBoolean( String name, Boolean val )
	{
		super( name, val );
	}
	
	@Override
	public PermissionValue<Boolean> createChild( Object val )
	{
		try
		{
			@SuppressWarnings( "unchecked" )
			PermissionValue<Boolean> newVal = (PermissionValue<Boolean>) clone();
			newVal.setValue( ObjectUtil.castToBool( val ) );
			return newVal;
		}
		catch( CloneNotSupportedException e )
		{
			throw new RuntimeException( e );
		}
	}
}