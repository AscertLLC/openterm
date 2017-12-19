/*
 * Copyright (c) 2016, 2017 Ascert, LLC.
 * www.ascert.com
 *
 * Based on original code from FreeHost3270, copyright for derivations from original works remain:
 *  Copyright (C) 1998, 2001  Art Gillespie
 *  Copyright (2) 2005 the http://FreeHost3270.Sourceforge.net
 *
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

import java.util.List;

import com.ascert.open.term.core.Host;

import java.util.logging.Logger;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.ascert.open.term.core.SimpleConfig;
import com.ascert.open.term.core.TerminalFactoryRegistrar;

/**
 * Behaviour is a little different here to original Freehost3270. Session server feature has been bypassed (at least for now). In it's
 * place, we have a TerminalFactory model which allows support for multiple device type emulators
 *
 * @see #KEY_HOSTS
 * @see #KEY_HOST
 * @see #KEY_PORT
 * @see #KEY_TERMTYPE
 * @since 0.2
 */
public class ClientLauncher
{

    private static final Logger log = Logger.getLogger(ClientLauncher.class.getName());

    public static final String KEY_TERM_FACTORIES = "com.ascert.open.term.factories";
    public static final String KEY_HOSTS = "com.ascert.open.term.hosts";

    public static SimpleConfig config;

    /**
     * Launches the stand-alone client application.
     *
     * @param args DOCUMENT ME!
     */
    public static void main(String[] args)
        throws Exception
    {
        OpenTermConfig.initConfig(new OpenTermConfig("openterm.properties", true));

        setLookAndFeel();

        String factories = OpenTermConfig.getProp(KEY_TERM_FACTORIES);
        String hosts = OpenTermConfig.getProp(KEY_HOSTS);
        String favouriteHosts = OpenTermConfig.getProp("favourite.hosts");
        
        log.fine("launching FreeHost standalone GUI client with parameters:");
        log.fine(KEY_HOSTS + " = " + hosts);
        log.fine(" favour.hosts = " + favouriteHosts);

        TerminalFactoryRegistrar.initTermTypeFactories(factories);

        List<Host> availableHosts = Host.getHostStringAsList(favouriteHosts, true);
        availableHosts.addAll(Host.getHostStringAsList(hosts, false));
        ApplicationFrame appFrame = new ApplicationFrame(availableHosts, null);
        appFrame.setVisible(true);
    }

    private static void setLookAndFeel()
    {
        String lafName = OpenTermConfig.getProp("laf.name");

        try
        {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
            {
                if (info.getName().equals(lafName))
                {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
        catch (Exception e)
        {
            // If Nimbus is not hosts, you can set the GUI to another look and feel.
        }
    }

}
