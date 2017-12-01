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
 * This interface servers purely to provide (as it's name suggests) a very simple configuration provider to the internal terminal handling
 * classes for storage and retrieval of basic key/value property pairs. Intentionally, no richer types are supported. Sticking to a basic
 * API lowers the barrier to implementations, and discourages use for general storage. Very few, if any, config type items cannot be stored
 * in a String. Equally intentionally, it's API calls are easily mapped to concrete implementations such as standard Java Properties and
 * Preferences.
 *
 * There are many many other libraries tackling this same problem, and we will not go into the merits of them here. But all of them come
 * with baggage, implementation constraints, specific storage issues etc. As a standalone emulator, it's felt better to keep as much of this
 * baggage as low as possible. Anyone packaging or building this emulator out into a larger model can easily map this basic API onto some
 * richer implementation, but in doing so will not pollute the internal classes with preferences storage implementation details.
 *
 * @version 1,0 13-Oct-2017
 * @author rhw
 * @history 13-Oct-2017 rhw Created
 */
public interface SimpleConfig
{
    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////

    default String getProperty(String key)
    {
        return getProperty(key, null);
    }

    String getProperty(String key, String defaultValue);

    String setProperty(String key, String value);

    default void loadDefaults(String propsName)
    {
    }

    // Convenience class to get a SimpleConfig instance which always redirects to System properties
    public static class SyspropsConfig
    {

        private static SimpleConfig instance = new SimpleConfig()
        {
            @Override
            public String getProperty(String key, String defaultValue)
            {
                return System.getProperty(key, defaultValue);
            }

            @Override
            public String setProperty(String key, String value)
            {
                return System.setProperty(key, value);
            }
        };

        public static SimpleConfig getInstance()
        {
            return instance;
        }
    }

}
