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
 * StringPrompt is the base class for any prompt that accepts an arbitrary string from the user.
 */
public abstract class StringPrompt implements Prompt
{
	
	/**
	 * Ensures that the prompt waits for the user to provide input.
	 * 
	 * @param context
	 *           Context information about the conversation.
	 * @return True.
	 */
	public boolean blocksForInput( ConversationContext context )
	{
		return true;
	}
}
