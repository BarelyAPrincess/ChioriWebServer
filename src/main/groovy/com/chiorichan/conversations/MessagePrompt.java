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
 * MessagePrompt is the base class for any prompt that only displays a message to the user and requires no input.
 */
public abstract class MessagePrompt implements Prompt
{
	
	public MessagePrompt()
	{
		super();
	}
	
	/**
	 * Message prompts never wait for user input before continuing.
	 * 
	 * @param context
	 *           Context information about the conversation.
	 * @return Always false.
	 */
	public boolean blocksForInput( ConversationContext context )
	{
		return false;
	}
	
	/**
	 * Accepts and ignores any user input, returning the next prompt in the prompt graph instead.
	 * 
	 * @param context
	 *           Context information about the conversation.
	 * @param input
	 *           Ignored.
	 * @return The next prompt in the prompt graph.
	 */
	public Prompt acceptInput( ConversationContext context, String input )
	{
		return getNextPrompt( context );
	}
	
	/**
	 * Override this method to return the next prompt in the prompt graph.
	 * 
	 * @param context
	 *           Context information about the conversation.
	 * @return The next prompt in the prompt graph.
	 */
	protected abstract Prompt getNextPrompt( ConversationContext context );
}
