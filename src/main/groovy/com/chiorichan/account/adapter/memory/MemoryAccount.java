package com.chiorichan.account.adapter.memory;

import com.chiorichan.account.Account;
import com.chiorichan.account.AccountMetaData;
import com.chiorichan.account.LoginException;
import com.chiorichan.account.adapter.AccountLookupAdapter;

public class MemoryAccount extends Account<AccountLookupAdapter>
{
	public MemoryAccount(AccountMetaData meta, AccountLookupAdapter adapter) throws LoginException
	{
		super( meta, adapter );
	}
	
	public MemoryAccount(String userId, AccountLookupAdapter adapter) throws LoginException
	{
		super( userId, adapter );
	}
	
	@Override
	public void preLoginCheck() throws LoginException
	{
		
	}
	
	@Override
	public void postLoginCheck() throws LoginException
	{
		
	}
	
	@Override
	public boolean isYou( String id )
	{
		if ( metaData.getString( "username" ).equals( id ) )
			return true;
		
		if ( metaData.getString( "acctId" ).equals( id ) )
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
		return getString( "username" );
	}

	@Override
	public String getUsername()
	{
		return getString( "username" );
	}
}
