package com.chiorichan.event.server;

import com.chiorichan.RunLevel;

public abstract class ServerRunLevelEvent extends ServerEvent
{
	protected static RunLevel previousLevel;
	protected static RunLevel currentLevel;
	
	public RunLevel getLastRunLevel()
	{
		return previousLevel;
	}
	
	public RunLevel getRunLevel()
	{
		return currentLevel;
	}
}
