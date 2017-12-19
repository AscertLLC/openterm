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
package com.ascert.open.term.application;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * About FreeHost3270 dialog. Shows short summary information about FreeHost3270 application, copyrights and pointer to the projects home
 * site.
 */
public class AboutFrame extends JDialog
{

    public AboutFrame(Frame owner, String name)
    {
        super(owner, true);
        setTitle("About " + name);

        setLayout(new BorderLayout());

        String AboutMsg = OpenTermConfig.getProp("help.about.msg", "").replaceAll("@@VERSION-NO@@", OpenTermConfig.
                                                                                  getProp("product.version"));
        add("Center", new JLabel(AboutMsg, JLabel.CENTER));

        JPanel p = new JPanel();
        p.setLayout(new FlowLayout());
        p.add(new JButton(new AbstractAction("OK")
        {
            public void actionPerformed(ActionEvent evt)
            {
                AboutFrame.this.setVisible(false);
            }
        }));
        add("South", p);
        pack();

        Dimension screen_size;
        Dimension dlg;
        screen_size = Toolkit.getDefaultToolkit().getScreenSize();
        dlg = this.getSize();
        setLocation((screen_size.width - dlg.width) / 2,
                    (screen_size.height - dlg.height) / 2);
    }
}
