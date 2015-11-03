/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan;

import com.chiorichan.event.server.ServerEvent;

public class ServerRunLevelEvent extends ServerEvent
{
	protected static RunLevel previousLevel;
	protected static RunLevel currentLevel;
	
	public ServerRunLevelEvent()
	{
		currentLevel = RunLevel.INITIALIZATION;
	}
	
	public ServerRunLevelEvent( RunLevel level )
	{
		currentLevel = level;
	}
	
	public RunLevel getLastRunLevel()
	{
		return previousLevel;
	}
	
	public RunLevel getRunLevel()
	{
		return currentLevel;
	}
	
	void setRunLevel( RunLevel level )
	{
		previousLevel = currentLevel;
		currentLevel = level;
		
		Loader.getLogger().fine( "Server Runlevel has been changed to '" + level.name() + "'" );
	}
}
