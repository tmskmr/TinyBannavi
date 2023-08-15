package tainavi;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

public class VWTreeCellRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1L;

	private VWListedTreeNode tnode = null;
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean selected, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		
		Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		// 'instanceof'は使っていいものやらわるいものやら
		tnode = value instanceof VWListedTreeNode ? (VWListedTreeNode) value : null;
		return c;
	}
	
	@Override
	public Color getForeground() {
		if ( tnode != null && tnode.isUnUsed() ) {
			return Color.RED;
		}
		return super.getForeground();
	}

}
