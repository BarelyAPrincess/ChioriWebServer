/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.event;

/**
 * Used when an event can finish early based on a conditional check<br>
 * Keep in mind that {@link EventPriority#MONITOR} will still fire regardless
 */
public interface Conditional
{
	/**
	 * Should we execute the next {@link RegisteredListener}
	 * 
	 * @param context
	 *            The next {@link RegisteredListener} in the event chain
	 * @return return true to execute
	 */
	boolean conditional( RegisteredListener context ) throws EventException;
}
