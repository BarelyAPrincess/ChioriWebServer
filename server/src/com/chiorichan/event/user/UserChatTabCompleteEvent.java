package com.chiorichan.event.user;

import java.util.Collection;

import org.apache.commons.lang3.Validate;

import com.chiorichan.event.HandlerList;
import com.chiorichan.user.User;

/**
 * Called when a User attempts to tab-complete a chat message.
 */
public class UserChatTabCompleteEvent extends UserEvent
{
	private static final HandlerList handlers = new HandlerList();
	private final String message;
	private final String lastToken;
	private final Collection<String> completions;
	
	public UserChatTabCompleteEvent(final User who, final String message, final Collection<String> completions)
	{
		super( who );
		Validate.notNull( message, "Message cannot be null" );
		Validate.notNull( completions, "Completions cannot be null" );
		this.message = message;
		int i = message.lastIndexOf( ' ' );
		if ( i < 0 )
		{
			this.lastToken = message;
		}
		else
		{
			this.lastToken = message.substring( i + 1 );
		}
		this.completions = completions;
	}
	
	/**
	 * Gets the chat message being tab-completed.
	 * 
	 * @return the chat message
	 */
	public String getChatMessage()
	{
		return message;
	}
	
	/**
	 * Gets the last 'token' of the message being tab-completed. The token is the substring starting with the character
	 * after the last space in the message.
	 * 
	 * @return The last token for the chat message
	 */
	public String getLastToken()
	{
		return lastToken;
	}
	
	/**
	 * This is the collection of completions for this event.
	 * 
	 * @return the current completions
	 */
	public Collection<String> getTabCompletions()
	{
		return completions;
	}
	
	@Override
	public HandlerList getHandlers()
	{
		return handlers;
	}
	
	public static HandlerList getHandlerList()
	{
		return handlers;
	}
}
