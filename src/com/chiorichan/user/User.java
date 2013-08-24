package com.chiorichan.user;

import java.util.Collection;
import java.util.Map;

import com.chiorichan.Main;
import com.chiorichan.plugin.Plugin;
import com.chiorichan.serialization.ConfigurationSerializable;

public class User implements ConfigurationSerializable
{
	public Main server;
	
	public Main getServer()
	{
		return server;
	}

	public String getName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void kick( String kickMessage )
	{
		
	}

	public void save()
	{
		
	}

	public void recalculatePermissions()
	{
		
	}

	@Override
	public Map<String, Object> serialize()
	{
		return null;
	}

	public void sendPluginMessage( Plugin source, String channel, byte[] message )
	{
		// TODO Auto-generated method stub
		
	}

	public Collection<? extends String> getListeningPluginChannels()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasPermission( String broadcastChannelAdministrative )
	{
		// TODO Auto-generated method stub
		return false;
	}

	public void sendMessage( String string )
	{
		// TODO Auto-generated method stub
		
	}

	public void setBanned( boolean b )
	{
		// TODO Auto-generated method stub
		
	}

	public String getAddress()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void setOp( boolean b )
	{
		// TODO Auto-generated method stub
		
	}

	public boolean isOp()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public Object getDisplayName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public boolean canSee( User user )
	{
		// TODO Auto-generated method stub
		return false;
	}

	public void setWhitelisted( boolean b )
	{
		// TODO Auto-generated method stub
		
	}

	public boolean isWhitelisted()
	{
		// TODO Auto-generated method stub
		return false;
	}
	
}
