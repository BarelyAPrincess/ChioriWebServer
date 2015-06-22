/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.event;

import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.lang.PluginDescriptionInvalidException;
import com.chiorichan.plugin.PluginDescriptionFile;

public abstract class BuiltinEventCreator implements EventCreator
{
	private YamlConfiguration yaml = new YamlConfiguration();
	
	@Override
	public final PluginDescriptionFile getDescription()
	{
		try
		{
			return new PluginDescriptionFile( yaml );
		}
		catch ( PluginDescriptionInvalidException e )
		{
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public boolean isEnabled()
	{
		return true;
	}
}
