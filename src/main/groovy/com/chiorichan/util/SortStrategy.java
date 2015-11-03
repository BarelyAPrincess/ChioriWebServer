/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.util;

/**
 * Provides a desired Sorting Strategy to utility classes
 */
enum SortStrategy
{
	/**
	 * If the map contains more than one key with the same value, predecessors will be overridden
	 */
	Default,
	/**
	 * Will increment keys to next available index to make room
	 */
	MoveNext,
	/**
	 * Will decrement keys to last available index to make room
	 */
	MovePrevious;
}
