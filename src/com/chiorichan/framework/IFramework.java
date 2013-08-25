package com.chiorichan.framework;

import com.caucho.quercus.env.ArrayValueImpl;

public interface IFramework
{
	public String hello();
	
	public void error();
	
	public void initalize( ArrayValueImpl phpArray );
	
	public String pageLoad( String source );
}
