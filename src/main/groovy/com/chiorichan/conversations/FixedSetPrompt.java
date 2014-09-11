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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * FixedSetPrompt is the base class for any prompt that requires a fixed set response from the user.
 */
public abstract class FixedSetPrompt extends ValidatingPrompt
{
	
	protected List<String> fixedSet;
	
	/**
	 * Creates a FixedSetPrompt from a set of strings. foo = new FixedSetPrompt("bar", "cheese", "panda");
	 * 
	 * @param fixedSet
	 *           A fixed set of strings, one of which the user must type.
	 */
	public FixedSetPrompt(String... fixedSet)
	{
		super();
		this.fixedSet = Arrays.asList( fixedSet );
	}
	
	@Override
	protected boolean isInputValid( ConversationContext context, String input )
	{
		return fixedSet.contains( input );
	}
	
	/**
	 * Utility function to create a formatted string containing all the options declared in the constructor.
	 * 
	 * @return the options formatted like "[bar, cheese, panda]" if bar, cheese, and panda were the options used
	 */
	protected String formatFixedSet()
	{
		return "[" + StringUtils.join( fixedSet, ", " ) + "]";
	}
}
