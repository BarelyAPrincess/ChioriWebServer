package com.chiorichan.event.user;

import java.util.IllegalFormatException;
import java.util.Set;

import com.chiorichan.event.Cancellable;
import com.chiorichan.event.HandlerList;
import com.chiorichan.user.User;

/**
 * This event will sometimes fire synchronously, depending on how it was triggered. The constructor provides a boolean
 * to indicate if the event was fired synchronously or asynchronously. When asynchronous, this event can be called from
 * any thread, but the main thread, and has limited access to the API.<br>
 * <br>
 * If a User is the direct cause of this event by incoming packet, this event will be asynchronous. If a plugin triggers
 * this event by compelling a User to chat, this event will be synchronous.<br>
 * <br>
 * <b>Care should be taken to check {@link #isAsynchronous()} and treat the event appropriately.</b>
 */
public class AsyncUserChatEvent extends UserEvent implements Cancellable
{
	private static final HandlerList handlers = new HandlerList();
	private boolean cancel = false;
	private String message;
	private String format = "<%1$s> %2$s";
	private final Set<User> recipients;
	
	/**
	 * 
	 * @param async
	 *           This changes the event to a synchronous state.
	 * @param who
	 *           the chat sender
	 * @param message
	 *           the message sent
	 * @param Users
	 *           the Users to receive the message. This may be a lazy or unmodifiable collection.
	 */
	public AsyncUserChatEvent(final boolean async, final User who, final String message, final Set<User> Users)
	{
		super( who, async );
		this.message = message;
		recipients = Users;
	}
	
	/**
	 * Gets the message that the User is attempting to send. This message will be used with {@link #getFormat()}.
	 * 
	 * @return Message the User is attempting to send
	 */
	public String getMessage()
	{
		return message;
	}
	
	/**
	 * Sets the message that the User will send. This message will be used with {@link #getFormat()}.
	 * 
	 * @param message
	 *           New message that the User will send
	 */
	public void setMessage( String message )
	{
		this.message = message;
	}
	
	/**
	 * Gets the format to use to display this chat message. When this event finishes execution, the first format
	 * parameter is the {@link User#getDisplayName()} and the second parameter is {@link #getMessage()}
	 * 
	 * @return {@link String#format(String, Object...)} compatible format string
	 */
	public String getFormat()
	{
		return format;
	}
	
	/**
	 * Sets the format to use to display this chat message. When this event finishes execution, the first format
	 * parameter is the {@link User#getDisplayName()} and the second parameter is {@link #getMessage()}
	 * 
	 * @param format
	 *           {@link String#format(String, Object...)} compatible format string
	 * @throws IllegalFormatException
	 *            if the underlying API throws the exception
	 * @throws NullPointerException
	 *            if format is null
	 * @see String#format(String, Object...)
	 */
	public void setFormat( final String format ) throws IllegalFormatException, NullPointerException
	{
		// Oh for a better way to do this!
		try
		{
			String.format( format, User, message );
		}
		catch ( RuntimeException ex )
		{
			ex.fillInStackTrace();
			throw ex;
		}
		
		this.format = format;
	}
	
	/**
	 * Gets a set of recipients that this chat message will be displayed to. The set returned is not guaranteed to be
	 * mutable and may auto-populate on access. Any listener accessing the returned set should be aware that it may
	 * reduce performance for a lazy set implementation.<br>
	 * <br>
	 * <b>Listeners should be aware that modifying the list may throw {@link UnsupportedOperationException} if the event
	 * caller provides an unmodifiable set.</b>
	 * 
	 * @return All Users who will see this chat message
	 */
	public Set<User> getRecipients()
	{
		return recipients;
	}
	
	public boolean isCancelled()
	{
		return cancel;
	}
	
	public void setCancelled( boolean cancel )
	{
		this.cancel = cancel;
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
