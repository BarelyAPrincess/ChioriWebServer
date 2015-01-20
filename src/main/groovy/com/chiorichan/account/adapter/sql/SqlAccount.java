package com.chiorichan.account.adapter.sql;

import java.sql.SQLException;

import com.chiorichan.account.Account;
import com.chiorichan.account.AccountMetaData;
import com.chiorichan.account.LoginException;
import com.chiorichan.account.LoginExceptionReason;
import com.chiorichan.account.adapter.AccountLookupAdapter;
import com.chiorichan.permission.PermissibleType;
import com.chiorichan.util.Common;

public class SqlAccount extends Account<SqlAdapter>
{
	public SqlAccount(AccountMetaData meta, SqlAdapter adapter) throws LoginException
	{
		super( meta, adapter );
	}
	
	@Override
	public void preLoginCheck() throws LoginException
	{
		if ( metaData.getInteger( "numloginfail" ) > 5 )
			if ( metaData.getInteger( "lastloginfail" ) > ( Common.getEpoch() - 1800 ) )
				throw new LoginException( LoginExceptionReason.underAttackPleaseWait );
		
		if ( !metaData.getString( "actnum" ).equals( "0" ) )
			throw new LoginException( LoginExceptionReason.accountNotActivated );
	}
	
	@Override
	public void postLoginCheck() throws LoginException
	{
		try
		{
			lookupAdapter.sql.queryUpdate( "UPDATE `accounts` SET `lastActive` = '" + Common.getEpoch() + "', `lastLogin` = '" + Common.getEpoch() + "', `lastLoginFail` = 0, `numLoginFail` = 0 WHERE `accountID` = '" + getAcctId() + "'" );
		}
		catch ( SQLException e )
		{
			throw new LoginException( e );
		}
	}
	
	@Override
	public boolean isYou( String id )
	{
		for ( String f : lookupAdapter.accountFields )
			if ( metaData.getString( f ) != null && metaData.getString( f ).equals( id ) )
				return true;
		
		return false;
	}
	
	@Override
	public String getPassword()
	{
		return getString( "password" );
	}

	@Override
	public String getDisplayName()
	{
		return ( getString( "fname" ).isEmpty() ) ? getString( "name" ) : getString( "fname" ) + " " + getString( "name" );
	}

	@Override
	public String getUsername()
	{
		return getString( "username" );
	}
}
