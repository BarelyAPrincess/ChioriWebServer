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

import com.chiorichan.account.bases.Account;


/**
 * A listener for a specific Plugin Channel, which will receive notifications of messages sent from a client.
 */
public interface PluginMessageListener
{
	/**
	 * A method that will be thrown when a PluginMessageSource sends a plugin message on a registered channel.
	 * 
	 * @param channel
	 *           Channel that the message was sent through.
	 * @param player
	 *           Source of the message.
	 * @param message
	 *           The raw message that was sent.
	 */
	public void onPluginMessageReceived( String channel, Account player, byte[] message );
}
