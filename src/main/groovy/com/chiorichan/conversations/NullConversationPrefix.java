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
 * NullConversationPrefix is a {@link ConversationPrefix} implementation that displays nothing in front of conversation
 * output.
 */
public class NullConversationPrefix implements ConversationPrefix
{
	
	/**
	 * Prepends each conversation message with an empty string.
	 * 
	 * @param context
	 *           Context information about the conversation.
	 * @return An empty string.
	 */
	public String getPrefix( ConversationContext context )
	{
		return "";
	}
}
