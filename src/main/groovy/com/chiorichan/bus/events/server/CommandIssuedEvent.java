/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.bus.events.server;

import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.bus.events.HandlerList;

/**
 * Server Command events
 */
public class CommandIssuedEvent extends ServerEvent
{
	private static final HandlerList handlers = new HandlerList();
	private String command;
	private final SentientHandler sender;
	
	public CommandIssuedEvent(final String command, final SentientHandler sender)
	{
		this.command = command;
		this.sender = sender;
	}
	
	/**
	 * Gets the command that the user is attempting to execute from the console
	 * 
	 * @return Command the user is attempting to execute
	 */
	public String getCommand()
	{
		return command;
	}
	
	/**
	 * Sets the command that the server will execute
	 * 
	 * @param message
	 *           New message that the server will execute
	 */
	public void setCommand( String message )
	{
		this.command = message;
	}
	
	/**
	 * Get the command sender.
	 * 
	 * @return The sender
	 */
	public SentientHandler getSender()
	{
		return sender;
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
