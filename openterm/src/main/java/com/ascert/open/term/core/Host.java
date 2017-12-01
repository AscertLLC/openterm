/*
 * FreeHost3270 a suite of terminal 3270 access utilities.
 * Copyright (C) 1998, 2001  Art Gillespie
 * Copyright (2) 2005 the http://FreeHost3270.Sourceforge.net
 *                        Project Contributors.
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

import java.io.Serializable;

/**
 * Holds information about a destination terminal server host.
 *
 * @since 0.1
 */
public class Host implements Serializable
{

    private String termType;
    private String hostName;
    private int port;
    protected boolean encryption;

    public Host(String hostName, int port, String termType)
    {
        this(hostName, port, termType, false);
    }

    public Host(String hostName, int port, String termType, boolean encryption)
    {
        this.hostName = hostName;
        this.port = port;
        this.termType = termType;
        this.encryption = encryption;
    }

    public String getTermType()
    {
        return termType;
    }

    public String getHostName()
    {
        return hostName;
    }

    public int getPort()
    {
        return port;
    }

    /**
     * @return the encryption
     */
    public boolean isEncryption()
    {
        return encryption;
    }

    public String toString()
    {
        return String.format("%s : %d (%s)", getHostName(), getPort(), getTermType());
    }
}
