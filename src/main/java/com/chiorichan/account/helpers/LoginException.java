package com.chiorichan.account.helpers;

import com.chiorichan.account.bases.Account;

public class LoginException extends Exception
{
	private static final long serialVersionUID = 5522301956671473324L;
	private Account acct = null;
	
	public LoginException(Exception e)
	{
		super( e );
	}
	
	public LoginException(LoginExceptionReasons reason, Account _acct)
	{
		this( reason );
		acct = _acct;
	}
	
	public LoginException(LoginExceptionReasons reason)
	{
		super( reason.getReason() );
	}
	
	public static LoginExceptionReasons customExceptionReason( String reason )
	{
		return LoginExceptionReasons.customReason.setReason( reason );
	}
	
	public Account getAccount()
	{
		return acct;
	}

	public LoginException setAccount( Account _acct )
	{
		acct = _acct;
		return this;
	}
}
