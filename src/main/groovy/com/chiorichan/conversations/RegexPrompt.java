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

import java.util.regex.Pattern;

/**
 * RegexPrompt is the base class for any prompt that requires an input validated by a regular expression.
 */
public abstract class RegexPrompt extends ValidatingPrompt
{
	
	private Pattern pattern;
	
	public RegexPrompt(String regex)
	{
		this( Pattern.compile( regex ) );
	}
	
	public RegexPrompt(Pattern pattern)
	{
		super();
		this.pattern = pattern;
	}
	
	@Override
	protected boolean isInputValid( ConversationContext context, String input )
	{
		return pattern.matcher( input ).matches();
	}
}
