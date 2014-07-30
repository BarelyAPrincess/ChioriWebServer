package com.chiorichan.account.bases;

public interface SentientHandler
{
	public boolean kick( String kickMessage );

	public void sendMessage( String... msg );

	public void attachSentient( Sentient sentient );
	
	public void removeSentient();
	
	public boolean isValid();

	public Sentient getSentient();
	
	public String getIpAddr();
}
