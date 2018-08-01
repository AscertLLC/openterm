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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import gnu.vnc.Screen;

public class VNCScreenEvents {
    
    /**
     *  for debug-loging (using Log4J or System.err depending on avliability)
     */
    static Object log;
    /**
     *  for debug-loging (using Log4J or System.err depending on avliability)
     */
    static java.lang.reflect.Method logmethod;
    
    /**
     *  for debug-loging (using Log4J or System.err depending on avliability)
     */
    private void logDebug(String msg) {
        try {
            if(log==null)
            {
                log = System.err;
                logmethod = java.io.PrintStream.class.getMethod("println", new Class[]{String.class});
            }
            
            logmethod.invoke(log, new Object[]{msg});
        }
        catch(Exception x) {
            x.printStackTrace();
            System.err.println(msg);
        }
    }
    //
    // Construction
    //
    //char character;
    
    public VNCScreenEvents( Screen screen, RFBClients clients ) {
        this.screen = screen;
        this.clients = clients;
        this.evtComponent = screen instanceof Component ? (Component) screen : new Panel();
        
        // convenience references for later
        if (screen instanceof MouseListener)
        {
            this.mouseListener = (MouseListener) screen;
        }
        
        if (screen instanceof MouseMotionListener)
        {
            this.mouseMotionListener = (MouseMotionListener) screen;
        }
        
        if (screen instanceof MouseWheelListener)
        {
            this.mouseWheelListener = (MouseWheelListener) screen;
        }
    }
    
    //
    // Operations
    //
    
    /**
     * translate the from the RFBClient received KeyEvent and 
     * send it to the fireKeyEvent method. 
     * At the time key is the primaryLevelUnicode key, and the KeyEvent needs the raw code
     * TODO add or use the list from KeyEvent.class to extend the vk-list in keysym.java
     */
    public void translateKeyEvent( RFBClient client, boolean down, int key ) {
        // Get state
        State state = getState( client );

        // Characters - only RETRANSLATE the characters
        char character = (char) keysym.toChar(key);

        // Modifiers
        int newKeyModifiers = keysym.toMask( key );
        if( newKeyModifiers != 0x0 ) {
            if( down )
                state.keyModifiers |= newKeyModifiers;
            else
                state.keyModifiers &= ~newKeyModifiers;
            // do not return, because swing needs also the modify keys as events
            //return;
        }
        
        // check for capital character without pressed shift 
        if ((state.keyModifiers %2)==0 & Character.isUpperCase(character) )
        	state.keyModifiers |= KeyEvent.SHIFT_MASK;
     
        // Virtual Key Code
        int virtualKeyCode = keysym.toVKall( key );

        if (virtualKeyCode == 0x0)
        {
        	virtualKeyCode = key;
        }
        
        if( down ) {
            // Pressed
            fireKeyEvent(client, KeyEvent.KEY_PRESSED, virtualKeyCode, character, state.keyModifiers);
        	// Typed (for character and number keys only)
            if( (key >= 0x0020 & key <= 0x007E) |
            		((key >= 0x00A0) & (key <= 0x00FF)))//  || ( virtualKey == KeyEvent.VK_BACK_SPACE ) )
            {
            	// !!! the virtualKeyCode should stay 0
                fireKeyEvent(client, KeyEvent.KEY_TYPED, 0, character, state.keyModifiers);
            }
        }
        else {
            // Released
            fireKeyEvent(client, KeyEvent.KEY_RELEASED, virtualKeyCode, character, state.keyModifiers);            
        }
    }
    
    public int getButtonNum(int modifier)
    {
        if ((modifier & MouseEvent.BUTTON1_DOWN_MASK) > 0)
        {
            return 0;
        }
        else if ((modifier & MouseEvent.BUTTON2_DOWN_MASK) > 0)
        {
            return 1;
        }
        else if ((modifier & MouseEvent.BUTTON3_DOWN_MASK) > 0)
        {
            return 2;
        }
        
        return -1;
    }
    
    public void translatePointerEvent( RFBClient client, int buttonMask, int x, int y ) {
        // Get state
        State state = getState( client );
        
        // Modifiers
        int newMouseModifiers = 0;
        int wheelRotation = 0;
        boolean pressed = false;

        if( ( buttonMask & rfb.Button1Mask ) != 0 ){
            newMouseModifiers |= MouseEvent.BUTTON1_DOWN_MASK;
        }
        if( ( buttonMask & rfb.Button2Mask ) != 0 ){
            newMouseModifiers |= MouseEvent.BUTTON2_DOWN_MASK;
        }
        if( ( buttonMask & rfb.Button3Mask ) != 0 ){
            newMouseModifiers |= MouseEvent.BUTTON3_DOWN_MASK;
        }
        
        pressed = newMouseModifiers != 0;
        int button = pressed ? getButtonNum(newMouseModifiers) : getButtonNum(state.mouseModifiers);
        
        if ( ( buttonMask & rfb.Button4Mask) != 0 ){
        	// wheel up
        	wheelRotation = -1;
        }
        if ( ( buttonMask & rfb.Button5Mask) != 0 ){
        	// wheel down
        	wheelRotation = 1;
        }
        
        // Wheel Events
        // TODO maybe it is better to avoid firing MOUSE_MOVED on MOUSE_WHEEL 
        if (wheelRotation != 0)
        	fireWheelEvent(client, MouseEvent.MOUSE_WHEEL, x, y, 0, state.keyModifiers | state.mouseModifiers, wheelRotation);
        
        state.dragging = false;
        if( newMouseModifiers == state.mouseModifiers ) {
            // No buttons changed state
            if( newMouseModifiers == 0 ) {
                // Moved (no button pressed)
                fireMouseMotionEvent(client, MouseEvent.MOUSE_MOVED, x, y, 0, state.keyModifiers | state.mouseModifiers, button );
            }
            else {
                // Dragged (button pressed)
                state.dragging = true;
                fireMouseMotionEvent(client, MouseEvent.MOUSE_DRAGGED, x, y, 0, state.keyModifiers | state.mouseModifiers, button );
            }
        }
        else {
            if( pressed == true){
                // Pressed
                //state.mouseModifiers = newMouseModifiers;
                fireMouseEvent(client, MouseEvent.MOUSE_PRESSED, x, y, 1, state.keyModifiers | newMouseModifiers, button );
                // Strictly speaking, CLICKED should be after release, but that complicates this approach to dbl click detection
                //state.lastMouseClickTime[button] = System.currentTimeMillis();
                //fireMouseEvent(client, newComponent, MouseEvent.MOUSE_CLICKED, x, y, numClicks, state.keyModifiers | state.mouseModifiers, button );
            }
            else{
                // Released (old modifiers)
                fireMouseEvent(client, MouseEvent.MOUSE_RELEASED, x, y, 0, state.keyModifiers | state.mouseModifiers, button );
                // Strictly speaking, CLICKED should be after release - need to also fix double click handling here 
                int numClicks = 1;
                
                // if 300ms since last click, doubleclick
                long diff = System.currentTimeMillis()-state.lastMouseClickTime[button];
                if(diff < 1000){
                    numClicks = 2;
                }
                state.lastMouseClickTime[button]= System.currentTimeMillis();
                fireMouseEvent(client, MouseEvent.MOUSE_CLICKED, x, y, numClicks, state.keyModifiers | state.mouseModifiers, button );
            }
            
            // Save modifiers
            state.mouseModifiers = newMouseModifiers;
        }
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////
    // Private
    
    private Component evtComponent;
    private Screen screen;
    private MouseListener mouseListener;
    private MouseMotionListener mouseMotionListener;
    private MouseWheelListener mouseWheelListener;
    
    private RFBClients clients;
    private static HashMap eventMap = new HashMap();
    
    private static class State {
        public int keyModifiers = 0;
        public int mouseModifiers = 0;
        public int oldX;
		public int oldY;
        public boolean dragging = false;
        public long lastMouseClickTime[] = new long[5];
        
    }
    
    // We're not really using the 'pool' aspect, just the deferred execution part
    //TODO - possible optimisation would be to share the events and screen update queue, which would reduce per-session
    //       thread usage count. Revisit if or when becomes an issue
    ScheduledThreadPoolExecutor eventsHandler = new ScheduledThreadPoolExecutor(1);
    
    public static HashMap getEventMap(){
        return eventMap;
    }
    
    private State getState( RFBClient client ) {
        State state = (State) clients.getProperty( client, "events" );
        if( state == null ) {
            state = new State();
            clients.setProperty( client, "events", state );
        }
        
        return state;
    }
    
    /*
     * The function to create the KeyEvent
     * 		TODO: check if there is a need of the location translation
     */
    private void fireKeyEvent(RFBClient client, int id, int vk, char character, int keyModifiers) 
    {
        KeyEvent ke = new KeyEvent( evtComponent, id, System.currentTimeMillis(), keyModifiers, vk, character, KeyEvent.KEY_LOCATION_UNKNOWN );
        logDebug("fireKeyEvent(id="+id+", vk="+vk+" character="+character+" getKeyText="+ke.getKeyText(ke.getKeyCode())+" screen="+screen.getClass().getName());
        //fireEvent(client, ke );
        eventsHandler.execute(new Runnable()
            {
                public void run()
                {
                    screen.processScreenKey(ke);
                }
            });
    }
    
    private void fireMouseEvent(RFBClient client, int id, int x, int y, int clicks, int modifiers, int button ) 
    {
        if (mouseListener == null)  { return; }
        
        //logDebug("fireMouseEvent(id="+id+", btn="+button+", clk="+clicks+", x="+x+", y="+y+" screen=["+screen.getClass().getName()+"]");
        MouseEvent me;
        
        if (button < 0)
        {
            me = new MouseEvent( evtComponent, id, System.currentTimeMillis(), modifiers, x, y, clicks, false );
        }
        else
        {
            me = new MouseEvent( evtComponent, id, System.currentTimeMillis(), modifiers, x, y, clicks, false, button+1 );
        }
        
        eventsHandler.execute(new Runnable()
        {
            public void run()
            {
                switch (me.getID())
                {
                    case MouseEvent.MOUSE_PRESSED:      mouseListener.mousePressed(me);
                                                        break;
                    case MouseEvent.MOUSE_RELEASED:     mouseListener.mouseReleased(me);
                                                        break;
                    case MouseEvent.MOUSE_CLICKED:      mouseListener.mouseClicked(me);
                                                        break;
                }
            }
        });
    }

    
    private void fireMouseMotionEvent(RFBClient client, int id, int x, int y, int clicks, int modifiers, int button ) 
    {
        if (mouseMotionListener == null)        { return; }
        
        //logDebug("fireMouseMotionEvent(id="+id+", btn="+button+", clk="+clicks+", x="+x+", y="+y+" screen=["+screen.getClass().getName()+"]");
        MouseEvent me;
        
        if (button < 0)
        {
            me = new MouseEvent( evtComponent, id, System.currentTimeMillis(), modifiers, x, y, clicks, false );
        }
        else
        {
            me = new MouseEvent( evtComponent, id, System.currentTimeMillis(), modifiers, x, y, clicks, false, button+1 );
        }
        
        eventsHandler.execute(new Runnable()
        {
            public void run()
            {
                switch (me.getID())
                {
                    case MouseEvent.MOUSE_MOVED:        mouseMotionListener.mouseMoved(me);
                                                        break;
                    case MouseEvent.MOUSE_DRAGGED:      mouseMotionListener.mouseDragged(me);
                                                        break;
                }
            }
        });
    }
    
    
    private void fireWheelEvent(RFBClient client, int id, int x, int y, int clicks, int modifiers, int rotation ) 
    {
        if (mouseWheelListener == null)             { return; }
        
        MouseWheelEvent mwe = new MouseWheelEvent(evtComponent, id, System.currentTimeMillis(), modifiers, x, y, clicks, false, 
                                                  MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, rotation);
        
        eventsHandler.execute(new Runnable()
        {
            public void run()
            {
                mouseWheelListener.mouseWheelMoved(mwe);
            }
        });
        
    }
    
}
