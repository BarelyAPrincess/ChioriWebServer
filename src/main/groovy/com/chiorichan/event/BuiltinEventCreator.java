package com.chiorichan.event;

import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.plugin.InvalidDescriptionException;
import com.chiorichan.plugin.PluginDescriptionFile;

public abstract class BuiltinEventCreator implements EventCreator
{
	private YamlConfiguration yaml = new YamlConfiguration();
	
	final public PluginDescriptionFile getDescription()
	{
		try
		{
			return new PluginDescriptionFile( yaml );
		}
		catch ( InvalidDescriptionException e )
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean isEnabled()
	{
		return true;
	}
}
