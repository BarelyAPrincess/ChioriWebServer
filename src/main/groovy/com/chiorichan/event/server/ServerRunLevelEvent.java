/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.event.server;

import com.chiorichan.RunLevel;

public abstract class ServerRunLevelEvent extends ServerEvent
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
}
