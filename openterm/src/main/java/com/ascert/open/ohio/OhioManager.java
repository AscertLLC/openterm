/*
 * Java interfaces for Open Host Interface Objects (OHIO)
 *      https://tools.ietf.org/html/draft-ietf-tn3270e-ohio-01
 *
 * Copyright (C) 2016, Ascert LLC
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
package com.ascert.open.ohio;

/**
 * The central repository for access to all OHIO sessions. The OhioManager contains a list of all OhioSession objects available on this
 * system.
 */
public interface OhioManager
{

    // An OhioSessions object containing the OhioSession objects
    // available on this system. This list of objects is a static
    // snapshot at the time the OhioSessions object is created.
    // Use the OhioSessions.refresh method to obtain a new
    // snapshot.    
    OhioSessions getSessions();

    // Returns an OhioSession object based on the search parameters
    // provided.
    //      ConfigurationResource       A vendor specific string used to
    //                                  provide configuration information.
    //      SessionName                 The unique name associated with an
    //                                  OhioSession.
    // The parameters are used as follows:
    //  Is ConfigurationResource provided?
    //      Yes - Is SessionName provided?
    //          Yes - Is OhioSession object with matching
    //                SessionName available on the system?
    //              Yes - Error, attempting to create an
    //                    OhioSession object with a non-unique
    //                    SessionName.
    //              No - Create an OhioSession object using
    //                   SessionName and ConfigurationResource.
    //          No - Start a new OhioSession using
    //               ConfigurationResource and generating a new
    //               SessionName.
    //      No - Is SessionName provided?
    //          Yes - Is OhioSession object with matching
    //                SessionName available on the system?
    //              Yes - Return identified OhioSession object.
    //              No - Return null.
    //          No - Return null.    
    OhioSession openSession(String configurationResource, String sessionName);

    // Closes an OhioSession object. The OhioSession is
    // considered invalid and is removed from the list of
    // OhioSession objects.
    //      SessionObject       The OhioSession to close.
    void closeSession(OhioSession sessionObject);

    // Closes an OhioSession object. The OhioSession is
    // considered invalid and is removed from the list of
    // OhioSession objects.
    //      SessionName     The SessionName of the OhioSession to
    //                      close.
    void closeSession(String sessionName);
}
