package com.chiorichan.user.builtin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import com.chiorichan.database.SqlConnector;
import com.chiorichan.framework.Site;
import com.chiorichan.user.LoginException;
import com.chiorichan.user.LoginExceptionReasons;
import com.chiorichan.user.LookupAdapterException;
import com.chiorichan.user.User;
import com.chiorichan.user.UserMetaData;
import com.chiorichan.util.Common;

public class SqlAdapter implements UserLookupAdapter
{
	SqlConnector sql;
	String table;
	List<String> userFields;
	
	public SqlAdapter( Site site ) throws LookupAdapterException
	{
		sql = site.getDatabase();
		table = site.getYaml().getString( "users.table", "users" );
		userFields = site.getYaml().getStringList( "users.fields", new ArrayList<String>() );
	}
	
	public ResultSet getResultSet( String uid ) throws SQLException
	{
		if ( uid == null || uid.isEmpty() )
			return null;
		
		ResultSet rs = sql.query( "SELECT * FROM `users` WHERE `userID` = '" + uid + "' LIMIT 1;" );
		
		if ( rs == null || sql.getRowCount( rs ) < 1 )
			return null;
		
		return rs;
	}
	
	@Override
	public void saveUser( UserMetaData user )
	{
		
	}
	
	@Override
	public UserMetaData reloadUser( UserMetaData user )
	{
		return null;
	}
	
	@Override
	public UserMetaData loadUser( String username ) throws LoginException
	{
		try
		{
			UserMetaData meta = new UserMetaData();
			
			if ( username == null || username.isEmpty() )
				throw new LoginException( LoginExceptionReasons.emptyUsername );
			
			String additionalUserFields = "";
			for ( String f : userFields )
			{
				additionalUserFields += " OR `" + f + "` = '" + username + "'";
			}
			
			ResultSet rs = sql.query( "SELECT * FROM `" + table + "` WHERE `username` = '" + username + "' OR `userID` = '" + username + "'" + additionalUserFields + ";" );
			
			if ( rs == null || sql.getRowCount( rs ) < 1 )
				throw new LoginException( LoginExceptionReasons.incorrectLogin );
			
			meta.setAll( SqlConnector.convertRow( rs ) );
			
			meta.set( "displayName", ( rs.getString( "fname" ).isEmpty() ) ? rs.getString( "name" ) : rs.getString( "fname" ) + " " + rs.getString( "name" ) );
			
			return meta;
		}
		catch ( SQLException | JSONException e )
		{
			throw new LoginException( e );
		}
	}
	
	@Override
	public void preLoginCheck( User user ) throws LoginException
	{
		UserMetaData meta = user.getMetaData();
		
		if ( meta.getInteger( "numloginfail" ) > 5 )
			if ( meta.getInteger( "lastloginfail" ) > ( Common.getEpoch() - 1800 ) )
				throw new LoginException( LoginExceptionReasons.underAttackPleaseWait );
		
		if ( !meta.getString( "actnum" ).equals( "0" ) )
			throw new LoginException( LoginExceptionReasons.accountNotActivated );
	}
	
	@Override
	public void postLoginCheck( User user ) throws LoginException
	{
		sql.queryUpdate( "UPDATE `users` SET `lastactive` = '" + Common.getEpoch() + "', `lastlogin` = '" + Common.getEpoch() + "', `lastloginfail` = 0, `numloginfail` = 0 WHERE `userID` = '" + user.getUserId() + "'" );
	}
	
	@Override
	public void failedLoginUpdate( User user )
	{
		// TODO Update use as top reflect this failure.
		// sql.queryUpdate( "UPDATE `users` SET `lastactive` = '" + Common.getEpoch() + "', `lastloginfail` = 0, `numloginfail` = 0 WHERE `userID` = '" + user.getUserId() + "'" );
	}

	@Override
	public boolean matchUser( User user, String username )
	{
		UserMetaData meta = user.getMetaData();
		
		for ( String f : userFields )
		{
			if ( meta.getString( f ).equals( username ) )
				return true;
		}
		
		return false;
	}
}
