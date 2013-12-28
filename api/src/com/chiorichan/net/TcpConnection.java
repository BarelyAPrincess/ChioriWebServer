package com.chiorichan.net;

import com.esotericsoftware.kryonet.Connection;

abstract class TcpConnection extends Connection
{
	abstract void onConnect();
	
	abstract void onIdle();
	
	abstract void onDisconnect();

	abstract void onReceived( Packet var1 );
}