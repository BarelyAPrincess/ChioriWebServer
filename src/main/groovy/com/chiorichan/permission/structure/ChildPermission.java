package com.chiorichan.permission.structure;

import java.util.List;

import com.chiorichan.framework.Site;

public class ChildPermission
{
	public Permission perm;
	public List<Site> sites;
	public PermissionValue value;
	
	public ChildPermission( Permission parent, List<Site> siteList, PermissionValue childValue )
	{
		perm = parent;
		sites = siteList;
		value = childValue;
	}
	
	public Permission getPermission()
	{
		return perm;
	}
	
	public List<Site> getSites()
	{
		return sites;
	}
	
	public PermissionValue getValue()
	{
		return value;
	}
}
