/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.bus.events;

public interface Cancellable
{
	/**
	 * Gets the cancellation state of this event. A cancelled event will not be executed in the server, but will still
	 * pass to other plugins
	 * 
	 * @return true if this event is cancelled
	 */
	public boolean isCancelled();
	
	/**
	 * Sets the cancellation state of this event. A cancelled event will not be executed in the server, but will still
	 * pass to other plugins.
	 * 
	 * @param cancel
	 *           true if you wish to cancel this event
	 */
	public void setCancelled( boolean cancel );
}
