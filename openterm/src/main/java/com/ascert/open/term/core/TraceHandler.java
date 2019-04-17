/*
 * Copyright (c) 2016, 2017 Ascert, LLC.
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

/**
 * Very basic interface to allow implementations to supply handlers for tracing and capturing low level comms data. Only handles data at
 * present, but can easily be extended in future to trace events e.g. connect, disconnect etc.
 *
 * @version 1,0 22-Nov-2017
 * @author srm

 */
public interface TraceHandler
{

    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////
    void incomingData(byte[] data, int offset, int len);

    default void incomingData(byte[] data)
    {
        if (data != null)
        {
            incomingData(data, 0, data.length);
        }
    }

    void outgoingData(byte[] data, int offset, int len);

    default void outgoingData(byte[] data)
    {
        if (data != null)
        {
            outgoingData(data, 0, data.length);
        }
    }

}
