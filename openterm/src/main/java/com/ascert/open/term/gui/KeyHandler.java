/*
 * Copyright (c) 2016, 2017 Ascert, LLC.
 * www.ascert.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ascert.open.term.gui;

import java.awt.event.KeyListener;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.KeyStroke;

import com.ascert.open.term.core.InputCharHandler;

public interface KeyHandler
{

    InputMap getInputMap(InputMap parent);

    ActionMap getActionMap(ActionMap parent);

    JButton[][] getFKeyButtons();

    KeyListener getKeyListener();

    default void doKeyAction(String keyName, boolean clientRefresh)
    {
        doKeyAction(keyName, clientRefresh, true);
    }

    void doKeyAction(String keyName, boolean clientRefresh, boolean observeKbdLock);
    
    default void doKeyAction(KeyStroke keyStroke)
    {
        doKeyAction(keyStroke, true, true);
    }
    
    void doKeyAction(KeyStroke keyStroke, boolean clientRefresh, boolean observeKbdLock);

    public InputCharHandler getCharHandler();

}
