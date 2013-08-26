package com.chiorichan.framework;

import com.chiorichan.Main;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.event.server.RequestEvent;
import com.chiorichan.plugin.java.JavaPlugin;

public class PluginMain extends JavaPlugin implements Listener
{
	public void onEnable()
	{
		Main.getPluginManager().registerEvents( this, this );
		
		//Main.getServer().registerBean( null, "framework" );
	}
	
	public void onDisable()
	{
		
	}
	
	@EventHandler( priority = EventPriority.HIGHEST )
	public void onRequestEvent( RequestEvent event )
	{
		//event.setStatus( 418, "I'm a teapot!" );
		//event.setCancelled( true );
	}
}
