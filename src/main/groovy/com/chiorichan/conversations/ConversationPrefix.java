/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.conversations;


/**
 * A ConversationPrefix implementation prepends all output from the conversation to the player. The ConversationPrefix
 * can be used to display the plugin name or conversation status as the conversation evolves.
 */
public interface ConversationPrefix
{
	
	/**
	 * Gets the prefix to use before each message to the player.
	 * 
	 * @param context
	 *           Context information about the conversation.
	 * @return The prefix text.
	 */
	String getPrefix( ConversationContext context );
}
