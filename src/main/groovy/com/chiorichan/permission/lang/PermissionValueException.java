/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.lang;

public class PermissionValueException extends PermissionException
{
	private static final long serialVersionUID = -4762649378128218189L;
	
	public PermissionValueException( String message )
	{
		super( message );
	}
	
	public PermissionValueException( String message, Object... objs )
	{
		super( String.format( message, objs ) );
	}
}
