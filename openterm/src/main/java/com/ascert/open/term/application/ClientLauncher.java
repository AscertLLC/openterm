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
import gnu.rfb.server.RFBClient;

import com.ascert.open.rfb.server.RFBSocketHost;

import gnu.rfb.server.RFBServer;

import com.ascert.open.rfb.server.RFBServerFactory;
import com.ascert.open.rfb.server.RFBWebSocketHost;

import com.ascert.open.vnc.VNCScreenRobot;
    
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
    public static final String KEY_SOCKET_PORT = "com.ascert.open.term.server.socket";
    public static final String KEY_WEBSOCKET_PORT = "com.ascert.open.term.server.websocket";

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
        int socketPort = OpenTermConfig.getIntProp(KEY_SOCKET_PORT, -1);
        int websocketPort = OpenTermConfig.getIntProp(KEY_WEBSOCKET_PORT, -1);
        
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
        
        if (socketPort == -1 && websocketPort == -1)  
        {
            setLookAndFeel();
            startEmulator(availableHosts);
        }
        else
        {
            startEmulatorServer(socketPort, websocketPort, availableHosts);
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
    
    
    //-------------------------------------------------------------------------------------------------------
    // Everything below here is experimental/prototype code for a sort of terminal server
    // which can be connected to using a standard VNC client
    //
    
    private static List<RFBServerFactory> rfbFactories = new ArrayList<> ();
    private static List<RFBSocketHost> rfbHosts = new ArrayList<> ();
    private static RFBWebSocketHost hst;
    
    /**
     * Starts a simple emulator server
     */
    private static void startEmulatorServer(int socketPort, int websocketPort, List<Host> availableHosts)
        throws Exception
    {
        if (availableHosts.size() < 1)
        {
            throw new RuntimeException("Must be at least 1 host configured to act as an Emulator Server.");
        }
        
        initServerFactories(availableHosts);
        
        if (socketPort != -1)
        {
            startSocketServer(socketPort);
        }
        
        if (websocketPort != -1)
        {
            startWebSocketServer(websocketPort);
        }
    }
    
    private static void initServerFactories(List<Host> availableHosts)
    {
        for (int ix = 0; ix < availableHosts.size(); ix++)
        {
            rfbFactories.add(new RemoteScreenServerFactory(ix, availableHosts.get(ix)));
        }
    }

    private static void startSocketServer(int port)
        throws Exception
    {
        log.fine(String.format(" socket server port: %d, host count: %d", port, rfbFactories.size()));
        
        for (int ix = 0; ix < rfbFactories.size(); ix++)
        {
            rfbHosts.add(new RFBSocketHost(port, rfbFactories.get(ix)));
        }
    }
    
    private static void startWebSocketServer(int port)
        throws Exception
    {
        log.fine(String.format(" websocket server port: %d, host count: %d", port, rfbFactories.size()));

        hst = new RFBWebSocketHost(port);
        
        for (int ix = 0; ix < rfbFactories.size(); ix++)
        {
            //TODO - need way to paramaterise the path
            String path = String.format("/host/%d", ix);
            hst.addFactory(path, rfbFactories.get(ix));
        }
        
        hst.start();
    }

    
    public static class RemoteScreenServerFactory implements RFBServerFactory
    {
        private Host host;
        private final int displayNum;
        private int connCount;
        private RFBAuthenticator authenticator;
        
        public RemoteScreenServerFactory(int displayNum, Host host)
        {
            this.displayNum = displayNum;
            this.host = host;
            //TODO - need to allow proper password setting, probably on a per-host basis
            authenticator = new DefaultRFBAuthenticator("password");
        }
        
        @Override
        public RFBServer getInstance(boolean newClientConnection) throws InstantiationException, IllegalAccessException,
                                                                         IllegalArgumentException, InvocationTargetException
        {
            
            RFBServer retval = null;
            
            if (newClientConnection)
            {
                // In theory a JPanel is a lightweight component, and hence possibly renderable headless. Also doesn't need to be 
                // made visible or receive focus for use.
                EmulatorPanel pnlEmul8 = new EmulatorPanel(host, true);
                retval = new RemoteTerminal(pnlEmul8, String.format("%s (%d)", getDisplayName(), connCount++));
            }
            return retval;
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
            return host.getDisplayName();
        }

        @Override
        public RFBAuthenticator getAuthenticator()
        {
            //TODO - need to allow proper password setting, probably on a per-host basis
            return this.authenticator;
        }
     
    }
    
    // Bit of a hack this, but gives us quick and dirt way to detect when to disconnect terminal from host
    // A listener model would be cleaner
    
    public static class RemoteTerminal extends VNCScreenRobot
    {
        private final EmulatorPanel pnlEmul8;
        
        public RemoteTerminal(EmulatorPanel pnlEmul8, String displayName)
        {
            super(pnlEmul8.getTerminalScreen(), displayName);
            this.pnlEmul8 = pnlEmul8;
        }
        
        public void removeClient( RFBClient client )
        {
            super.removeClient(client);
            if (getClientCount() == 0)
            {
                pnlEmul8.disconnect();
            }
        }
        
    }
    
}
