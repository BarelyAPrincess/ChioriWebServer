package com.chiorichan.command.defaults;

import java.io.File;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.command.Command;
import com.chiorichan.framework.Site;

public class CacheCommand extends VanillaCommand
{
	public CacheCommand()
	{
		super( "cache" );
		this.description = "Manages the site cache files.";
		this.usageMessage = "cache <sideId> (purge|list|patterns), cache <sideId> add <pattern>";
		this.setPermission( "chiori.command.cache" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		if ( args.length < 2 || args.length > 3 )
		{
			sender.sendMessage( ChatColor.RED + "Incorrect arguments: " + usageMessage );
			return true;
		}
		
		Site site = Loader.getSiteManager().getSiteById( args[0] );
		
		if ( site == null )
		{
			sender.sendMessage( ChatColor.RED + "Could not find a site by that id." );
			return true;
		}
		
		if ( args.length == 2 )
		{
			if ( args[1].equalsIgnoreCase( "purge" ) )
			{
				File cacheDir = site.getCacheDirectory();
				
				for ( File f : cacheDir.listFiles() )
					f.delete();
				
				return true;
			}
			else if ( args[1].equalsIgnoreCase( "list" ) )
			{
				File cacheDir = site.getCacheDirectory();
				
				sender.sendMessage( ChatColor.AQUA + "<--- Cached Files --->" );
				
				for ( File f : cacheDir.listFiles() )
					sender.sendMessage( ChatColor.AQUA + f.getAbsolutePath() );
				
				return true;
			}
			else if ( args[1].equalsIgnoreCase( "patterns" ) )
			{
				sender.sendMessage( ChatColor.AQUA + "<--- Patterns --->" );
				
				int n = 0;
				for ( String s : site.getCachePatterns() )
				{
					sender.sendMessage( ChatColor.AQUA + "" + n + ": `" + s + "`" );
					n++;
				}
				
				return true;
			}
		}
		else if ( args.length == 3 )
		{
			if ( args[1].equalsIgnoreCase( "add" ) )
			{
				site.addToCachePatterns( args[2].trim() );
				
				sender.sendMessage( ChatColor.AQUA + "Added pattern `" + args[2].trim() + "` to site `" + site.getSiteId() + "`" );
				
				return true;
			}
		}
		
		return false;
	}
}
