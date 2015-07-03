/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.terminal.commands.advanced;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.permission.PermissibleEntity;

public class CommandBinding
{
	protected Method method;
	protected Object object;
	protected Map<String, String> params = new HashMap<String, String>();
	
	public CommandBinding( Object object, Method method )
	{
		this.object = object;
		this.method = method;
	}
	
	public void call( Object... args ) throws Exception
	{
		method.invoke( object, args );
	}
	
	public boolean checkPermissions( PermissibleEntity entity )
	{
		String permission = getMethodAnnotation().permission();
		
		if ( permission.contains( "<" ) )
			for ( Entry<String, String> entry : getParams().entrySet() )
				if ( entry.getValue() != null )
					permission = permission.replace( "<" + entry.getKey() + ">", entry.getValue().toLowerCase() );
		
		return entity.checkPermission( permission ).isTrue();
	}
	
	public CommandHandler getMethodAnnotation()
	{
		return method.getAnnotation( CommandHandler.class );
	}
	
	public Map<String, String> getParams()
	{
		return params;
	}
	
	public void setParams( Map<String, String> params )
	{
		this.params = params;
	}
}
