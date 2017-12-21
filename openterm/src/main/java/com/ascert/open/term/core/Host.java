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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Holds information about a destination terminal server host.
 *
 * @since 0.1
 */
public class Host implements Serializable
{
    private static final Logger log = Logger.getLogger(Host.class.getName());

    private String termType;
    private String hostName;
    private int port;
    protected boolean encryption;
    protected boolean favourite;


    public static List<Host> getHostStringAsList(String hosts, boolean favourites)
    {
        List<Host> hostLst = new ArrayList<>();
        
        if (hosts != null && !hosts.trim().isEmpty())
        {
            //Pretty crude - adapted from original code
            for (String availableHost : hosts.split(";"))
            {
                String[] opts = availableHost.split(",");
                String hostName = opts[0];
                int hostPort = opts.length > 1 ? Integer.parseInt(opts[1]) : 23;
                String useSSL = opts.length > 2 ? opts[2] : "false";
                String termType = opts.length > 3 ? opts[3] : "IBM-3278-2";
                hostLst.add(new Host(hostName, hostPort, termType, "true".equalsIgnoreCase(useSSL), favourites));
            }
        }
        
        return hostLst;
    }

    
    // Crude approach, but workable for now
    public static String getFavouritesAsConfigString(List<Host> hosts)
    {
        StringBuilder buf = new StringBuilder();
        
        for (Host host : hosts)
        {
            if (host.isFavourite())
            {
                buf.append(host.toString());
                buf.append(";");
            }
        }
        
        return buf.toString();
    }
    
    
    public Host(String hostName, int port, String termType)
    {
        this(hostName, port, termType, false);
    }

    public Host(String hostName, int port, String termType, boolean encryption)
    {
        this(hostName, port, termType, false, false);
    }
    
    public Host(String hostName, int port, String termType, boolean encryption, boolean favourite)
    {
        this.hostName = hostName;
        this.port = port;
        this.termType = termType;
        this.encryption = encryption;
        this.favourite = favourite;
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

    /**
     * @return the favourite
     */
    public boolean isFavourite()
    {
        return favourite;
    }

    /**
     * @param favourite the favourite to set
     */
    public void setFavourite(boolean favourite)
    {
        this.favourite = favourite;
    }
    
    public String toString()
    {
        return String.format("%s,%d,%b,%s", getHostName(), getPort(), isEncryption(), getTermType());
    }

}
