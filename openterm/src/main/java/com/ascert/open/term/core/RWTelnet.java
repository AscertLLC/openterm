/*
 * Copyright (c) 2016, 2017 Ascert, LLC.
 * www.ascert.com
 *
 * Based on original code from FreeHost3270, copyright for derivations from original works remain:
 *  Copyright (C) 1998, 2001  Art Gillespie
 *  Copyright (2) 2005 the http://FreeHost3270.Sourceforge.net
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
package com.ascert.open.term.core;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * TODO: - look into TN3270E protocol, SSCP LU device name, options etc https://tools.ietf.org/html/rfc1647
 *
 *
 * @since 0.1
 */
public class RWTelnet implements Runnable
{

    private static final Logger log = Logger.getLogger(RWTelnet.class.getName());

    /*          TELNET protocol constants       */
 /*          found on page 14 of RFC 845     */
    public static final short SE = 240;             //End of subnegotiation parameters
    public static final short NOP = 241;            //No Operation
    public static final short DATA_MARK = 242;            //data mark Operation
    public static final short BREAK = 243;            //break Operation
    public static final short SB = 250;             //Begin subnegotiation
    public static final short WILL = 251;           //Will perform an indicated option
    public static final short WONT = 252;           //Won't perform an indicated option
    public static final short DO = 253;             //Please perform an indicated option
    public static final short DONT = 254;           //Please don't perform an indicated option
    public static final short IAC = 255;            //The following bytes are a telnet command
    public static final short EOR = 239;            //End of record

    /*             TELNET OPTIONS                */
    public static final short OPT_BINARY = 0;           //Use 8-bit data path
    public static final short OPT_ECHO = 1;             //Echo
    public static final short OPT_SUPPRESS_GA = 3;
    public static final short TIMING_MARK = 6;
    public static final short OPT_TERMINAL_TYPE = 24;   // 
    public static final short OPT_EOR = 25;         //End of Record
    public static final short OPT_WINDOW = 31;          //Window size
    public static final short OPT_LINE_MODE = 34;

    public static final short OPTION_IS = 0;        //option is
    public static final short OPTION_SEND = 1;      //send option

    /*             TELNET STATES (internal)      */
    public static final short TN_DEFAULT = 0;       //default incoming data
    public static final short TN_IAC = 1;           //next is command
    public static final short TN_CMD = 2;           //incoming command
    public static final short TN_SUB_CMD = 3;       //incoming sub-command

    /*  CUSTOM CODES                                                                        */
    /**
     * Custom byte for flagging the next bytes as broadcast message.
     */
    public static final short BROADCAST = 245;

    public static byte[] shortArrayToByte(short[] buff, int offset, int len)
    {
        return shortArrayToByte(buff, offset, len, null);
    }

    public static byte[] shortArrayToByte(short[] buff, int offset, int len, byte[] dest)
    {
        if (dest == null)
        {
            dest = new byte[len];
        }

        for (int i = 0; i < len; i++)
        {
            // Cast our results from the byte array to unsigned shorts so none are negative
            dest[i] = (byte) buff[i + offset];
        }

        return dest;
    }

    public static short[] byteArrayToShort(byte[] buff)
    {
        return byteArrayToShort(buff, buff.length);
    }

    public static short[] byteArrayToShort(byte[] buff, int len)
    {
        short[] sBuf = new short[len];

        for (int i = 0; i < len; i++)
        {
            // Cast our results from the byte array to unsigned shorts so none are negative
            sBuf[i] = (short) Byte.toUnsignedInt(buff[i]);
        }

        return sBuf;
    }

    private InputStream is;
    private OutputStream os;
    protected TnStreamParser tnParser;
    private Socket tnSocket;
    private SSLSocket tnSocketSSL;
    private Thread sessionThread;
    protected short[] bufferTerm; //put the 3270 bytes in here
    protected int bufferTermLen;
    private boolean[] doHistory;
    private short[] inBuf; //put raw data from the inputstream here
    private byte[] key;
    private short[] subOptionBuffer;
    private boolean[] willHistory;
    private boolean encryption;
    private int connectionTimeout;
    private int inBufLen;
    private int keyCounter;
    private int subOptionBufferLen;
    private int tnState;
    private short tnCommand;

    // Optional Tn commands - respond positively to a DO or WILL 
    private Set<Short> optTnCmds = new HashSet<>();
    // Required Tn commands - an error if we get a DONT or WONT
    private Set<Short> reqdTnCmds = new HashSet<>();
    // Currently negotiated commands - used to prevent loops
    private Set<Short> negTnCmds = new HashSet<>();
    // Determines whether optional and required commands are sent initially
    private final boolean sendInitialCmds;
    // Line mode options - if not null, will be used at startup
    protected byte[] lineModeOpts = null;

    protected TraceHandler traceHandler = null;

    /**
     * DOCUMENT ME!
     *
     * @param rw          the Parser for the incoming data stream.
     * @param tn3270Model the tn3270 model number corresponding to this session.
     */
    public RWTelnet(TnStreamParser tnParser)
    {
        this(tnParser, new Short[]
         {
             OPT_TERMINAL_TYPE
        }, new Short[]
         {
             OPT_BINARY, OPT_EOR
        }, false);
    }

    public RWTelnet(TnStreamParser tnParser, Short[] reqdCmds, Short[] optCmds, boolean sendInitialCmds)
    {
        this.tnParser = tnParser;
        this.sendInitialCmds = sendInitialCmds;

        bufferTerm = new short[50000];
        bufferTermLen = 0;
        subOptionBuffer = new short[50];
        subOptionBufferLen = 0;
        tnState = TN_DEFAULT;
        keyCounter = 0;
        connectionTimeout = 60;
        doHistory = new boolean[3];
        willHistory = new boolean[3];
        // Temp for now - allow caller to supply
        reqdTnCmds.addAll(Arrays.asList(reqdCmds));
        optTnCmds.addAll(Arrays.asList(optCmds));
        // A Telnet client should never negotiate or refuse echo - so we add it all here in 
        // case, and make sure not set as required
        optTnCmds.add(OPT_ECHO);
        reqdTnCmds.remove(OPT_ECHO);
    }

    /**
     * The 'thread code' for this class. Consumers need to invoke this method to begin the communications process. It will run indefinitely
     * until the socket read returns -1 (Host disconnected) or disconnect is called. Usage <code> Thread t = new Thread(<i>RWTelnet
     * instance</i>);<BR> t.run;</CODE> Any problems encountered (IOException, Host Disconnect) will be transmitted back to the consumer via
     * the TnAction interface.
     */
    public void run()
    {
        int n = 0;

        log.finer("started the telnet thread");

        try
        {
            sendInitialCommands();

            while (true)
            {
                if ((inBufLen = readSocket()) == -1)
                {
                    log.finer("telnet socket is empty, disconnecting");
                    tnParser.status(TnAction.DISCONNECTED_BY_REMOTE_HOST);
                    break;
                }

                synchronized (this)
                {
                    parseData();
                }
            }

            //rw.connectionStatus(TnAction.DISCONNECTED_BY_REMOTE_HOST);
        }
        catch (IOException e)
        {
            log.severe("failure in telnet thread loop: " + e.getMessage());
            tnParser.status(TnAction.DISCONNECTED_BY_REMOTE_HOST);
        }
        finally
        {
            disconnect();
        }
    }

    /**
     * Performs a direct connection to a host terminal server.
     *
     * @param host destination server host name.
     * @param port destination terminal server port number.
     *
     * @throws UnknownHostException DOCUMENT ME!
     * @throws IOException          DOCUMENT ME!
     */
    protected void connect(String host, int port)
        throws UnknownHostException, IOException
    {
        log.fine("connecting to " + host + ":" + port);
        if (encryption)
        {

            log.fine("encrypted connection");
            SSLSocketFactory sslFact = (SSLSocketFactory) SSLSocketFactory.getDefault();
            tnSocketSSL = (SSLSocket) sslFact.createSocket();
            tnSocketSSL.connect(new InetSocketAddress(host, port), connectionTimeout * 1000);
            connect(tnSocketSSL.getInputStream(), tnSocketSSL.getOutputStream());
        }
        else
        {
            tnSocket = new Socket();
            tnSocket.connect(new InetSocketAddress(host, port), connectionTimeout * 1000);
            connect(tnSocket.getInputStream(), tnSocket.getOutputStream());
        }
    }

    protected void connect(InputStream is, OutputStream os)
    {
        this.is = is;
        this.os = os;
        sessionThread = new Thread(this);
        sessionThread.start();
    }
    
    /**
     * Disconnects the current session.
     */
    protected void disconnect()
    {
        if (sessionThread == null || tnSocket == null && tnSocketSSL == null)
        {
            log.info("socket is null, not connected");
            return;
        }

        try
        {
            sessionThread.interrupt();
            silentClose(is);
            silentClose(os);
            silentClose(encryption ? tnSocketSSL : tnSocket);
            willHistory = new boolean[3];
            doHistory = new boolean[3];

            log.fine("disconnected");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            log.severe(e.getMessage());
        }
        finally
        {
            is = null;
            os = null;
            tnSocketSSL = null;
            tnSocket = null;
        }
    }

    public boolean isConnected()
    {

        if (tnSocket != null)
        {
            return tnSocket.isConnected();
        }
        else if (tnSocketSSL != null)
        {
            return tnSocketSSL.isConnected();
        }
        else if (is != null && os != null)
        {
            // we have streams, so must be a direct connection
            return true;
        }

        return false;
    }

    protected void silentClose(Closeable cls)
    {
        if (cls != null)
        {
            try
            {
                cls.close();
            }
            catch (Exception e)
            {
                log.finest("Closeable exception: " + e);
            }
        }

    }

    /**
     * Processes broadcast message. Is called when a broadcast message is received.
     *
     * @param netBuf DOCUMENT ME!
     */
    protected void receiveMessage(short[] netBuf)
    {
        log.fine("received broadcast message");

        char[] msg = new char[netBuf.length];

        for (int i = 2; i < netBuf.length; i++)
        {
            msg[i - 2] = (char) netBuf[i];
        }

        tnParser.broadcastMessage(new String(msg).trim());
    }

    public boolean using(short opt)
    {
        return negTnCmds.contains(opt);
    }

    /**
     * This method provides outbound communication to the Telnet host.
     *
     * @param out    an array of shorts, representing the data to be sent to the host.
     * @param outLen the number of valid bytes in the out array
     *
     * @throws IOException DOCUMENT ME!
     */
    public void sendData(short[] out, int outLen) throws IOException
    {
        sendData(shortArrayToByte(out, 0, outLen, new byte[outLen + 2]), outLen);
    }

    public void sendData(byte[] out) throws IOException
    {
        sendData(out, out.length);
    }

    public void sendData(byte[] out, int outLen) throws IOException
    {

        if (os == null)
        {
            log.warning("attempt to send when telnet not connected, discarding");
            return;
        }

        if (using(OPT_EOR))
        {
            //add the is a command telnet command
            out[outLen++] = (byte) IAC;
            //add the end of record telnet command
            out[outLen++] = (byte) EOR;
        }

        send(out, 0, outLen);

        //System.out.println("Sent " + tmpByteBuf.length + " bytes");
        //for(int i = 0; i < tmpByteBuf.length; i++)
        //System.out.print(Integer.toHexString(tmpByteBuf[i]) + " ");
    }

    public void send(byte[] out, int off, int outLen) throws IOException
    {
        //write the data out to the EncryptedOutputStream
        os.write(out, off, outLen);
        os.flush();

        if (traceHandler != null)
        {
            // possible we'd want this waited/queued to some async handler thread
            traceHandler.outgoingData(out, off, outLen);
        }
    }

    public void sendBreak() throws IOException
    {
        sendData(new byte[]
        {
            (byte) IAC, (byte) BREAK
        });
    }

    /**
     * Turns the encryption on and off.
     *
     * @param encryption True = on False = off
     */
    protected void setEncryption(boolean encryption)
    {
        this.encryption = encryption;
    }

    /**
     * Sets the connection timeout for the connect(String,int) method.
     *
     * @param timeout integer value in seconds
     */
    protected void setConnectionTimeout(int timeout)
    {
        this.connectionTimeout = timeout;
    }

    public synchronized void setSessionData(String key, String value)
    {
        log.fine("SessionData Key: " + key + " Value: " + value);

        byte[] keyByte = charToByte(key.toCharArray());
        byte[] valueByte = charToByte(value.toCharArray());
        byte[] outData = new byte[keyByte.length + valueByte.length + 4];
        outData[0] = (byte) 0xCC;
        outData[1] = (byte) 0xCC;
        System.arraycopy(keyByte, 0, outData, 2, keyByte.length);
        outData[keyByte.length + 2] = (byte) 0xCC;
        System.arraycopy(valueByte, 0, outData, keyByte.length + 3,
                         valueByte.length);
        outData[keyByte.length + valueByte.length + 3] = (byte) 0xCC;

        try
        {
            os.write(outData, 0, outData.length);
            log.fine("SessionData sent to server");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private byte[] charToByte(char[] c)
    {
        byte[] ret = new byte[c.length];

        for (int i = 0; i < c.length; i++)
        {
            ret[i] = (byte) c[i];
        }

        return ret;
    }

    /**
     * This method sends TELNET specific commands to the telnet host. Primarily this would be used for protocol feature negotiation.
     *
     * @param tnCmd    DOCUMENT ME!
     * @param tnOption DOCUMENT ME!
     */
    private void sendCommand(short tnCmd, short tnOption)
        throws IOException
    {
        log.finer(String.format("sending command: %s - %s", decodeCmd(tnCmd), decodeOpt(tnOption)));

        byte[] tmpBuffer = new byte[3];
        tmpBuffer[0] = (byte) IAC;
        tmpBuffer[1] = (byte) tnCmd;
        tmpBuffer[2] = (byte) tnOption;

        send(tmpBuffer, 0, 3);
    }

    /**
     * This is a special instance of sendCommand where the client specifies that it is a 3270 client.
     *
     * @throws IOException DOCUMENT ME!
     */
    public void sendTerminalType() throws IOException
    {
        String type = tnParser.getTermType();
        log.fine("sending terminal type: " + type);
        byte[] bTyp = (type != null) ? type.getBytes() : "NO-TYPE".getBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(new byte[]
        {
            (byte) IAC, (byte) SB
        });
        baos.write(new byte[]
        {
            (byte) OPT_TERMINAL_TYPE, (byte) OPTION_IS
        });
        baos.write(bTyp);
        baos.write(new byte[]
        {
            (byte) IAC, (byte) SE
        });

        if (getLineModeOpts() != null)
        {
            log.finer("sending TN SUB OPTS : " + decodeSubOpts(byteArrayToShort(getLineModeOpts()), getLineModeOpts().length));

            // strictly, line mode opts should be sent afte a DO LINE_MODE
            // for current usaes, sending after we've sent terminal type is workable
            baos.write(new byte[]
            {
                (byte) IAC, (byte) SB
            });
            baos.write(this.getLineModeOpts());
            baos.write(new byte[]
            {
                (byte) IAC, (byte) SE
            });
        }

        send(baos.toByteArray(), 0, baos.size());
    }

    // Override if needed
    protected void doIsTerminalType(String termType)
    {
    }

    //Probably should use lookup maps or Enums - quick hack for now or better tracing
    private String decodeOpt(short opt)
    {
        switch (opt)
        {
            case OPT_BINARY:
                return "BINARY";
            case OPT_EOR:
                return "OPT_EOR";
            case OPT_TERMINAL_TYPE:
                return "TERMINAL_TYPE";
            case OPT_ECHO:
                return "ECHO";
            case OPT_WINDOW:
                return "WINDOW";
            case OPT_LINE_MODE:
                return "LINE_MODE";
            case OPT_SUPPRESS_GA:
                return "SUPPRESS_GA";
            default:
                return String.format("0x%x", opt);
        }
    }

    private String decodeCmd(short tnCmd)
    {
        switch (tnCmd)
        {
            case WILL:
                return "WILL";
            case DO:
                return "DO";
            case WONT:
                return "WONT";
            case DONT:
                return "DONT";
            default:
                return String.format("0x%x", tnCmd);
        }
    }

    public String decodeSubOpts(short[] subOptBuf, int len)
    {
        StringBuffer dBuf = new StringBuffer();
        dBuf.append(decodeOpt(subOptBuf[0]));
        dBuf.append(" - ");
        for (int ix = 1; ix < len; ix++)
        {
            dBuf.append(String.format("0x%x ", subOptBuf[ix]));
        }

        return dBuf.toString();
    }

    /**
     * This method handles incoming TELNET-specific commands. specifically, WILL, WONT, DO, DONT.
     *
     * @param tnCmd    the incoming telnet command
     * @param tnOption the option for which the command is being sent
     *
     * @throws IOException DOCUMENT ME!
     */
    private void handleTnCommand(short tnCmd, short tnOption)
        throws IOException
    {
        log.finer(String.format("received telnet command: %s - %s", decodeCmd(tnCmd), decodeOpt(tnOption)));

        short cmd;

        switch (tnCmd)
        {
            case WILL:
            case DO:
                if (optTnCmds.contains(tnOption) || reqdTnCmds.contains(tnOption))
                {
                    cmd = (tnCmd == WILL) ? DO : WILL;
                    if (tnCmd == DO && !negTnCmds.add(tnOption))
                    {
                        // Prevent endless loop negotiation as per RFC854
                        log.finer("tn option already present: " + tnOption);
                    }
                    else
                    {
                        log.finer("tn option not already present: " + tnOption);
                        sendCommand(cmd, tnOption);
                    }
                }
                else
                {
                    cmd = (tnCmd == WILL) ? DONT : WONT;
                    negTnCmds.remove(tnOption);
                    sendCommand(cmd, tnOption);
                }
                break;

            case WONT:
            case DONT:
                if (reqdTnCmds.contains(tnOption))
                {
                    // Nasty, but what else can we do??
                    throw new IOException("Telnet WONT/DONT received for required option: " + decodeOpt(tnOption));
                }
                else
                {
                    cmd = (tnCmd == WONT) ? DONT : WONT;
                    // Record option, and also prevent endless loop negotiation as per RFC854
                    if (tnCmd == DONT && !negTnCmds.remove(tnOption))
                    {
                        log.finer("tn option was not present: " + tnOption);
                    }
                    else
                    {
                        log.finer("tn option was present: " + tnOption);
                        sendCommand(cmd, tnOption);
                    }
                    sendCommand(cmd, tnOption);
                }
        }

    }

    protected void processTermBuffer() throws IOException
    {
        tnParser.parse(bufferTerm, bufferTermLen);
        bufferTermLen = 0;
    }

    private void handleUnframedData() throws IOException
    {
        if (!using(OPT_EOR) && bufferTermLen > 0)
        {
            processTermBuffer();
        }
    }

    /**
     * Checks the input stream for commands and routes the stream appropriately. Standard data is stored in the <code>bufferTerm</code>
     * array and passed to the <code>RWTelnetAction</code> interface's <code>refresh(buf, int)</code> method when an EOR (End-of-record)
     * byte is detected. Other telnet commands (WILL WONT DO DONT IAC) are handled in accordance to RFC 845
     *
     * @throws IOException DOCUMENT ME!
     */
    private void parseData() throws IOException
    {
        short curr_byte;
        log.finer("parsing data");

        //this if clause traps the inputStream if it is a broadcast message
        if ((inBuf[0] == IAC) && (inBuf[1] == BROADCAST))
        {
            receiveMessage(inBuf);
            inBufLen = 0;
            return;
        }

        for (int i = 0; i < inBufLen; i++)
        {
            curr_byte = inBuf[i];

            switch (tnState)
            {
                case TN_DEFAULT:

                    if (curr_byte == IAC)
                    {
                        tnState = TN_IAC;
                    }
                    else
                    {
                        try
                        {
                            bufferTerm[bufferTermLen++] = curr_byte;
                        }
                        catch (ArrayIndexOutOfBoundsException ee)
                        {
                            log.fine("telnet buffer size: " + bufferTerm.length + " len: " + bufferTermLen);
                            return;
                        }
                    }
                    break;

                case TN_IAC:

                    switch (curr_byte)
                    {
                        case IAC:
                            //Two IACs in a row means this is really a single occurrence
                            //of byte 255 (0xFF).  (255 is its own escape character)
                            bufferTerm[bufferTermLen++] = curr_byte;
                            //Since it wasn't really an IAC, reset the tnState to default
                            tnState = TN_DEFAULT;
                            break;

                        case EOR:
                            //Done with this data record, send to Implementation via TnAction interface
                            log.finer("TN EOR data len: " + bufferTermLen);
                            processTermBuffer();
                            tnState = TN_DEFAULT;
                            break;

                        case WILL:
                        case WONT:
                        case DO:
                        case DONT:
                            // Check for and handle any unframed data that has been buffered as it 
                            // might be important to do this before processing any command
                            handleUnframedData();
                            tnCommand = curr_byte;
                            tnState = TN_CMD;
                            break;

                        case SB:
                            // Check for and handle any unframed data that has been buffered as it 
                            // might be important to do this before processing any command
                            handleUnframedData();
                            //System.err.println("Sub-option: " + subOptionBufferLen);
                            subOptionBufferLen = 0;
                            tnState = TN_SUB_CMD;
                            break;
                    }
                    break;

                case TN_CMD:
                    //System.out.println("CMD...");
                    handleTnCommand(tnCommand, curr_byte);
                    //System.out.println("did command...");
                    tnState = TN_DEFAULT;
                    break;

                case TN_SUB_CMD:
                    if (curr_byte != SE)
                    {
                        // buffer until we reach end of negotiation
                        subOptionBuffer[subOptionBufferLen++] = curr_byte;
                        break;
                    }
                    handleSubOptions();

                    tnState = TN_DEFAULT;
                    break;
            }
        }

        // Handle any leftover unframed data
        handleUnframedData();
    }

    public void handleSubOptions() throws IOException
    {
        log.finer("> received TN SUB OPTS : " + decodeSubOpts(subOptionBuffer, subOptionBufferLen - 1));

        switch (subOptionBuffer[0])
        {
            case OPT_TERMINAL_TYPE:
                switch (subOptionBuffer[1])
                {
                    case OPTION_SEND:
                        sendTerminalType();
                        break;
                    case OPTION_IS:
                        byte[] bType = RWTelnet.shortArrayToByte(subOptionBuffer, 2, subOptionBufferLen - 3);
                        doIsTerminalType(new String(bType));
                        break;
                    default:
                        log.severe("Invalid Terminal Type command sub-option:: " + subOptionBuffer[1]);
                }
                break;

            default:
                this.tnParser.telnetSubOpts(this.subOptionBuffer, this.subOptionBufferLen - 1);
        }
    }

    private int readSocket() throws IOException
    {
        inBufLen = 2048;

        // Since inputstreams require a byte array as a parameter (and
        // not a short[]), we have to use this temporary buffer to
        // store the results.
        byte[] tmpByteBuf = new byte[inBufLen];
        int bytes_read = is.read(tmpByteBuf, 0, inBufLen);

        if (bytes_read > -1)
        {
            // System.out.println("Done... ");
            if (traceHandler != null)
            {
                // possible we'd want this waited/queued to some async handler thread
                traceHandler.incomingData(tmpByteBuf, 0, bytes_read);
            }

            inBuf = byteArrayToShort(tmpByteBuf, bytes_read);
        }

        //System.out.println("Bytes read: " + bytes_read);
        return bytes_read;
    }

    public void processDataIn(byte[] buff)
        throws IOException
    {
        processDataIn(buff, buff.length);
    }

    public void processDataIn(byte[] buff, int len)
        throws IOException
    {
        inBuf = byteArrayToShort(buff, len);
        inBufLen = len;
        parseData();
    }

    public void setOutputStream(OutputStream os)
    {
        this.os = os;
    }

    public void setTraceHandler(TraceHandler traceHandler)
    {
        this.traceHandler = traceHandler;
    }

    public void sendTnCommand(short tnCmd, short tnOption)
        throws IOException
    {
        switch (tnCmd)
        {
            case WILL:
            case DO:
                reqdTnCmds.add(tnOption);
                //negTnCmds.add(tnOption);
                break;

            case WONT:
            case DONT:
                reqdTnCmds.remove(tnOption);
                //negTnCmds.remove(tnOption);
                break;
        }

        sendCommand(tnCmd, tnOption);
    }

    public void sendTnSubOpts(short[] optVals)
        throws IOException
    {
        log.finer("sending TN SUB OPTS : " + decodeSubOpts(optVals, optVals.length));

        byte[] tmpBuffer = new byte[4 + optVals.length];
        tmpBuffer[0] = (byte) IAC;
        tmpBuffer[1] = (byte) SB;

        for (int ix = 0; ix < optVals.length; ix++)
        {
            tmpBuffer[2 + ix] = (byte) optVals[ix];
        }

        tmpBuffer[tmpBuffer.length - 2] = (byte) IAC;
        tmpBuffer[tmpBuffer.length - 1] = (byte) SE;

        send(tmpBuffer, 0, tmpBuffer.length);
    }

    private void sendInitialCommands()
        throws IOException
    {
        if (!this.sendInitialCmds)
        {
            return;
        }

        Set<Short> cmds = new HashSet<>();
        cmds.addAll(this.reqdTnCmds);
        cmds.addAll(this.optTnCmds);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (short cmd : cmds)
        {
            // All except ECHO are fair game for client to say they will support.
            if (cmd != OPT_ECHO)
            {
                log.finer(String.format("sending initial command: %s - %s", decodeCmd(WILL), decodeOpt(cmd)));
                baos.write((byte) IAC);
                baos.write((byte) WILL);
                baos.write((byte) cmd);
                //negTnCmds.add(cmd);
            }
        }

        send(baos.toByteArray(), 0, baos.size());
    }

    /**
     * @return the lineModeOpts
     */
    public byte[] getLineModeOpts()
    {
        return lineModeOpts;
    }

    /**
     * @param lineModeOpts the lineModeOpts to set
     */
    public void setLineModeOpts(byte[] lineModeOpts)
    {
        this.lineModeOpts = lineModeOpts;
    }

}
