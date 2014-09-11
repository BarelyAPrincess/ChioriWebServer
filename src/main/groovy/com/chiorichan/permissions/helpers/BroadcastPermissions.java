/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permissions.helpers;

import com.chiorichan.permissions.Permission;
import com.chiorichan.permissions.PermissionDefault;

public final class BroadcastPermissions
{
	private static final String ROOT = "bukkit.broadcast";
	private static final String PREFIX = ROOT + ".";
	
	private BroadcastPermissions()
	{
	}
	
	public static Permission registerPermissions( Permission parent )
	{
		Permission broadcasts = DefaultPermissions.registerPermission( ROOT, "Allows the user to receive all broadcast messages", parent );
		
		DefaultPermissions.registerPermission( PREFIX + "admin", "Allows the user to receive administrative broadcasts", PermissionDefault.OP, broadcasts );
		DefaultPermissions.registerPermission( PREFIX + "user", "Allows the user to receive user broadcasts", PermissionDefault.TRUE, broadcasts );
		
		broadcasts.recalculatePermissibles();
		
		return broadcasts;
	}
}
