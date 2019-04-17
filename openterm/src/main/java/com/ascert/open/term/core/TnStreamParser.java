/*
 * Copyright (cx) 2016, 2017 Ascert, LLC.
 * www.ascert.com
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

import java.io.IOException;

import com.ascert.open.ohio.Ohio.OHIO_AID;

/**
 *
 * @version 1,0 24-Apr-2017
 * @author srm

 */
public interface TnStreamParser
{
    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////

    /**
     * Temporary measure to parse data stream from ghost
     *
     * @return the tnParser
     */
    default void parse(byte[] inBuf, int inBufLen) throws IOException
    {
        parse(RWTelnet.byteArrayToShort(inBuf, inBufLen), inBufLen);
    }

    /**
     * This method takes an input buffer and executes the appropriate commands and orders.
     */
    void parse(short[] inBuf, int inBufLen) throws IOException;

    /**
     * Telnet sub-options may be important to the processing of some telnet clients e.g. control echo of output when using NSK line mode.
     *
     * @param subOptionBuffer
     * @param subOptionLen
     */
    default void telnetSubOpts(short[] subOptionBuffer, int subOptionLen)
    {
    }

    ;


    public void Fkey(OHIO_AID aid);

    //TODO - these may go or get refactored
    default void status(int msg)
    {
    }

    default void broadcastMessage(String msg)
    {
    }

    public String getTermType();

}
