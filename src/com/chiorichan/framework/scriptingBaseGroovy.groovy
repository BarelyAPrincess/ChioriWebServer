package com.chiorichan.framework

abstract class scriptingBaseGroovy extends scriptingBaseJava
{
	Framework getFramework()
	{
		return chiori;
	}
	
	//TODO: Make better and add support for other object types
	void var_dump( String var )
	{
		println var
	}
	
	void echo( String var )
	{
		println var
	}
	
	void include( String file )
	{
		println ( getFramework().getServer().fileReader( file ) );
	}
	
	String getVersion()
	{
		return getFramework().getVersion();
	}
	
	String getProduct()
	{
		return getFramework().getProduct();
	}
	
	@Deprecated
	String get_version()
	{
		return getFramework().getProduct() + " " + getFramework().getVersion();
	}
}