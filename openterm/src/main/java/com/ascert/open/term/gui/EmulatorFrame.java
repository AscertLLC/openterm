/*
 * Copyright (c) 2016, 2017 Ascert, LLC.
 * www.ascert.com
 *
 * Based on original code from FreeHost3270, copyright for derivations from original works remain:
 *  Copyright (C) 1998, 2001  Art Gillespie
 *  Copyright (2) 2005 the http://FreeHost3270.Sourceforge.net
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

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.*;

import com.ascert.open.term.core.Host;



/**
 * Emulator swing frame - simply a single window housing an EmulatorPanel when used standalone.
 */
public class EmulatorFrame extends JFrame
{
    private static final Logger log = Logger.getLogger(EmulatorFrame.class.getName());

    public EmulatorPanel pnlEmul8;
    
    /**
     * No-ops constructor. Asks users to enter the connection settings in the corresponding dialog box then proceeds as normal.
     */
    public EmulatorFrame()
    {
        this(new ArrayList<Host> ());
    }

    public EmulatorFrame(List<Host> available)
    {
        super();
        pnlEmul8 = new EmulatorPanel(available, this);
        add(pnlEmul8);        
        init();
    }

//    public void focusGained(FocusEvent evt)
//    {
//        rhp.requestFocus();
//    }
//
//    public void focusLost(FocusEvent evt)
//    {
//    }

    public void processEvent(AWTEvent evt)
    {
        if (evt.getID() == Event.WINDOW_DESTROY)
        {
            pnlEmul8.exit();
            dispose();
        }

        super.processEvent(evt);
    }

    /**
     * Performs operations neccessary to construct main application frame.
     *
     * @param host      DOCUMENT ME!
     * @param port      DOCUMENT ME!
     * @param available DOCUMENT ME!
     * @param parentFrame    DOCUMENT ME!
     */
    private void init()
    {
        setResizable(false);
        //setLayout(new BorderLayout());

        validate();
        repaint();
        pack();

        // Center on screen
        Dimension screen_size;
        Dimension frame_size;
        screen_size = Toolkit.getDefaultToolkit().getScreenSize();
        frame_size = this.getSize();

        int offX = frame_size.width;
        int offY = frame_size.height;

        Component parent = getParent();
        if (parent != null)
        {
            // If we have parentFrame component, offset the new window from it (cascade windows)
            setLocation(parent.getLocation().x + 20, parent.getLocation().y + 20);
        }
        else
        {
            setLocation((screen_size.width - offX) / 2, (screen_size.height - offY) / 2);
        }

//        addFocusListener(this);
    }

}
