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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import gnu.rfb.server.RFBAuthenticator;
import gnu.rfb.server.RFBServer;

/**
 *
 * @version 1,0 02-Aug-2018
 * @author srm
 * @history
 *      02-Aug-2018    rhw        Created
 */
public class RFBStandaloneWebSocketHost extends WebSocketServer
{

    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    private static final Logger log = Logger.getLogger(RFBStandaloneWebSocketHost.class.getName());

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////

    private Map<String, ProtocolConnection> protConn = new ConcurrentHashMap<> ();
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////

	public RFBStandaloneWebSocketHost( ) throws UnknownHostException 
    {
        // technically speaking, VNC port 5800 is for serving clients etc. But it's a convenient
        // standard to re-use
		super( new InetSocketAddress( 5800 ) );
	}
    
	public RFBStandaloneWebSocketHost( int port ) throws UnknownHostException 
    {
		super( new InetSocketAddress( port ) );
	}

	public RFBStandaloneWebSocketHost( InetSocketAddress address ) 
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
        protConn.putIfAbsent(path, new ProtocolConnection()
        {
            @Override
            public RFBProtocolAdapter getProtocolHandler(boolean newConnection) throws Exception
            {
                return new RFBProtocolAdapter(factory.getInstance(newConnection), factory.getAuthenticator());
            }
        });
    }

    
    public void addServer(String path, RFBServer server, RFBAuthenticator auth)
    {
        protConn.putIfAbsent(path, new ProtocolConnection()
        {
            @Override
            public RFBProtocolAdapter getProtocolHandler(boolean newConnection) throws Exception
            {
                return new RFBProtocolAdapter(server, auth);
            }
        });
    }
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS - WebSocketServer
    //////////////////////////////////////////////////
    
    //TODO - cribbed example code for now
    
	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) 
    {
        String path = handshake.getResourceDescriptor();
        ProtocolConnection prot = protConn.get(path);
        
        if (prot == null)
        {
            log.warning(String.format("Connection received for unknown path: %s (from: %s)", path, conn.getRemoteSocketAddress().getAddress().getHostAddress()));
            conn.close(CloseFrame.REFUSE, "Path not recognised");
            return;
        }
        
        try
        {
            RFBProtocolAdapter handler = prot.getProtocolHandler(true);
            handler.connect(new StandaloneWebSocketAdapter(conn));
            conn.setAttachment(handler);
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
            RFBProtocolAdapter prot = conn.<RFBProtocolAdapter>getAttachment();
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
            RFBProtocolAdapter prot = conn.<RFBProtocolAdapter>getAttachment();
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
		if( conn != null ) 
        {
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

    public interface ProtocolConnection
    {
        public RFBProtocolAdapter getProtocolHandler(boolean newConnection) throws Exception;
    }
    
    
    public static class StandaloneWebSocketAdapter
        implements WebSocketProvider
    {
        private final WebSocket conn;
        
        public StandaloneWebSocketAdapter(WebSocket conn)
        {
            this.conn = conn;
        }
        
        @Override
        public void send(byte[] byt) throws Exception
        {
            conn.send(byt);
        }

        @Override
        public void send(String txt) throws Exception
        {
            conn.send(txt);
        }

        @Override
        public void close()
        {
            conn.close();
        }
        
    }
    

}
