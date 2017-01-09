/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
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
