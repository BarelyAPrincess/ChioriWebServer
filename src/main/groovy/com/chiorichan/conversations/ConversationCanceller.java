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
 * A ConversationCanceller is a class that cancels an active {@link Conversation}. A Conversation can have more than one
 * ConversationCanceller.
 */
public interface ConversationCanceller extends Cloneable
{
	
	/**
	 * Sets the conversation this ConversationCanceller can optionally cancel.
	 * 
	 * @param conversation
	 *           A conversation.
	 */
	public void setConversation( Conversation conversation );
	
	/**
	 * Cancels a conversation based on user input.
	 * 
	 * @param context
	 *           Context information about the conversation.
	 * @param input
	 *           The input text from the user.
	 * @return True to cancel the conversation, False otherwise.
	 */
	public boolean cancelBasedOnInput( ConversationContext context, String input );
	
	/**
	 * Allows the {@link ConversationFactory} to duplicate this ConversationCanceller when creating a new
	 * {@link Conversation}.
	 * <p>
	 * Implementing this method should reset any internal object state.
	 * 
	 * @return A clone.
	 */
	public ConversationCanceller clone();
}
