/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission.backend.memory;

import java.util.Map;

import com.chiorichan.permission.backend.MemoryBackend;
import com.chiorichan.permission.backend.PermissibleGroupProxy;
import com.chiorichan.permission.structure.Permission;

public class MemoryGroup extends PermissibleGroupProxy
{
	public MemoryGroup( String name, MemoryBackend backend )
	{
		super( name, backend );
	}
	
	@Override
	public boolean isPermissionSet( String req )
	{
		return false;
	}
	
	@Override
	public boolean isPermissionSet( Permission req )
	{
		return false;
	}
	
	@Override
	public boolean hasPermission( String req )
	{
		return false;
	}
	
	@Override
	public boolean hasPermission( Permission req )
	{
		return false;
	}
	
	@Override
	public boolean isOp()
	{
		return false;
	}
	
	@Override
	public String getPrefix( String siteName )
	{
		return null;
	}
	
	@Override
	public void setPrefix( String prefix, String siteName )
	{
		
	}
	
	@Override
	public String getSuffix( String siteName )
	{
		return null;
	}
	
	@Override
	public void setSuffix( String suffix, String siteName )
	{
		
	}
	
	@Override
	public String[] getPermissions( String site )
	{
		return null;
	}
	
	@Override
	public Map<String, String[]> getAllPermissions()
	{
		return null;
	}
	
	@Override
	public void save()
	{
		
	}
	
	@Override
	public void remove()
	{
		
	}
	
	@Override
	public String[] getSites()
	{
		return null;
	}
}
