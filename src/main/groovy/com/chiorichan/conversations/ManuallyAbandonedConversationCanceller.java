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
 * The ManuallyAbandonedConversationCanceller is only used as part of a {@link ConversationAbandonedEvent} to indicate
 * that the conversation was manually abandoned by programatically calling the abandon() method on it.
 */
public class ManuallyAbandonedConversationCanceller implements ConversationCanceller
{
	public void setConversation( Conversation conversation )
	{
		throw new UnsupportedOperationException();
	}
	
	public boolean cancelBasedOnInput( ConversationContext context, String input )
	{
		throw new UnsupportedOperationException();
	}
	
	public ConversationCanceller clone()
	{
		throw new UnsupportedOperationException();
	}
}
