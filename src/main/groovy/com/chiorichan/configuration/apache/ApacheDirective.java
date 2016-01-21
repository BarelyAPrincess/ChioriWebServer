package com.chiorichan.configuration.apache;

import java.util.List;

import com.google.common.collect.Lists;

public class ApacheDirective
{
	protected final ApacheSection parent;
	protected final List<String> arguments;
	protected final String key;

	protected String source;
	protected int lineNum;

	public ApacheDirective()
	{
		parent = null;
		key = null;
		arguments = Lists.newArrayList();
	}

	public ApacheDirective( ApacheSection parent, String key, List<String> arguments )
	{
		this.parent = parent;
		this.key = key;
		this.arguments = arguments;
	}

	public String[] getArguments()
	{
		return arguments.toArray( new String[0] );
	}

	public String getKey()
	{
		return key;
	}

	public void hasArguments( int required, String describ ) throws ApacheDirectiveException
	{
		if ( describ == null || describ.length() == 0 )
			for ( int i = 0; i < required; i++ )
				describ += "<arg" + i + "> ";

		if ( arguments.size() < required )
			throw new ApacheDirectiveException( "Directive '" + key + "' missing required number of arguments, e.g., " + key + " " + describ );
	}

	public void isSection() throws ApacheDirectiveException
	{
		if ( ! ( this instanceof ApacheSection ) )
			throw new ApacheDirectiveException( "Directive '" + key + "' must be surrounded by brackets." );
	}
}
