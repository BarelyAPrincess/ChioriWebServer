package com.chiorichan;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jline.console.ConsoleReader;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import sun.misc.IOUtils;

import com.chiorichan.Warning.WarningState;
import com.chiorichan.command.ConsoleCommandSender;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.updater.AutoUpdater;
import com.chiorichan.updater.ChioriDLUpdaterService;
import com.chiorichan.user.UserManager;
import com.chiorichan.util.FileUtil;
import com.chiorichan.util.Versioning;
import com.chiorichan.util.WebUtils;

public class Client implements ConsoleParent
{
	private final AutoUpdater updater;
	private final Yaml yaml = new Yaml( new SafeConstructor() );
	private static YamlConfiguration configuration;
	private static Client instance;
	private static OptionSet options;
	private static long startTime = System.currentTimeMillis();
	
	private WarningState warningState = WarningState.DEFAULT;
	protected final static Console console = new Console();
	protected UserManager userManager;
	
	public static String clientId;
	
	public static void main( String... args ) throws Exception
	{
		OptionSet options = null;
		
		try
		{
			OptionParser parser = getOptionParser();
			
			try
			{
				options = parser.parse( args );
			}
			catch ( joptsimple.OptionException ex )
			{
				Logger.getLogger( Loader.class.getName() ).log( Level.SEVERE, ex.getLocalizedMessage() );
			}
			
			if ( ( options == null ) || ( options.has( "?" ) ) )
			{
				try
				{
					parser.printHelpOn( System.out );
				}
				catch ( IOException ex )
				{
					Logger.getLogger( Loader.class.getName() ).log( Level.SEVERE, null, ex );
				}
			}
			else if ( options.has( "v" ) )
			{
				System.out.println( Versioning.getVersion() );
			}
			else
			{
				new Client( options );
			}
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
			
			if ( getLogger() != null )
				getLogger().severe( ChatColor.RED + "" + ChatColor.NEGATIVE + "SEVERE ERROR (" + ( System.currentTimeMillis() - startTime ) + "ms)! Press 'Ctrl-c' to quit!'" );
			else
				System.err.println( "SEVERE ERROR (" + ( System.currentTimeMillis() - startTime ) + "ms)! Press 'Ctrl-c' to quit!'" );
			
			// TODO Make it so this exception (and possibly other critical exceptions) are reported to us without user interaction. Should also find a way that the log can be sent along with it.
			
			try
			{
				console.isRunning = false;
				
				if ( configuration != null && configuration.getBoolean( "server.haltOnSevereError" ) )
				{
					Scanner keyboard = new Scanner( System.in );
					keyboard.nextLine();
					keyboard.close();
				}
			}
			catch ( Exception e )
			{}
		}
	}
	
	public static OptionParser getOptionParser()
	{
		OptionParser parser = new OptionParser()
		{
			{
				acceptsAll( Arrays.asList( "?", "help" ), "Show the help" );
				
				acceptsAll( Arrays.asList( "c", "config", "settings" ), "Configuration file to use" ).withRequiredArg().ofType( File.class ).defaultsTo( new File( "client.yml" ) ).describedAs( "Yml file" );
				
				acceptsAll( Arrays.asList( "d", "date-format" ), "Format of the date to display in the console (for log entries)" ).withRequiredArg().ofType( SimpleDateFormat.class ).describedAs( "Log date format" );
				
				acceptsAll( Arrays.asList( "log-pattern" ), "Specfies the log filename pattern" ).withRequiredArg().ofType( String.class ).defaultsTo( "client.log" ).describedAs( "Log filename" );
				
				acceptsAll( Arrays.asList( "log-limit" ), "Limits the maximum size of the log file (0 = unlimited)" ).withRequiredArg().ofType( Integer.class ).defaultsTo( 0 ).describedAs( "Max log size" );
				
				acceptsAll( Arrays.asList( "log-count" ), "Specified how many log files to cycle through" ).withRequiredArg().ofType( Integer.class ).defaultsTo( 1 ).describedAs( "Log count" );
				
				acceptsAll( Arrays.asList( "log-append" ), "Whether to append to the log file" ).withRequiredArg().ofType( Boolean.class ).defaultsTo( true ).describedAs( "Log append" );
				
				acceptsAll( Arrays.asList( "log-strip-color" ), "Strips color codes from log file" );
				
				acceptsAll( Arrays.asList( "nojline" ), "Disables jline and emulates the vanilla console" );
				
				acceptsAll( Arrays.asList( "noconsole" ), "Disables the console" );
				
				acceptsAll( Arrays.asList( "nocolor" ), "Disables the console color formatting" );
				
				acceptsAll( Arrays.asList( "v", "version" ), "Show the Version" );
			}
		};
		
		return parser;
	}
	
	public Client(OptionSet options0)
	{
		instance = this;
		options = options0;
		
		console.init( this, options );
		
		if ( Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L )
			getLogger().warning( "To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar server.jar\"" );
		
		if ( getConfigFile() == null )
			getLogger().panic( "We had problems loading the configuration file! Did you define the --config argument?" );
		
		try
		{
			configuration = YamlConfiguration.loadConfiguration( getConfigFile() );
		}
		catch ( Exception e )
		{
			try
			{
				FileUtil.copy( new File( getClass().getClassLoader().getResource( "com/chiorichan/client.yml" ).toURI() ), getConfigFile() );
				configuration = YamlConfiguration.loadConfiguration( getConfigFile() );
			}
			catch ( URISyntaxException e1 )
			{}
		}
		
		configuration.options().copyDefaults( true );
		configuration.setDefaults( YamlConfiguration.loadConfiguration( getClass().getClassLoader().getResourceAsStream( "com/chiorichan/chiori.yml" ) ) );
		clientId = configuration.getString( "server.installationUID", clientId );
		
		if ( clientId == null || clientId.isEmpty() || clientId.equalsIgnoreCase( "null" ) )
		{
			clientId = UUID.randomUUID().toString();
			configuration.set( "server.installationUID", clientId );
		}
		
		saveConfig();
		
		warningState = WarningState.value( configuration.getString( "settings.deprecated-verbose" ) );
		
		updater = new AutoUpdater( new ChioriDLUpdaterService( configuration.getString( "auto-updater.host" ) ), configuration.getString( "auto-updater.preferred-channel" ) );
		updater.setEnabled( configuration.getBoolean( "auto-updater.enabled" ) );
		updater.setSuggestChannels( configuration.getBoolean( "auto-updater.suggest-channels" ) );
		updater.getOnBroken().addAll( configuration.getStringList( "auto-updater.on-broken" ) );
		updater.getOnUpdate().addAll( configuration.getStringList( "auto-updater.on-update" ) );
		
		WebUtils.sendTracking( "startClient", "start", Versioning.getVersion() + " (Build #" + Versioning.getBuildNumber() + ")" );
		
		console.primaryThread.start();
		
		getLogger().info( ChatColor.DARK_AQUA + "" + ChatColor.NEGATIVE + "Done (" + ( System.currentTimeMillis() - startTime ) + "ms)! For help, type \"help\" or \"?\"" );
		
		updater.check();
	}
	
	private static void showBanner()
	{
		try
		{
			InputStream is = Loader.class.getClassLoader().getResourceAsStream( "com/chiorichan/banner.txt" );
			String[] banner = new String( IOUtils.readFully( is, is.available(), true ) ).split( "\\n" );
			
			for ( String l : banner )
				instance.getConsole().sendMessage( ChatColor.GOLD + l );
			
			getLogger().info( ChatColor.NEGATIVE + "" + ChatColor.GOLD + "Starting " + Versioning.getProduct() + " Version " + Versioning.getVersion() );
			getLogger().info( ChatColor.NEGATIVE + "" + ChatColor.GOLD + Versioning.getCopyright() );
		}
		catch ( Exception e )
		{}
	}
	
	public static YamlConfiguration getConfig()
	{
		return configuration;
	}
	
	private String getConfigString( String variable, String defaultValue )
	{
		return configuration.getString( variable, defaultValue );
	}
	
	private int getConfigInt( String variable, int defaultValue )
	{
		return configuration.getInt( variable, defaultValue );
	}
	
	private boolean getConfigBoolean( String variable, boolean defaultValue )
	{
		return configuration.getBoolean( variable, defaultValue );
	}
	
	public String getUpdateFolder()
	{
		return configuration.getString( "settings.update-folder", "update" );
	}
	
	public File getUpdateFolderFile()
	{
		return new File( (File) options.valueOf( "plugins" ), configuration.getString( "settings.update-folder", "update" ) );
	}
	
	public static Client getInstance()
	{
		return instance;
	}
	
	public String toString()
	{
		return Versioning.getProduct() + " " + Versioning.getVersion();
	}
	
	public ConsoleReader getReader()
	{
		return console.reader;
	}
	
	public static void shutdown()
	{
		instance.getConsole().isRunning = false;
	}
	
	public ConsoleCommandSender getConsoleSender()
	{
		return console;
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
	
	private File getConfigFile()
	{
		return (File) options.valueOf( "config" );
	}
	
	private void saveConfig()
	{
		try
		{
			configuration.save( getConfigFile() );
		}
		catch ( IOException ex )
		{
			Logger.getLogger( Loader.class.getName() ).log( Level.SEVERE, "Could not save " + getConfigFile(), ex );
		}
	}
	
	public Console getConsole()
	{
		return console;
	}
	
	public static ConsoleLogManager getLogger()
	{
		return console.getLogger();
	}
	
	public static String getName()
	{
		return Versioning.getProduct();
	}
	
	public static String getVersion()
	{
		return Versioning.getVersion();
	}
	
	public OptionSet getOptions()
	{
		return options;
	}
	
	public AutoUpdater getAutoUpdater()
	{
		return updater;
	}

	@Override
	public boolean isRunning()
	{
		return true;
	}

	@Override
	public boolean getWarnOnOverload()
	{
		return true;
	}

	@Override
	public void stop()
	{
		shutdown();
	}
}