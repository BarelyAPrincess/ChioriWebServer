package com.chiorichan.permission;

import com.chiorichan.Loader;
import com.chiorichan.permission.event.PermissibleEntityEvent;

public abstract class PermissibleGroup extends PermissibleEntity implements Comparable<PermissibleGroup>
{
	protected int weight = 0;
	
	public PermissibleGroup( String groupName )
	{
		super( groupName );
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
