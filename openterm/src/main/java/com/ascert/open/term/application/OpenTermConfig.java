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
package com.ascert.open.term.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.ascert.open.term.core.SimpleConfig;

/**
 * Basic implementation of SimpleConfig class that is wired into System Properties, (optionally) Java Preferences, and also allowing default
 * properties courtesy of the current classloader (i.e. typically within the current JAR)
 *
 * Property resolution is as follows: - System properties: thus ensuring -D override will take precedence - Java Preferences: in the user
 * collection. For most day to day user preferences - Pre-defined properties: allowing implementation defaults to be shipped as part of the
 * JAR
 *
 * @version 1,0 13-Oct-2017
 * @author rhw
 * @history 13-Oct-2017 rhw Created
 */
public class OpenTermConfig
    implements SimpleConfig
{

    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////
    private static final Logger log = Logger.getLogger(OpenTermConfig.class.getName());

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    // Static singletons are not a tidy approach, but in the simple standalone model they get the job done
    // This can be improved upon fairly easily if needed at some stage e.g. with an outer framework which supports proper
    // dependency injection.
    private static SimpleConfig appWideConfig;

    public static SimpleConfig getConfig()
    {
        return appWideConfig != null ? appWideConfig : SimpleConfig.SyspropsConfig.getInstance();
    }

    public static synchronized void initConfig(SimpleConfig config)
    {
        if (appWideConfig == null)
        {
            appWideConfig = config;
        }
        else
        {
            log.warning("Duplicate attempt to initConfig");
        }
    }

    public static void loadConfigDefaults(String propsName)
    {
        getConfig().loadDefaults(propsName);
    }

    public static int getIntProp(String key, int defaultValue)
    {
        int retval;
        try
        {
            retval = Integer.parseInt(getProp(key));
        }
        catch (NumberFormatException nfe)
        {
            retval = defaultValue;
        }
        
        return retval;
    }
    
    public static String getProp(String key, String defaultValue)
    {
        return getConfig().getProperty(key, defaultValue);
    }

    public static String getProp(String key)
    {
        return getConfig().getProperty(key);
    }

    public static String setProp(String key, String value)
    {
        return getConfig().setProperty(key, value);
    }

    public static boolean clearPrefs()
    {
        return getConfig().clear();
    }
    
    // Keys in property files sometimes need quotes to preserve spaces or make them easier to read
    // This utility method simplifies removing them
    public static String stripQuotes(String str)
    {
        if (str != null)
        {
            str = str.replaceAll("^[\"']+|[\"']+$", "");
        }
        return str;
    }

    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    private Preferences prefsNode;
    private Properties defaults = new Properties();

    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    public OpenTermConfig()
    {
        this(null, false);
    }

    public OpenTermConfig(String defaults, boolean useJavaPrefs)
    {
        if (useJavaPrefs)
        {
            prefsNode = Preferences.userNodeForPackage(OpenTermConfig.class);
        }

        if (defaults != null)
        {
            loadDefaults(defaults);
        }
    }

    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // INTERFACE METHODS - SimpleConfig
    //////////////////////////////////////////////////
    @Override
    public String getProperty(String key, String defaultValue)
    {
        // System props takes precedence so that command line launch -D properties are always used
        String val = System.getProperty(key);

        if (val == null && prefsNode != null)
        {
            val = prefsNode.get(key, null);
        }

        if (val == null)
        {
            // fallback to embedded defaults
            val = defaults.getProperty(key);
        }

        log.finer(String.format("getProperty: key=%s, val=[%s], default=[%s]", key, val, defaultValue));
        return val != null ? val : defaultValue;
    }

    @Override
    public String setProperty(String key, String value)
    {
        // not totally clear whether previous value should be at Prefs level, or including fallbacks
        String prevVal = getProperty(key);
        log.finer(String.format("setProperty: key=%s, val=[%s]", key, value));
        prefsNode.put(key, value);
        return prevVal;
    }

    public void loadDefaults(String propsName)
    {
        log.finer(propsName);

        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(propsName))
        {
            if (is != null)
            {
                defaults.load(is);
            }
            else
            {
                log.warning("Unable to find default properties: " + propsName);
            }
        }
        catch (IOException ioe)
        {
            log.warning("Exception loading default properties: " + ioe);
        }
    }

    public boolean clear()
    {
        try
        {
            prefsNode.clear();
            return true;
        }
        catch (BackingStoreException ex)
        {
            log.severe("Exception clearing preferences: " + ex);
            return false;
        }
    }
    
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
