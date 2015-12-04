/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.tasks;

public interface TaskRegistrar
{
	/**
	 * Returns a value indicating whether or not this creator is currently enabled
	 * 
	 * @return true if this creator is enabled, otherwise false
	 */
	boolean isEnabled();
	
	/**
	 * Returns the name of the creator.
	 * <p>
	 * This should return the bare name of the creator and should be used for comparison.
	 * 
	 * @return name of the creator
	 */
	String getName();
}
