package com.yellowbkpk.geo.waydetails;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.JMenuItem;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Shortcut;

public class WayDetailsPlugin extends Plugin {

    private JMenuItem wayDetailsMenuItem;
    private Way selectedWay;

    public WayDetailsPlugin(PluginInformation info) {
        super(info);

        wayDetailsMenuItem = MainMenu.add(Main.main.menu.toolsMenu, new WayDetailsAction());
        wayDetailsMenuItem.setEnabled(false);

        // Register as a listener for selection change
        org.openstreetmap.josm.data.osm.DataSet.addSelectionListener(new SelectionChangedListener() {

            public void selectionChanged(Collection<? extends OsmPrimitive> selection) {
                if (selection.size() == 1 && isAllWays(selection)) {
                    enableWayDetailsMenuItem();
                    selectedWay = (Way) selection.iterator().next();
                } else {
                    disableWayDetailsMenuItem();
                    selectedWay = null;
                }
            }
        });
    }
    
    private final class WayDetailsAction extends JosmAction {
        private static final long serialVersionUID = 1L;
        
        public WayDetailsAction() {
            super(tr("Way details..."),
                    null,
                    tr("Details about the selected way."),
                    Shortcut.registerShortcut("tools:way_details",
                            tr("Tool: {0}", tr("Way Details")),
                            KeyEvent.VK_E,
                            Shortcut.GROUP_EDIT,
                            KeyEvent.CTRL_DOWN_MASK),
                    true);
        }

        public void actionPerformed(ActionEvent e) {
            showDetailsDialogForWay(selectedWay);
        }
        
    }

    private void showDetailsDialogForWay(Way way) {
        System.out.println("Way details for way: " + way);
    }

    private void enableWayDetailsMenuItem() {
        wayDetailsMenuItem.setEnabled(true);
    }

    private void disableWayDetailsMenuItem() {
        wayDetailsMenuItem.setEnabled(false);
    }

    private boolean isAllWays(Collection<? extends OsmPrimitive> selection) {
        if (selection != null) {
            for (OsmPrimitive primitive : selection) {
                if (primitive.getType().equals(OsmPrimitiveType.WAY)) {
                    continue;
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}