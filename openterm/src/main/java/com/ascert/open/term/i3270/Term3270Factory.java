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
package com.ascert.open.term.i3270;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import com.ascert.open.term.core.Terminal;
import com.ascert.open.term.core.TerminalFactory;
import com.ascert.open.term.gui.SpringUtilities;

/**
 *
 * @version 1,0 31-May-2017
 * @author srm
 * @history 31-May-2017 srm Created
 */
public class Term3270Factory implements TerminalFactory
{

    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////
    private static final Logger log = Logger.getLogger(Term3270Factory.class.getName());

    private static String[] termTypes = new String[]
    {
        "IBM-3278", "IBM-3279",
    };

    private static Set<String> setTermModels;

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    // Convenience static method for uses where we don't want to construct the factory
    public static Terminal get3270Terminal(String termType)
    {
        if (!isSupported3270Model(termType))
        {
            log.severe("Unrecognised term type: " + termType);
            return null;
        }

        return new Term3270(termType);
    }

    // Convenience static method for uses where we don't want to construct the factory
    public synchronized static boolean isSupported3270Model(String termType)
    {
        if (setTermModels == null)
        {
            // Lazy init
            setTermModels = new HashSet<>();
            for (String type : termTypes)
            {
                for (int ix = 2; ix <= 5; ix++)
                {
                    setTermModels.add(type + "-" + ix);
                    setTermModels.add(type + "-" + ix + "-E");
                }
            }
        }

        return setTermModels.contains(termType);
    }

    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // INTERFACE METHODS - TerminalFactory
    //////////////////////////////////////////////////
    @Override
    public String[] getTerminalTypes()
    {
        return termTypes;
    }

    @Override
    public synchronized Component getOptionsPanel(String termType)
    {
        return new OptsPanel3270(termType);
    }

    public boolean isSupported(String termType)
    {
        return isSupported3270Model(termType);
    }

    public Terminal getTerminal(String termType)
    {
        return get3270Terminal(termType);
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
    
    public class OptsPanel3270 extends JPanel
    {

        String termType;
        JComboBox modelCombo;
        JCheckBox extAttr3270;

        public OptsPanel3270(String termType)
        {
            super();
            this.termType = termType;
            setLayout(new SpringLayout());
            
            String[] modelTypes =
            {
                "Model 2 (24x80)",
                "Model 3 (32x80)",
                "Model 4 (43x80)",
                "Model 5 (27x132)"
            };
            
            modelCombo = new JComboBox(modelTypes);
            extAttr3270 = new JCheckBox();
            
            Object[][] flds = new Object[][] { 
                    { "Model:", modelCombo },
                    { "Extended attributes:", extAttr3270 },
                };
        
            for (int ix = 0; ix < flds.length; ix++)
            {
                JLabel lbl = new JLabel(flds[ix][0].toString(), JLabel.TRAILING);
                add(lbl);
                Component c = (Component) flds[ix][1];
                lbl.setLabelFor(c);
                add(c);            
            }
        
            SpringUtilities.makeCompactGrid(this, 
                                        flds.length, 2, //rows, cols
                                        8, 4,        //initX, initY
                                        8, 4);       //xPad, yPad
            

            modelCombo.setSelectedIndex(0);
            extAttr3270.setSelected(true);
        }

        // Return the string encoded form of the terminal type represented by these options
        public String toString()
        {
            return Term3270.formatTermTypeString(termType, modelCombo.getSelectedIndex() + 2, extAttr3270.isSelected());
        }
    }

}
