package com.chiorichan.exception;

import java.net.MalformedURLException;
import java.net.URL;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * This class is used to send exception reports to the developer.
 * 
 * @author Chiori Greene
 */
public class ExceptionReport
{
	private static final ThreadLocal<Yaml> YAML_INSTANCE = new ThreadLocal<Yaml>()
	{
		@Override
		protected Yaml initialValue()
		{
			DumperOptions opts = new DumperOptions();
			opts.setDefaultFlowStyle( DumperOptions.FlowStyle.FLOW );
			opts.setDefaultScalarStyle( DumperOptions.ScalarStyle.DOUBLE_QUOTED );
			opts.setPrettyFlow( true );
			opts.setWidth( Integer.MAX_VALUE ); // Don't wrap scalars -- json no like
			return new Yaml( opts );
		}
	};
	private static final URL GIST_POST_URL;
	
	static
	{
		try
		{
			GIST_POST_URL = new URL( "https://api.github.com/gists" );
		}
		catch ( MalformedURLException e )
		{
			throw new ExceptionInInitializerError( e );
		}
	}
}
