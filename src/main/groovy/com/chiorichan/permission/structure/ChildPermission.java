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

import java.util.List;

public class ChildPermission
{
	public Permission perm;
	public List<String> refs;
	public PermissionValue<?> value;
	
	public ChildPermission( Permission parent, List<String> refList, PermissionValue<?> childValue )
	{
		perm = parent;
		refs = refList;
		value = childValue;
	}
	
	public Permission getPermission()
	{
		return perm;
	}
	
	public List<String> getRefs()
	{
		return refs;
	}
	
	public PermissionValue<?> getValue()
	{
		return value;
	}
}
