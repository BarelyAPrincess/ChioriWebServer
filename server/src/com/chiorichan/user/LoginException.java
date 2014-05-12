package com.chiorichan.user;

public class LoginException extends Exception
{
	private static final long serialVersionUID = 5522301956671473324L;
	private User user = null;
	
	public LoginException(Exception e)
	{
		super( e );
	}
	
	public LoginException(LoginExceptionReasons reason, User _user)
	{
		this( reason );
		user = _user;
	}
	
	public LoginException(LoginExceptionReasons reason)
	{
		super( reason.getReason() );
	}
	
	public static LoginExceptionReasons customExceptionReason( String reason )
	{
		return LoginExceptionReasons.customReason.setReason( reason );
	}
	
	public User getUser()
	{
		return user;
	}

	public LoginException setUser( User _user )
	{
		user = _user;
		return this;
	}
}
