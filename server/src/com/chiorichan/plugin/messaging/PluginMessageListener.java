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
