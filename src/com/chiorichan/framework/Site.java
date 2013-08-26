package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.chiorichan.Main;

public class Site
{
	public String siteId, title, domain, metatags, aliases, protectedFiles;
	
	public Site( ResultSet rs ) throws SQLException
	{
		siteId = rs.getString( "siteID" );
		title = rs.getString( "title" );
		domain = rs.getString( "domain" );
		protectedFiles = rs.getString( "protected" );
		metatags = rs.getString( "metatags" );
		aliases = rs.getString( "aliases" );
		
		Main.getLogger().info( "Loading site '" + siteId + "' with title '" + title + "' from Framework Database." );
	}
	
	public Site( String id, String title0, String domain0 )
	{
		siteId = id;
		title = title0;
		domain = domain0;
		protectedFiles = "";
		metatags = "";
		aliases = "";
	}
}