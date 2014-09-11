/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.plugin.messaging;

/**
 * Represents the different directions a plugin channel may go.
 */
public enum PluginChannelDirection
{
	/**
	 * The plugin channel is being sent to the server from a client.
	 */
	INCOMING,
	
	/**
	 * The plugin channel is being sent to a client from the server.
	 */
	OUTGOING
}
