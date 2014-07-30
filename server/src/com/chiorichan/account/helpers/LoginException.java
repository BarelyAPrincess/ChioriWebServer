package com.chiorichan.account.helpers;

import com.chiorichan.account.bases.Account;

public class LoginException extends Exception
{
	private static final long serialVersionUID = 5522301956671473324L;
	private Account user = null;
	
	public LoginException(Exception e)
	{
		super( e );
	}
	
	public LoginException(LoginExceptionReasons reason, Account _user)
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
	
	public Account getUser()
	{
		return user;
	}

	public LoginException setUser( Account _user )
	{
		user = _user;
		return this;
	}
}
