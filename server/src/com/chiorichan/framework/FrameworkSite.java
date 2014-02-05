package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.chiorichan.database.SqlConnector;

public class FrameworkSite extends Site
{
	public FrameworkSite(ResultSet rs) throws SiteException
	{
		super( rs );
	}
	
	Site setDatabase( SqlConnector sql )
	{
		this.sql = sql;
		
		return this;
	}
}
