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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import gnu.rfb.server.RFBAuthenticator;
import gnu.rfb.server.RFBServer;

/**
 *
 * @version 1,0 30-Aug-2018
 * @author srm

 *      30-Aug-2018    srm        Created
 */
public class RFBProtocolAdapter extends OutputStream
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////
    
    private static final Logger log = Logger.getLogger(RFBProtocolAdapter.class.getName());

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////

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

    private WebSocketProvider wsProv;
    private RFBServer server;
    private RFBAuthenticator auth;
    private RFBProtocolHandler handler;
    private ByteArrayOutputStream toWebSocket = new ByteArrayOutputStream(2048);
    // Possibly overkill and thread heavy, but using streams disconnects thread handling from web server
    // and also allows consistent/common model for handling with RFBSocketHost. Note that the
    // Data in from client
    private PipedOutputStream fromWebSocket = new PipedOutputStream();
    private PipedInputStream toHandler = new PipedInputStream();

    public RFBProtocolAdapter(RFBServer server, RFBAuthenticator auth)
    {
        this.server = server;
        this.auth = auth;
    }

    public void connect(WebSocketProvider wsAdapt) throws Exception
    {
        this.wsProv = wsAdapt;
        toHandler.connect(fromWebSocket);
        handler = new RFBProtocolHandler(toHandler, this, server, auth);
    }

    public void shutdown() throws IOException
    {
        //TODO - close stream. Need to check this is all that is needed and the protocol handler will detect
        //       the close and disconect/shutdown the client
        fromWebSocket.close();
        toHandler.close();
        toWebSocket.close();
    }

    public void clientDataIn(byte[] array) throws IOException
    {
        clientDataIn(array, 0, array.length);
    }

    public void clientDataIn(byte[] array, int offset, int len)  throws IOException
    {
        fromWebSocket.write(array, offset, len);
        fromWebSocket.flush();
    }
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS - OutputStream
    //////////////////////////////////////////////////
    
    public synchronized void write(int b) throws IOException
    {
        toWebSocket.write(b);
    }

    public synchronized void write(byte[] b) throws IOException
    {
        toWebSocket.write(b);
    }

    public synchronized void write(byte[] b, int off, int len) throws IOException
    {
        toWebSocket.write(b, off, len);
    }

    public synchronized void flush()
    {
        try
        {
            wsProv.send(toWebSocket.toByteArray());
            toWebSocket.reset();
        }
        catch (Exception ex)
        {
            // seems to happen around close - diag included if ever needed
            log.log(Level.FINEST, "protocol stream flush", ex);
        }
    }

    public void close()
    {
        wsProv.close();
    }
    
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////

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

}

//////////////////////////////////////////////////
// NON-STATIC INNER CLASSES
//////////////////////////////////////////////////
