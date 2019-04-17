/*
 * Copyright (c) 2000-2018 Ascert, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Ascert, LLC. ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with the terms
 * of the license agreement you entered into with Ascert.
 *
 * ASCERT MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
 * OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. ASCERT SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */
package com.ascert.open.rfb.server;


import java.io.IOException;
import java.nio.ByteBuffer;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.*;

import gnu.rfb.server.RFBAuthenticator;
import gnu.rfb.server.RFBServer;

/**
 *
 * @version 1,0 30-Aug-2018
 * @author srm

 *      30-Aug-2018    srm        Created
 */
public class RFBJettyWebSocket extends WebSocketAdapter
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////
    
    private static final Logger log = Logger.getLogger(RFBJettyWebSocket.class.getName());

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    
    protected Session session;
    private RemoteEndpoint remote;
    private RFBProtocolAdapter protAdapt;
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    
    public RFBJettyWebSocket(RFBServer server, RFBAuthenticator auth)
    {
        protAdapt = new RFBProtocolAdapter(server, auth);
    }
    
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////
    
    public RemoteEndpoint getRemote()
    {
        return remote;
    }
    
    public Session getSession()
    {
        return session;
    }

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////

    public void onWebSocketConnect(Session session)
    {
        //UpgradeRequest req = session.getUpgradeRequest();
        //String path = (req instanceof ServletUpgradeRequest) ? ((ServletUpgradeRequest) req).getRequestPath() : "";
        
        log.fine(String.format("MyEchoSocket onConnect - %s", session.getRemoteAddress()));
        this.session = session;
        this.remote = session.getRemote();
        
        try
        {
            protAdapt.connect(new JettyWebSocketAdapter());
        }
        catch (Exception ex)
        {
            log.severe(String.format("Failed to create RFBServer", ex));
            session.close(StatusCode.ABNORMAL, "VNC Server creation error - " + ex.getMessage());
        }
    }

    public void onWebSocketClose(int statusCode, String reason)
    {
        log.fine(String.format("MyEchoSocket onClose(%d, %s) - %s", statusCode, reason, session.getRemoteAddress())); 
        
        try
        {
            if (protAdapt != null)
            {
                protAdapt.shutdown();
            }
        }
        catch (IOException ex)
        {
            log.warning(String.format("Error closing protocol stream",ex));
        }
        finally
        {
            this.protAdapt = null;
            this.remote = null;
            this.session = null;
        }
    }
    
    public void onWebSocketBinary(byte[] payload, int offset, int len) 
    {
        if (session == null)
        {
            // no connection, do nothing. Apparently possible due to async behavior
            return;
        }
        
        try
        {
            protAdapt.clientDataIn(payload, offset, len);
        }
        catch (IOException ex)
        {
            log.severe(String.format("Error sending to protocol handler", ex));
            session.close(StatusCode.ABNORMAL, "VNC Server i/o error - " + ex.getMessage());
        }
    }    

    public void onWebSocketText(String message)
    {
        if (session == null)
        {
            // no connection, do nothing. Apparently possible due to async behavior
            return;
        }

        // Not really a part of VNC protocol, track with warnings in case occurs often
        log.warning(String.format("MyEchoSocket onText(%s) - %s", message, session.getRemoteAddress()));            
    }

    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PRIVATE INSTANCE METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // STATIC INNER CLASSES
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // NON-STATIC INNER CLASSES
    //////////////////////////////////////////////////

    public class JettyWebSocketAdapter
        implements WebSocketProvider
    {

        @Override
        public void send(byte[] byt) throws Exception
        {
            getRemote().sendBytes(ByteBuffer.wrap(byt));
        }

        @Override
        public void send(String txt) throws Exception
        {
            getRemote().sendString(txt);
        }

        @Override
        public void close()
        {
            getSession().close();
        }        
    }
}
