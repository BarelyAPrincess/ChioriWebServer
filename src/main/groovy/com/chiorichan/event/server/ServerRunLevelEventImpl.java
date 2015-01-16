package com.chiorichan.event.server;

import com.chiorichan.RunLevel;

public class ServerRunLevelEventImpl extends ServerRunLevelEvent
{
	public void setRunLevel( RunLevel level )
	{
		previousLevel = currentLevel;
		currentLevel = level;
	}
}
