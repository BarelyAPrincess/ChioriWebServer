package com.chiorichan.framework

import com.chiorichan.http.HttpRequest
import com.chiorichan.http.HttpResponse
import com.chiorichan.http.PersistentSession
import com.chiorichan.util.Versioning
import com.google.common.base.Joiner

abstract class ScriptingBaseGroovy extends ScriptingBaseJava
{
    Framework getFramework()
    {
        if ( chiori == null )
        chiori = request.getFramework();
		
        return chiori;
    }
	
    void var_dump ( Object obj )
    {
        println var_export( obj )
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
}