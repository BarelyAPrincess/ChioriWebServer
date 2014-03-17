package com.chiorichan.updater;

public interface DownloadListener
{
	public void stateChanged( String fileName, float progress );
	public void stateDone();
}
