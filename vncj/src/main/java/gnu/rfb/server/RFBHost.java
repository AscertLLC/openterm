package gnu.rfb.server;

/**
 * <br><br><center><table border="1" width="80%"><hr>
 * <strong><a href="http://www.amherst.edu/~tliron/vncj">VNCj</a></strong>
 * <p>
* Copyright (C) 2000-2002 by Tal Liron
 * <p>
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public License
* as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* <a href="http://www.gnu.org/copyleft/lesser.html">GNU Lesser General Public License</a>
 * for more details.
 * <p>
* You should have received a copy of the <a href="http://www.gnu.org/copyleft/lesser.html">
* GNU Lesser General Public License</a> along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * <hr></table></center>
 **/

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import gnu.logging.*;


public class RFBHost implements Runnable {
    
    ///////////////////////////////////////////////////////////////////////////////////////
    // Private
    
    //private RFBServer sharedServer = null;
    private boolean isRunning;
    private boolean threadFinished;
    private ServerSocket serverSocket;
    private ArrayList<RFBSocket> clientSockets = new ArrayList();
    private RFBServerFactory factory;
    
    //
    // Construction
    //
    
    public RFBHost( int display, String displayName, Class rfbServerClass, RFBAuthenticator authenticator ) throws NoSuchMethodException 
    {
        this(new ShareableServerFactory(display, displayName, rfbServerClass, authenticator));
    }
    
    public RFBHost(RFBServerFactory factory )
    {
        this.factory = factory;
        new Thread( this ).start();        
    }
    
    //
    // Runnable
    //
    
    public void run() {
        isRunning=true;
        threadFinished=false;
        try {
            serverSocket = new ServerSocket( 5900 + factory.getDisplay() );
            //setSharedServer(factory.getInstance(false));
        }
        catch(Exception e) {
            VLogger.getLogger().log("Got an exception, shutting down server VNCServer for: " + factory.getDisplayName(),e); 
            close();
        }
        
        while( isRunning() ) {
            // Create client for each connected socket
            RFBSocket r;
			try {
				r = new RFBSocket( serverSocket.accept(), factory.getInstance(true), this, factory.getAuthenticator() );
                clientSockets.add(r);
			} catch (Exception e) {
				if (!isRunning()) {
					System.out.println("Server Stopped.");
					return;
				}
				// TODO Auto-generated catch block
				throw new RuntimeException(
                    "Error accepting client connection", e);
			}
        }
        threadFinished=true;
        System.out.println("Thread Finished");
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
                ((RFBSocket)iter.next()).close();
            }
        }
        catch(IOException e){
            VLogger.getLogger().log("Got an exception while shutting down server VNCServer for: " + factory.getDisplayName(),e); 
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
            ((RFBSocket)iter.next()).close();
        }

    	try
    	{
    		this.serverSocket.close();
    	}
    	catch (IOException ex)
    	{
    		throw new RuntimeException("Error closing server", ex);
    	}

        System.out.println("end of the work");
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
