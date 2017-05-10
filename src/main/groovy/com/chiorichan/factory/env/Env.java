/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.factory.env;

import com.chiorichan.logger.Log;
import com.chiorichan.utils.UtilIO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class Env
{
	private Properties envVariables;

	public Env()
	{
		envVariables = new Properties();
	}

	public Env( Properties env )
	{
		envVariables = env;
	}

	public Env( File file ) throws IOException
	{
		this();
		envVariables.load( new FileInputStream( file ) );

		File gitIgnore = new File( file.getAbsoluteFile().getParentFile(), ".gitignore" );
		if ( gitIgnore.exists() )
		{
			List<String> gitIgnoreContents = UtilIO.readFileToLines( gitIgnore );
			boolean gitIgnored = false;

			// TODO Tweak for better detection of .gitignore contents
			for ( String line : gitIgnoreContents )
				if ( line.toLowerCase().equals( file.getName() ) )
					gitIgnored = true;

			if ( !gitIgnored )
				Log.get().warning( String.format( "The environment file [%s] is not present in the [.gitignore] file, it's recommended you do so for security reasons.", UtilIO.relPath( file ) ) );
		}
	}

	public String getEnvVariables( String var )
	{
		return envVariables.getProperty( var );
	}

	public Properties getProperties()
	{
		return envVariables;
	}
}
