package com.chiorichan.user;

public class LoginException extends Exception
{
	private static final long serialVersionUID = 5522301956671473324L;
	private User user = null;
	
	public LoginException(Exception e)
	{
		super( e );
	}
	
	public LoginException(ExceptionReasons reason, User _user)
	{
		this( reason );
		user = _user;
	}
	
	public LoginException(ExceptionReasons reason)
	{
		super( reason.getReason() );
	}
	
	public static ExceptionReasons customExceptionReason( String reason )
	{
		return ExceptionReasons.customReason.setReason( reason );
	}
	
	public User getUser()
	{
		return user;
	}
	
	public enum ExceptionReasons
	{
		accountNotActivated( " Account is not activated." ), underAttackPleaseWait( " Max fail login tries reached. Account locked for 30 minutes." ), emptyUsername( "The specified username was empty. Please try again." ), emptyPassword( "The specified password was empty. Please try again." ), incorrectLogin( "Username and Password provided did not match any users on file." ), successLogin( "Your login has been successfully authenticated." ), unknownError( "Your login has failed due to an unknown internal error, Please try again." ), permissionsError( "Fatal error was detected with your user permissions. Please notify an administrator ASAP." ), customReason( "Someone forget to set my custom exception reason. :(" );
		
		String reason;
		
		ExceptionReasons(String _reason)
		{
			reason = _reason;
		}
		
		protected ExceptionReasons setReason( String message )
		{
			reason = message;
			return this;
		}
		
		public String getReason()
		{
			return reason;
		}
	}

	public LoginException setUser( User _user )
	{
		user = _user;
		return this;
	}
}
