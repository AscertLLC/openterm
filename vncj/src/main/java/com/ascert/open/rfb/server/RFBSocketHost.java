/*
 * Copyright (c) 2018 Ascert, LLC.
 * www.ascert.com
 *
 * Based on original code from vncj (https://github.com/tliron/vncj)
 * Rights for for derivations from original works remain:
 *      Copyright (C) 2000-2002 by Tal Liron
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
package com.ascert.open.rfb.server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import gnu.rfb.server.RFBAuthenticator;
import gnu.rfb.server.RFBServer;


public class RFBSocketHost implements Runnable 
{
    
    private static final Logger log = Logger.getLogger(RFBSocketHost.class.getName());
    
    ///////////////////////////////////////////////////////////////////////////////////////
    // Private
    
    private boolean isRunning;
    private boolean threadFinished;
    private int port = 5900;
    private ServerSocket serverSocket;
    private ArrayList<RFBProtocolHandler> clientSockets = new ArrayList();
    private RFBServerFactory factory;
    
    //
    // Construction
    //
    
    public RFBSocketHost( int port, int display, String displayName, Class rfbServerClass, RFBAuthenticator authenticator ) throws NoSuchMethodException 
    {
        this(port, new ShareableServerFactory(display, displayName, rfbServerClass, authenticator));
    }
    
    public RFBSocketHost(int port, RFBServerFactory factory)
    {
        this.factory = factory;
        this.port = port;
        new Thread( this ).start();        
    }
    
    //
    // Runnable
    //
    
    public void run() {
        log.info("Socket server started on port: " + port);
        isRunning=true;
        threadFinished=false;
        try {
            serverSocket = new ServerSocket( port + factory.getDisplay() );
        }
        catch(Exception e) {
            log.log(Level.SEVERE, "Exception shutting down server VNCServer: " + factory.getDisplayName(), e); 
            close();
        }
        
        while( isRunning() ) {
            // Create client for each connected socket
            RFBProtocolHandler r;
			try {
                Socket s = serverSocket.accept();
                
				r = new RFBProtocolHandler( new BufferedInputStream (s.getInputStream()),
                                            new BufferedOutputStream(s.getOutputStream(), 16384 ),
                                            factory.getInstance(true), 
                                            factory.getAuthenticator() );
                clientSockets.add(r);
			} catch (Exception e) {
				if (!isRunning()) {
					log.info("Socket server stopped on port: " + port);
					return;
				}
				// TODO Auto-generated catch block
				throw new RuntimeException(
                    "Error accepting client connection", e);
			}
        }
        threadFinished=true;
        log.finest("Thread Finished");
    }
    
    public void close(){
        try{
            serverSocket.close();
            serverSocket=null;
            isRunning = false;
            // Block until the thread has exited gracefully
            while(threadFinished == false){
                try{
                    Thread.currentThread().sleep(20);
                }
                catch(InterruptedException x){
                }
            }
            
            // now go through all of there clientSockets that were spawned
            Iterator iter = clientSockets.iterator();
            while(iter.hasNext()){
                ((RFBProtocolHandler)iter.next()).close();
            }
        }
        catch(IOException e){
            log.log(Level.SEVERE, "Exception shutting down server VNCServer: " + factory.getDisplayName(), e); 
        }
        finally{
            serverSocket=null;
        }        
    }
    
    public synchronized void stop()
    {
    	this.isRunning = false;
        // now go through all of there clientSockets that were spawned
        Iterator iter = clientSockets.iterator();
        while(iter.hasNext()){
            ((RFBProtocolHandler)iter.next()).close();
        }

    	try
    	{
    		this.serverSocket.close();
    	}
    	catch (IOException ex)
    	{
    		throw new RuntimeException("Error closing server", ex);
    	}

        log.finest("end of the work");
        Thread.currentThread().interrupt();
    }
    
    public String getDisplayName(){
        return(factory.getDisplayName());
    }
    
    private synchronized boolean isRunning() {
        return this.isRunning;
    }
    
    
    public static class ShareableServerFactory implements RFBServerFactory
    {
        protected int display;
        protected String displayName;
        
        private Constructor constructor;
        private RFBServer shareableInstance;
        protected RFBAuthenticator authenticator;
        
        public ShareableServerFactory( int display, String displayName, Class rfbServerClass, RFBAuthenticator authenticator ) throws NoSuchMethodException 
        {
            this.display = display;
            this.displayName = displayName;
            // Get constructor
            constructor = rfbServerClass.getDeclaredConstructor( new Class[] { int.class, String.class } );

            // Are we assignable to RFBServer
            if( !RFBServer.class.isAssignableFrom( rfbServerClass ) )
                throw new NoSuchMethodException( "Class does not support RFBServer interface" );
        }
        
        public synchronized RFBServer getInstance(boolean newClientConnection) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
        {
            if (shareableInstance == null)
            {
                shareableInstance = (RFBServer) constructor.newInstance( new Object[] { display , displayName } );
            }
            
            return shareableInstance;
        }

        public boolean isShareable()
        {
            return true;
        }

        /**
         * @return the display
         */
        public int getDisplay()
        {
            return display;
        }

        /**
         * @return the displayName
         */
        public String getDisplayName()
        {
            return displayName;
        }

        /**
         * @return the authenticator
         */
        public RFBAuthenticator getAuthenticator()
        {
            return authenticator;
        }
        
    }
    
}
