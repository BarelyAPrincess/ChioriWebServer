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

import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.plugin.Plugin;

/**
 * PlayerNamePrompt is the base class for any prompt that requires the player to enter another player's name.
 */
public abstract class PlayerNamePrompt extends ValidatingPrompt
{
	//private Plugin plugin;
	
	public PlayerNamePrompt(Plugin plugin)
	{
		super();
		//this.plugin = plugin;
	}
	
	@Override
	protected boolean isInputValid( ConversationContext context, String input )
	{
		return Loader.getAccountsManager().getAccount( input ) != null;
		
	}
	
	@Override
	protected Prompt acceptValidatedInput( ConversationContext context, String input )
	{
		return acceptValidatedInput( context, Loader.getAccountsManager().getAccount( input ) );
	}
	
	/**
	 * Override this method to perform some action with the user's player name response.
	 * 
	 * @param context
	 *             Context information about the conversation.
	 * @param input
	 *             The user's player name response.
	 * @return The next {@link Prompt} in the prompt graph.
	 */
	protected abstract Prompt acceptValidatedInput( ConversationContext context, Account input );
}
