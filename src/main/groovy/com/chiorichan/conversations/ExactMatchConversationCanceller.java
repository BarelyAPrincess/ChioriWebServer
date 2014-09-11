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
 * An ExactMatchConversationCanceller cancels a conversation if the user enters an exact input string
 */
public class ExactMatchConversationCanceller implements ConversationCanceller
{
	private String escapeSequence;
	
	/**
	 * Builds an ExactMatchConversationCanceller.
	 * 
	 * @param escapeSequence
	 *           The string that, if entered by the user, will cancel the conversation.
	 */
	public ExactMatchConversationCanceller(String escapeSequence)
	{
		this.escapeSequence = escapeSequence;
	}
	
	public void setConversation( Conversation conversation )
	{
	}
	
	public boolean cancelBasedOnInput( ConversationContext context, String input )
	{
		return input.equals( escapeSequence );
	}
	
	public ConversationCanceller clone()
	{
		return new ExactMatchConversationCanceller( escapeSequence );
	}
}
