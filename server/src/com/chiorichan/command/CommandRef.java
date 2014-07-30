package com.chiorichan.command;

import com.chiorichan.account.bases.SentientHandler;

public class CommandRef
{
	public final String command;
	public final SentientHandler sender;
	
	public CommandRef(SentientHandler _sender, String _command)
	{
		this.sender = _sender;
		this.command = _command;
	}
}
