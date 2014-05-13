package ru.tehkode.permissions.bukkit.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.commands.Command;
import ru.tehkode.utils.StringUtils;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.CommandSender;
import com.chiorichan.framework.Site;
import com.chiorichan.plugin.Plugin;

public class SiteCommands extends PermissionsCommand
{
	
	@Command( name = "pex", syntax = "sites", description = "Print loaded sites", isPrimary = true, permission = "permissions.manage.sites" )
	public void sitesTree( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		List<Site> sites = Loader.getInstance().getSites();
		
		PermissionManager manager = PermissionsEx.getPermissionManager();
		
		sender.sendMessage( "Sites on server: " );
		for ( Site site : sites )
		{
			String[] parentSites = manager.getSiteInheritance( site.getName() );
			String output = "  " + site.getName();
			if ( parentSites.length > 0 )
			{
				output += ChatColor.GREEN + " [" + ChatColor.WHITE + StringUtils.implode( parentSites, ", " ) + ChatColor.GREEN + "]";
			}
			
			sender.sendMessage( output );
		}
	}
	
	@Command( name = "pex", syntax = "site <site>", description = "Print <site> inheritance info", permission = "permissions.manage.sites" )
	public void sitePrintInheritance( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		PermissionManager manager = PermissionsEx.getPermissionManager();
		if ( Loader.getInstance().getSiteById( siteName ) == null )
		{
			sender.sendMessage( "Specified site \"" + args.get( "site" ) + "\" not found." );
			return;
		}
		
		String[] parentSites = manager.getSiteInheritance( siteName );
		
		sender.sendMessage( "Site " + siteName + " inherit:" );
		if ( parentSites.length == 0 )
		{
			sender.sendMessage( "nothing :3" );
			return;
		}
		
		for ( String parentSite : parentSites )
		{
			String[] parents = manager.getSiteInheritance( parentSite );
			String output = "  " + parentSite;
			if ( parentSites.length > 0 )
			{
				output += ChatColor.GREEN + " [" + ChatColor.WHITE + StringUtils.implode( parentSites, ", " ) + ChatColor.GREEN + "]";
			}
			
			sender.sendMessage( output );
		}
	}
	
	@Command( name = "pex", syntax = "site <site> inherit <parentSites>", description = "Set <parentSites> for <site>", permission = "permissions.manage.sites.inheritance" )
	public void siteSetInheritance( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		PermissionManager manager = PermissionsEx.getPermissionManager();
		if ( Loader.getInstance().getSiteById( siteName ) == null )
		{
			sender.sendMessage( "Specified site \"" + args.get( "site" ) + "\" not found." );
			return;
		}
		
		List<String> parents = new ArrayList<String>();
		String parentSites = args.get( "parentSites" );
		if ( parentSites.contains( "," ) )
		{
			for ( String site : parentSites.split( "," ) )
			{
				site = this.autoCompleteSiteName( site, "parentSites" );
				if ( !parents.contains( site ) )
				{
					parents.add( site.trim() );
				}
			}
		}
		else
		{
			parents.add( parentSites.trim() );
		}
		
		manager.setSiteInheritance( siteName, parents.toArray( new String[0] ) );
		
		sender.sendMessage( "Site " + siteName + " inherits " + StringUtils.implode( parents, ", " ) );
	}
}
