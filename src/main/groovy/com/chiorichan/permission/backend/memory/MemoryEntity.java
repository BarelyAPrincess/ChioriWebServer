/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.backend.memory;

import com.chiorichan.permission.PermissibleEntityProxy;

public class MemoryEntity extends PermissibleEntityProxy
{
	public MemoryEntity( String userName, MemoryBackend backend )
	{
		super( userName, backend );
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
	public void reloadPermissions()
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void reloadGroups()
	{
		// TODO Auto-generated method stub
	}
}
