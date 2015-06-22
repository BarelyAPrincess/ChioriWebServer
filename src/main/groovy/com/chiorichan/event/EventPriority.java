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
 * Represents an event's priority in execution
 */
public enum EventPriority
{
	/**
	 * Event call is of very low importance and should be ran first, to allow other plugins to further customize the
	 * outcome
	 */
	LOWEST( 0 ),
	/**
	 * Event call is of low importance
	 */
	LOW( 1 ),
	/**
	 * Event call is neither important or unimportant, and may be ran normally
	 */
	NORMAL( 2 ),
	/**
	 * Event call is of high importance
	 */
	HIGH( 3 ),
	/**
	 * Event call is critical and must have the final say in what happens to the event
	 */
	HIGHEST( 4 ),
	/**
	 * Event is listened to purely for monitoring the outcome of an event.
	 * <p/>
	 * No modifications to the event should be made under this priority
	 */
	MONITOR( 5 );
	
	private final int slot;
	
	private EventPriority( int slot )
	{
		this.slot = slot;
	}
	
	public int getSlot()
	{
		return slot;
	}
}
