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

import java.util.List;

import com.chiorichan.net.packet.CommandPacket;
import com.chiorichan.net.packet.DataPacket;
import com.chiorichan.net.packet.KickPacket;
import com.chiorichan.net.packet.PingPacket;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

class PacketManager extends Listener
{
	protected PacketManager()
	{
		
	}
	
	protected void registerApiPackets( Kryo kryo )
	{
		kryo.register( CommandPacket.class );
		kryo.register( PingPacket.class );
		kryo.register( KickPacket.class );
		kryo.register( DataPacket.class );
	}
	
	@SuppressWarnings( "unchecked" )
	public void handleBasePacket( Connection var0, BasePacket var1 )
	{
		if ( var1 instanceof DataPacket )
		{
			DataPacket packet = (DataPacket) var1;
			
			switch ( packet.getKeyword() )
			{
				case "Messages":
					if ( packet.getPayload() instanceof List )
						for ( String s : (List<String>) packet.getPayload() )
							System.out.println( s );
					else if ( packet.getPayload() instanceof String )
						//Loader.getLogger().info( (String) packet.getPayload() );
					break;
				case "beginSession":
					//if ( var0 instanceof ServerConnection )
					//	( (ServerConnection) var0 ).beginSession();
					break;
			}
		}
		else if ( var1 instanceof KickPacket )
		{
			//Loader.getLogger().warning( "Kicked from Server: " + ( (KickPacket) var1 ).getReason() );
		}
	}
}
