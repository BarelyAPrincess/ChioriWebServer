package com.chiorichan.server;

import com.caucho.resin.BeanEmbed;
import com.caucho.resin.HttpEmbed;
import com.caucho.resin.ResinEmbed;
import com.caucho.resin.WebAppEmbed;

public class Server extends Thread
{
	private final static ResinEmbed resin = new ResinEmbed();
	private boolean running = false;
	
	public Server()
	{
		super();
		
		setName( "Digital Signage Server" );
		ServiceBean service = new ServiceBean();
		
		resin.addBean(new BeanEmbed(service, "signage"));
		
		resin.addWebApp( new WebAppEmbed( "/", Server.class.getClassLoader().getResource( "webroot" ).toExternalForm().substring( 5 ) ) );
	}
	
	public void setIp( String ip )
	{
		// TODO: Bind server to listening IP Address. Is this possible?
	}
	
	public void setPort( int port )
	{
		if ( !running )
		{
			HttpEmbed http = new HttpEmbed( port );
			resin.addPort( http );
		}
	}
	
	public void run()
	{
		running = true;
		
		resin.start();
		resin.join();
	}
	
	public static ResinEmbed getResinServer()
	{
		return resin;
	}
}
