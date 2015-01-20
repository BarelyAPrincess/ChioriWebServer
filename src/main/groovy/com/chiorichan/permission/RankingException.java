package com.chiorichan.permission;

public class RankingException extends Exception
{
	protected PermissibleEntity target = null;
	protected PermissibleEntity promoter = null;
	
	public RankingException(String message, PermissibleEntity target, PermissibleEntity promoter)
	{
		super( message );
		this.target = target;
		this.promoter = promoter;
	}
	
	public PermissibleEntity getTarget()
	{
		return target;
	}
	
	public PermissibleEntity getPromoter()
	{
		return promoter;
	}
}
