// License: GPL. For details, see LICENSE file.
package com.yellowbkpk.geo.waydetails.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.dialogs.relation.WayConnectionType;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class WayRefTableModel extends AbstractTableModel implements TableModelListener, SelectionChangedListener, DataSetListener {

    /**
     * data of the table model: The list of members and the cached WayConnectionType of each member.
     **/
    private Way way;
    private List<WayConnectionType> connectionType = null;

    private DefaultListSelectionModel listSelectionModel;
    private CopyOnWriteArrayList<INodeRefModelListener> listeners;
    private OsmDataLayer layer;

    /**
     * constructor
     */
    public WayRefTableModel(OsmDataLayer layer, Way way) {
        this.way = way;
        listeners = new CopyOnWriteArrayList<INodeRefModelListener>();
        this.layer = layer;
        addTableModelListener(this);
    }

    public OsmDataLayer getLayer() {
        return layer;
    }

    public void register() {
        DataSet.addSelectionListener(this);
        getLayer().data.addDataSetListener(this);
    }

    public void unregister() {
        DataSet.removeSelectionListener(this);
        getLayer().data.removeDataSetListener(this);
    }

    /* --------------------------------------------------------------------------- */
    /* Interface SelectionChangedListener                                          */
    /* --------------------------------------------------------------------------- */
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        if (Main.main.getEditLayer() != this.layer) return;
        // just trigger a repaint
        Collection<Node> sel = getSelectedMembers();
        fireTableDataChanged();
        setSelectedMembers(sel);
    }

    /* --------------------------------------------------------------------------- */
    /* Interface DataSetListener                                                   */
    /* --------------------------------------------------------------------------- */
    public void dataChanged(DataChangedEvent event) {
        // just trigger a repaint - the display name of the relation members may
        // have changed
        Collection<Node> sel = getSelectedMembers();
        fireTableDataChanged();
        setSelectedMembers(sel);
    }

    public void nodeMoved(NodeMovedEvent event) {/* ignore */}
    public void primtivesAdded(PrimitivesAddedEvent event) {/* ignore */}

    public void primtivesRemoved(PrimitivesRemovedEvent event) {
        // ignore - the relation in the editor might become out of sync with the relation
        // in the dataset. We will deal with it when the relation editor is closed or
        // when the changes in the editor are applied.
    }

    public void relationMembersChanged(RelationMembersChangedEvent event) {
        // ignore - the relation in the editor might become out of sync with the relation
        // in the dataset. We will deal with it when the relation editor is closed or
        // when the changes in the editor are applied.
    }

    public void tagsChanged(TagsChangedEvent event) {
        // just refresh the respective table cells
        //
        Collection<Node> sel = getSelectedMembers();
        List<Node> nodes = way.getNodes();
        for (int i=0; i < nodes.size();i++) {
            if (nodes.get(i) == event.getPrimitive()) {
                fireTableCellUpdated(i, 1 /* the column with the primitive name */);
            }
        }
        setSelectedMembers(sel);
    }

    public void wayNodesChanged(WayNodesChangedEvent event) {/* ignore */}

    public void otherDatasetChange(AbstractDatasetChangedEvent event) {/* ignore */}
    /* --------------------------------------------------------------------------- */

    public void addMemberModelListener(INodeRefModelListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public void removeMemberModelListener(INodeRefModelListener listener) {
        listeners.remove(listener);
    }

    protected void fireMakeMemberVisible(int index) {
        for (INodeRefModelListener listener : listeners) {
            listener.makeMemberVisible(index);
        }
    }

    public int getColumnCount() {
        return 1;
    }

    public int getRowCount() {
        return way.getNodesCount();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
        case 0:
            return way.getNode(rowIndex).getId();
        }
        // should not happen
        return null;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        Node member = way.getNode(rowIndex);
    }

    public Node getReferredPrimitive(int idx) {
        return way.getNode(idx);
    }

    public void moveUp(int[] selectedRows) {
        if (!canMoveUp(selectedRows))
            return;

        List<Node> nodes = way.getNodes();
        for (int row : selectedRows) {
            Node member1 = nodes.get(row);
            Node member2 = nodes.get(row - 1);
            nodes.set(row, member2);
            nodes.set(row - 1, member1);
        }
        way.setNodes(nodes);
        fireTableDataChanged();
        getSelectionModel().setValueIsAdjusting(true);
        getSelectionModel().clearSelection();
        for (int row : selectedRows) {
            row--;
            getSelectionModel().addSelectionInterval(row, row);
        }
        getSelectionModel().setValueIsAdjusting(false);
        fireMakeMemberVisible(selectedRows[0] - 1);
    }

    public void moveDown(int[] selectedRows) {
        if (!canMoveDown(selectedRows))
            return;

        List<Node> nodes = way.getNodes();
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            int row = selectedRows[i];
            Node member1 = nodes.get(row);
            Node member2 = nodes.get(row + 1);
            nodes.set(row, member2);
            nodes.set(row + 1, member1);
        }
        way.setNodes(nodes);
        fireTableDataChanged();
        getSelectionModel();
        getSelectionModel().setValueIsAdjusting(true);
        getSelectionModel().clearSelection();
        for (int row : selectedRows) {
            row++;
            getSelectionModel().addSelectionInterval(row, row);
        }
        getSelectionModel().setValueIsAdjusting(false);
        fireMakeMemberVisible(selectedRows[0] + 1);
    }

    public void remove(int[] selectedRows) {
        if (!canRemove(selectedRows))
            return;
        int offset = 0;
        List<Node> nodes = way.getNodes();
        for (int row : selectedRows) {
            row -= offset;
            nodes.remove(row);
            offset++;
        }
        way.setNodes(nodes);
        fireTableDataChanged();
    }

    public boolean canMoveUp(int[] rows) {
        if (rows == null || rows.length == 0)
            return false;
        Arrays.sort(rows);
        return rows[0] > 0 && way.getNodes().size() > 0;
    }

    public boolean canMoveDown(int[] rows) {
        if (rows == null || rows.length == 0)
            return false;
        Arrays.sort(rows);
        return way.getNodes().size() > 0 && rows[rows.length - 1] < way.getNodes().size() - 1;
    }

    public boolean canRemove(int[] rows) {
        if (rows == null || rows.length == 0)
            return false;
        return true;
    }

    public DefaultListSelectionModel getSelectionModel() {
        if (listSelectionModel == null) {
            listSelectionModel = new DefaultListSelectionModel();
            listSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
        return listSelectionModel;
    }

    protected List<Integer> getSelectedIndices() {
        ArrayList<Integer> selectedIndices = new ArrayList<Integer>();
        for (int i = 0; i < way.getNodes().size(); i++) {
            if (getSelectionModel().isSelectedIndex(i)) {
                selectedIndices.add(i);
            }
        }
        return selectedIndices;
    }

    private void addMembersAtIndex(List<Node> primitives, int index) {
        if (primitives == null)
            return;
        int idx = index;
        List<Node> nodes = way.getNodes();
        for (Node primitive : primitives) {
            nodes.set(idx++, primitive);
        }
        way.setNodes(nodes);
        fireTableDataChanged();
        getSelectionModel().clearSelection();
        getSelectionModel().addSelectionInterval(index, index + primitives.size() - 1);
//        fireMakeMemberVisible(index);
    }

    public void addMembersAtBeginning(List<Node> primitives) {
        addMembersAtIndex(primitives, 0);
    }

    public void addMembersAtEnd(List<Node> primitives) {
        addMembersAtIndex(primitives, way.getNodesCount());
    }

    public void addMembersBeforeIdx(List<Node> primitives, int idx) {
        addMembersAtIndex(primitives, idx);
    }

    public void addMembersAfterIdx(List<Node> primitives, int idx) {
        addMembersAtIndex(primitives, idx + 1);
    }

    /**
     * Get the currently selected relation members
     *
     * @return a collection with the currently selected relation members
     */
    public Collection<Node> getSelectedMembers() {
        ArrayList<Node> selectedMembers = new ArrayList<Node>();
        for (int i : getSelectedIndices()) {
            selectedMembers.add(way.getNode(i));
        }
        return selectedMembers;
    }

    /**
     * Replies the set of selected referers. Never null, but may be empty.
     *
     * @return the set of selected referers
     */
    public Collection<OsmPrimitive> getSelectedChildPrimitives() {
        Collection<OsmPrimitive> ret = new ArrayList<OsmPrimitive>(getSelectedMembers());
        return ret;
    }

    /**
     * Replies the set of selected referers. Never null, but may be empty.
     *
     * @return the set of selected referers
     */
    public Set<OsmPrimitive> getChildPrimitives(Collection<? extends OsmPrimitive> referenceSet) {
        HashSet<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
        if (referenceSet == null) return null;
        for (Node m: way.getNodes()) {
            if (referenceSet.contains(m)) {
                ret.add(m);
            }
        }
        return ret;
    }

    /**
     * Selects the members in the collection selectedMembers
     *
     * @param selectedMembers the collection of selected members
     */
    public void setSelectedMembers(Collection<Node> selectedMembers) {
        if (selectedMembers == null || selectedMembers.isEmpty()) {
            getSelectionModel().clearSelection();
            return;
        }

        // lookup the indices for the respective members
        //
        Set<Integer> selectedIndices = new HashSet<Integer>();
        for (Node member : selectedMembers) {
            int idx = way.getNodes().indexOf(member);
            if ( idx >= 0) {
                selectedIndices.add(idx);
            }
        }
        setSelectedMembersIdx(selectedIndices);
    }

    /**
     * Selects the members in the collection selectedIndices
     *
     * @param selectedIndices the collection of selected member indices
     */
    public void setSelectedMembersIdx(Collection<Integer> selectedIndices) {
        if (selectedIndices == null || selectedIndices.isEmpty()) {
            getSelectionModel().clearSelection();
            return;
        }
        // select the members
        //
        getSelectionModel().setValueIsAdjusting(true);
        getSelectionModel().clearSelection();
        for (int row : selectedIndices) {
            getSelectionModel().addSelectionInterval(row, row);
        }
        getSelectionModel().setValueIsAdjusting(false);
        // make the first selected member visible
        //
        if (selectedIndices.size() > 0) {
            fireMakeMemberVisible(Collections.min(selectedIndices));
        }
    }

    /**
     * Replies true if <code>primitive</code> is currently selected in the layer this
     * model is attached to
     *
     * @param primitive the primitive
     * @return true if <code>primitive</code> is currently selected in the layer this
     * model is attached to, false otherwise
     */
    public boolean isInJosmSelection(OsmPrimitive primitive) {
        return layer.data.isSelected(primitive);
    }

    /**
     * Replies true if the layer this model belongs to is equal to the active
     * layer
     *
     * @return true if the layer this model belongs to is equal to the active
     * layer
     */
    protected boolean isActiveLayer() {
        if (Main.map == null || Main.map.mapView == null) return false;
        return Main.map.mapView.getActiveLayer() == layer;
    }

    /**
     * get a node we can link against when sorting members
     * @param element the element we want to link against
     * @param linked_element already linked against element
     * @return the unlinked node if element is a way, the node itself if element is a node, null otherwise
     */
    private static Node getUnusedNode(RelationMember element, RelationMember linked_element)
    {
        Node result = null;

        if (element.isWay()) {
            Way w = element.getWay();
            if (linked_element.isWay()) {
                Way x = linked_element.getWay();
                if ((w.firstNode() == x.firstNode()) || (w.firstNode() == x.lastNode())) {
                    result = w.lastNode();
                } else {
                    result = w.firstNode();
                }
            } else if (linked_element.isNode()) {
                Node m = linked_element.getNode();
                if (w.firstNode() == m) {
                    result = w.lastNode();
                } else {
                    result = w.firstNode();
                }
            }
        } else if (element.isNode()) {
            Node n = element.getNode();
            result = n;
        }

        return result;
    }


    public void tableChanged(TableModelEvent e) {
        connectionType = null;
    }

    /**
     * Reverse the relation members.
     */
    void reverse() {
        List<Integer> selectedIndices = getSelectedIndices();
        List<Integer> selectedIndicesReversed = getSelectedIndices();

        if (selectedIndices.size() <= 1) {
            List<Node> nodes = way.getNodes();
            Collections.reverse(nodes);
            way.setNodes(nodes);
            fireTableDataChanged();
//            setSelectedMembers(way);
        } else {
            Collections.reverse(selectedIndicesReversed);

            ArrayList<Node> newMembers = new ArrayList<Node>(way.getNodes());

            for (int i=0; i < selectedIndices.size(); i++) {
                newMembers.set(selectedIndices.get(i), way.getNode(selectedIndicesReversed.get(i)));
            }

            if (way.getNodesCount() != newMembers.size()) throw new AssertionError();
            List<Node> nodes = way.getNodes();
            nodes.clear();
            nodes.addAll(newMembers);
            way.setNodes(nodes);
            fireTableDataChanged();
            setSelectedMembersIdx(selectedIndices);
        }
    }

}
