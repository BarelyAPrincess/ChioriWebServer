package com.chiorichan;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;

import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.server.Server;

public class Main
{
	private static Logger log = Logger.getLogger( "WebServer" );
	private static File fileConfig = new File( Main.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "/server.yml" );
	private static YamlConfiguration config;
	private static Server server;
	private static ResourceLoader resourceLoader;
	
	public static Logger getLogger()
	{
		return log;
	}
	
	public static YamlConfiguration getAppConfig()
	{
		return config;
	}
	
	public static void main( String... args ) throws Exception
	{
		new Main();
	}
	
	public Main()
	{
		// Initalize configuration
		if ( fileConfig.exists() )
		{
			config = YamlConfiguration.loadConfiguration( fileConfig );
			config.addDefault( "general.mode", "dual" );
			config.addDefault( "general.ip", "0.0.0.0" );
			config.addDefault( "general.port", "8080" );
			config.addDefault( "general.pack", "default" );
		}
		else
		{
			config = new YamlConfiguration();
			config.set( "general.mode", "dual" );
			config.set( "general.ip", "0.0.0.0" );
			config.set( "general.port", "8080" );
			config.set( "general.pack", "default" );
			
			try
			{
				config.save( fileConfig );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
		
		// Why does this not work? or does it?
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			public void run()
			{
				try
				{
					Main.getLogger().log( Level.INFO, "Application if now closing." );
					Main.getAppConfig().save( fileConfig );
				}
				catch ( IOException e )
				{
					e.printStackTrace();
				}
			}
		} );
		
		resourceLoader = ResourceLoader.buildLoader( Main.class.getProtectionDomain().getCodeSource().getLocation().getPath() + System.getProperty( "file.separator", "/" ) + "packages" + System.getProperty( "file.separator", "/" ) + config.getString( "general.pack", "default" ) );
		
		if ( resourceLoader == null )
			log.warning( "Could not load the resource pack!" );
		
		server = new Server();
		server.setPort( config.getInt( "general.port", 8080 ) );
		server.setIp( config.getString( "general.ip", "0.0.0.0" ) );
	}
	
	public static Server getServer()
	{
		return server;
	}
	
	public static void setServer( Server server )
	{
		Main.server = server;
	}
	
	public static int parseAlignment( String align )
	{
		try
		{
			Field field = Class.forName( "javax.swing.JLabel" ).getField( align.trim().toUpperCase() );
			return (int) field.get( null );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		
		return JLabel.LEFT; // Align left by default.
	}
	
	public static Color parseColor( String color )
	{
		Pattern c = Pattern.compile( "rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)" );
		Matcher m = c.matcher( color );
		
		// First try to parse RGB(0,0,0);
		if ( m.matches() )
		{
			return new Color( Integer.valueOf( m.group( 1 ) ), // r
			Integer.valueOf( m.group( 2 ) ), // g
			Integer.valueOf( m.group( 3 ) ) ); // b
		}
		
		try
		{
			Field field = Class.forName( "java.awt.Color" ).getField( color.trim().toUpperCase() );
			return (Color) field.get( null );
		}
		catch ( Exception e )
		{}
		
		try
		{
			return Color.decode( color );
		}
		catch ( Exception e )
		{}
		
		return null;
	}
	
	public static ResourceLoader getResourceLoader()
	{
		return resourceLoader;
	}
}
