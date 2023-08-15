package tainavi;

import javax.swing.tree.DefaultMutableTreeNode;

public class VWListedTreeNode extends DefaultMutableTreeNode {

	private static final long serialVersionUID = 1L;

	private SearchItem item = null;
	
	public final boolean isUsed() { return ! isUnUsed(); }
	public final boolean isUnUsed() { return item != null ? ! item.isMatched() : false; }
	
	public VWListedTreeNode(Object userObject) {
		super(userObject);
		if (userObject instanceof SearchItem) {
			item = (SearchItem) userObject;
		}
	}
}
