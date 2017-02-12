/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.plugin.console;

import jcurses.event.ActionEvent;
import jcurses.event.ActionListener;
import jcurses.event.ItemEvent;
import jcurses.event.ItemListener;
import jcurses.event.ValueChangedEvent;
import jcurses.event.ValueChangedListener;
import jcurses.event.WindowEvent;
import jcurses.event.WindowListener;
import jcurses.system.CharColor;
import jcurses.util.Message;
import jcurses.widgets.Button;
import jcurses.widgets.DefaultLayoutManager;
import jcurses.widgets.Label;
import jcurses.widgets.TextField;
import jcurses.widgets.Widget;
import jcurses.widgets.WidgetsConstants;
import jcurses.widgets.Window;

/**
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class TestWindow extends Window implements ItemListener, ActionListener, ValueChangedListener, WindowListener, WidgetsConstants
{
	private static TestWindow window = null;
	private static TextField textfield = null;
	private static Button button = null;
	
	TestWindow( int width, int height )
	{
		super( width, height, true, "JCurses Test" );
	}
	
	void init()
	{
		DefaultLayoutManager mgr = new DefaultLayoutManager();
		mgr.bindToContainer( window.getRootPanel() );
		mgr.addWidget( new Label( "Hello World!", new CharColor( CharColor.WHITE, CharColor.GREEN ) ), 0, 0, 20, 10, WidgetsConstants.ALIGNMENT_CENTER, WidgetsConstants.ALIGNMENT_CENTER );
		
		textfield = new TextField( 10 );
		mgr.addWidget( textfield, 0, 0, 20, 20, WidgetsConstants.ALIGNMENT_CENTER, WidgetsConstants.ALIGNMENT_CENTER );
		
		button = new Button( "Quit" );
		mgr.addWidget( button, 0, 0, 20, 30, WidgetsConstants.ALIGNMENT_CENTER, WidgetsConstants.ALIGNMENT_CENTER );
		
		button.setShortCut( 'q' );
		button.addListener( this );
		window.addListener( this );
		window.show();
	}
	
	@Override
	public void actionPerformed( ActionEvent event )
	{
		Widget w = event.getSource();
		if ( w == button )
		{
			new Message( "HowTo", "You are about to quit", "OK" ).show();
			window.close();
		}
	}
	
	@Override
	public void stateChanged( ItemEvent e )
	{
	}
	
	@Override
	public void valueChanged( ValueChangedEvent e )
	{
	}
	
	@Override
	public void windowChanged( WindowEvent event )
	{
		if ( event.getType() == WindowEvent.CLOSING )
		{
			event.getSourceWindow().close();
			// Toolkit.clearScreen(new CharColor(CharColor.WHITE, CharColor.BLACK));
		}
	}
}
