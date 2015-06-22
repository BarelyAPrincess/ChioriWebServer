/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.event;

import com.chiorichan.plugin.PluginDescriptionFile;

public interface EventCreator
{
	/**
	 * Returns the plugin.yaml file containing the details for this plugin
	 * 
	 * @return Contents of the plugin.yaml file
	 */
	PluginDescriptionFile getDescription();
	
	/**
	 * Returns a value indicating whether or not this plugin is currently enabled
	 * 
	 * @return true if this plugin is enabled, otherwise false
	 */
	boolean isEnabled();
	
	/**
	 * Returns the name of the plugin.
	 * <p>
	 * This should return the bare name of the plugin and should be used for comparison.
	 * 
	 * @return name of the plugin
	 */
	String getName();
}
