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

import java.util.ArrayList;
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
 * @see #KEY_AVAILABLE
 * @see #KEY_HOST
 * @see #KEY_PORT
 * @see #KEY_TERMTYPE
 * @since 0.2
 */
public class ClientLauncher
{

    private static final Logger log = Logger.getLogger(ClientLauncher.class.getName());

    public static final String KEY_TERM_FACTORIES = "com.ascert.open.term.factories";

    public static final String KEY_HOST = "com.ascert.open.term.host";
    public static final String KEY_PORT = "com.ascert.open.term.port";
    public static final String KEY_TERMTYPE = "com.ascert.open.term.termtype";
    public static final String KEY_SSL = "com.ascert.open.term.ssl";

    public static final String KEY_AVAILABLE = "com.ascert.open.term.available";

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

        String factories = System.getProperty(KEY_TERM_FACTORIES);
        String otHost = System.getProperty(KEY_HOST);
        String otPortStr = System.getProperty(KEY_PORT);
        String otSSL = System.getProperty(KEY_SSL);
        String otTermType = System.getProperty(KEY_TERMTYPE);

        String available = System.getProperty(KEY_AVAILABLE);

        log.fine("launching FreeHost standalone GUI client with parameters:");
        log.fine(KEY_AVAILABLE + " = " + available);
        log.fine(KEY_HOST + " = " + otHost);
        log.fine(KEY_PORT + " = " + otPortStr);
        log.fine(KEY_SSL + " = " + otSSL);
        log.fine(KEY_TERMTYPE + " = " + otTermType);

        TerminalFactoryRegistrar.initTermTypeFactories(factories);

        ApplicationFrame appFrame;

        if (otHost != null && otPortStr != null && otTermType != null)
        {
            int otPort = 23;

            try
            {
                otPort = Integer.parseInt(otPortStr);
            }
            catch (NumberFormatException e)
            {
                log.severe(e.getMessage());
                e.printStackTrace();
            }
            appFrame = new ApplicationFrame(otHost, otPort, otTermType, "true".equalsIgnoreCase(otSSL));

        }
        else if (available != null)
        {
            //Pretty crude - carried over from original code
            List<Host> availableHosts = new ArrayList<>();

            for (String availableHost : available.split(";"))
            {
                String[] opts = availableHost.split(",");
                //TODO - Better bounds checking
                String hostName = opts[0];
                int hostPort = Integer.parseInt(opts[1]);
                String useSSL = opts[2];
                String termType = opts[3];
                availableHosts.add(new Host(hostName, hostPort, termType, "true".equalsIgnoreCase(useSSL)));
            }

            appFrame = new ApplicationFrame(availableHosts, null);
        }
        else
        {
            appFrame = new ApplicationFrame();
        }

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
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }
    }

}
