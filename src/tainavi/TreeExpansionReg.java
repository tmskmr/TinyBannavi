package tainavi;

import java.util.ArrayList;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * リスト形式・新聞形式のサイドツリーのノード展開情報を記録します。
 */
public class TreeExpansionReg {

	private static final String MSGID = "[ツリー展開情報] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;
	
	private JTree tree;
	private String fname;
	private ArrayList<String[]> pathlist;
	
	private TreeNode[] getSelectedPathSub(DefaultMutableTreeNode node, String[] names, int idx) {
		if (names.length > idx) {
			for (int i=0; i<node.getChildCount(); i++) {
				if (node.getChildAt(i).toString().equals(names[idx])) {
					TreeNode[] childs = getSelectedPath((DefaultMutableTreeNode)node.getChildAt(i), names, idx+1);
					if (childs == null) {
						// もう子供がいない
						if (idx != 0) {
							return(new TreeNode[] {node.getChildAt(i)});
						}
						else {
							return(new TreeNode[] {node, node.getChildAt(i)});
						}
					}
					else {
						// 子供がいた
						if (idx != 0) {
							TreeNode[] tmp = new TreeNode[childs.length+1];
							tmp[0] = node.getChildAt(i);
							for (int j=0; j<childs.length; j++) {
								tmp[j+1] = childs[j];
							}
							return(tmp);
						}
						else {
							TreeNode[] tmp = new TreeNode[childs.length+2];
							tmp[0] = node;
							tmp[1] = node.getChildAt(i);
							for (int j=0; j<childs.length; j++) {
								tmp[j+2] = childs[j];
							}
							return(tmp);
						}
					}
				}
			}
		}
		return null;
	}
	
	private void reg(DefaultMutableTreeNode node) {
		for ( int i=0; i<node.getChildCount(); i++ ) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
			if ( ! child.isLeaf() ) {
				TreePath path = new TreePath(child.getPath());
				if ( tree.isExpanded(path) ) {
					String[] names = new String[path.getPathCount()];
					int j=0;
					for ( Object o : path.getPath() ) {
						names[j++] = o.toString();
					}
					pathlist.add(names);
				}
				reg(child);
			}
		}
	}

	// リストを取得する
	private TreeNode getChild(TreeModel model, Object parent, String q) {
		//
		for ( int i=model.getChildCount(parent), index=0; index<i; index++ ) {
			TreeNode child = (DefaultMutableTreeNode)model.getChild(parent, index);
			if ( child != null && child.toString().equals(q) ) {
				return child;
			}
		}
		return null;
	}
	private Object[] getChildren(TreeModel model, String[] nodenames) {
		//
		ArrayList<TreeNode> nodes =  new ArrayList<TreeNode>();
		Object parent = model.getRoot();
		nodes.add((TreeNode)parent);
		for ( int i=1; i<nodenames.length; i++ ) {
			TreeNode child = getChild(model,parent,nodenames[i]);
			if ( child == null ) {
				return null;
			}
			nodes.add(child);
			parent = child;
		}
		if ( nodenames.length == nodes.size() ) {
			return nodes.toArray();
		}
		return null;
	}
	
	/*
	 * 公開メソッド
	 */
	
	/**
	 * 展開されているツリーの情報を保存する
	 */
	public void reg() {
		pathlist.clear();
		try {
			reg((DefaultMutableTreeNode) tree.getModel().getRoot());
		}
		catch ( Exception e ) {
			System.err.println(ERRID+"展開情報の記録に失敗しました");
		}
	}
	
	/**
	 * ツリーの再表示のため選択中だったパスを返却する
	 */
	public TreeNode[] getSelectedPath(DefaultMutableTreeNode node, String[] names, int idx) {
		try {
			return getSelectedPathSub(node, names, idx);
		}
		catch ( Exception e ) {
			System.err.println(ERRID+"展開情報の検索に失敗しました");
		}
		return null;
	}
	
	/**
	 * ツリーの展開状態をすべて吐き出すっ
	 */
	public ArrayList<TreePath> get() {
		//
		try{
			ArrayList<TreePath> newpaths = new ArrayList<TreePath>();
			TreeModel model = tree.getModel();
			for ( String[] nodenames : pathlist ) {
				Object[] nodes =  getChildren(model,nodenames);
				if ( nodes != null ) {
					newpaths.add(new TreePath(nodes));
				}
			}
			return newpaths;
		}
		catch ( Exception e ) {
			System.err.println(ERRID+"展開情報の検索に失敗しました");
		}
		return null;
	}

	public void save() {
		System.out.println(MSGID+"展開状態を保存します: "+fname);
		if ( ! CommonUtils.writeXML(fname, pathlist) ) {
			System.err.println(ERRID+"展開情報を保存できませんでした: "+fname);
		}
	}
	
	public void load() {
		System.out.println(MSGID+"展開状態を読み込みます: "+fname);
		@SuppressWarnings("unchecked")
		ArrayList<String[]> nameslist = (ArrayList<String[]>) CommonUtils.readXML(fname);
		if ( nameslist == null ) {
			System.err.println(ERRID+"展開情報を取得できませんでした: "+fname);
			return;
		}
		pathlist = nameslist;
	}
	
	/**
	 *  コンストラクタ
	 */
	public TreeExpansionReg(JTree tree, String fname) {
		this.tree = tree;
		this.fname = fname;
		this.pathlist = new ArrayList<String[]>();
	}
}
