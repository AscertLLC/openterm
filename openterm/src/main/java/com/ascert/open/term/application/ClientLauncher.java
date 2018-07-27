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
import java.util.List;

import com.ascert.open.term.core.Host;

import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.ascert.open.term.core.SimpleConfig;
import com.ascert.open.term.core.TerminalFactoryRegistrar;
import com.ascert.open.term.gui.EmulatorFrame;

import gnu.rfb.server.DefaultRFBAuthenticator;
import gnu.rfb.server.RFBAuthenticator;
import gnu.rfb.server.RFBHost;
import gnu.vnc.awt.VNCComponentRobot;
    
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

        setLookAndFeel();

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
            startEmulator(availableHosts);
        }
        else
        {
            //TODO - start a server daemon in here e.g. to listen for VNC connections
            //       for now just add a wait so we can see if any errors come out
            startEmulatorServer(serverPort, availableHosts);
            
        }
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
    
    /**
     * Starts a simple interactive emulator
     */
    private static void startEmulator(List<Host> availableHosts)
        throws IOException
    {
        EmulatorFrame frmEmul8 = new EmulatorFrame(availableHosts);
        //TODO - temp, just to show we have a visible/usable panel even if not visible
        saveScreenShot(frmEmul8.pnlEmul8, "frmEmul8.pnlEmul8-before-visible.png");
        frmEmul8.setVisible(true);
        log.fine("*** emulator frame size: " + frmEmul8.getBounds());
    }
    
    //
    // Everything below here is experimental/prototype code for a sort of terminal server
    // which can be connected to using a standard VNC client
    //
    
    public static class RemoteScreenServer extends VNCComponentRobot
    {
     
        public RemoteScreenServer(int display, String displayName )
        {
            super(display, displayName);
        }

    }
    
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
        
        // temp for now - junk really, just to try
        
        EmulatorFrame frmEmul8 = new EmulatorFrame(availableHosts);
        frmEmul8.setVisible(true);
        
        //TODO - sure this will move!
        RemoteScreenServer.add(frmEmul8.pnlEmul8.getTerminalScreen());
        
        RFBHost rfbHost = new RFBHost(0, "host:0", RemoteScreenServer.class, new DefaultRFBAuthenticator("password") ) ;

        
//            JPanel pnlEmul8 = new EmulatorPanel(availableHosts, null);
//            
//            // allow to connect
//            synchronized(Thread.currentThread())
//            {
//                Thread.currentThread().wait(5 * 1000);
//            }
//            
//            Dimension dim = new Dimension(800,800);
//            pnlEmul8.setSize(dim);
//            pnlEmul8.setMinimumSize(dim);
//            pnlEmul8.setMaximumSize(dim);
//            pnlEmul8.setPreferredSize(dim);            
//            saveScreenShot(pnlEmul8, "pnlEmul8.png");
//            
//            JInternalFrame ifrm = new JInternalFrame();
//            ifrm.setSize(dim);
//            ifrm.setMinimumSize(dim);
//            ifrm.setMaximumSize(dim);
//            ifrm.setPreferredSize(dim);            
//            ifrm.add(pnlEmul8);
//            ifrm.pack();
//            saveScreenShot(ifrm, "ifrm.png");
//
//            JDesktopPane dsk = new JDesktopPane();
//            dsk.setSize(dim);
//            dsk.setMinimumSize(dim);
//            dsk.setMaximumSize(dim);
//            dsk.setPreferredSize(dim);            
//            dsk.add(ifrm);
//            //dsk.setVisible(true);
//            saveScreenShot(dsk, "dsk.png");
//            
//            System.out.println("*** pnlEmul8 size: " + pnlEmul8.getBounds() + ", graphics: " + pnlEmul8.getGraphics());
//            System.out.println("*** iframe size: " + ifrm.getBounds() + ", graphics: " + ifrm.getGraphics());
//            System.out.println("*** desktop size: " + dsk.getBounds());
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
