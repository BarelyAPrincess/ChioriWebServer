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

import java.util.EventListener;

/**
 */
public interface ConversationAbandonedListener extends EventListener
{
	/**
	 * Called whenever a {@link Conversation} is abandoned.
	 * 
	 * @param abandonedEvent
	 *           Contains details about the abandoned conversation.
	 */
	public void conversationAbandoned( ConversationAbandonedEvent abandonedEvent );
}
