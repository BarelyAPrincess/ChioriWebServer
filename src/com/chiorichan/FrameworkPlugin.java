package com.chiorichan;

import com.chiorichan.event.Event;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.plugin.java.JavaPlugin;

public class FrameworkPlugin extends JavaPlugin
{
	public void onEnable()
	{
		getServer().getLogger().info( "Chiori Framework Plugin for Chiori Web Server is now enabled!" );
	}
	
	public void onDisable()
	{
		
	}
	
	@EventHandler( priority = EventPriority.HIGHEST )
	public void userLoginEvent( Event event )
	{
		
	}
}
