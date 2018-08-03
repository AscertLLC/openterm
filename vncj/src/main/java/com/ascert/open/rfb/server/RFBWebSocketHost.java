/*
 * Copyright (c) 2018 Ascert, LLC.
 * www.ascert.com

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import gnu.rfb.server.RFBServer;

/**
 *
 * @version 1,0 02-Aug-2018
 * @author srm
 * @history
 *      02-Aug-2018    rhw        Created
 */
public class RFBWebSocketHost extends WebSocketServer
{

    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    private static final Logger log = Logger.getLogger(RFBWebSocketHost.class.getName());

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////

    public Map<String, RFBServerFactory> factoryMap = new ConcurrentHashMap<> ();
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////

	public RFBWebSocketHost( ) throws UnknownHostException 
    {
        // technically speaking, VNC port 5800 is for serving clients etc. But it's a convenient
        // standard to re-use
		super( new InetSocketAddress( 5800 ) );
	}
    
	public RFBWebSocketHost( int port ) throws UnknownHostException 
    {
		super( new InetSocketAddress( port ) );
	}

	public RFBWebSocketHost( InetSocketAddress address ) 
    {
        super( address );
    }
    
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    
    public void addFactory(String path, RFBServerFactory factory)
    {
        factoryMap.putIfAbsent(path, factory);
    }

    //////////////////////////////////////////////////
    // INTERFACE METHODS - WebSocketServer
    //////////////////////////////////////////////////
    
    //TODO - cribbed example code for now
    
	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) 
    {
        String path = handshake.getResourceDescriptor();
        RFBServerFactory factory = factoryMap.get(path);
        
        if (factory == null)
        {
            log.warning(String.format("Connection received for unknown path: %s (from: %s)", path, conn.getRemoteSocketAddress().getAddress().getHostAddress()));
            conn.close(CloseFrame.REFUSE, "Path not recgnised");
            return;
        }

        try
        {
            ProtocolStream prot = new ProtocolStream(factory, conn);
            conn.setAttachment(prot);
        }
        catch (Exception ex)
        {
            log.severe(String.format("Failed to create RFBServer",ex));
            conn.close(CloseFrame.ABNORMAL_CLOSE, "VNC Server creation error");
        }
	}

	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) 
    {
        try
        {
            ProtocolStream prot = conn.<ProtocolStream>getAttachment();
            if (prot != null)
            {
                prot.shutdown();
            }
        }
        catch (IOException ex)
        {
            log.warning(String.format("Error closing protocol stream",ex));
        }
	}

	@Override
	public void onMessage( WebSocket conn, String message ) 
    {
        log.warning(String.format("Illegal string message received from (from: %s)", conn.getRemoteSocketAddress().getAddress().getHostAddress()));
        conn.close(CloseFrame.REFUSE, "String messages not supported");
	}
    
	@Override
	public void onMessage( WebSocket conn, ByteBuffer message ) 
    {
        try
        {
            ProtocolStream prot = conn.<ProtocolStream>getAttachment();
            prot.clientDataIn(message.array());
        }
        catch (IOException ex)
        {
            log.severe(String.format("Error sending to protocol handler",ex));
            conn.close(CloseFrame.ABNORMAL_CLOSE, "VNC Server i/o error - " + ex.getMessage());
        }
	}


	@Override
	public void onError( WebSocket conn, Exception ex ) 
    {
        log.log(Level.WARNING,"WebSocket server error.", ex);
		if( conn != null ) {
            conn.close(CloseFrame.ABNORMAL_CLOSE, "VNC Server error - " + ex.getMessage());
		}
	}

	@Override
	public void onStart() 
    {
		log.info("WebSocket server started on port: " + this.getPort());
	}

    //////////////////////////////////////////////////
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PRIVATE INSTANCE METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // STATIC INNER CLASSES
    //////////////////////////////////////////////////

    public class ProtocolStream extends OutputStream
    {
        WebSocket conn;
        RFBServer server;
        RFBProtocolHandler handler;
        ByteArrayOutputStream toWebSocket = new ByteArrayOutputStream(2048);
        
        // Possibly overkill and thread heavy, but using streams disconnects thread handling from web server
        // and also allows consistent/common model for handling with RFBSocketHost. Note that the
        
        // Data in from client
        PipedOutputStream fromWebSocket = new PipedOutputStream();
        PipedInputStream toHandler = new PipedInputStream();
        
        public ProtocolStream(RFBServerFactory factory, WebSocket conn) throws Exception 
        {
            this.conn = conn;
            
            server = factory.getInstance(true);
            toHandler.connect(fromWebSocket);
            
            handler = new RFBProtocolHandler(toHandler, this, server, factory.getAuthenticator());
        }

        public void shutdown() throws IOException
        {
            //TODO - close stream. Need to check this is all that is needed and the protocol handler will detect
            //       the close and disconect/shutdown the client
            fromWebSocket.close();
            toHandler.close();
            toWebSocket.close();
        }

        public void clientDataIn(byte[] array)  throws IOException
        {
            fromWebSocket.write(array);
            fromWebSocket.flush();
        }

        //////////////////////////////////////////////////
        // INTERFACE METHODS - OutputStream
        //////////////////////////////////////////////////
        
        public synchronized void write(int b) throws IOException
        {
            toWebSocket.write(b);
        }

        public synchronized void flush()
        {
            try
            {
                conn.send(toWebSocket.toByteArray());
                toWebSocket.reset();
            }
            catch (WebsocketNotConnectedException wex)
            {
                // seems to happen around close - diag included if ever needed
                log.log(Level.FINEST, "protocol stream flush", wex);
            }
        }

        public synchronized void write(byte[] b) throws IOException
        {
            toWebSocket.write(b);
        }
            
        public synchronized void write(byte[] b, int off, int len) throws IOException
        {
            toWebSocket.write(b, off, len);
        }

        public void close()
        {
            conn.close();
        }
    }
    
    //////////////////////////////////////////////////
    // NON-STATIC INNER CLASSES
    //////////////////////////////////////////////////

}
