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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ascert.open.term.application.OpenTermConfig;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;

/**
 * Holds information about a destination terminal server host.
 *
 * @since 0.1
 */
public class Host implements Serializable, SimpleConfig
{
    private static final Logger log = Logger.getLogger(Host.class.getName());

    Properties props = new Properties();

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
                try (StringWriter sw = new StringWriter())
                {
                    buf.append("<<<");
                    host.props.store(sw, null);
                    // Strip out the initial comment line - serves no use but wasting space
                    //TODO - could be a useful utility method really!
                    String sProps = sw.toString();
                    buf.append(sProps.substring(sProps.indexOf('\n')));
                    buf.append(">>>");
                }
                catch (Exception ex)
                {
                    log.severe("Exception storing favourites: " + ex);
                }
            }
        }
        
        return buf.toString();
    }
    
    
    public static List<Host> getHostListFromConfigString(String cfgString)
    {
        List<Host> hostLst = new ArrayList<>();
        Pattern p = Pattern.compile("(<<<)(.*?)(>>>)", DOTALL);
        
        if (cfgString != null && !cfgString.trim().isEmpty())
        {
            Matcher mtch = p.matcher(cfgString);
            while (mtch.find())
            {
                try (StringReader sr = new StringReader(mtch.group(2)))
                {
                    Host hst = new Host();
                    hst.props.load(sr);
                    hostLst.add(hst);
                }
                catch (Exception ex)
                {
                    log.severe("Exception loading favourites: " + ex);
                }
            }
        }
        
        return hostLst;
    }
    
    public Host()
    {
    }
    
    public Host(String hostName, int port, String termType)
    {
        this(hostName, port, termType, false);
    }

    public Host(String hostName, int port, String termType, boolean encryption)
    {
        this(hostName, port, termType, encryption, false);
    }
    
    public Host(String hostName, int port, String termType, boolean encryption, boolean favourite)
    {
        setHostName(hostName);
        setPort(port);
        setTermType(termType);
        setEncryption(encryption);
        setFavourite(favourite);
    }

    
    public String toString()
    {
        //System.out.println(String.format("{{%s}}", props.toString()));
        //return String.format("%s,%d,%b,%s", getHostName(), getPort(), isEncryption(), getTermType());
        return String.format("{{%s}}", props.toString());
    }

    public String getDisplayName()
    {
        return props.getProperty("host.displayName", String.format("%s:%d", getHostName(), getPort()));
    }
    
    /**
     * @return the termType
     */
    public String getTermType()
    {
        return getProperty("host.termType");
    }

    /**
     * @param termType the termType to set
     */
    public void setTermType(String termType)
    {
        setProperty("host.termType", termType);
    }

    /**
     * @return the hostName
     */
    public String getHostName()
    {
        return getProperty("host.hostName");
    }

    /**
     * @param hostName the hostName to set
     */
    public void setHostName(String hostName)
    {
        setProperty("host.hostName", hostName);
    }

    /**
     * @return the port
     */
    public int getPort()
    {
        return Integer.parseInt(getProperty("host.hostPort", "0"));
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port)
    {
        setProperty("host.hostPort", Integer.toString(port));
    }

    /**
     * @return the encryption
     */
    public boolean isEncryption()
    {
        return Boolean.parseBoolean(getProperty("host.encryption"));
    }

    /**
     * @param encryption the encryption to set
     */
    public void setEncryption(boolean encryption)
    {
        setProperty("host.encryption", Boolean.toString(encryption));
    }

    /**
     * @return the favourite
     */
    public boolean isFavourite()
    {
        return Boolean.parseBoolean(getProperty("host.favourite"));
    }

    /**
     * @param favourite the favourite to set
     */
    public void setFavourite(boolean favourite)
    {
        setProperty("host.favourite", Boolean.toString(favourite));
    }

    @Override
    public String getProperty(String key, String defaultValue)
    {
        String val = props.getProperty(key);
        //TODO - might need to revisit whether we always fall back here
        if (val == null)
        {
            val = OpenTermConfig.getProp(key, defaultValue);
        }
        
        log.finer(String.format("Host property: %s, val=%s", key, val));
        return val;
    }

    @Override
    public String setProperty(String key, String value)
    {
        Object prev = props.setProperty(key, value);
        return (prev != null) ? prev.toString() : null;
    }
        
    public boolean clear()
    {
        // if we ever model object field values as props, we need to make sure this doesn't clear them!
        props.clear();
        return true;
    }
    
    public void addProperties(Properties props)
    {
        log.finer("Adding Host props: " + props);
        if (props != null)
        {
            this.props.putAll(props);
        }
    }
    
}
