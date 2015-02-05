/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission.backend;

import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.PermissionBackend;

public abstract class PermissibleGroupProxy extends PermissibleGroup
{
	PermissionBackend backend;
	
	public PermissibleGroupProxy( String name, PermissionBackend permBackend )
	{
		super( name );
		backend = permBackend;
	}
}
