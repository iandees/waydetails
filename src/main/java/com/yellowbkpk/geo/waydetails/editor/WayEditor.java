package com.yellowbkpk.geo.waydetails.editor;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

public class WayEditor extends ExtendedDialog {
    private static final long serialVersionUID = 1L;

    private OsmDataLayer layer;
    private Way way;
    private Way waySnapshot;

    /** the member table */
    private WayRefTable memberTable;
    private WayRefTableModel memberTableModel;

    public WayEditor(OsmDataLayer layer, Way way) {
        super(Main.parent,
            "",
            new String[] { tr("Apply Changes"), tr("Cancel")},
            false,
            false
        );
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        this.layer = layer;
        setWay(way);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildWayMemberPanel(), BorderLayout.CENTER);
        getContentPane().add(buildManipulationToolbar(), BorderLayout.NORTH);
        getContentPane().add(buildOkCancelButtonPanel(), BorderLayout.SOUTH);

        setSize(findMaxDialogSize());
    }

    protected void setWay(Way way) {
        setWaySnapshot((way == null) ? null : new Way(way));
        this.way = way;
        updateTitle();
    }
    
    protected Way getWay() {
        return way;
    }

    protected OsmDataLayer getLayer() {
        return layer;
    }

    @Override
    protected Dimension findMaxDialogSize() {
        return new Dimension(400, 650);
    }

    protected void updateTitle() {
        setTitle(tr("Edit way #{0} in layer ''{1}''", way.getId(), layer.getName()));
    }

    protected void setWaySnapshot(Way snapshot) {
        waySnapshot = snapshot;
    }

    private JPanel buildWayMemberPanel() {
        final JPanel pnl = new JPanel(new BorderLayout());
        // setting up the member table
        memberTableModel = new WayRefTableModel(getLayer(), getWay());
        memberTableModel.register();
        memberTable = new WayRefTable(getLayer(),memberTableModel);
//        memberTable.addMouseListener(new MemberTableDblClickAdapter());
        memberTableModel.addMemberModelListener(memberTable);

        final JScrollPane scrollPane = new JScrollPane(memberTable);
        
        pnl.add(scrollPane, BorderLayout.CENTER);
        
        return pnl;
    }

    private JToolBar buildManipulationToolbar() {
        JToolBar tb  = new JToolBar();
        tb.setFloatable(false);

        // -- move up action
        MoveUpAction moveUpAction = new MoveUpAction();
        memberTableModel.getSelectionModel().addListSelectionListener(moveUpAction);
        tb.add(moveUpAction);

        // -- move down action
        MoveDownAction moveDownAction = new MoveDownAction();
        memberTableModel.getSelectionModel().addListSelectionListener(moveDownAction);
        tb.add(moveDownAction);

        // -- delete action
        RemoveAction removeSelectedAction = new RemoveAction();
        memberTable.getSelectionModel().addListSelectionListener(removeSelectedAction);
        tb.add(removeSelectedAction);
        
        return tb;
    }

    protected JPanel buildOkCancelButtonPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.CENTER));

        pnl.add(new SideButton(new ApplyAction()));
        pnl.add(new SideButton(new OKAction()));
        pnl.add(new SideButton(new CancelAction()));
        return pnl;
    }

    class ApplyAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public ApplyAction() {
            putValue(SHORT_DESCRIPTION, tr("Apply the current updates"));
            putValue(SMALL_ICON, ImageProvider.get("save"));
            putValue(NAME, tr("Apply"));
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) {
            Main.main.undoRedo.add(new ChangeCommand(waySnapshot, way));
            setWay(way);
            getLayer().data.fireSelectionChanged();
        }
    }

    class OKAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public OKAction() {
            putValue(SHORT_DESCRIPTION, tr("Apply the updates and close the dialog"));
            putValue(SMALL_ICON, ImageProvider.get("ok"));
            putValue(NAME, tr("OK"));
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) {
            Main.main.undoRedo.add(new ChangeCommand(waySnapshot, way));
            getLayer().data.fireSelectionChanged();
            setVisible(false);
        }
    }

    class CancelAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public CancelAction() {
            putValue(SHORT_DESCRIPTION, tr("Cancel the updates and close the dialog"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
            putValue(NAME, tr("Cancel"));

            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
            getRootPane().getActionMap().put("ESCAPE", this);
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }
    
    class MoveUpAction extends AbstractAction implements ListSelectionListener {
        private static final long serialVersionUID = 1L;

        public MoveUpAction() {
            putValue(SHORT_DESCRIPTION, tr("Move the currently selected members up"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "moveup"));
            Shortcut.registerShortcut("wayeditor:moveup", tr("Way Editor: Move Up"), KeyEvent.VK_N,
                    Shortcut.GROUP_MNEMONIC);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            memberTableModel.moveUp(memberTable.getSelectedRows());
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(memberTableModel.canMoveUp(memberTable.getSelectedRows()));
        }
    }

    class MoveDownAction extends AbstractAction implements ListSelectionListener {
        private static final long serialVersionUID = 1L;

        public MoveDownAction() {
            putValue(SHORT_DESCRIPTION, tr("Move the currently selected members down"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "movedown"));
            Shortcut.registerShortcut("wayeditor:moveup", tr("Way Editor: Move Down"), KeyEvent.VK_J,
                    Shortcut.GROUP_MNEMONIC);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            memberTableModel.moveDown(memberTable.getSelectedRows());
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(memberTableModel.canMoveDown(memberTable.getSelectedRows()));
        }
    }

    class RemoveAction extends AbstractAction implements ListSelectionListener {
        private static final long serialVersionUID = 1L;

        public RemoveAction() {
            putValue(SHORT_DESCRIPTION, tr("Remove the currently selected members from this way"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "remove"));
            Shortcut.registerShortcut("wayeditor:remove", tr("Way Editor: Remove"), KeyEvent.VK_J,
                    Shortcut.GROUP_MNEMONIC);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            memberTableModel.remove(memberTable.getSelectedRows());
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(memberTableModel.canRemove(memberTable.getSelectedRows()));
        }
    }
}
