/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission;

import com.chiorichan.permission.structure.Permission;

interface PermissibleParent
{
	/**
	 * 
	 * @param req
	 *             The permissible to check against
	 * @return Boolean
	 *         Is this entity permitted
	 */
	public boolean isPermissionSet( String req );
	
	public boolean isPermissionSet( Permission req );
	
	/**
	 * 
	 * @param req
	 *             The permissible to check against
	 * @return Boolean
	 *         Is this entity permitted
	 */
	public boolean hasPermission( String req );
	
	public boolean hasPermission( Permission req );
	
	/**
	 * Is this permissible on the OP list.
	 * 
	 * @return true if OP
	 */
	public boolean isOp();
}