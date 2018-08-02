/*
 * Copyright (c) 2018 Ascert, LLC.
 * www.ascert.com
 *
 * Based on original code from vncj (https://github.com/tliron/vncj)
 * Rights for for derivations from original works remain:
 *      Copyright (C) 2000-2002 by Tal Liron
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

import gnu.rfb.*;
import gnu.logging.*;
import gnu.rfb.server.RFBAuthenticator;
import gnu.rfb.server.RFBClient;
import gnu.rfb.server.RFBServer;

import java.io.*;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class RFBProtocolHandler implements RFBClient, Runnable
{

    ///////////////////////////////////////////////////////////////////////////////////////
    // Private
    private RFBAuthenticator authenticator;
    private RFBServer server = null;
    private DataInputStream input;
    private DataOutputStream output;

    private PixelFormat pixelFormat = null;
    private String protocolVersionMsg = "";
    private boolean shared = true;
    private int[] encodings = new int[0];
    private int preferredEncoding = rfb.EncodingHextile;
    private boolean isRunning = false;
    private boolean threadFinished = false;
    private Vector updateQueue = new Vector();
    private boolean keepAliveSupported = false;

    private int keepAliveInterval = 20 * 1000;    // default 20s to check connection is alive
    private long lastServerMsgTs = 0;

    //TODO - def need to review the number of Qs, syncs, and threads we have per client and see if they can be rationalised/optimised
    ScheduledThreadPoolExecutor updateHandler = new ScheduledThreadPoolExecutor(1);

    /**
     * new constructor by Marcus Wolschon
     */
    public RFBProtocolHandler(InputStream is, OutputStream os, RFBServer server, RFBAuthenticator authenticator) throws IOException
    {
        this.server = server;

        this.authenticator = authenticator;
        // Streams
        input = new DataInputStream(is);
        output = new DataOutputStream(os);

        // Start socket listener thread        
        new Thread(this).start();
    }

    //
    // RFBClient
    //
    // Attributes
    public synchronized PixelFormat getPixelFormat()
    {
        return pixelFormat;
    }

    public synchronized String getProtocolVersionMsg()
    {
        return protocolVersionMsg;
    }

    public synchronized boolean getShared()
    {
        return shared;
    }

    public synchronized int getPreferredEncoding()
    {
        return preferredEncoding;
    }

    public synchronized void setPreferredEncoding(int encoding)
    {
        if (encodings.length > 0)
        {
            for (int i = 0; i < encodings.length; i++)
            {
                if (encoding == encodings[i])
                {
                    // Encoding is supported
                    preferredEncoding = encoding;
                    return;
                }
            }
        }
        else
        {
            // No list
            preferredEncoding = encoding;
        }
    }

    public synchronized int[] getEncodings()
    {
        return encodings;
    }

    // Messages from server to client
    public synchronized void writeFrameBufferUpdate(Rect rects[]) throws IOException
    {

        int prev = output.size();
        writeServerMessageType(rfb.FrameBufferUpdate);
        output.writeByte(0); // padding

        // Count rects
        int count = 0;
        int i;
        for (i = 0; i < rects.length; i++)
        {
            count += rects[i].count;
        }
        output.writeShort(count);

        for (i = 0; i < rects.length; i++)
        {
            rects[i].writeData(output);
        }

        int tot = output.size();
        System.out.println(">>> update bytes written total: " + tot + ", new: " + (tot - prev));
        output.flush();
    }

    public synchronized void writeSetColourMapEntries(int firstColour, Colour colours[]) throws IOException
    {
        writeServerMessageType(rfb.SetColourMapEntries);
        output.writeByte(0); // padding
        output.writeShort(firstColour);
        output.writeShort(colours.length);
        for (int i = 0; i < colours.length; i++)
        {
            output.writeShort(colours[i].r);
            output.writeShort(colours[i].g);
            output.writeShort(colours[i].b);
        }
        output.flush();
    }

    public synchronized void writeBell() throws IOException
    {
        writeServerMessageType(rfb.Bell);
    }

    public synchronized void writeServerCutText(String text) throws IOException
    {
        writeServerMessageType(rfb.ServerCutText);
        output.writeByte(0);  // padding
        output.writeShort(0); // padding
        output.writeInt(text.length());
        output.writeBytes(text);
        output.writeByte(0);
        output.flush();
    }

    // Warning - most clients don't seem to support Keep Alive messages
    public synchronized void writeKeepAlive() throws IOException
    {
        if (keepAliveSupported && (System.currentTimeMillis() - this.lastServerMsgTs) > this.keepAliveInterval)
        {
            writeServerMessageType(rfb.KeepAlive);
        }
    }

    // Operations
    public synchronized void close()
    {
        isRunning = false;
        // Block until the thread has exited gracefully
        while (threadFinished == false)
        {
            try
            {
                Thread.currentThread().sleep(20);
            }
            catch (InterruptedException x)
            {
            }
        }
        try
        {
            output.close();
            input.close();
        }
        catch (IOException e)
        {
            //VLogger.getLogger().log("Got and exception shutting down RFBProtocolHandler ",e);
        }
        finally
        {
            output = null;
            input = null;
        }
    }

    //
    // Runnable
    //
    public void run()
    {
        isRunning = true;
        try
        {
            //                 System.err.println("DEBUG[RFBProtocolHandler] run() calling writeProtocolVersionMsg()");
            // Handshaking
            writeProtocolVersionMsg();
            //                 System.err.println("DEBUG[RFBProtocolHandler] run() calling readProtocolVersionMsg()");
            readProtocolVersionMsg();
            //                 System.err.println("DEBUG[RFBProtocolHandler] run() calling writeAuthScheme()");
            //if(((DefaultRFBAuthenticator)authenticator).authenticate(input,output)==false){
            if (authenticator.authenticate(input, output, this) == false)
            {
                System.out.println("Authentiation failed");
                return;
            }
            //                 System.err.println("DEBUG[RFBProtocolHandler] run() calling readClientInit()");
            readClientInit();
            //                 System.err.println("DEBUG[RFBProtocolHandler] run() calling initServer()");
            initServer();
            //                 System.err.println("DEBUG[RFBProtocolHandler] run() calling writeServerInit()");
            writeServerInit();
            //                 System.err.println("DEBUG[RFBProtocolHandler] run() message loop");

            // RFBClient read message loop
            while (isRunning)
            {
                switch (input.readUnsignedByte())
                {
                    case rfb.SetPixelFormat:
                        readSetPixelFormat();
                        break;
                    case rfb.FixColourMapEntries:
                        readFixColourMapEntries();
                        break;
                    case rfb.SetEncodings:
                        readSetEncodings();
                        break;
                    case rfb.FrameBufferUpdateRequest:
                        readFrameBufferUpdateRequest();
                        break;
                    case rfb.KeyEvent:
                        readKeyEvent();
                        break;
                    case rfb.PointerEvent:
                        readPointerEvent();
                        break;
                    case rfb.ClientCutText:
                        readClientCutText();
                        break;
                }
            }
        }
        catch (EOFException eof)
        {
            // This is a normal disconnect
            System.out.println("Client disconnected.");
        }
        catch (IOException x)
        {
            System.out.println("IOException on read, drop the client");
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }

        if (server != null)
        {
            server.removeClient(this);
        }

        threadFinished = true;
        close();
    }

    //TODO - the concept of 'shared' needs some work here. Original GNU abstraction and code
    //       was not really that helpful or correct
    private void initServer() throws IOException
    {
        // We may already have a shared server
        //TODO - check server allows sharing
        if (shared)
        {
            //TODO - not sure this does anything, if it was a shareable server this would be same value as initially
            //       supplied anyway
            //server = host.getSharedServer();
        }
        else
        {
            //TODO - according to spec, we should disconnect other clients here
        }

        server.addClient(this);
        server.setClientProtocolVersionMsg(this, protocolVersionMsg);
    }

    // Handshaking
    private synchronized void writeProtocolVersionMsg() throws IOException
    {
        output.writeBytes(rfb.ProtocolVersionMsg);
        output.flush();
    }

    private synchronized void readProtocolVersionMsg() throws IOException
    {
        byte[] b = new byte[12];
        input.readFully(b);
        protocolVersionMsg = new String(b);
    }

    private synchronized void readClientInit() throws IOException
    {
        shared = input.readUnsignedByte() == 1;
        VLogger.getLogger().log("Client option - shared: " + shared);

    }

    private synchronized void writeServerInit() throws IOException
    {
        //         System.err.println("DEBUG[RFBProtocolHandler] writeServerInit() writing FB-dimension");
        output.writeShort(server.getFrameBufferWidth(this));
        output.writeShort(server.getFrameBufferHeight(this));
        //         System.err.println("DEBUG[RFBProtocolHandler] writeServerInit() writing pixel-format");
        server.getPreferredPixelFormat(this).writeData(output);
        //         System.err.println("DEBUG[RFBProtocolHandler] writeServerInit() writing padding");
        output.writeByte(0); // padding
        output.writeByte(0); // padding
        output.writeByte(0); // padding
        //         System.err.println("DEBUG[RFBProtocolHandler] writeServerInit() writing desktopname");
        String desktopName = server.getDesktopName(this);
        output.writeInt(desktopName.length());
        output.writeBytes(desktopName);
        output.flush();
    }

    // Authentication
    private synchronized void writeAuthScheme() throws IOException
    {
        output.writeInt(authenticator.getAuthScheme(this));
        output.flush();
    }

    // Messages from server to client
    private synchronized void writeServerMessageType(int type) throws IOException
    {
        this.lastServerMsgTs = System.currentTimeMillis();
        output.writeByte(type);
    }

    // Messages from client to server
    private synchronized void readSetPixelFormat() throws IOException
    {
        input.readUnsignedByte();  // padding
        input.readUnsignedShort(); // padding
        pixelFormat = new PixelFormat(input);
        input.readUnsignedByte();  // padding
        input.readUnsignedShort(); // padding

        // Delegate to server
        server.setPixelFormat(this, pixelFormat);
    }

    private synchronized void readFixColourMapEntries() throws IOException
    {
        input.readUnsignedByte(); // padding
        int firstColour = input.readUnsignedShort();
        int nColours = input.readUnsignedShort();
        Colour colourMap[] = new Colour[nColours];
        for (int i = 0; i < nColours; i++)
        {
            colourMap[i].readData(input);
        }

        // Delegate to server
        server.fixColourMapEntries(this, firstColour, colourMap);
    }

    private synchronized void readSetEncodings() throws IOException
    {
        input.readUnsignedByte(); // padding
        int nEncodings = input.readUnsignedShort();
        encodings = new int[nEncodings];
        for (int i = 0; i < nEncodings; i++)
        {
            encodings[i] = input.readInt();
        }

        preferredEncoding = Rect.bestEncoding(encodings);
        System.out.println("preferredEncoding: " + preferredEncoding);
        // Delegate to server
        server.setEncodings(this, encodings);
    }

    private synchronized void readFrameBufferUpdateRequest() throws IOException
    {
        boolean incremental = (input.readUnsignedByte() == 1);
        int x = input.readUnsignedShort();
        int y = input.readUnsignedShort();
        int w = input.readUnsignedShort();
        int h = input.readUnsignedShort();
        UpdateRequest r = new UpdateRequest(incremental, x, y, w, h);
        synchronized (updateQueue)
        {
            int index = updateQueue.indexOf(r);
            if (index >= 0)
            {
                // replace only if update is non incremental
                if (r.incremental == false)
                {
                    updateQueue.setElementAt(r, index);
                }
            }
            else
            {
                updateQueue.add(r);
            }
        }
    }

    private void doFrameBufferUpdate() throws IOException
    {
        synchronized (updateQueue)
        {
            Iterator iter = updateQueue.iterator();
            while (iter.hasNext())
            {
                UpdateRequest ur = (UpdateRequest) iter.next();
                iter.remove();
                // Delegate to server
                try
                {
                    System.out.println("RFBSocket is doing an update");
                    server.frameBufferUpdateRequest(this, ur.incremental, ur.x, ur.y, ur.w, ur.h);
                    System.out.println("RFBSocket is done");
                }
                catch (IOException e)  // some times we have w==h==0 and it would result in a blue screen on the official VNC client.
                {
                    System.out.println(" !!!! RFBSocket exception: " + e);
                    // if there is nothing to encode, encode the top left pixel instead
                    if (e.getMessage().startsWith("rects.length == 0"))
                    {
                        server.frameBufferUpdateRequest(this, false, 0, 0, 1, 1);
                    }
                    else
                    {
                        // rethrow it
                        throw e;
                    }
                }
            }
        }
    }

    private synchronized void readKeyEvent() throws IOException
    {
        boolean down = (input.readUnsignedByte() == 1);
        input.readUnsignedShort(); // padding
        int key = input.readInt();

        // Delegate to server
        server.keyEvent(this, down, key);
    }

    private synchronized void readPointerEvent() throws IOException
    {
        int buttonMask = input.readUnsignedByte();
        int x = input.readUnsignedShort();
        int y = input.readUnsignedShort();

        // Delegate to server
        server.pointerEvent(this, buttonMask, x, y);
    }

    private synchronized void readClientCutText() throws IOException
    {
        input.readUnsignedByte();  // padding
        input.readUnsignedShort(); // padding
        int length = input.readInt();
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        String text = new String(bytes);

        // Delegate to server
        server.clientCutText(this, text);
    }
    
//TODO - possible actually we might want to be able to get some external client address identifier
//       this class is no longer socket specific though, to it'd need remodelling
//    public InetAddress getInetAddress(){
//        return(socket.getInetAddress());
//    }

    public void updateAvailable()
    {
        updateHandler.execute(new Runnable()
        {
            public void run()
            {
                try
                {
                    doFrameBufferUpdate();
                }
                catch (IOException ex)
                {
                    // In fact we're probably doubling these, since read thread will most likely also show them
                    System.out.println("IOException on socket write, drop the client");
                    close();
                }
            }
        });
    }

    private class UpdateRequest
    {
        boolean incremental;
        int x;
        int y;
        int w;
        int h;

        public UpdateRequest(boolean incremental, int x, int y, int w, int h)
        {
            this.incremental = incremental;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public boolean equals(Object obj)
        {
            UpdateRequest u2 = (UpdateRequest) obj;
            return (x == u2.x && y == u2.y && w == u2.w && h == u2.h);
        }

    }

}
