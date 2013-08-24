package com.chiorichan.event.user;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.event.Cancellable;
import com.chiorichan.event.HandlerList;
import com.chiorichan.user.User;

/**
 * Called early in the command handling process. This event is only for very exceptional cases and you should not
 * normally use it.
 */
public class UserCommandPreprocessEvent extends UserEvent implements Cancellable
{
	private static final HandlerList handlers = new HandlerList();
	private boolean cancel = false;
	private String message;
	private String format = "<%1$s> %2$s";
	private final Set<User> recipients;
	
	public UserCommandPreprocessEvent(final User User, final String message)
	{
		super( User );
		this.recipients = new HashSet<User>( Arrays.asList( User.getServer().getOnlineUsers() ) );
		this.message = message;
	}
	
	public UserCommandPreprocessEvent(final User User, final String message, final Set<User> recipients)
	{
		super( User );
		this.recipients = recipients;
		this.message = message;
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
	 * Gets the command that the User is attempting to send. All commands begin with a special character;
	 * implementations do not consider the first character when executing the content.
	 * 
	 * @return Message the User is attempting to send
	 */
	public String getMessage()
	{
		return message;
	}
	
	/**
	 * Sets the command that the User will send. All commands begin with a special character; implementations do not
	 * consider the first character when executing the content.
	 * 
	 * @param command
	 *           New message that the User will send
	 * @throws IllegalArgumentException
	 *            if command is null or empty
	 */
	public void setMessage( String command ) throws IllegalArgumentException
	{
		Validate.notNull( command, "Command cannot be null" );
		Validate.notEmpty( command, "Command cannot be empty" );
		this.message = command;
	}
	
	/**
	 * Sets the User that this command will be executed as.
	 * 
	 * @param User
	 *           New User which this event will execute as
	 * @throws IllegalArgumentException
	 *            if the User provided is null
	 */
	public void setUser( final User User ) throws IllegalArgumentException
	{
		Validate.notNull( User, "User cannot be null" );
		this.User = User;
	}
	
	/**
	 * Gets the format to use to display this chat message
	 * 
	 * @deprecated This method is provided for backward compatibility with no guarantee to the use of the format.
	 * @return String.Format compatible format string
	 */
	@Deprecated
	public String getFormat()
	{
		return format;
	}
	
	/**
	 * Sets the format to use to display this chat message
	 * 
	 * @deprecated This method is provided for backward compatibility with no guarantee to the effect of modifying the
	 *             format.
	 * @param format
	 *           String.Format compatible format string
	 */
	@Deprecated
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
