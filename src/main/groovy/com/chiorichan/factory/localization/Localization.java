package com.chiorichan.factory.localization;

import com.chiorichan.Versioning;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.types.yaml.YamlConfiguration;
import com.chiorichan.helpers.Pair;
import com.chiorichan.tasks.Timings;
import com.chiorichan.zutils.ZIO;
import com.chiorichan.zutils.ZObjects;
import com.chiorichan.zutils.ZStrings;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Localization
{
	// TODO Implement language specification, right now only "en" is available
	private Map<String, Pair<Long, ConfigurationSection>> langCache = new ConcurrentHashMap<>();
	private String locale = "en";
	private File baseFile;

	public Localization( File baseFile )
	{
		ZObjects.notNull( baseFile );
		this.baseFile = new File( baseFile, locale );
		if ( !baseFile.exists() )
			baseFile.mkdirs();
	}

	public void setLocale( String locale )
	{
		this.locale = locale;
		this.baseFile = new File( baseFile, locale );
		if ( !baseFile.exists() )
			baseFile.mkdirs();
	}

	public String getLocale()
	{
		return locale;
	}

	private ConfigurationSection getLang( String key ) throws LocalizationException
	{
		// TODO Make it so yaml files can have multi-layer deep keys too, not just directories.
		// Might be worth implementing a common system for this under the ZIO or ZFiles.

		File file = new File( baseFile, key.replace( ".", "/" ) + ".yaml" );

		if ( !file.exists() )
			throw new LocalizationException( "Language file does not exist. [" + ZIO.relPath( file ) + "]" );

		ConfigurationSection yaml = YamlConfiguration.loadConfiguration( file );

		String lang = file.getName().substring( 0, file.getName().indexOf( "." ) );
		if ( yaml.getKeys().size() == 1 && yaml.isConfigurationSection( lang ) && yaml.getKeys().toArray( new String[0] )[0].equals( lang ) )
			yaml = yaml.getConfigurationSection( lang );

		return yaml;
	}

	public String localeTrans( String key ) throws LocalizationException
	{
		ZObjects.notEmpty( key );
		key = ZStrings.trimAll( key, '.' );
		if ( !key.contains( "." ) )
			throw new LocalizationException( "Language key must contain a prefix file, e.g., general.yaml -> general.welcomeText. [" + key + "]" );
		if ( !key.matches( "^[a-zA-Z0-9._-]*$" ) )
			throw new LocalizationException( "Language key contains illegal characters. [" + key + "]" );

		String prefix = key.substring( 0, key.lastIndexOf( "." ) );

		if ( ZObjects.isEmpty( prefix ) )
			throw new LocalizationException( "Language prefix is empty." );

		key = key.substring( key.lastIndexOf( "." ) + 1 );

		if ( ZObjects.isEmpty( key ) )
			throw new LocalizationException( "Language key is empty." );

		ConfigurationSection lang;
		if ( langCache.containsKey( prefix ) && langCache.get( prefix ).getKey() > Timings.epoch() - ( Versioning.isDevelopment() ? Timings.SECOND_15 : Timings.HOUR ) )
			lang = langCache.get( prefix ).getValue();
		else
		{
			lang = getLang( prefix );
			langCache.put( prefix, new Pair<>( Timings.epoch(), lang ) );
		}

		return lang.getString( key );
	}

	public String localeTrans( String key, Map<String, String> params ) throws LocalizationException
	{
		String str = localeTrans( key );

		for ( Map.Entry<String, String> param : params.entrySet() )
		{
			int inx = str.toLowerCase().indexOf( ":" + param.getKey().toLowerCase() );
			if ( inx == -1 )
				throw new LocalizationException( "Locale param is not found within language string. {key: " + param.getKey() + ", string: " + str + "}" );
			String tester = str.substring( inx, param.getKey().length() );
			String val = param.getValue();

			if ( ZStrings.isUppercase( tester ) )
				val = val.toUpperCase();
			if ( ZStrings.isCapitalizedWords( tester ) )
				val = ZStrings.capitalizeWords( val );

			str = str.substring( 0, inx ) + val + str.substring( inx + param.getKey().length() );
		}

		return str;
	}

	public String localePlural( String key, int cnt ) throws LocalizationException
	{
		String str = localeTrans( key );

		if ( str.contains( "|" ) )
		{
			String[] choices = str.split( "|" );

			for ( String choice : choices )
				if ( choice.startsWith( "[" ) && choice.contains( "]" ) )
				{
					String range = choice.substring( 1, choice.indexOf( "]" ) );
					try
					{
						if ( Integer.parseInt( range ) == cnt )
							return choice.substring( choice.indexOf( "]" ) ).trim();
					}
					catch ( NumberFormatException e )
					{
						String[] numbers = range.contains( "," ) ? range.split( "," ) : new String[] {range};
						for ( String num : numbers )
							try
							{
								if ( num.contains( "-" ) )
								{
									String[] lr = num.split( "-" );
									for ( int n = Integer.parseInt( lr[0] ); n <= Integer.parseInt( lr[1] ); n++ )
										if ( n == cnt )
											return choice.substring( choice.indexOf( "]" ) ).trim();
								}
								else if ( Integer.parseInt( num ) == cnt )
									return choice.substring( choice.indexOf( "]" ) ).trim();
							}
							catch ( NumberFormatException ee )
							{
								// Ignore
							}
					}
				}
				else
					break;

			if ( choices.length == 2 )
			{
				if ( cnt <= 1 )
					return choices[0];
				return choices[1];
			}
			else if ( choices.length == 3 )
			{
				if ( cnt == 0 )
					return choices[0];
				if ( cnt == 1 )
					return choices[1];
				return choices[2];
			}
			else
				return choices[0];
		}

		return str;
	}
}
