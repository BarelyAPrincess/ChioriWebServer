/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission;

public interface Permissible extends PermissibleParent
{
	/**
	 * @return String
	 * 			a unique name
	 */
	public String getName();
	
	/**
	 * Web users id will be in the form of `siteId`_`acctId`.
	 * @return String
	 * 			a unique identifier
	 */
	public String getId();
	
	/**
	 * Handler type
	 * @return PermissibleType
	 * 			the connection method
	 */
	public PermissibleType getType();
	
	/**
	 * If the entity is connected remotely then return the Remote Address.
	 * @return String
	 * 			an IPv4/IPv6 Address or null if no remote handlers
	 */
	public String getIpAddr();
}
