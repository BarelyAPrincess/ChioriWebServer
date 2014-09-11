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
 * The Conversable interface is used to indicate objects that can have conversations.
 */
public interface Conversable
{
	
	/**
	 * Tests to see of a Conversable object is actively engaged in a conversation.
	 * 
	 * @return True if a conversation is in progress
	 */
	public boolean isConversing();
	
	/**
	 * Accepts input into the active conversation. If no conversation is in progress, this method does nothing.
	 * 
	 * @param input
	 *           The input message into the conversation
	 */
	public void acceptConversationInput( String input );
	
	/**
	 * Enters into a dialog with a Conversation object.
	 * 
	 * @param conversation
	 *           The conversation to begin
	 * @return True if the conversation should proceed, false if it has been enqueued
	 */
	public boolean beginConversation( Conversation conversation );
	
	/**
	 * Abandons an active conversation.
	 * 
	 * @param conversation
	 *           The conversation to abandon
	 */
	public void abandonConversation( Conversation conversation );
	
	/**
	 * Abandons an active conversation.
	 * 
	 * @param conversation
	 *           The conversation to abandon
	 * @param details
	 *           Details about why the conversation was abandoned
	 */
	public void abandonConversation( Conversation conversation, ConversationAbandonedEvent details );
	
	/**
	 * Sends this sender a message raw
	 * 
	 * @param message
	 *           Message to be displayed
	 */
	public void sendRawMessage( String message );
}
