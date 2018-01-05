/*
 * Copyright (c) 2016, 2017 Ascert, LLC.
 * www.ascert.com
 *
 * Based on original code from FreeHost3270, copyright for derivations from original works remain:
 *  Copyright (C) 1998, 2001  Art Gillespie
 *  Copyright (2) 2005 the http://FreeHost3270.Sourceforge.net
 *
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

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.*;

import com.ascert.open.term.core.Host;
import com.ascert.open.term.core.TerminalFactory;
import com.ascert.open.term.core.TerminalFactoryRegistrar;

/**
 * Dialog that serves as a widget to specify connection and device settings to the host.
 *
 * @since 0.2
 */
public class NewHostDialog extends JDialog implements ActionListener,
                                                     PropertyChangeListener
{

    private static final Logger log = Logger.getLogger(NewHostDialog.class.getName());

    private static final String okString = "Ok";
    private static final String cancelString = "Cancel";

    // Pretty crude model, but will do for now
    private Vector<TermOptsHandler> termFactoryList = new Vector<>();

    private JComboBox typeCombo;
    private JCheckBox useEncryptionField;
    private JCheckBox addToFavourites;
    private JFormattedTextField portField;
    private JOptionPane optionPane;
    private JTextField hostField;

    private JPanel termOptsPanel;
    private CardLayout cl;

    private Host host = new Host();

    public NewHostDialog()
    {
        this(null, null, null);
    }

    public NewHostDialog(Frame owner, String hostName, Integer portNumber)
    {
        this(null, null, null, false);
    }

    /**
     * Constructs a new edit host connection settings dialog window.
     *
     * @param owner         DOCUMENT ME!
     * @param hostName      default value for host name. If <code>null</code> none is used.
     * @param portNumber    default value for port number. If <code>null</code> none is used.
     * @param useEncryption default state for 'use encryption' check box.
     */
    public NewHostDialog(Frame owner, String hostName, Integer portNumber, boolean useEncryption)
    {
        super(owner, "Connection settings", true);

        for (TerminalFactory factory : TerminalFactoryRegistrar.getTerminalFactories())
        {
            addTerminalFactory(factory);
        }

        hostField = new JTextField(20);
        hostField.setText(hostName != null ? hostName : "");
        portField = new JFormattedTextField(NumberFormat.getIntegerInstance());
        portField.setColumns(4);
        portField.setValue(portNumber != null ? portNumber : 23);
        useEncryptionField = new JCheckBox();
        useEncryptionField.setSelected(useEncryption);
        addToFavourites = new JCheckBox();

        termOptsPanel = new JPanel();
        cl = new CardLayout();
        termOptsPanel.setLayout(cl);

        for (TermOptsHandler toh : termFactoryList)
        {
            termOptsPanel.add(toh.getOptsPanel(), toh.toString());
        }

        typeCombo = new JComboBox(new DefaultComboBoxModel<TermOptsHandler>(termFactoryList));
        typeCombo.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                if (e.getStateChange() == ItemEvent.SELECTED)
                {
                    TermOptsHandler toh = termFactoryList.get(typeCombo.getSelectedIndex());
                    cl.show(termOptsPanel, toh.toString());
                }
            }
        });

        typeCombo.setSelectedIndex(0);

        Object[][] flds = new Object[][] { 
                { "Host:", hostField },
                { "Port:", portField },
                { "Use SSL:", useEncryptionField },
                { "Favourite:", addToFavourites },
                { "Type:", typeCombo },
            };
        
        JPanel optPnl = new JPanel(new SpringLayout());
        for (int ix = 0; ix < flds.length; ix++)
        {
            JLabel lbl = new JLabel(flds[ix][0].toString(), JLabel.TRAILING);
            optPnl.add(lbl);
            Component c = (Component) flds[ix][1];
            lbl.setLabelFor(c);
            optPnl.add(c);            
        }
        
        SpringUtilities.makeCompactGrid(optPnl, 
                                        flds.length, 2, //rows, cols
                                        8, 4,        //initX, initY
                                        8, 4);       //xPad, yPad
        
        Object[] controls =
        {
            optPnl, 
            termOptsPanel
        };

        Object[] options =
        {
            okString, cancelString
        };

        optionPane = new JOptionPane(controls, JOptionPane.PLAIN_MESSAGE,
                                     JOptionPane.YES_NO_OPTION, null, options, options[0]);

        setContentPane(optionPane);

        // Handle window closing event
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent we)
            {
                optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
            }
        });

        // ensure the Host Name field gets the focus
        addComponentListener(new ComponentAdapter()
        {
            public void componentShown(ComponentEvent ce)
            {
                hostField.requestFocusInWindow();
            }
        });

        optionPane.addPropertyChangeListener(this);

        pack();

        // center dialog frame on the desktop (root window)
        Dimension frame_size;

        // center dialog frame on the desktop (root window)
        Dimension screen_size;
        screen_size = Toolkit.getDefaultToolkit().getScreenSize();
        frame_size = this.getSize();

        int offX = (int) frame_size.getWidth();
        int offY = (int) frame_size.getHeight();
        setLocation((screen_size.width - offX) / 2,
                    (screen_size.height - offY) / 2);
    }

    public void actionPerformed(ActionEvent evt)
    {
    }

    /**
     * This method clears the dialog and hides it.
     */
    public void clearAndHide()
    {
        //hostField.setText(null);
        setVisible(false);
    }

    /**
     * Returns host selected name by user.
     *
     * @return DOCUMENT ME!
     */
    public Host getHost()
    {
        return host;
    }

    public void propertyChange(PropertyChangeEvent e)
    {
        String prop = e.getPropertyName();

        if (isVisible() && (e.getSource() == optionPane) && (JOptionPane.VALUE_PROPERTY.equals(prop) || JOptionPane.INPUT_VALUE_PROPERTY.
                                                             equals(prop)))
        {
            Object value = optionPane.getValue();

            if (value == JOptionPane.UNINITIALIZED_VALUE)
            {
                // ignore reset
                return;
            }

            // Reset the JOptionPane's value.  If you don't do this,
            // then if the user presses the same button next time, no
            // property change event will be fired.
            optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

            if (okString.equals(value))
            {
                // we're done; clear and dismiss the dialog
                TermOptsHandler toh = termFactoryList.get(typeCombo.getSelectedIndex());
                toh.setHostOptions(host);
                
                host.setHostName(hostField.getText());
                Number port = (Number) portField.getValue();
                host.setPort(port.intValue());
                host.setEncryption(useEncryptionField.isSelected());
                host.setFavourite(addToFavourites.isSelected());
                
                log.fine(String.format("New host %s", host.toString()));
                clearAndHide();
            }
            else
            {
                // user closed dialog or clicked cancel
                host = null;
                log.warning("Closed or canceled");
                clearAndHide();
            }
        }
    }

    public void addTerminalFactory(TerminalFactory termFactory)
    {
        for (String type : termFactory.getTerminalTypes())
        {
            termFactoryList.add(new TermOptsHandler(type, termFactory));
        }
    }

    public static class TermOptsHandler
    {
        String type;
        TerminalFactory termFactory;
        TermOptions optsPnl;

        public TermOptsHandler(String type, TerminalFactory termFactory)
        {
            this.type = type;
            this.termFactory = termFactory;
            optsPnl = termFactory.getOptionsPanel(type);
        }

        public Component getOptsPanel()
        {
            return (optsPnl != null & optsPnl instanceof Component) ? (Component) optsPnl : new JPanel();
        }

        public void setHostOptions(Host host)
        {
            String pnlType = optsPnl.getTermType();
            host.setTermType(pnlType != null ? pnlType : type);
            host.addProperties(optsPnl.getProperties());
            host.getProperty("t6530.service.choice");
        }

        public String toString()
        {
            return type;
        }

    }
}
