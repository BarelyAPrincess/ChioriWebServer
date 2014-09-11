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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;

/**
 * BooleanPrompt is the base class for any prompt that requires a boolean response from the user.
 */
public abstract class BooleanPrompt extends ValidatingPrompt
{
	
	public BooleanPrompt()
	{
		super();
	}
	
	@Override
	protected boolean isInputValid( ConversationContext context, String input )
	{
		String[] accepted = { "true", "false", "on", "off", "yes", "no" };
		return ArrayUtils.contains( accepted, input.toLowerCase() );
	}
	
	@Override
	protected Prompt acceptValidatedInput( ConversationContext context, String input )
	{
		return acceptValidatedInput( context, BooleanUtils.toBoolean( input ) );
	}
	
	/**
	 * Override this method to perform some action with the user's boolean response.
	 * 
	 * @param context
	 *           Context information about the conversation.
	 * @param input
	 *           The user's boolean response.
	 * @return The next {@link Prompt} in the prompt graph.
	 */
	protected abstract Prompt acceptValidatedInput( ConversationContext context, boolean input );
}
