package com.chiorichan.framework;

import java.util.List;

import com.chiorichan.database.SqlConnector;

public class FrameworkConfigurationManager
{
	protected Framework fw;
	
	public FrameworkConfigurationManager(Framework fw0)
	{
		fw = fw0;
	}
	
	public Object get( String key )
	{
		try
		{
			return fw.getRequest().getSite().getYaml().get( key );
		}
		catch ( Exception e )
		{
			return null;
		}
	}
	
	public boolean keyExists( String key )
	{
		return ( fw.getRequest().getSite().getYaml().get( key ) != null );
	}
	
	public String getString( String key )
	{
		return fw.getRequest().getSite().getYaml().getString( key );
	}
	
	public List<?> getArray( String key )
	{
		return fw.getRequest().getSite().getYaml().getList( key );
	}
	
	public boolean getBoolean( String key )
	{
		return fw.getRequest().getSite().getYaml().getBoolean( key );
	}
	
	public int getInt( String key )
	{
		return fw.getRequest().getSite().getYaml().getInt( key );
	}
	
	public SqlConnector getDatabase()
	{
		return fw.getRequest().getSite().sql;
	}
}
