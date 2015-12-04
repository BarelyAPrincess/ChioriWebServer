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
import com.chiorichan.plugin.PluginInformation;
import com.chiorichan.plugin.lang.PluginInformationException;
import com.chiorichan.tasks.TaskRegistrar;

public abstract class BuiltinRegistrar implements EventRegistrar, TaskRegistrar
{
	private YamlConfiguration yaml = new YamlConfiguration();
	
	@Override
	public final PluginInformation getDescription()
	{
		try
		{
			return new PluginInformation( yaml );
		}
		catch ( PluginInformationException e )
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
