/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission.structure;

import java.util.List;

import com.chiorichan.framework.Site;

public class ChildPermission
{
	public Permission perm;
	public List<Site> sites;
	public PermissionValue<?> value;
	
	public ChildPermission( Permission parent, List<Site> siteList, PermissionValue<?> childValue )
	{
		perm = parent;
		sites = siteList;
		value = childValue;
	}
	
	public Permission getPermission()
	{
		return perm;
	}
	
	public List<Site> getSites()
	{
		return sites;
	}
	
	public PermissionValue<?> getValue()
	{
		return value;
	}
}
