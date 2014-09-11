/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.net;

import groovy.lang.Binding;

import java.util.Arrays;

import com.chiorichan.account.bases.Sentient;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.factory.CodeEvalFactory;
import com.chiorichan.net.packet.DataPacket;
import com.chiorichan.net.packet.KickPacket;
import com.esotericsoftware.kryonet.Connection;

public class ServerConnection extends Connection implements SentientHandler
{
	protected Binding binding = new Binding();
	protected CodeEvalFactory factory;
	protected Sentient currentSentient = null;
	
	@Override
	public boolean kick( String kickMessage )
	{
		sendTCP( new KickPacket( kickMessage ) );
		close();
		
		return true;
	}
	
	@Override
	public void sendMessage( String... messages )
	{
		sendTCP( new DataPacket( "Messages", Arrays.asList( messages ) ) );
	}
	
	@Override
	public String getIpAddr()
	{
		return getRemoteAddressTCP().getAddress().getHostAddress();
	}
	
	public void beginSession()
	{
		sendTCP( new DataPacket( "Message", Arrays.asList( "Welcome to Chiori Web Server!", "Please enter your username and press enter:" ) ) );
	}
	
	@Override
	public void attachSentient( Sentient sentient )
	{
		currentSentient = sentient;
	}
	
	@Override
	public boolean isValid()
	{
		return isConnected();
	}
	
	@Override
	public Sentient getSentient()
	{
		return currentSentient;
	}
	
	@Override
	public void removeSentient()
	{
		currentSentient = null;
	}

	@Override
	public String getName()
	{
		if ( currentSentient == null )
			return "(NULL)";
		
		return currentSentient.getName();
	}
}
