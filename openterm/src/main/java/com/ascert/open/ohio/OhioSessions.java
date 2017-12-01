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
 * Contains a collection of OhioSession objects. This list is a static snapshot of the list of OhioSession objects available at the time of
 * the snapshot.
 */
public interface OhioSessions
{

    // The number of OhioSession objects contained in this
    // collection.
    int getCount();

    // The OhioSession object at the given index. "One based"
    // indexing is used in all Ohio collections. For example, the
    // first OhioSession in this collection is at index 1.
    //      index           The index of the target OhioSession.
    OhioSession item(int index);

    // The OhioSession object with the given SessionName. Returns
    // null if no object with that name exists in this collection.
    //      SessionName     The target name.
    OhioSession item(String sessionName);

    // Updates the collection of OhioSession objects. All
    // OhioSession objects that are available on the system at the
    // time of the refresh will be added to the collection.
    // Indexing of OhioSession objects will not be preserved across
    // refreshes.    
    void refresh();
}
