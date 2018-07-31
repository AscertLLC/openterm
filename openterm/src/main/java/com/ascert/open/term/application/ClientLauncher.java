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

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.ascert.open.term.core.Host;

import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.ascert.open.term.core.SimpleConfig;
import com.ascert.open.term.core.TerminalFactoryRegistrar;
import com.ascert.open.term.gui.EmulatorFrame;
import com.ascert.open.term.gui.EmulatorPanel;

import gnu.rfb.server.DefaultRFBAuthenticator;
import gnu.rfb.server.RFBAuthenticator;
import gnu.rfb.server.RFBHost;
import gnu.rfb.server.RFBServer;
import gnu.rfb.server.RFBServerFactory;
import gnu.vnc.awt.VNCScreenRobot;
    
/**
 * Behaviour is a little different here to original Freehost3270. A new TerminalFactory model has been added that supports emulators for 
 * different device types. The session server feature has been removed. In it's place is a new "VNC terminal server"
 * concept allowing remote emulators to be supported using any standard VNC client software. 
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
    public static final String KEY_SERVER = "com.ascert.open.term.server.port";

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

        String factories = OpenTermConfig.getProp(KEY_TERM_FACTORIES);
        String hosts = OpenTermConfig.getProp(KEY_HOSTS);
        String favouriteHosts = OpenTermConfig.getProp("favourite.hosts");
        int serverPort = OpenTermConfig.getIntProp(KEY_SERVER, -1);
        
        log.fine("launching FreeHost standalone GUI client with parameters:");
        log.fine(KEY_HOSTS + " = " + hosts);
        log.fine(" favourite.hosts = " + favouriteHosts);

        TerminalFactoryRegistrar.initTermTypeFactories(factories);

        List<Host> availableHosts = Host.getHostListFromConfigString(favouriteHosts);
        availableHosts.addAll(Host.getHostStringAsList(hosts, false));
        
        //TODO - headless will not render a proper frame, so we have a 'server' mode
        //       concept instead
        //GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment(); 
        //if (!ge.isHeadless())
        
        if (serverPort == -1)  
        {
            setLookAndFeel();
            startEmulator(availableHosts);
        }
        else
        {
            //TODO - start a server daemon in here e.g. to listen for VNC connections
            //       for now just add a wait so we can see if any errors come out
            startEmulatorServer(serverPort, availableHosts);
        }
    }
    
    /** Do not call this method in headless or terminal server modes */    
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
    
    /**
     * Starts a simple interactive emulator
     */
    private static void startEmulator(List<Host> availableHosts)
        throws IOException
    {
        EmulatorFrame frmEmul8 = new EmulatorFrame(availableHosts);
        //TODO - temp diag code to show we have a usable panel even if not visible
        //saveScreenShot(frmEmul8.pnlEmul8, "frmEmul8.pnlEmul8-before-visible.png");
        frmEmul8.setVisible(true);
        log.fine("*** emulator frame size: " + frmEmul8.getBounds());
    }
    
    //-------------------------------------------------------------------------------------------------------
    // Everything below here is experimental/prototype code for a sort of terminal server
    // which can be connected to using a standard VNC client
    //
    
    public static class RemoteScreenServerFactory implements RFBServerFactory
    {
        // bit silly havign a single host list, but client code expects a list
        List<Host> hosts = new ArrayList<> ();
        private final int displayNum;
        private int connCount;
        private RFBAuthenticator authenticator;
        
        public RemoteScreenServerFactory(int displayNum, Host host)
        {
            this.displayNum = displayNum;
            this.hosts.add(host);
            //TODO - need to allow proper password setting, probably on a per-host basis
            authenticator = new DefaultRFBAuthenticator("password");
        }
        
        @Override
        public RFBServer getInstance(boolean newClientConnection) throws InstantiationException, IllegalAccessException,
                                                                         IllegalArgumentException, InvocationTargetException
        {
            // In theory a JPanel is a lightweight component, and hence possibly renderable headless. Also doesn't need to be 
            // made visible or receive focus for use.
            EmulatorPanel pnlEmul8 = new EmulatorPanel(hosts, null);
            //EmulatorFrame frmEmul8 = new EmulatorFrame(hosts);
            //EmulatorPanel pnlEmul8 = frmEmul8.pnlEmul8;
            //frmEmul8.setVisible(true);
            return new VNCScreenRobot(pnlEmul8.getTerminalScreen(), String.format("%s (%d)", getDisplayName(), connCount++));
        }

        @Override
        public boolean isShareable()
        {
            return false;
        }

        @Override
        public int getDisplay()
        {
            return displayNum;
        }

        @Override
        public String getDisplayName()
        {
            return this.hosts.get(0).getDisplayName();
        }

        @Override
        public RFBAuthenticator getAuthenticator()
        {
            //TODO - need to allow proper password setting, probably on a per-host basis
            return this.authenticator;
        }
     
    }
    
    private static List<RFBHost> rfbHosts = new ArrayList<> ();

    
    /**
     * Starts a simple emulator server
     */
    private static void startEmulatorServer(int serverPort, List<Host> availableHosts)
        throws Exception
    {
        if (availableHosts.size() < 1)
        {
            throw new RuntimeException("Must be at least 1 host configured to act as an Emulator Server.");
        }

        log.fine(String.format(" server port: %d, host count: %d", serverPort, availableHosts.size()));

        for (int ix = 0; ix < availableHosts.size(); ix++)
        {
            RFBServerFactory factory = new RemoteScreenServerFactory(ix, availableHosts.get(ix));
            //TODO - need to allow password setting, poss per host
            rfbHosts.add(new RFBHost(factory));
        }
        
    }
    
    private static void saveScreenShot(Component comp, String fileName)
        throws IOException
    {
            BufferedImage bi = getScreenShot(comp);
            File outFile = new File(fileName);
            ImageIO.write(bi, "png", outFile);
    }
    
    private static BufferedImage getScreenShot(Component panel)
    {
        BufferedImage bi = new BufferedImage(
            panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        panel.paint(bi.getGraphics());
        return bi;
    }
    
}
