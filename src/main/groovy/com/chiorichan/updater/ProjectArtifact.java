/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.updater;

import java.util.List;

public class ProjectArtifact
{
	public String description;
	public String displayName;
	public String displayNameOrNull;
	public String name;
	public String url;
	public boolean buildable;
	public List<BuildArtifact> builds;
	public String color;
	public BuildArtifact firstBuild;
	public List<HealthReport> healthReport;
	public boolean inQueue;
	public boolean keepDependencies;
	public BuildArtifact lastBuild;
	public BuildArtifact lastCompletedBuild;
	public BuildArtifact lastFailedBuild;
	public BuildArtifact lastStableBuild;
	public BuildArtifact lastSuccessfulBuild;
	public BuildArtifact lastUnstableBuild;
	public BuildArtifact lastUnsuccessfulBuild;
	public int nextBuildNumber;
	
	public class BuildArtifact
	{
		public int number;
		public String url;
		
		public String toString()
		{
			return "{#" + number + ",url:" + url + "}";
		}
	}
	
	public class HealthReport
	{
		public String description;
		public String iconUrl;
		public int score;
		
		public String toString()
		{
			return "{description:" + description + ",iconUrl:" + iconUrl + ",score:" + score + "}";
		}
	}
	
	public String toString()
	{
		StringBuilder healthf = new StringBuilder();
		
		if ( healthReport != null )
		{
			for ( HealthReport ba : healthReport )
			{
				healthf.append( "," + ba.toString() + "\n" );
			}
			healthf.deleteCharAt( 1 );
		}
		
		StringBuilder buildsf = new StringBuilder();
		
		if ( builds != null )
		{
			for ( BuildArtifact ba : builds )
			{
				buildsf.append( "," + ba.toString() + "\n" );
			}
			buildsf.deleteCharAt( 1 );
		}
		
		return name + "(Description:" + description + ",URL:" + url + ",Builds:[" + buildsf.toString() + "],color:" + color + ",firstBuild:" + firstBuild + ",healthReport:[" + healthf.toString() + "],lastSuccessfulBuild:" + lastSuccessfulBuild + ",lastStableBuild:" + lastStableBuild + ",lastUnsuccessfulBuild:" + lastUnsuccessfulBuild + ",lastUnstableBuild:" + lastUnstableBuild + ")";
	}
}
