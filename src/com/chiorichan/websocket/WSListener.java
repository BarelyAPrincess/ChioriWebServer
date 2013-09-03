package com.chiorichan.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;

import com.caucho.websocket.AbstractWebSocketListener;
import com.caucho.websocket.WebSocketContext;

public class WSListener extends AbstractWebSocketListener
{
	@Override
	public void onReadText( WebSocketContext context, Reader is ) throws IOException
	{
		PrintWriter out = context.startTextMessage();
		
		int ch;
		
		while ( ( ch = is.read() ) >= 0 )
		{
			out.print( (char) ch );
		}
		
		out.close();
		is.close();
	}
	
	public void onStart( WebSocketContext context ) throws IOException
	{
		PrintWriter w = context.startTextMessage();
		w.print( "Hello World!" );
		w.close();
	}
	
	public void onReadBinary( WebSocketContext context, InputStream is ) throws IOException
	{
		
	}
	
	public void onClose( WebSocketContext context ) throws IOException
	{
		
	}
	
	public void onDisconnect( WebSocketContext context ) throws IOException
	{
		
	}
	
	public void onTimeout( WebSocketContext context ) throws IOException
	{
		
	}
}
