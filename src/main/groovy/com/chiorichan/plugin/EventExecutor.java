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

import com.chiorichan.bus.bases.EventException;
import com.chiorichan.bus.events.Event;
import com.chiorichan.bus.events.Listener;

/**
 * Interface which defines the class for event call backs to plugins
 */
public interface EventExecutor
{
	public void execute( Listener listener, Event event ) throws EventException;
}
