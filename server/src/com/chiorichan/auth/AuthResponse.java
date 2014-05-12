package com.chiorichan.auth;


public class AuthResponse
{
	private String reason;
	private boolean success;
	
	public AuthResponse( String _reason, boolean _success )
	{
		reason = _reason;
		success = _success;
	}
	
	public AuthResponse setReason( String message )
	{
		reason = message;
		return this;
	}
	
	public String getReason()
	{
		return reason;
	}
	
	public void setSuccess( boolean _success )
	{
		success = _success;
	}
	
	public boolean getSuccess()
	{
		return success;
	}
}
