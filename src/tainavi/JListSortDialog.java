package tainavi;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.table.DefaultTableModel;


public class JListSortDialog extends JEscCancelDialog {

	private static final long serialVersionUID = 1L;

	private boolean reg = false;
	
	private ArrayList<String> rowData = null;

	private JPanel jpan = null;
	private JButton jbtn_update = null;
	private JButton jbtn_cancel = null;
	private JButton jbtn_sort = null;
	private JButton jbtn_remove = null;
	private JButton jbtn_up = null;
	private JButton jbtn_down = null;
	
	private JScrollPane jscr_entries = null;
	private JNETable jtbl_entries = null;
	
	/*
	 *  コンストラクタ
	 */
	
	public JListSortDialog(String wTitle, ArrayList<String> oList) {
		
		super();

		rowData = oList;
		
		this.setModal(true);
		this.setTitle(wTitle);
		this.setContentPane(getJPan());
		
		// タイトルバーの高さも考慮する必要がある
		Dimension d = jpan.getPreferredSize();
		this.pack();
		this.setPreferredSize(new Dimension(
				d.width+(this.getInsets().left+this.getInsets().right),
				d.height+(this.getInsets().top+this.getInsets().bottom)));
		this.setResizable(false);
	}
	
	/*
	 * 公開メソッド
	 */
	
	public boolean isRegistered() { return reg; } 

	/*
	 * 非公開メソッド
	 */

	private JPanel getJPan() {
		if (jpan == null) {
			
			jpan = new JPanel();
			jpan.setLayout(new SpringLayout());
			
			//
			int y = 10;
			CommonSwingUtils.putComponentOn(jpan, getJScr_entries(), 400, 500, 10, y);
			
			CommonSwingUtils.putComponentOn(jpan, getJBtn_sort("ソート"), 100, 25, 10+400+10, y+500-(10+25)*8);
			
			CommonSwingUtils.putComponentOn(jpan, getJBtn_remove("削除"), 100, 25, 10+400+10, y+500-(10+25)*4);
			
			CommonSwingUtils.putComponentOn(jpan, getJBtn_up("上へ"), 100, 25, 10+400+10, y+500-(10+25)*2);
			CommonSwingUtils.putComponentOn(jpan, getJBtn_down("下へ"), 100, 25, 10+400+10, y+500-(10+25)*1);

			y+=(500+10);
			
			CommonSwingUtils.putComponentOn(jpan, getJBtn_update("更新"), 100, 25, 10+20, y);
			CommonSwingUtils.putComponentOn(jpan, getJBtn_cancel("ｷｬﾝｾﾙ"), 100, 25, 10+400-(100+20), y);
			
			y+=(25+10);
			
			Dimension d = new Dimension(530,y);
			jpan.setPreferredSize(d);
		}
		return jpan;
	}

	// テーブル作成
	private JScrollPane getJScr_entries() {
		if (jscr_entries == null) {
			jscr_entries = new JScrollPane();
			jscr_entries.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			jscr_entries.getVerticalScrollBar().setUnitIncrement(25);
			jscr_entries.setViewportView(getJTbl_entries());
		}
		return jscr_entries;
	}
	private JNETable getJTbl_entries() {
		if (jtbl_entries == null) {
			String[] colname = { "アイテム" };
			DefaultTableModel model = new DefaultTableModel(colname,0);
			jtbl_entries = new JNETable(model,false) {

				private static final long serialVersionUID = 1L;

				@Override
				public Object getValueAt(int row, int column) {
					return rowData.get(row);
				}
				@Override
				public int getRowCount() {
					return rowData.size();
				}
			};
			jtbl_entries.getTableHeader().setReorderingAllowed(false);
			
			// 本来ならコンストラクタで指定できるべきであったが
			jtbl_entries.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		}
		return jtbl_entries;
	}
	
	// ソート
	private JButton getJBtn_sort(String s) {
		if (jbtn_sort == null) {
			jbtn_sort = new JButton(s);
			jbtn_sort.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					ArrayList<String> tmpData = new ArrayList<String>();
					for ( String data : rowData ) {
						int index = 0;
						for ( ; index<tmpData.size(); index++ ) {
							String tmp = tmpData.get(index);
							if ( tmp.compareTo(data) > 0 ) {
								break;
							}
						}
						tmpData.add(index,data);
					}
					
					rowData.clear();
					for ( String tmp : tmpData ) {
						rowData.add(tmp);
					}
					
					((DefaultTableModel) jtbl_entries.getModel()).fireTableDataChanged();
				}
			});
		}
		return jbtn_sort;
	}
	
	// 削除
	private JButton getJBtn_remove(String s) {
		if (jbtn_remove == null) {
			jbtn_remove = new JButton(s);
			jbtn_remove.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int vrow = jtbl_entries.getSelectedRow();
					for ( int i=0; i<jtbl_entries.getSelectedRowCount(); i++ ) {
						rowData.remove(vrow);
					}
					((DefaultTableModel) jtbl_entries.getModel()).fireTableDataChanged();
				}
			});
		}
		return jbtn_remove;
	}
	
	// 上へ・下へ
	private JButton getJBtn_up(String s) {
		if (jbtn_up == null) {
			jbtn_up = new JButton(s);
			jbtn_up.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int vrow = jtbl_entries.getSelectedRow();
					int cnt = jtbl_entries.getSelectedRowCount();
					if (vrow >= 1) {
						String a = rowData.get(vrow-1);
						for ( int i=0; i<cnt; i++ ) {
							rowData.set(vrow+i-1,rowData.get(vrow+i));
						}
						rowData.set(vrow+cnt-1,a);
						((DefaultTableModel) jtbl_entries.getModel()).fireTableDataChanged();
						jtbl_entries.setRowSelectionInterval(vrow-1, vrow-1+(cnt-1));
					}
				}
			});
		}
		return jbtn_up;
	}
	private JButton getJBtn_down(String s) {
		if (jbtn_down == null) {
			jbtn_down = new JButton(s);
			jbtn_down.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int vrow = jtbl_entries.getSelectedRow();
					int cnt = jtbl_entries.getSelectedRowCount();
					if ((vrow+cnt-1) <= jtbl_entries.getRowCount()-2) {
						String a = rowData.get(vrow+cnt);
						for ( int i=cnt-1; i>=0; i-- ) {
							rowData.set(vrow+i+1,rowData.get(vrow+i));
						}
						rowData.set(vrow,a);
						((DefaultTableModel) jtbl_entries.getModel()).fireTableDataChanged();
						jtbl_entries.setRowSelectionInterval(vrow+1, vrow+1+(cnt-1));
					}
				}
			});
		}
		return jbtn_down;
	}
	
	//
	private JButton getJBtn_update(String s) {
		if (jbtn_update == null) {
			jbtn_update = new JButton(s);
			jbtn_update.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					reg = true;
					dispose();
				}
			});
		}
		return jbtn_update;
	}
	private JButton getJBtn_cancel(String s) {
		if (jbtn_cancel == null) {
			jbtn_cancel = new JButton(s);
			jbtn_cancel.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					doCancel();
				}
			});
		}
		return jbtn_cancel;
	}
	
	@Override
	protected void doCancel() {
		reg = false;
		dispose();
	}
	
}
