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
import com.chiorichan.plugin.Plugin;

/**
 * An InactivityConversationCanceller will cancel a {@link Conversation} after a period of inactivity by the user.
 */
public class InactivityConversationCanceller implements ConversationCanceller
{
	protected Plugin plugin;
	protected int timeoutSeconds;
	protected Conversation conversation;
	private int taskId = -1;
	
	/**
	 * Creates an InactivityConversationCanceller.
	 * 
	 * @param plugin
	 *           The owning plugin.
	 * @param timeoutSeconds
	 *           The number of seconds of inactivity to wait.
	 */
	public InactivityConversationCanceller(Plugin plugin, int timeoutSeconds)
	{
		this.plugin = plugin;
		this.timeoutSeconds = timeoutSeconds;
	}
	
	public void setConversation( Conversation conversation )
	{
		this.conversation = conversation;
		startTimer();
	}
	
	public boolean cancelBasedOnInput( ConversationContext context, String input )
	{
		// Reset the inactivity timer
		stopTimer();
		startTimer();
		return false;
	}
	
	public ConversationCanceller clone()
	{
		return new InactivityConversationCanceller( plugin, timeoutSeconds );
	}
	
	/**
	 * Starts an inactivity timer.
	 */
	private void startTimer()
	{
		taskId = Loader.getScheduler().scheduleSyncDelayedTask( plugin, new Runnable()
		{
			public void run()
			{
				if ( conversation.getState() == Conversation.ConversationState.UNSTARTED )
				{
					startTimer();
				}
				else if ( conversation.getState() == Conversation.ConversationState.STARTED )
				{
					cancelling( conversation );
					conversation.abandon( new ConversationAbandonedEvent( conversation, InactivityConversationCanceller.this ) );
				}
			}
		}, timeoutSeconds * 20 );
	}
	
	/**
	 * Stops the active inactivity timer.
	 */
	private void stopTimer()
	{
		if ( taskId != -1 )
		{
			Loader.getScheduler().cancelTask( taskId );
			taskId = -1;
		}
	}
	
	/**
	 * Subclasses of InactivityConversationCanceller can override this method to take additional actions when the
	 * inactivity timer abandons the conversation.
	 * 
	 * @param conversation
	 *           The conversation being abandoned.
	 */
	protected void cancelling( Conversation conversation )
	{
		
	}
}
