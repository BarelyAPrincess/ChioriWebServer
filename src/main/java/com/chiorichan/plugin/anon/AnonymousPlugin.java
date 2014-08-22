package com.chiorichan.plugin.anon;

import java.io.File;
import java.io.InputStream;

import com.chiorichan.Loader;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.command.Command;
import com.chiorichan.configuration.file.FileConfiguration;
import com.chiorichan.plugin.PluginBase;
import com.chiorichan.plugin.PluginDescriptionFile;
import com.chiorichan.plugin.PluginLoader;

/**
 * Used when you would like to register events or commands but affected code was not started from a plugin.
 * ex: new AnonymousPlugin( parentObject )
 * 
 * @author Chiori Greene
 */
public final class AnonymousPlugin extends PluginBase
{
	private boolean isNaggable = false;
	private final static AnonymousLoader anonLoader = new AnonymousLoader();
	protected Object parent;
	
	public AnonymousPlugin()
	{
		
	}
	
	public AnonymousPlugin(Object obj)
	{
		parent = obj;
	}
	
	@Override
	public File getDataFolder()
	{
		return new File( Loader.getRoot(), "data" );
	}
	
	@Override
	public PluginDescriptionFile getDescription()
	{
		return null;
	}
	
	@Override
	public FileConfiguration getConfig()
	{
		return null;
	}
	
	@Override
	public InputStream getResource( String filename )
	{
		return null;
	}
	
	@Override
	public void saveConfig()
	{
		
	}
	
	@Override
	public void saveDefaultConfig()
	{
		
	}
	
	@Override
	public void saveResource( String resourcePath, boolean replace )
	{
		
	}
	
	@Override
	public void reloadConfig()
	{
		
	}
	
	@Override
	public PluginLoader getPluginLoader()
	{
		return anonLoader;
	}
	
	@Override
	public boolean isEnabled()
	{
		return true;
	}
	
	@Override
	public void onDisable()
	{
		
	}
	
	@Override
	public void onLoad()
	{
		
	}
	
	@Override
	public void onEnable()
	{
		
	}
	
	@Override
	public boolean isNaggable()
	{
		return isNaggable;
	}
	
	@Override
	public void setNaggable( boolean canNag )
	{
		isNaggable = canNag;
	}
	
	@Override
	public boolean onCommand( SentientHandler sender, Command command, String label, String[] args )
	{
		return false;
	}
}
