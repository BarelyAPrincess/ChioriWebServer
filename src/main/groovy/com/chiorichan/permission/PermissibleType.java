package com.chiorichan.permission;

import java.util.Set;

import com.google.common.collect.Sets;

public class PermissibleType
{
	private static Set<PermissibleType> handlerTypes = Sets.newHashSet();
	private Set<String> names;
	
	static
	{
		new PermissibleType( "UNKNOWN" );
		// new PermissibleType( "CONSOLE" );
		// new PermissibleType( "TELNET" );
		new PermissibleType( "TCP" );
		new PermissibleType( "HTTP" );
	}
	
	private PermissibleType(String... typeName)
	{
		for ( String s : typeName )
			names.add( s.toUpperCase() );
			
		handlerTypes.add( this );
	}
	
	public Set<String> getNames()
	{
		return names;
	}
	
	public static PermissibleType lookup( String typeName )
	{
		for ( PermissibleType pt : handlerTypes )
		{
			if ( pt.getNames().contains( typeName.toUpperCase() ) )
				return pt;
		}
		
		return new PermissibleType( typeName );
	}
}
