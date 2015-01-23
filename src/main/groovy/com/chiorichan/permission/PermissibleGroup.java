package com.chiorichan.permission;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.chiorichan.Loader;
import com.chiorichan.permission.event.PermissibleEntityEvent;

public abstract class PermissibleGroup extends PermissibleEntity implements Comparable<PermissibleGroup>
{
	protected int weight = 0;
	
	public PermissibleGroup( String groupName )
	{
		super( groupName );
	}
	
	@Override
	public void initialize()
	{
		super.initialize();
		
		if ( this.isDebug() )
		{
			PermissionManager.getLogger().info( "Group " + getName() + " initialized" );
		}
	}
	
	public int getWeight()
	{
		return weight;
	}
	
	public void setWeight( int weight )
	{
		this.weight = weight;
		Loader.getPermissionsManager().callEvent( new PermissibleEntityEvent( this, PermissibleEntityEvent.Action.WEIGHT_CHANGED ) );
	}
	
	@Override
	public int compareTo( PermissibleGroup o )
	{
		return this.getWeight() - o.getWeight();
	}
}
