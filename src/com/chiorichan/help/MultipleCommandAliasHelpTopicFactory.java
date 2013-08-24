package com.chiorichan.help;

import com.chiorichan.command.MultipleCommandAlias;

/**
 * This class creates {@link MultipleCommandAliasHelpTopic} help topics from {@link MultipleCommandAlias} commands.
 */
public class MultipleCommandAliasHelpTopicFactory implements HelpTopicFactory<MultipleCommandAlias>
{
	
	public HelpTopic createTopic( MultipleCommandAlias multipleCommandAlias )
	{
		return new MultipleCommandAliasHelpTopic( multipleCommandAlias );
	}
}
