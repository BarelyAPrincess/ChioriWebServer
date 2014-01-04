package com.chiorichan.http;

public enum ReqFailureReason
{
	ACCEPTED(-1, "User has been permitted to view this page."),
	OP_ONLY(0, "This page is limited to Operators only!"),
	NO_ACCESS(1, "Your current login does not have the required permission to view this page!"),
	NO_USER(2, "You must be logged in to view that page!");
	
	//"This page is limited to members with access to the \"" + reqLevel + "\" permission or better."
	
	private int reasonId;
	private String reasonDescription;
	
	ReqFailureReason( int id, String reason )
	{
		reasonId = id;
		reasonDescription = reason;
	}
	
	public String getReason()
	{
		return reasonDescription;
	}
	
	public void setReason( String reason )
	{
		reasonDescription = reason;
	}
	
	public int getId()
	{
		return reasonId;
	}
}
