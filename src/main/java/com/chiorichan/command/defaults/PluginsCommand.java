package com.chiorichan.command.defaults;

import java.util.Arrays;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.plugin.Plugin;

public class PluginsCommand extends ChioriCommand
{
	public PluginsCommand(String name)
	{
		super( name );
		this.description = "Gets a list of plugins running on the server";
		this.usageMessage = "/plugins";
		this.setPermission( "chiori.command.plugins" );
		this.setAliases( Arrays.asList( "pl" ) );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		sender.sendMessage( "Plugins " + getPluginList() );
		return true;
	}
	
	private String getPluginList()
	{
		StringBuilder pluginList = new StringBuilder();
		Plugin[] plugins = Loader.getPluginManager().getPlugins();
		
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
