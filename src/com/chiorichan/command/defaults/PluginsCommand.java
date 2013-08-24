package com.chiorichan.command.defaults;

import java.util.Arrays;

import com.chiorichan.ChatColor;
import com.chiorichan.Main;
import com.chiorichan.command.CommandSender;
import com.chiorichan.plugin.Plugin;

public class PluginsCommand extends ChioriCommand
{
	public PluginsCommand(String name)
	{
		super( name );
		this.description = "Gets a list of plugins running on the server";
		this.usageMessage = "/plugins";
		this.setPermission( "bukkit.command.plugins" );
		this.setAliases( Arrays.asList( "pl" ) );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		sender.sendMessage( "Plugins " + getPluginList() );
		return true;
	}
	
	private String getPluginList()
	{
		StringBuilder pluginList = new StringBuilder();
		Plugin[] plugins = Main.getInstance().getPluginManager().getPlugins();
		
		for ( Plugin plugin : plugins )
		{
			if ( pluginList.length() > 0 )
			{
				pluginList.append( ChatColor.WHITE );
				pluginList.append( ", " );
			}
			
			pluginList.append( plugin.isEnabled() ? ChatColor.GREEN : ChatColor.RED );
			pluginList.append( plugin.getDescription().getName() );
		}
		
		return "(" + plugins.length + "): " + pluginList.toString();
	}
}
