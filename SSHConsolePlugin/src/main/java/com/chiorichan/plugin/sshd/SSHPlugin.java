package com.chiorichan.plugin.sshd;

import com.chiorichan.lang.PluginException;
import com.chiorichan.plugin.loader.Plugin;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ProcessShellFactory;

import java.io.File;
import java.io.IOException;

public class SSHPlugin extends Plugin
{
	private SshServer sshd;

	@Override
	public void onDisable() throws PluginException
	{
		try
		{
			sshd.stop();
		}
		catch ( IOException e )
		{
			throw new PluginException( e );
		}
	}

	@Override
	public void onEnable() throws PluginException
	{
		try
		{
			sshd.start();
		}
		catch ( IOException e )
		{
			throw new PluginException( e );
		}
	}

	@Override
	public void onLoad() throws PluginException
	{
		sshd = SshServer.setUpDefaultServer();
		sshd.setKeyPairProvider( new SimpleGeneratorHostKeyProvider( new File( getDataFolder(), "hostkey" ) ) );
		sshd.setShellFactory( new ProcessShellFactory( new String[] {"/bin/sh", "-i", "-l"} ) );
		sshd.setPort( 2804 );
	}
}
