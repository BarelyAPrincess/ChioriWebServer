package com.chiorichan.net;

import groovy.lang.Binding;

import java.util.Arrays;

import com.chiorichan.framework.Evaling;
import com.chiorichan.framework.Site;
import com.chiorichan.net.packet.DataPacket;
import com.chiorichan.net.packet.KickPacket;
import com.chiorichan.user.User;
import com.chiorichan.user.UserHandler;
import com.esotericsoftware.kryonet.Connection;

public class ServerConnection extends Connection implements UserHandler
{
	protected Binding binding = new Binding();
	protected Evaling eval;
	protected User currentUser = null;
	
	@Override
	public void kick( String kickMessage )
	{
		sendTCP( new KickPacket( kickMessage ) );
		close();
	}
	
	@Override
	public void sendMessage( String[] messages )
	{
		sendTCP( new DataPacket( "Messages", Arrays.asList( messages ) ) );
	}
	
	@Override
	public Site getSite()
	{
		return null;
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
}
