package com.chiorichan.framework;

import com.caucho.quercus.env.ArrayValueImpl;
import com.chiorichan.Main;



public class Framework implements IFramework
{
	@Override
	public String hello()
	{
		return "Hello, Rainbow Dash!!! :D";
	}

	@Override
	public void error()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initalize( ArrayValueImpl phpArray )
	{
		Main.getLogger().info( "Array " + phpArray.getSize() );
		
	}

	@Override
	public String pageLoad( String source )
	{
		// TODO Auto-generated method stub
		return null;
	}
}
