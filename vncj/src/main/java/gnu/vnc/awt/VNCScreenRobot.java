package gnu.vnc.awt;

/**
* <br><br><center><table border="1" width="80%"><hr>
* <strong><a href="http://www.amherst.edu/~tliron/vncj">VNCj</a></strong>
* <p>
* Copyright (C) 2000-2002 by Tal Liron
* <p>
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public License
* as published by the Free Software Foundation; either version 2.1
* of the License, or (at your option) any later version.
* <p>
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* <a href="http://www.gnu.org/copyleft/lesser.html">GNU Lesser General Public License</a>
* for more details.
* <p>
* You should have received a copy of the <a href="http://www.gnu.org/copyleft/lesser.html">
* GNU Lesser General Public License</a> along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
* <hr></table></center>
**/
 
import gnu.rfb.*;
import gnu.rfb.server.*;

import java.io.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import gnu.awt.PixelsOwner;
import gnu.logging.VLogger;
import gnu.vnc.VNCQueue;
import gnu.vnc.ScreenImageListener;
import gnu.vnc.Screen;

public class VNCScreenRobot implements RFBServer, PixelsOwner, ScreenImageListener
{
    
	public VNCScreenRobot(Screen screen, String displayName)
	{
		this.displayName = displayName;
        this.screen = screen;
        
		events = new VNCScreenEvents( screen, clients );
		queue = new VNCQueue( clients );
        
        initPixels();
        
        updateScreenShot();
        queue.takeSnapshot(this);

        screen.addScreenListener(this);
	}
	
	//
	// RFBServer
	//

	// Clients
	
    //TODO - seems like we should really throw some kind of NotShareable exception if multiple clients are not allowed to 
    //       share this server
	public void addClient( RFBClient client )
	{
        System.out.println("*** add client: " + client);
		clients.addClient( client );
	}
	
	public void removeClient( RFBClient client )
	{
        //TODO - handle client disconnection
        System.out.println("*** remove client: " + client);
		clients.removeClient( client );
	}
	
	// Attributes
	
	public String getDesktopName( RFBClient client )
	{
		return displayName;
	}
	
	public int getFrameBufferWidth( RFBClient client )
	{
		return getPixelWidth();
	}
	
	public int getFrameBufferHeight( RFBClient client )
	{
		return getPixelHeight();
	}
	
	public PixelFormat getPreferredPixelFormat( RFBClient client )
	{
        //???
		return PixelFormat.RGB888;
	}
	
	public boolean isSharingAllowed()
	{
        // Although we allow sharing, in some configurations there may not be a physical way to achieve it
        // e.g. a ServerFactory creates a new server instance for each incoming client socket. In these cases
        // there would need to be some other channel that allowed a viewer/client to connect to an existing instance
		return true;
	}
	
	// Messages from client to server

	public void setClientProtocolVersionMsg( RFBClient client, String protocolVersionMsg ) throws IOException
	{
	}
	
	public void setShared( RFBClient client, boolean shared ) throws IOException
	{
	}
	
	public void setPixelFormat( RFBClient client, PixelFormat pixelFormat ) throws IOException
	{
        //???
		pixelFormat.setDirectColorModel( (DirectColorModel) Toolkit.getDefaultToolkit().getColorModel() );
	}
	
	public void setEncodings( RFBClient client, int[] encodings ) throws IOException
	{
	}
	
	public void fixColourMapEntries( RFBClient client, int firstColour, Colour[] colourMap ) throws IOException
	{
	}
	
	public void frameBufferUpdateRequest( RFBClient client, boolean incremental, int x, int y, int w, int h ) throws IOException
	{
        VLogger.getLogger().log(String.format("update request - x: %d, y: %d, w:%d, h:%d, incremental: %b", x, y, w, h, incremental));
        //TODO - we don't do incrementals at this stage. An exception will be thrown if we attempt to do this
        //       with no incremental rectangles queued
        queue.frameBufferUpdate( client, false, x, y, w, h);
	}
	
    
	public void keyEvent( RFBClient client, boolean down, int key )
	{
        if (screen.isScreenInputEnabled())
        {
            events.translateKeyEvent( client, down, key );
        }
	}
    
	public void pointerEvent( RFBClient client, int buttonMask, int x, int y )
	{
        if (screen.isScreenInputEnabled())
        {
            events.translatePointerEvent( client, buttonMask, x, y );
        }
	}
		
	public void clientCutText( RFBClient client, String text ) throws IOException
	{
	}

	///////////////////////////////////////////////////////////////////////////////////////
	// Private

	private RFBClients clients = new RFBClients();
	private VNCScreenEvents events;
	protected VNCQueue queue;

	private String displayName;
	private int mouseModifiers = 0;
    private Screen screen;
    private BufferedImage imgBuffer;

    // We're not really using the 'pool' aspect, just the deferred execution part
    ScheduledThreadPoolExecutor updateHandler = new ScheduledThreadPoolExecutor(1);
        
	private void updateAll()
	{
        System.out.println("*** updateAll");
        
        // skip if more than 2 events to process in queue i.e. 1 in process and 1 waiting. Just checking for 1 could miss an update which is 
        // nearly complete
        if (updateHandler.getQueue().size() > 2 )    { return; }
        
        updateHandler.schedule(new Callable()
            {
                public Object call() throws Exception
                {
                    System.out.println("*** update Scheduled");
                    updateScreenShot();
                    System.out.println("*** update done *** ");
                    return null;
                }
            
            }, 200, TimeUnit.MILLISECONDS);
	}
    
    
    private int[] updateImage()
    {
        return screen.getScreenPixels();
    }

    
    private boolean updateScreenShot()
    {
        boolean changed = false;
        int[] oldPixels = pixelArray;
        int[] newPixels;

        // Messy, but change detection needs it
        synchronized (this)
        {
            newPixels = updateImage();

            for (int ix = 0; ix < newPixels.length && !changed; ix++)
            {
                if (newPixels[ix] != oldPixels[ix])
                {
                    changed = true;
                }
            }
            
            pixelArray = newPixels;
        }
        
        if (changed)
        {
            System.out.println("screen changed, new pixels: " + newPixels.length);            
            queue.takeSnapshot(this);
        }
        
        return changed;
    }
    
    
	//
	// PixelsOwner
	//
	private int[] pixelArray = null;
    
    private void initPixels()
    {
        //System.out.println("component w:" + getPixelWidth() + ", h:" + getPixelHeight());
        this.pixelArray = new int[getPixelWidth() * getPixelHeight()];
        imgBuffer = new BufferedImage(getPixelWidth(), getPixelHeight(), BufferedImage.TYPE_INT_ARGB);
    }
    
	public int[] getPixels()
	{
        return pixelArray;
	}

	public void setPixelArray( int[] pixelArray, int pixelWidth, int pixelHeight )
	{
		this.pixelArray = pixelArray;
		//this.width = pixelWidth;
		//this.height = pixelHeight;
	}

	public int getPixelWidth()
	{
		return screen.getScreenBuffer().getWidth();
	}

	public int getPixelHeight()
	{
		return screen.getScreenBuffer().getHeight();
	}    


    //////////////////////////////////////////////////
    // INTERFACE METHODS - ScreenImageListener
    //////////////////////////////////////////////////    
    
    @Override
    public void screenUpdated(Screen imgScrn)
    {
        updateAll();
    }
    
}

