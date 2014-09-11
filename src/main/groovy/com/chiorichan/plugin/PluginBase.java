/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.plugin;


/**
 * Represents a base {@link Plugin}
 * <p>
 * Extend this class if your plugin is not a {@link org.bukkit.plugin.java.JavaPlugin}
 */
public abstract class PluginBase implements Plugin
{
	@Override
	public final int hashCode()
	{
		return getName().hashCode();
	}
	
	@Override
	public final boolean equals( Object obj )
	{
		if ( this == obj )
		{
			return true;
		}
		if ( obj == null )
		{
			return false;
		}
		if ( !( obj instanceof Plugin ) )
		{
			return false;
		}
		try
		{
			return getName().equals( ( (Plugin) obj ).getName() );
		}
		catch ( NullPointerException e )
		{
			return false;
		}
	}
	
	public final String getName()
	{
		return getDescription().getName();
	}
}
