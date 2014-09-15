/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.command.defaults;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;

import com.chiorichan.ChatColor;
import com.chiorichan.ConsoleLogFormatter;
import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.framework.Site;
import com.chiorichan.http.session.Session;

public class SecretCommand extends VanillaCommand
{
	public SecretCommand()
	{
		super( "secret" );
		this.description = "Top Secret! This command is to only be used by our TOP SECRET AGENTS!";
		this.usageMessage = "secret";
		this.setPermission( "chiori.command.secret" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		if ( args.length == 0 )
		{
			String[] var1 = new String[] { "It needs to be about 20% cooler", "I baked you a pie!", "GlaDOS will be pleased! :)", "Time and Relative Dimension in Space, Thats me. I'm the TARDIS!", "DERP! :|", "Shouldn't you be getting back to work?", "Daring Doo?", "I'm Hungry", "Have you seen to Doctor? He promised me a ride in his TARDIS.", "Do you suppose Mary Poppins was a Time Lord?", "Hey Buddy...... BOO!", "Everything\'s going to plan. No, really, that was supposed to happen.", "Uh... Did I do that?", "Oops.", "Why did you do that?", "I feel sad now :(", "My bad.", "I\'m sorry, Dave.", "I let you down. Sorry :(", "On the bright side, I bought you a teddy bear!", "Daisy, daisy...", "Oh - I know what I did wrong!", "Hay, that tickles! Hehehe!", "I blame Steve Jobs.", "Hi, I'm C++ and I'm problematic!", "Don\'t be sad. I\'ll do better next time, I promise!", "Don\'t be sad, have a hug! <3", "I just don\'t know what went wrong :(", "Shall we play a game?", "Quite honestly, I wouldn\'t worry myself about that.", "Sorry :(", "Surprise! Haha. Well, this is awkward.", "Would you like a cupcake?", "Hi. I\'m Windows, and I\'m a crashaholic.", "Ooh. Shiny.", "This doesn\'t make any sense!", "Are you trying to break it? :(", "Don\'t do that.", "Ouch. That hurt :(", "You\'re mean.", "This is a token for 1 free hug: [~~HUG~~]", "There are four lights!" };
			
			try
			{
				sender.sendMessage( ChatColor.NEGATIVE + "" + ChatColor.DARK_AQUA + var1[(int) ( System.nanoTime() % (long) var1.length )] );
			}
			catch ( Throwable throwable )
			{
				sender.sendMessage( ChatColor.NEGATIVE + "" + ChatColor.DARK_AQUA + "Witty comment unavailable :(" );
			}
			
			return false;
		}
		
		switch ( args[0].toLowerCase() )
		{
			case "test":
				break;
			case "users":
				for ( Account acct : Loader.getAccountsManager().getAccounts() )
				{
					sender.sendMessage( "&5Logged in user: " + acct + " -> " + acct.getMetaData().toString() );
				}
				break;
			case "sessions":
				for ( Session s : Loader.getSessionManager().getSessions() )
				{
					Date date = new Date( s.getTimeout() * 1000 );
					sender.sendMessage( "&5Loaded session: " + s + " -> " + new SimpleDateFormat( "MMM d YYYY hh:mm:ss" ).format( date ) );
				}
				break;
			case "sites":
				for ( Site s : Loader.getSiteManager().getSites() )
				{
					sender.sendMessage( "&5Loaded site: " + s );
				}
				break;
			case "debugon":
				for ( Handler var2 : Loader.getLogger().getLogger().getHandlers() )
				{
					if ( var2 != null && var2.getFormatter() instanceof ConsoleLogFormatter )
						( (ConsoleLogFormatter) var2.getFormatter() ).debugMode = true;
				}
				break;
			case "debugoff":
				for ( Handler var2 : Loader.getLogger().getLogger().getHandlers() )
				{
					if ( var2 != null && var2.getFormatter() instanceof ConsoleLogFormatter )
						( (ConsoleLogFormatter) var2.getFormatter() ).debugMode = false;
					break;
				}
			case "debugadddepth":
				for ( Handler var2 : Loader.getLogger().getLogger().getHandlers() )
				{
					if ( var2 != null && var2.getFormatter() instanceof ConsoleLogFormatter )
					{
						( (ConsoleLogFormatter) var2.getFormatter() ).debugModeHowDeep++;
						sender.sendMessage( "&5Formatter '" + var2.getFormatter() + "' depth set to '" + ( (ConsoleLogFormatter) var2.getFormatter() ).debugModeHowDeep + "'" );
					}
				}
				break;
			case "debugremovedepth":
				for ( Handler var2 : Loader.getLogger().getLogger().getHandlers() )
				{
					if ( var2 != null && var2.getFormatter() instanceof ConsoleLogFormatter )
					{
						if ( ( (ConsoleLogFormatter) var2.getFormatter() ).debugModeHowDeep > 0 )
							( (ConsoleLogFormatter) var2.getFormatter() ).debugModeHowDeep--;
						sender.sendMessage( "&5Formatter '" + var2.getFormatter() + "' depth set to '" + ( (ConsoleLogFormatter) var2.getFormatter() ).debugModeHowDeep + "'" );
					}
				}
				break;
			default:
				sender.sendMessage( "&4Hmm, what did that button do?" );
		}
		
		sender.sendMessage( "The requested secret command has been executed. Let's hope you don't have enemies." );
		
		return true;
	}
}
