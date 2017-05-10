/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.updater;

import java.util.List;
import java.util.stream.Collectors;

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
		String healthStr = healthReport.stream().map( HealthReport::toString ).collect( Collectors.joining( "," ) );
		String buildsStr = builds.stream().map( BuildArtifact::toString ).collect( Collectors.joining( "," ) );

		return name + "{Description:" + description + ",URL:" + url + ",Builds:{" + buildsStr + "},color:" + color + ",firstBuild:" + firstBuild + ",healthReport:{" + healthStr + "},lastSuccessfulBuild:" + lastSuccessfulBuild + ",lastStableBuild:" + lastStableBuild + ",lastUnsuccessfulBuild:" + lastUnsuccessfulBuild + ",lastUnstableBuild:" + lastUnstableBuild + "}";
	}
}
