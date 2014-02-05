package com.chiorichan.event.user;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.Warning;
import com.chiorichan.event.Cancellable;
import com.chiorichan.event.HandlerList;
import com.chiorichan.user.User;

/**
 * Holds information for User chat and commands
 * 
 * @deprecated This event will fire from the main thread and allows the use of all of the Bukkit API, unlike the
 *             {@link AsyncUserChatEvent}.<br>
 * <br>
 *             Listening to this event forces chat to wait for the main thread which causes delays for chat.<br>
 *             {@link AsyncUserChatEvent} is the encouraged alternative for thread safe implementations.
 */
@Deprecated
@Warning( reason = "Listening to this event forces chat to wait for the main thread, delaying chat messages." )
public class UserChatEvent extends UserEvent implements Cancellable
{
	private static final HandlerList handlers = new HandlerList();
	private boolean cancel = false;
	private String message;
	private String format;
	private final Set<User> recipients;
	
	public UserChatEvent(final User User, final String message)
	{
		super( User );
		this.message = message;
		this.format = "<%1$s> %2$s";
		this.recipients = new HashSet<User>( Arrays.asList( Loader.getInstance().getOnlineUsers() ) );
	}
	
	public UserChatEvent(final User User, final String message, final String format, final Set<User> recipients)
	{
		super( User );
		this.message = message;
		this.format = format;
		this.recipients = recipients;
	}
	
	public boolean isCancelled()
	{
		return cancel;
	}
	
	public void setCancelled( boolean cancel )
	{
		this.cancel = cancel;
	}
	
	/**
	 * Gets the message that the User is attempting to send
	 * 
	 * @return Message the User is attempting to send
	 */
	public String getMessage()
	{
		return message;
	}
	
	/**
	 * Sets the message that the User will send
	 * 
	 * @param message
	 *           New message that the User will send
	 */
	public void setMessage( String message )
	{
		this.message = message;
	}
	
	/**
	 * Sets the User that this message will display as, or command will be executed as
	 * 
	 * @param User
	 *           New User which this event will execute as
	 */
	public void setUser( final User User )
	{
		Validate.notNull( User, "User cannot be null" );
		this.User = User;
	}
	
	/**
	 * Gets the format to use to display this chat message
	 * 
	 * @return String.Format compatible format string
	 */
	public String getFormat()
	{
		return format;
	}
	
	/**
	 * Sets the format to use to display this chat message
	 * 
	 * @param format
	 *           String.Format compatible format string
	 */
	public void setFormat( final String format )
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
	 * Gets a set of recipients that this chat message will be displayed to
	 * 
	 * @return All Users who will see this chat message
	 */
	public Set<User> getRecipients()
	{
		return recipients;
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
