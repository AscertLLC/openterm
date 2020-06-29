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
package com.ascert.open.term.i3270;

import java.util.Scanner;
import java.util.logging.Logger;

import com.ascert.open.ohio.Ohio.OHIO_AID;
import com.ascert.open.ohio.Ohio.OHIO_TYPE;
import com.ascert.open.ohio.OhioPosition;

import com.ascert.open.term.core.AbstractTerminal;
import com.ascert.open.term.core.RWTelnet;
import com.ascert.open.term.core.TermChar;
import com.ascert.open.term.core.TermField;
import com.ascert.open.term.core.TnAction;
import com.ascert.open.term.gui.KeyHandler;

/**
 * This class represents a 3270 session to client implementations, and is the main API for creating 3270 applications.
 *
 * @since 0.1
 */
public class Term3270
    extends AbstractTerminal
{

    private static final Logger log = Logger.getLogger(Term3270.class.getName());

    public static String formatTermTypeString(String baseType, int tnModel, boolean extAttribs)
    {
        return String.format("%s%s-%d%s",
                             !baseType.startsWith("IBM-") ? "IBM-" : "",
                             baseType,
                             tnModel,
                             extAttribs ? "-E" : "");
    }

    private WaitObject waitObject;
    private volatile boolean dataArrived;

    private String baseType;
    private short tnModel;
    private boolean extAttribs;

    // Always false at present. Telnet handler doesn't yet support TN3270E. 
    // Marker purely in place for status line
    private boolean tn3270e = false;

    //private Term3270Char[] chars; // array of current characters
    // Default video attributes
    Term3270Char vaVideoDefault = new Term3270Char(0);

    public Term3270()
    {
        this("IBM-3278-2");
    }

    /**
     * Constructor. Sets the model type of the new 3270 object.
     *
     * <ul>
     * <li>
     * <code>2: 24x80</code>
     * </li>
     * <li>
     * <code>3: 32x80</code>
     * </li>
     * <li>
     * <code>4: 43x80</code>
     * </li>
     * <li>
     * <code>5: 27x132</code>
     * </li>
     * </ul>
     *
     *
     * @param modelNumber the 3270 model number.
     * @param client      {@link TnAction} interface used for communicating with the client implementation.
     */
    public Term3270(String termType)
    {
        super();
        ohTermType = OHIO_TYPE.OHIO_TYPE_3270;
        this.keyHandler = new KeyHandler3270(this);

        decodeTermTypeString(termType);
        // Reconstruct the term type string in case any invalid parts were defaulted
        this.termType = formatTermTypeString(baseType, tnModel, extAttribs);
        log.fine("RW3270 termType: " + termType);

        cols = 80;

        switch (tnModel)
        {
            case 2:
                rows = 24;
                break;

            case 3:
                rows = 32;
                break;

            case 4:
                rows = 43;
                break;

            case 5:
                rows = 27;
                cols = 132;
                break;
        }

        initTerm();
        // create the 3270 Stream Parser Object.  Pass it this
        // instance so it can call back changes to the field and
        // character buffer as necessary.
        tnParser = new Tn3270StreamParser(this);

        // create the TELNET object
        setTelnet(new RWTelnet(tnParser));
        waitObject = new WaitObject();
    }

    private void decodeTermTypeString(String termType)
    {
        Scanner scan = new Scanner(termType).useDelimiter("-");

        String ibm = scan.next();

        if (!"IBM".equals(ibm))
        {
            log.warning("Unrecognised term manufacturer: " + ibm);
        }

        baseType = scan.next();

        if (!"3278".equals(baseType) && !"3279".equals(baseType))
        {
            log.warning("Unrecognised term type: " + baseType);
            baseType = "3278";
        }

        tnModel = (short) scan.nextInt();

        if (tnModel < 2 || tnModel > 5)
        {
            log.warning("Unrecognised term model: " + tnModel);
            tnModel = 2;
        }

        if (scan.hasNext())
        {
            String ext = scan.next();
            if ("E".equals(ext))
            {
                // We don't use the extAttribs part for any validation or handling - purely for term type string creation
                extAttribs = true;
            }
            else
            {
                log.warning("Unrecognised term extension: " + ext);
            }
        }
    }

    public void initTerm()
    {
        pages.clear();
        initPages(1);
        setActivePage(0);
        setDisplayPage(0);
        //TODO - hack for now to adjust status line 2 rows down. 
        //       may replace with a better renderer which 
        statusLine = initTermChars(new Term3270Char[cols], (rows + 1) * cols);
    }

    @Override
    public boolean allowDirectScreenEditing()
    {
        return getFields().size() == 0;
    }

    @Override
    public char getEmptyChar()
    {
        return 0;
    }

    @Override
    public void Fkey(OHIO_AID key)
    {
        super.Fkey(key);

        if (aid == OHIO_AID.OHIO_AID_3270_CLEAR)
        {
            TermChar[] chars = getCharBuffer();

            for (int i = 0; i < chars.length; i++)
            {
                chars[i].clear();
            }

            resumeParentThread();
        }
    }

    @Override
    public TermChar[] getStatusLine()
    {

        // Quick hack for now, following x3270 indicators
        String commStatus = String.format("4%c%c", tn3270e ? 'B' : 'A', tn.isConnected() ? ' ' : '?');
        setStatusChars(0, commStatus);

        // According to various online sources, also marks comm status:
        //      https://msdn.microsoft.com/en-us/library/gg165006(v=bts.70).aspx
        //      https://msdn.microsoft.com/en-us/library/gg165006(v=bts.70).aspx
        String kbdStatus = "         ";

        if (!tn.isConnected())
        {
            //TODO - need to rewire host_ disconnected status handling so we can detect and show a COMM505 as well
            kbdStatus = "X COMM504";
        }
        else if (isKeyboardLocked())
        {
            // have no code yet to determine other X statuses e.g X ?+
            kbdStatus = "X SYSTEM ";
        }
        // Opinions seem to differ on whether it starts in column 9 or 10
        setStatusChars(8, kbdStatus);

        String cursorStatus = String.format("%03d/%03d", getCurrentRow(), getCurrentCol());
        setStatusChars(this.getCols() - 7, cursorStatus);

        log.finer(String.format("comm status: %s (%s)", commStatus, cursorStatus));
        return statusLine;
    }

    public TermChar getDefaultVAChar()
    {
        return vaVideoDefault;
    }

    /**
     * Sets session data on the session server
     *
     * @param key   DOCUMENT ME!
     * @param value DOCUMENT ME!
     */
    public void setSessionData(String key, String value)
    {
        tn.setSessionData(key, value);
    }

    /**
     * Blocks until the specified string is found in the data buffer (screen) or until the specified timeout is reached.
     *
     * <p>
     * This method is extrememly useful when the host may be sending several screens in response to your request, and you want to block
     * until you reach the response you're waiting for. For example:<br/>
     * <pre>
     * ...
     * // Create a Term3270 object
     * Term3270 rw = new Term3270(this);
     * // Connect to the SessionServer and specify a host
     * rw.connect("3.3.88.3", 6870, "hollis.harvard.edu", 23, true);
     * // This will block for 30 seconds or until the string
     * // H A R V A R D is found.  it also returns a boolean to
     * // let us check the results
     * <b>boolean t = rw.waitFor("H A R V A R D", 30);</b>
     * // Now we'll execute two different blocks of code depending
     * // on whether our string was found:
     * if(t) {
     *   out.print("Got it:");
     *   out.print(new String(rw.getDisplay()));
     * } else {
     *   out.print("Didn't get it:>");
     *   out.print(new String(rw.getDisplay()));
     * }
     * </pre>
     * </p>
     *
     * @param search  the string to wait for.
     * @param timeout number of seconds to wait for the search string.
     *
     * @return <code>true</code> if the string was found, <code>false</code> if the timeout was reached without finding it.
     */
    public boolean waitFor(String search, int timeout)
    {
        return waitForText(search, timeout*1000);
    }

    public boolean waitForText(String search, long timeoutMillis)
    {
        boolean found = contains(search);
        long cur = System.currentTimeMillis();
        long finish = cur + timeoutMillis;
        while (!found && cur < finish)
        {
            waitForNewData(finish-cur);
            cur = System.currentTimeMillis();
            found = contains(search);
        }
        return found;
    }

    /**
     * Blocks the currently executing thread until new data arrives from the host.
     *
     * <p>
     * This method is useful when using stateless client implementations (i.e. HTTP) or when you're scripting through several 3270 screens.
     * </p>
     *
     * <p>
     * For example:<br>
     * <pre>
     * ...
     * //Set the value of a field and press enter
     * Term3270Field.setData("My Data");
     * Term3270.enter();
     * //Wait for the new screen
     * Term3270.waitForNewData();
     * myClient.paintScreen()....
     * </pre>
     * </p>
     *
     * <p>
     * <em>IMPORTANT:</em> This method only blocks until the 3270 engine has received a response from the host and processed it. In the case
     * where the host has sent an EraseAllUnprotected or other Non-Data type command, this method will return normally, even though the
     * DataBuffer (screen) may be empty. Client implementations need to handle any host-specific anomalies such as screen clears, etc.
     * </p>
     */
    public void waitForNewData()
    {
        waitForNewData(0);
    }

    public boolean waitForNewData(long timeoutMillis)
    {
        synchronized (waitObject)
        {
            try
            {
                dataArrived = false;
                waitObject.wait(timeoutMillis);
                return dataArrived;
            }
            catch (InterruptedException e)
            {
            }
        }
        return false;
    }

    protected void resumeParentThread()
    {
        if (waitObject != null)
        {
            synchronized (waitObject)
            {
                dataArrived = true;
                waitObject.notifyAll();
            }
        }
    }

    protected TermChar[] getTermCharPage()
    {
        // create the character buffer
        return initTermChars(new Term3270Char[rows * cols], 0);
    }

    @Override
    protected TermChar[] initTermChars(TermChar[] tch, int baOff)
    {
        // create TermChar instances for each position in the databuffer (chars array)
        for (int i = 0; i < tch.length; i++)
        {
            tch[i] = new Term3270Char(i + baOff);
        }
        return tch;
    }

    protected TermField newTermField(TermChar currChar)
    {
        return new Term3270Field((Term3270Char) currChar, this);
    }

    static class WaitObject
    {

        public synchronized void getLock()
        {
        }
    }
}
