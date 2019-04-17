/*
 * Copyright (c) 2000-2018 Ascert, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Ascert, LLC. ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with the terms
 * of the license agreement you entered into with Ascert.
 *
 * ASCERT MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
 * OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. ASCERT SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */
package com.ascert.open.rfb.server;

/**
 * Interface to abstract differences between alternative WebSocket implementations. Specifically at this stage
 * the fast and tidy standalone version in GitHub by TooTallNate, and Jetty which is a commonly used heavier weight
 * server.
 * 
 * @version 1,0 30-Aug-2018
 * @author srm

 *      30-Aug-2018    srm        Created
 */
public interface WebSocketProvider
{

    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////

    void send(byte[] byt)
        throws Exception;
    
    void send(String txt) 
        throws Exception;
    
    void close();
        
}
