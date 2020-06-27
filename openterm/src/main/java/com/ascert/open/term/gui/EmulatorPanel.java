
/*
 * Copyright (c) 2016, 2017 Ascert, LLC.
 * www.ascert.com
 *
 * Based on original code from FreeHost3270, copyright for derivations from original works remain:
 *  Copyright (C) 1998, 2001  Art Gillespie
 *  Copyright (2) 2005 the http://FreeHost3270.Sourceforge.net
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
package com.ascert.open.term.gui;


import com.ascert.open.term.application.OpenTermConfig;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Event;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.*;

import com.ascert.open.term.core.Host;

import com.ascert.open.term.i3270.Term3270;
import com.ascert.open.term.core.Terminal;
import com.ascert.open.term.core.TermField;
import com.ascert.open.term.core.TerminalFactoryRegistrar;
import com.ascert.open.term.core.InputCharHandler;
import com.ascert.open.term.core.IsProtectedException;


/**
 * Main terminal emulator panel, which includes menu, F-key bar, and actual terminal screen.
 * Created as a JPanel for ease of inclusion inside other windows and embedding within other applications.
 */
//TODO - revisit all the pack and validate calls to see if really needed.
public class EmulatorPanel extends JPanel
    implements ActionListener, FocusListener
{

    private static final Logger log = Logger.getLogger(EmulatorPanel.class.getName());

    /**
     * These font sizes will be presented to the user.
     */
    public static final int[] FONT_SIZES =
    {
        6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 32, 36, 40
    };
    
    public static final String[] COLOR_NAMES =
    {
        "Black", "White", "Green", "Red", "Blue", "Orange", "Turquoise", "Dark Blue", "Light Green"
    };
    
    protected JFrame parentFrame;
    private JMenuBar menubar;
    private JMenu connect;
    private JToolBar toolbar;
    private TransferHandler th;
    private JTerminalScreen rhp;
    private Terminal term;
    private List<Host> available = new ArrayList<>();
    private boolean autoConnect = true;
    private boolean embeddedUse = false;
    private String productName;

    // Allows a view-only type mode, such as when being controlled by an external program
    // Although the keyboard and mouse input are disabled, characters and keys can still be injected
    // by a controlling program
    protected boolean interactionAllowed = true;
    private boolean serverMode;
    
    /**
     * No-ops constructor. Asks users to enter the connection settings in the corresponding dialog box then proceeds as normal.
     */
    public EmulatorPanel()
    {
        this(new ArrayList<Host> ());
    }

    /** 
     * Single host constructor, typical used for terminal server mode
     * 
     * @param host
     * @param server 
     */
    public EmulatorPanel(Host host, boolean serverMode)
    {
        super();
        this.serverMode = serverMode;
        this.available.add(host);
        init();
    }
    
    
    public EmulatorPanel(List<Host> hosts)
    {
        super();
        
        if (hosts != null)
        {
            this.available.addAll(hosts);
        }
        
        init();
    }

    // quick hack to allow a sort of "slave" mode that can be fed with data from outside
    public EmulatorPanel(Terminal term, boolean noConnection)
    {
        this.term = term;
        this.autoConnect = !noConnection;
        init();
    }

    // enabling embedded use ensures we don't try and exit the VM on window close
    public void setEmbeddedUse(boolean embeddedUse)
    {
        this.embeddedUse = embeddedUse;
    }

    /**
     * @return the interactionAllowed
     */
    public boolean isInteractionAllowed()
    {
        return interactionAllowed;
    }

    /**
     * @param kbdEnabled the interactionAllowed to set
     */
    public void setInteractionAllowed(boolean interactionAllowed)
    {
        this.interactionAllowed = interactionAllowed;
        refreshInteractionHandling();
    }
    
    
    /**
     * @return the parentFrame
     */
    public JFrame getParentFrame()
    {
        return parentFrame;
    }

    /**
     * @param parentFrame the parentFrame to set
     */
    public void setParentFrame(JFrame parentFrame)
    {
        this.parentFrame = parentFrame;
    }

    
    public void actionPerformed(ActionEvent evt)
    {
        log.fine("dispatching action event");
    }

    public void disconnect()
    {
        if (term != null)
        {
            term.disconnect();
        }
    }

    /**
     * Shuts the application down.
     */
    public void exit()
    {
        exit(!embeddedUse);
    }

    public void exit(boolean exitVm)
    {
        if (exitVm)
        {
            //If a disconnected option is needed in embedded cases, it can be done manually
            if (term != null)
            {
                term.disconnect();
            }
            System.exit(0);
        }
        
        if (getParentFrame() != null)
        {
            getParentFrame().dispose();
        }
    }

    public void focusGained(FocusEvent evt)
    {
        rhp.requestFocus();
    }

    public void focusLost(FocusEvent evt)
    {
    }

    public void processEvent(AWTEvent evt)
    {
        if (evt.getID() == Event.WINDOW_DESTROY)
        {
            exit();
        }

        super.processEvent(evt);
    }


    private void createTerminalScreen() {
        // Not perfect as initially it shows a blank screen, but at least it can be made of an expected type
        // Could be nicer to show something more "neutral"
        if (term == null && OpenTermConfig.getProp("startup.type") != null)
        {
            try
            {
                term = TerminalFactoryRegistrar.createTerminal(OpenTermConfig.getProp("startup.type").trim());
            }
            catch (Exception ex)
            {
                log.log(Level.WARNING, "Failed to create startup terminal type", ex);
            }
        }

        rhp = new JTerminalScreen(term != null ? term : new Term3270(), toolbar);
    }

    /**
     * Builds main menu. Constructs several menu items.
     */
    private void buildMainMenu()
    {
        JPanel pnlTools = new JPanel();
        pnlTools.setLayout(new BorderLayout());
        add("North", pnlTools);
        
        menubar = new JMenuBar();
        //setJMenuBar(menubar);
        pnlTools.add("North", menubar);
        
        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setVisible("true".equals(OpenTermConfig.getProp("toolbar.fkey.enabled")));
        pnlTools.add("Center", toolbar);
        
        /* Make sure that the menu does not trap F10, since we use it as PF10 */
        //TODO - probably want to do this also for Alt-F4 which acts as exit?
        InputMap inputMap = menubar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke("F10"), "none");

        JMenu file = new JMenu("Terminal");
        file.setMnemonic(KeyEvent.VK_T);

        AbstractAction prtAct = new AbstractAction("Print")
        {
            public void actionPerformed(ActionEvent evt)
            {
                rhp.printScreen();
            }
        };
        JMenuItem printItem = file.add(prtAct);
        printItem.setAccelerator(KeyStroke.getKeyStroke("alt P"));
        printItem.setMnemonic(KeyEvent.VK_P);

        file.addSeparator();

        file.add(new AbstractAction("Exit")
        {
            public void actionPerformed(ActionEvent evt)
            {
                exit();
            }
        });
        menubar.add(file);

        this.th = createTransferHandler();
        if (th != null)
        {
            JMenu edit = new JMenu("Edit");
            edit.setMnemonic(KeyEvent.VK_E);

            int items = 0;
            int srcActs = th.getSourceActions(this);

            if (srcActs == TransferHandler.COPY_OR_MOVE || srcActs == TransferHandler.MOVE)
            {
                JMenuItem cutItem = edit.add(TransferHandler.getCutAction());
                cutItem.setText("Cut");
                cutItem.setAccelerator(KeyStroke.getKeyStroke("alt X"));
                cutItem.setMnemonic(KeyEvent.VK_T);
                cutItem.setTransferHandler(th);
                items++;
            }

            if (srcActs == TransferHandler.COPY_OR_MOVE || srcActs == TransferHandler.COPY)
            {
                JMenuItem copyItem = edit.add(TransferHandler.getCopyAction());
                copyItem.setText("Copy");
                copyItem.setAccelerator(KeyStroke.getKeyStroke("alt C"));
                copyItem.setMnemonic(KeyEvent.VK_C);
                copyItem.setTransferHandler(th);
                items++;
            }

            // A bit of a bogus way to determine if the transfer handler
            // supports paste at all (e.g. maybe it only supports non-string
            // paste), but sufficient for the moment.
            if (th.canImport(new TransferHandler.TransferSupport(this, new StringSelection("")))) {
                JMenuItem pasteItem = edit.add(TransferHandler.getPasteAction());
                pasteItem.setText("Paste");
                pasteItem.setAccelerator(KeyStroke.getKeyStroke("alt V"));
                pasteItem.setMnemonic(KeyEvent.VK_P);
                pasteItem.setTransferHandler(th);
                items++;
            }

            if (items > 0)
            {
                edit.addSeparator();
            }

            AbstractAction selAllAct = new AbstractAction("Select All")
            {
                public void actionPerformed(ActionEvent evt)
                {
                    rhp.selectAll();
                }
            };
            JMenuItem selAllItem = edit.add(selAllAct);
            selAllItem.setAccelerator(KeyStroke.getKeyStroke("alt A"));
            selAllItem.setMnemonic(KeyEvent.VK_A);

            menubar.add(edit);
        }

        connect = new JMenu("Connect");
        connect.setMnemonic(KeyEvent.VK_N);
        initHostsMenu();
        menubar.add(connect);

        JMenu options = new JMenu("Options");
        options.setMnemonic(KeyEvent.VK_O);

        JMenu fonts = new JMenu("Font Size");
        ButtonGroup fontsGroup = new ButtonGroup();

        int currFontSize = rhp.getFontSize();
        
        for (int i = 0; i < FONT_SIZES.length; i++)
        {
            int size = FONT_SIZES[i];

            JRadioButtonMenuItem sizeItem = new JRadioButtonMenuItem(new AbstractAction(Integer.toString(size))
            {
                public void actionPerformed(ActionEvent evt)
                {
                    int size = Integer.parseInt(evt.getActionCommand());
                    fontSize((float) size);
                }
            });

            if (size == currFontSize)
            {
                sizeItem.setSelected(true);
            }

            fonts.add(sizeItem);
            fontsGroup.add(sizeItem);
        }

        options.add(fonts);

        JMenu fontcolor = new JMenu("Font Color");
        
        JMenu dfFontColor = getColorMenu("Default Font", JTerminalScreen.getFgColor(), new AbstractAction()
            {
                public void actionPerformed(ActionEvent evt)
                {
                    String name = evt.getActionCommand();
                    rhp.setForegroundColor(name);
                }
            });
        fontcolor.add(dfFontColor);

        JMenu bldFontColor = getColorMenu("Bold Font", JTerminalScreen.getBoldColor(), new AbstractAction()
            {
                public void actionPerformed(ActionEvent evt)
                {
                    String name = evt.getActionCommand();
                    rhp.setBoldColor(name);
                }
            });
        fontcolor.add(bldFontColor);
        options.add(fontcolor);
        options.addSeparator();

        JMenu bgcolor = getColorMenu("Background Color", JTerminalScreen.getBgColor(), new AbstractAction()
            {
                public void actionPerformed(ActionEvent evt)
                {
                    String name = evt.getActionCommand();
                    rhp.setBackgroundColor(name);
                }
            });
        options.add(bgcolor);
        options.addSeparator();
        
        JCheckBoxMenuItem optFkeyBar = new JCheckBoxMenuItem("Show F-Key bar");
        optFkeyBar.setSelected("true".equals(OpenTermConfig.getProp("toolbar.fkey.enabled")));
        optFkeyBar.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                OpenTermConfig.setProp("toolbar.fkey.enabled", Boolean.toString(optFkeyBar.isSelected()));
                //rhp.refresh();
                toolbar.setVisible(optFkeyBar.isSelected());                
                pack();
            }
        });
        options.add(optFkeyBar);

        options.addSeparator();
        options.add(new AbstractAction("Clear Saved Preferences")
            {
                public void actionPerformed(ActionEvent evt)
                {
                    int n = JOptionPane.showConfirmDialog(null,
                                                          "Are you sure you want to clear all saved preferences?",
                                                          "Clear Preferences", JOptionPane.YES_NO_OPTION);   
                    if (n == JOptionPane.YES_OPTION)
                    {
                        OpenTermConfig.clearPrefs();
                    }
                }
            });
        
        
        
        menubar.add(options);

        JMenu about = new JMenu("Help");
        about.setMnemonic(KeyEvent.VK_H);
        menubar.add(about);
        about.add(new AbstractAction("About")
        {
            public void actionPerformed(ActionEvent evt)
            {
                AboutDialog about = new AboutDialog(null, productName);
                about.setVisible(true);
            }
        });

        if (!this.interactionAllowed)
        {
            // Just in case RHP was null when interactions originally disabled
            refreshInteractionHandling();
        }
        
        add("Center", rhp);
    }

    
    private void refreshInteractionHandling()
    {
        if (rhp != null)
        {
            rhp.setKbdEnabled(interactionAllowed);
        }
        
        setMenusEnabled(interactionAllowed);
    }
    
    // Might at a later stage want to only disable certain menus
    private void setMenusEnabled(boolean enabled)
    {
        if (menubar != null)
        {
            for (int ix = 0; ix < menubar.getMenuCount(); ix ++)
            {
                menubar.getMenu(ix).setEnabled(enabled);
            }
        }
    }
    
    private JMenu getColorMenu(String menuText, String selectedItemName, Action act)
    {
        JMenu color = new JMenu(menuText);
        ButtonGroup colorGroup = new ButtonGroup();

        for (int i = 0; i < COLOR_NAMES.length; i++)
        {
            String name = COLOR_NAMES[i];
            JRadioButtonMenuItem colorItem = new JRadioButtonMenuItem(name);
            colorItem.addActionListener(act);

            if (COLOR_NAMES[i].equals(selectedItemName))
            {
                colorItem.setSelected(true);
            }

            color.add(colorItem);
            colorGroup.add(colorItem);
        }
        
        return color;
    }
    
    private void fontSize(float size)
    {
        rhp.setFontSize(size);
        revalidate();
        repaint();
        pack();
    }

    public void init(String host, int port, String type, boolean encryption)
    {
        log.fine("** host " + host);
        available.add(new Host(host, port, type, encryption));
        init();
    }

    /**
     * Performs operations neccessary to construct main application frame.
     *
     * @param host      DOCUMENT ME!
     * @param port      DOCUMENT ME!
     * @param available DOCUMENT ME!
     * @param parentFrame    DOCUMENT ME!
     */
    private void init()
    {
        productName = OpenTermConfig.getProp("product.name", "open.term");
        setTitle(productName);
        setLayout(new BorderLayout());

        createTerminalScreen();
        buildMainMenu();
        validate();
        repaint();
        pack();

        // Connect to the first host in the set of available hosts in
        // case if there is only one host available
        if (available.size() == 1 && autoConnect)
        {
            connect(available.get(0));
        }
        else
        {
            setTitle(productName + " - Not Connected");
        }

        addFocusListener(this);
    }

    protected void initHostsMenu()
    {
        connect.removeAll();
        Iterator<Host> hosts = available.iterator();

        //JMenuItem mnu = new JMenuItem();
        //Font boldFont = menu.getFont().deriveFont(Font.BOLD);
        
        while (hosts.hasNext())
        {
            final Host hst = hosts.next();
            // quick hack for now - tooltips maybe neater
            String menuTxt = hst.getDisplayName();
            connect.add(new AbstractAction(menuTxt)
            {
                public void actionPerformed(ActionEvent evt)
                {
                    disconnect();

                    try
                    {
                        connect(hst);
                    }
                    catch (Exception e)
                    {
                        showConnectionError(e.getMessage());
                    }
                }
            });
        }

        connect.add(new AbstractAction("New ...")
        {
            public void actionPerformed(ActionEvent evt)
            {
                NewHostDialog cfgDialog = new NewHostDialog();
                cfgDialog.setVisible(true);
                final Host newHost = cfgDialog.getHost();
                
                if (newHost != null)
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            disconnect();

                            try
                            {
                                connect(newHost);
                            }
                            catch (Exception e)
                            {
                                showConnectionError(e.getMessage());
                            }
                            
                            // Add to list regardless of connection success
                            if (newHost.isFavourite())
                            {
                                //TODO - could check if already present to avoid duplicates
                                available.add(newHost);
                                OpenTermConfig.setProp("favourite.hosts", Host.getFavouritesAsConfigString(available));
                                initHostsMenu();
                            }
                        }
                    });


                    rhp.requestFocusInWindow();
                }
            }
        });

        connect.addSeparator();
        connect.add(new AbstractAction("Disconnect")
        {
            public void actionPerformed(ActionEvent evt)
            {
                disconnect();
            }
        });
        
        connect.addSeparator();
        connect.add(new AbstractAction("Organize Favourites")
        {
            public void actionPerformed(ActionEvent evt)
            {
                FavouriteHostsPanel pnlHost = new FavouriteHostsPanel(available);
                int res = JOptionPane.showConfirmDialog(null, pnlHost, "Favourite Hosts", 
                                                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                
                if (res == JOptionPane.OK_OPTION)
                {
                    final List<Host> newHosts = pnlHost.getHosts();
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            available.clear();
                            available.addAll(newHosts);
                            OpenTermConfig.setProp("favourite.hosts", Host.getFavouritesAsConfigString(available));
                            initHostsMenu();
                        }
                    });
                }
            }
        });
    }
    
    protected void connect(Host host)
    {
        log.fine("** connect " + host);

        setTitle(productName + " - Connecting to " + host.getDisplayName());

        try
        {
            term = TerminalFactoryRegistrar.createTerminal(host);
            rhp.setTerminal(term);

            repaint();
            pack();

            term.connect();
            requestFocus();
            setTitle(productName + " - Connected to " + host.getDisplayName());
        }
        catch (Exception e)
        {
            showConnectionError(e.getMessage());
        }
    }

    private void showConnectionError(String message)
    {
        if (!serverMode)
        {
            JOptionPane.showMessageDialog(rhp, "Failed to connect to the server:\n" + message,
                                          "Connection failure", JOptionPane.WARNING_MESSAGE);
        }
        else
        {
            log.warning("Failed establish connection to host: " + message);
        }
    }

    //---------------------------------------
    // Bit brain dead - these methods will update any parent JFrame
    public void setTitle(String title)
    {
        if (getParentFrame() != null)
        {
            getParentFrame().setTitle(title);
        }
    }
    
    public void pack()
    {
        if (getParentFrame() != null)
        {
            getParentFrame().pack();
        }
    }

    public JTerminalScreen getTerminalScreen()
    {
        return rhp;
    }

    public TransferHandler getTransferHandler()
    {
        return th;
    }

    protected TransferHandler createTransferHandler()
    {
        return new EmulatorTransferHandler(this);
    }


    protected static class EmulatorTransferHandler extends TransferHandler
    {
        protected EmulatorPanel panel;

        public EmulatorTransferHandler(EmulatorPanel panel)
        {
            this.panel = panel;
        }

        public boolean canImport(TransferHandler.TransferSupport info)
        {
            return info.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        protected Transferable createTransferable(JComponent c)
        {
            JTerminalScreen rhp = panel.getTerminalScreen();
            String data = new String(rhp.getTerm().getDisplay());
            String sel;
            if (rhp.getSelectionMode() == JTerminalScreen.SelectionMode.RECTANGLE)
            {
                int cols = rhp.getTerm().getCols();
                Rectangle selRect = rhp.getSelectionRectangle();
                StringBuilder lines = new StringBuilder((selRect.width+2)*selRect.height);
                if (selRect.x >= 0 && selRect.y >= 0)
                {
                    int pos = selRect.y*cols + selRect.x;
                    for (int row=0; row<selRect.height && pos+selRect.width < data.length(); row++, pos+=cols)
                    {
                        if (row > 0) { lines.append(System.lineSeparator()); }
                        lines.append(data.substring(pos, pos+selRect.width));
                    }
                }
                sel = lines.toString();
            }
            else
            {
                int start = rhp.getSelectionStartPos();
                int end = rhp.getSelectionEndPos();
                sel = (start >= 0 && start < data.length() &&
                       end >= 0 && end < data.length() && start <= end)?
                  data.substring(start, end+1) : "";
            }
            return new StringSelection(sel);
        }

        public int getSourceActions(JComponent c)
        {
            return TransferHandler.COPY_OR_MOVE;
        }

        public boolean importData(TransferHandler.TransferSupport info)
        {
            boolean imported = false;
            try
            {
                JTerminalScreen rhp = panel.getTerminalScreen();
                String data = (String)info.getTransferable().getTransferData(DataFlavor.stringFlavor);
                imported = (data.length() == 0);
                Terminal term = rhp.getTerm();
                InputCharHandler charHandler = term.getCharHandler();
                int rowStart = term.getCursorPosition();
                if (rhp.getSelectionMode() != JTerminalScreen.SelectionMode.RECTANGLE)
                {
                    rowStart = rowStart / term.getCols() * term.getCols();
                }
                for (int c=0; c<data.length(); c++)
                {
                    char ch = data.charAt(c);
                    if (!Character.isISOControl(ch)) {
                        try
                        {
                            int beforePos = term.getCursorPosition();
                            if (!charHandler.type(ch, false))
                            {
                                break;
                            }
                            imported = true;
                            int afterPos = term.getCursorPosition();
                            if (beforePos == afterPos)
                            {
                                break;
                            }
                        }
                        catch (IsProtectedException pe)
                        {
                            break;
                        }
                    }
                    else if (ch == '\n' && term.allowDirectScreenEditing()) {
                        rowStart += term.getCols();
                        term.setCursorPosition(rowStart);
                    }
                }
                term.getClient().refresh();
            }
            catch (Exception e)
            {
                return false;
            }
            return imported;
        }

        protected void exportDone(JComponent c, Transferable data, int action)
        {
            JTerminalScreen rhp = panel.getTerminalScreen();
            if (action == TransferHandler.MOVE)
            {
                Terminal term = rhp.getTerm();
                // Cut characters from terminal
                if (rhp.getSelectionMode() == JTerminalScreen.SelectionMode.RECTANGLE)
                {
                    int cols = rhp.getTerm().getCols();
                    Rectangle selRect = rhp.getSelectionRectangle();
                    if (selRect.x >= 0 && selRect.y >= 0)
                    {
                        int pos = selRect.y*cols + selRect.x;
                        for (int row=0; row<selRect.height; row++, pos+=cols)
                        {
                            for (int col=0; col<selRect.width; col++)
                            {
                                TermField fld = term.getChar(pos+col).getField();
                                if (fld != null && !fld.isProtected())
                                {
                                    term.getChar(pos+col).setChar(term.getEmptyChar());
                                    try { fld.setModified(true); }
                                    catch (IsProtectedException shouldNotHappen) {}
                                }
                            }
                        }
                    }
                }
                else
                {
                    int start = rhp.getSelectionStartPos();
                    int end = rhp.getSelectionEndPos();
                    if (start >= 0 && end >= 0 && start <= end)
                    {
                        for (int ix = start; ix <= end; ix++)
                        {
                            TermField fld = term.getChar(ix).getField();
                            if (fld != null && !fld.isProtected())
                            {
                                term.getChar(ix).setChar(term.getEmptyChar());
                                try { fld.setModified(true); }
                                catch (IsProtectedException shouldNotHappen) {}
                            }
                        }
                    }
                }
                term.getClient().refresh();
            }
            rhp.clearSelection();
        }
    }
}
