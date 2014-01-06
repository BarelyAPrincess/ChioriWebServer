package com.chiorichan.framework

import com.chiorichan.http.HttpRequest
import com.chiorichan.http.HttpResponse
import com.chiorichan.http.PersistentSession
import com.chiorichan.util.Versioning

abstract class scriptingBaseGroovy extends scriptingBaseJava
{
	Framework getFramework()
	{
		if ( chiori == null )
			chiori = request.getFramework();
		
		return chiori;
	}
	
	HttpRequest getRequest()
	{
		return request;
	}
	
	HttpResponse getResponse()
	{
		return response;
	}
	
	PersistentSession getSession()
	{
		return request.getSession();
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
		return Versioning.getVersion();
	}
	
	String getProduct()
	{
		return Versioning.getProduct();
	}
	
	String getCopyright()
	{
		return Versioning.getCopyright();
	}
	
	@Deprecated
	String get_version()
	{
		return getFramework().getProduct() + " " + getFramework().getVersion();
	}
}