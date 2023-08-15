
package remoteCtrl;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Authenticator;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ActionMap;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SpringLayout;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.JComboBox;

import javax.swing.JLabel;

import tainavi.CommonSwingUtils;
import tainavi.CommonUtils;
import tainavi.RecorderInfo;
import tainavi.RecorderInfoList;
import tainavi.VWColorCellRenderer;
import tainavi.VWColorChooserDialog;

public class Viewer extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private JTabbedPane jTabbedPane = null;
	
	private JPanel jContentPane = null;
	private JComboBox jComboBox = null;
	private JPanel jPanel = null;
	private JLabel jLabel = null;
	
	private SpringLayout layout = null;
    private final int wBK = 100;
    private final int hBK = 30;
	
	private JPanel jSettingPane = null;
	private JScrollPane jScrollPane = null;
	private JTable jTable = null;
	private JButton jButton = null;
	
	private VWColorChooserDialog ccwin = null;

	private final String defFile = "env/RemoteCtrlIni.def";
	private final String defFileBK = "env/RemoteCtrl.def";
	
	final private ArrayList<BKSet> bkset = new ArrayList<BKSet>();
	final private ArrayList<String[]> rowData = new ArrayList<String[]>();
	
	private RecorderInfo recinfo = null;
	
	/**
	 * 鯛ナビのレコーダ一覧の中で"RD-"を含むもののみ抽出したリスト. 
	 */
	private RecorderInfoList recorderList = new RecorderInfoList();
	
	final int maxCol = 5;
	final int maxRow = 20;

	final String[][] keydefs = {
			{"08","BACK SPACE"},
			{"0A","ENTER"},
			{"10","SHIFT"},
			{"11","CTRL"},
			{"12","ALT"},
			{"13","PAUSE"},
			{"1B","ESC"},
			{"20","SPCAE"},
			{"21","PAGE UP"},
			{"22","PAGE DOWN"},
			{"23","END"},
			{"24","HOME"},
			{"25","左"},
			{"26","上"},
			{"27","右"},
			{"28","下"},
			{"2C",","},
			{"2D","-"},
			{"2E","."},
			{"2F","/"},
			{"30","0"},
			{"31","1"},
			{"32","2"},
			{"33","3"},
			{"34","4"},
			{"35","5"},
			{"36","6"},
			{"37","7"},
			{"38","8"},
			{"39","9"},
			{"3B",";"},
			{"3D","="},
			{"41","A"},
			{"42","B"},
			{"43","C"},
			{"44","D"},
			{"45","E"},
			{"46","F"},
			{"47","G"},
			{"48","H"},
			{"49","I"},
			{"4A","J"},
			{"4B","K"},
			{"4C","L"},
			{"4D","M"},
			{"4E","N"},
			{"4F","O"},
			{"50","P"},
			{"51","Q"},
			{"52","R"},
			{"53","S"},
			{"54","T"},
			{"55","U"},
			{"56","V"},
			{"57","W"},
			{"58","X"},
			{"59","Y"},
			{"5A","Z"},
			{"5B","["},
			{"5C","\\"},
			{"5D","]"},
			{"70","F1"},
			{"71","F2"},
			{"72","F3"},
			{"73","F4"},
			{"74","F5"},
			{"75","F6"},
			{"76","F7"},
			{"77","F8"},
			{"78","F9"},
			{"79","F10"},
			{"7A","F11"},
			{"7B","F12"},
			{"7F","DELETE"},
			{"9B","INS"},
			{"C0","`"},
			{"DE","'"},
	};
	
	final String[] codedefs = {
			"00(11)",
			"01(1)",
			"02(2)",
			"03(3)",
			"04(4)",
			"05(5)",
			"06(6)",
			"07(7)",
			"08(8)",
			"09(9)",
			"0A",
			"0B",
			"0C(12)",
			"0D(10)",
			"0E(ｻｰﾁ/文字)",
			"0F(入力切替)",
			"10",
			"11(ﾄﾚｲ開閉)",
			"12(電源切)",
			"13(再生)",
			"14",
			"15(録画)",
			"16(停止)",
			"17(一時停止)",
			"18(DVD/USB)",
			"19(HDD)",
			"1A(ﾀｲﾑｽﾘｯﾌﾟ)",
			"1B",
			"1C",
			"1D",
			"1E(CH上)",
			"1F(CH下)",
			"20",
			"21(放送切替)",
			"22(ﾒﾃﾞｨｱ)",
			"23(d ﾃﾞｰﾀ)",
			"24(入力1ｽﾙｰ)",
			"25(CH番号入力",
			"26",
			"27",
			"28",
			"29(青)",
			"2A(赤)",
			"2B(緑)",
			"2C(黄)",
			"2D",
			"2E(ごみ箱へ)",
			"2F",
			"30",
			"31",
			"32",
			"33",
			"34",
			"35",
			"36",
			"37(iLINK)",
			"38(ｱﾝｸﾞﾙ)",
			"39(字幕)",
			"3A(ﾀｲﾑﾊﾞｰ)",
			"3B(おまかせ)",
			"3C",
			"3D",
			"3E",
			"3F",
			"40(録画予約一覧)",
			"41(編集ﾅﾋﾞ)",
			"42(見るﾅﾋﾞ)",
			"43(見ながら)",
			"44(決定)",
			"45(ｸｲｯｸﾒﾆｭｰ)",
			"46(ｽﾀｰﾄﾒﾆｭｰ)",
			"47",
			"48",
			"49",
			"4A(ﾓｰﾄﾞ)",
			"4B(戻る)",
			"4C",
			"4D",
			"4E",
			"4F",
			"50",
			"51",
			"52(設定)",
			"53(ｸﾘｱ/先頭)",
			"54",
			"55(ﾜﾝﾀｯﾁﾘﾌﾟﾚｲ)",
			"56",
			"57(全削除)",
			"58",
			"59",
			"5A(表示/残量)",
			"5B(ﾜﾝﾀｯﾁｽｷｯﾌﾟ)",
			"5C",
			"5D",
			"5E(ｽﾞｰﾑ)",
			"5F",
			"60(終了)",
			"61",
			"62(ﾍﾙﾌﾟ)",
			"63(Ｗ録)",
			"64",
			"65",
			"66",
			"67",
			"68",
			"69",
			"6A",
			"6B",
			"6C",
			"6D(番組表)",
			"6E",
			"6F",
			"70",
			"71",
			"72",
			"73",
			"74(XDE)",
			"75",
			"76",
			"77",
			"78",
			"79",
			"7A",
			"7B",
			"7C",
			"7D",
			"7E",
			"7F",
			"80(逆ｽｷｯﾌﾟ)",
			"81",
			"82",
			"83",
			"84(戻ｽｷｯﾌﾟ)",
			"85",
			"86",
			"87",
			"88(逆ｽﾛｰ)",
			"89",
			"8A",
			"8B",
			"8C(戻ｽﾛｰ)",
			"8D",
			"8E",
			"8F",
			"90",
			"91",
			"92",
			"93",
			"94",
			"95",
			"96",
			"97",
			"98(早送り)",
			"99",
			"9A(早戻し)",
			"9B",
			"9C",
			"9D(ｺﾏ送り)",
			"9E(ｺﾏ戻し)",
			"9F",
			"A0",
			"A1",
			"A2(ﾄﾞﾗｲﾌﾞ切換)",
			"A3",
			"A4",
			"A5",
			"A6",
			"A7(録画ﾓｰﾄﾞ)",
			"A8",
			"A9",
			"AA",
			"AB",
			"AC",
			"AD(ﾜﾝﾀｯﾁﾘﾌﾟﾚｲ)",
			"AE(ﾜﾝﾀｯﾁｽｷｯﾌﾟ)",
			"AF",
			"B0(解像度)",
			"B1",
			"B2",
			"B3",
			"B4(番組説明)",
			"B5(番組ﾅﾋﾞ)",
			"B6",
			"B7",
			"B8",
			"B9",
			"BA",
			"BB",
			"BC",
			"BD",
			"BE",
			"BF",
			"C0(上)",
			"C1",
			"C2",
			"C3",
			"C4(右)",
			"C5",
			"C6",
			"C7",
			"C8(下)",
			"C9",
			"CA",
			"CB",
			"CC(左)",
			"CD",
			"CE",
			"CF",
			"D0(ﾄｯﾌﾟﾒﾆｭｰ)",
			"D1(ﾒﾆｭｰ)",
			"D2(ﾘﾀｰﾝ)",
			"D3(音声/音多)",
			"D4(ｱﾝｸﾞﾙ)",
			"D5(字幕)",
			"D6",
			"D7",
			"D8",
			"D9(ﾁｬﾌﾟﾀｰ分割/結合)",
			"DA(ﾀｲﾑﾊﾞｰ)",
			"DB",
			"DC",
			"DD",
			"DE(番組ﾅﾋﾞ)",
			"DF(かんたんﾀﾞﾋﾞﾝｸﾞ)",
			"E0",
			"E1",
			"E2",
			"E3",
			"E4",
			"E5",
			"E6",
			"E7",
			"E8",
			"E9",
			"EA",
			"EB",
			"EC",
			"ED",
			"EE",
			"EF",
			"F0",
			"F1",
			"F2",
			"F3",
			"F4",
			"F5",
			"F6",
			"F7",
			"F8",
			"F9",
			"FA",
			"FB",
			"FC",
			"FD",
			"FE",
			"FF(電源入)",
	};
	
	
	
	private String getCode2Key(String key) {
		for (String[] s : keydefs ) {
			if (s[0].equals(key)) {
				return s[1];
			}
		}
		return null;
	}
	private String getKey2Code(String code) {
		for (String[] s : keydefs ) {
			if (s[1].equals(code)) {
				return s[0];
			}
		}
		return null;
	}
	
	private String getCode2Code(String code) {
		if (code.length() > 0) {
			for (String cStr : codedefs) {
				if (cStr.startsWith(code)) {
					return cStr;
				}
			}
		}
		return null;
	}
	
	/*
	 * 操作タブのコンポーネント
	 */
	
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getJComboBox(), BorderLayout.NORTH);
			jContentPane.add(getJPanel(), BorderLayout.CENTER);
			jContentPane.add(getJLabel(), BorderLayout.SOUTH);

			this.addComponentListener(new ComponentListener() {
				@Override
				public void componentHidden(ComponentEvent arg0) {
				}
				@Override
				public void componentMoved(ComponentEvent arg0) {
				}
				@Override
				public void componentResized(ComponentEvent arg0) {
				}
				@Override
				public void componentShown(ComponentEvent arg0) {
					jComboBox.requestFocusInWindow();	// フォーカスを移動する
				}
			});
		}
		return jContentPane;
	}
	
	private JComboBox getJComboBox() {
		if (jComboBox == null) {
			jComboBox = new JComboBox();
			
			/*
			jComboBox.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,0),"none");
			jComboBox.getActionMap().put("none", null);
			*/

			// キー入力を受け付ける
			/*
			InputMap m = jComboBox.getInputMap();
			while (m != null){
				m.clear();
				m = m.getParent();
			}
			*/
			ActionMap n = jComboBox.getActionMap();
			while (n != null){
				n.clear();
				n = n.getParent();
			}
			jComboBox.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent arg0) {
					keyPressedAction(arg0.getKeyCode());
				}
			});

            DefaultComboBoxModel model = (DefaultComboBoxModel) jComboBox.getModel();
            for (RecorderInfo r : recorderList) {
            	model.addElement(r.getRecorderIPAddr()+":"+r.getRecorderPortNo()+":"+r.getRecorderId());
            }
            
            setSelectedRecorder();
            
            jComboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
		            setSelectedRecorder();
				}
            });
		}
		return jComboBox;
	}
	
	private JPanel getJPanel() {
		if (jPanel == null) {
			jPanel = new JPanel();
			
			jPanel.setPreferredSize(new Dimension(maxCol*wBK, maxRow*hBK));
			jPanel.setLayout(new SpringLayout());
			layout = (SpringLayout) jPanel.getLayout();
			
			genBKSet();
		}
		return jPanel;
	}
	
	private JLabel getJLabel() {
		if (jLabel == null) {
			jLabel = new JLabel();
			jLabel.setText(" ");
		}
		return jLabel;
	}
	
	// キーアクション付きのボタンのクラス
	private class BKSet extends JButton {
		public String button;
		public String key;
		public int row;
		public int column;
		public int kcode;
		public Color color;
		
		public int getKcode() {
			return kcode;
		}
		
		public void action() {
			String key = this.key.substring(0,2);
			jLabel.setText(this.button+"("+key+")");
			if ( ! key.equals("FF")) {
				tainavi.HDDRecorderUtils ru = new tainavi.HDDRecorderUtils();
				Authenticator.setDefault(ru.new MyAuthenticator(recinfo.getRecorderUser(), recinfo.getRecorderPasswd()));
				ru.reqGET("http://"+recinfo.getRecorderIPAddr()+":"+recinfo.getRecorderPortNo()+"/remote/remote.htm?key="+key, null);
				System.out.println(this.button+"->"+key);
			}
			else {
				tainavi.HDDRecorderUtils ru = new tainavi.HDDRecorderUtils();
				ru.setMacAddr(recinfo.getRecorderMacAddr());
				ru.setBroadcast(recinfo.getRecorderBroadcast());
				ru.wakeup();
			}
		}
		
		public BKSet(String button, String key, int row, int column, String kcode, Color c) {
			super();
			this.setText(button);
			this.button = button;
			this.key = key;
			this.row = row;
			this.column = column;
			this.kcode = -1;
			this.color = new Color(0,0,0);
			
			if ( ! button.equals("") ) {
				if ( ! kcode.equals("")) {
					this.kcode = Integer.parseInt(kcode,16);
				}
				
				this.setForeground(this.color = c);
				this.addMouseListener(new MouseAdapter() {
					@Override
					public void mousePressed(MouseEvent e) {
						action();
					}
				});
			}
			else {
				this.setEnabled(false);
			}
			
			this.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent arg0) {
					keyPressedAction(arg0.getKeyCode());
				}
			});

		}
	}
	
	// コンボボックスの操作
	private void setSelectedRecorder() {
        DefaultComboBoxModel model = (DefaultComboBoxModel) jComboBox.getModel();
        for (RecorderInfo r : recorderList) {
        	if (model.getSelectedItem().equals(r.MySelf())) {
	            recinfo = r;
	            System.out.println(model.getSelectedItem());
        		break;
        	}
        }
	}
	
	// キーを押したときの動作
	private void keyPressedAction(int kcode) {
		for (BKSet bk : bkset) {
			if (bk.getKcode() == kcode) {
				bk.requestFocusInWindow();	// フォーカスを移動する
				bk.action();				// アクションを実行する
				return;
			}
		}
		jLabel.setText("unknown keycode("+String.format("%02X",kcode)+")");
	}
	
	/*
	 * 設定タブのコンポーネント
	 */
	
	private JPanel getJSettingPane() {
		if (jSettingPane == null) {
			jSettingPane = new JPanel();
			jSettingPane.setLayout(new BorderLayout());
			jSettingPane.add(getJScrollPane(), BorderLayout.CENTER);
			jSettingPane.add(getJButton("設定"), BorderLayout.SOUTH);
			
			setBK4Table();
		}
		return jSettingPane;
	}
	
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setViewportView(getJTable());
		}
		return jScrollPane;
	}
	
	private JTable getJTable() {
		if (jTable == null) {
			final String[] colname = { "配置", "ラベル","コード","キー","色" };
			final int[] colwidth = { 25,150,100,100,50 };
			
			DefaultTableModel model = new DefaultTableModel(colname, 0) {
				@Override
				public Object getValueAt(int row, int column) {
					return rowData.get(row)[column];
				}
				@Override
				public void setValueAt(Object aValue, int row, int column) {
					rowData.get(row)[column] = (String) aValue;
				}
				@Override
				public int getRowCount() {
					return rowData.size();
				}
				@Override
				public boolean isCellEditable(int row, int column) {
					if (column == 0 || column == 4) {
						return false;
					}
					return true;
				}
			};
			jTable = new JTable(model) {
				private final Color evenColor = new Color(240,240,255);
				@Override
				public Component prepareRenderer(TableCellRenderer tcr, int row, int column) {
					Component c = super.prepareRenderer(tcr, row, column);
					if (column != 4) {
						if(isRowSelected(row)) {
							c.setForeground(getSelectionForeground());
							c.setBackground(getSelectionBackground());
					    }
						else {
							c.setForeground(getForeground());
							c.setBackground(((row/maxCol)%2==0)?evenColor:getBackground());
						}
					}
					return c;
			   }
			};
			
			DefaultTableColumnModel colModel = (DefaultTableColumnModel) jTable.getColumnModel();
			TableColumn column = null;
			for (int i=0; i<colModel.getColumnCount() && i<colwidth.length ; i++){
				column = colModel.getColumn(i);
				column.setPreferredWidth(colwidth[i]);
			}
			
			JComboBox jcbcode = new JComboBox();
			jcbcode.setEditable(false);
			jcbcode.addItem("");
			for (String cd : codedefs) {
				jcbcode.addItem(cd);
			}
			column = jTable.getColumn("コード");
			column.setCellEditor(new DefaultCellEditor(jcbcode));
			
			JComboBox jcbkey = new JComboBox();
			jcbkey.setEditable(false);
			jcbkey.addItem("");
			for (int i=1; i<255; i++) {
				String key = getCode2Key(String.format("%02X", i));
				if (key != null) {
					jcbkey.addItem(key);
				}
			}
			column = jTable.getColumn("キー");
			column.setCellEditor(new DefaultCellEditor(jcbkey));
			
			TableCellRenderer colorCellRenderer = new VWColorCellRenderer();
			jTable.getColumn("色").setCellRenderer(colorCellRenderer);
			
			jTable.addMouseListener(new MouseListener() {
				@Override
				public void mouseReleased(MouseEvent e) {}
				@Override
				public void mousePressed(MouseEvent e) {}
				@Override
				public void mouseExited(MouseEvent e) {}
				@Override
				public void mouseEntered(MouseEvent e) {}
				@Override
				public void mouseClicked(MouseEvent e) {
					JTable t = (JTable) e.getSource();
					Point p = e.getPoint();
					Point p2 = jTabbedPane.getLocationOnScreen();
					
					int col = t.convertColumnIndexToModel(t.columnAtPoint(p));
					if (col == 4) {
						int row = t.convertRowIndexToModel(t.rowAtPoint(p));
						ccwin.setColor(CommonUtils.str2color(rowData.get(row)[4].substring(0,7)));
						Rectangle r = jTable.getBounds();
						//ccwin.setPosition(p2.x,p2.y);
						CommonSwingUtils.setLocationCenter(Viewer.this, ccwin);
						ccwin.setVisible(true);
						Color c = ccwin.getSelectedColor();
						if (c != null) {
							String cs = CommonUtils.color2str(c);
							rowData.get(row)[4] = cs+";"+cs;
							((AbstractTableModel) jTable.getModel()).fireTableDataChanged();
						}
					}
				}
			});
		}
		return jTable;
	}
	
	private JButton getJButton(String s) {
		if (jButton == null) {
			jButton = new JButton(s);
			
			jButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					saveBKSet();
					genBKSet();
				}
			});
		}
		return jButton;
	}
	
	private void setBK4Table() {
		final String cStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		int n = 0;
		for (int row=0; row<maxRow; row++) {
			for (int col=0; col<maxCol; col++) {
				n = maxCol*row+col;
				String[] dat = new String[] {
					String.format("%s%d",cStr.substring(col,col+1),row+1),
					bkset.get(n).button,
					bkset.get(n).key,
					(bkset.get(n).kcode != -1)?(getCode2Key(String.format("%02X",bkset.get(n).kcode))):(""),
					CommonSwingUtils.getColoredString(bkset.get(n).color,CommonUtils.color2str(bkset.get(n).color))
				};
				rowData.add(dat);
			}
		}
		((AbstractTableModel) jTable.getModel()).fireTableDataChanged();
	}
	
	/*
	 * その他のメソッド
	 */
	
	// フォントを鯛ナビの設定におきかえる
    private void updateFont(final String font, final int fontSize) {
    	FontUIResource fontUIResource = null;
    	UIDefaults defaultTable = UIManager.getLookAndFeelDefaults();
    	for(Object o: defaultTable.keySet()) {
			if(o.toString().toLowerCase().endsWith("font")) {
				//System.out.println("set font to "+o.toString());
				fontUIResource = (FontUIResource) UIManager.get(o);
				fontUIResource = new FontUIResource(new Font(font, fontUIResource.getStyle(), fontSize));
				UIManager.put(o, fontUIResource);
			}
    	}
    	SwingUtilities.updateComponentTreeUI(this);
    }
	
    // ボタンの設定ファイルを読み出す
	private void genBKSet() {
		File f = new File(defFileBK);
		if ( ! f.exists() ) {
			return;
		}
		try {
			bkset.removeAll(bkset);
			
			BufferedReader reader = new BufferedReader(new FileReader(f));
			String str = null;
			boolean isEOF = false;
			for (int row=0; row<maxRow; row++) {
				for (int col=0; col<maxCol; col++) {
					if ( ! isEOF) {
						if  ((str = reader.readLine()) != null) {
							Matcher ma = Pattern.compile("^\\s*\"(.*?)\"\\s*,\\s*\"(.*?)\"\\s*,\\s*\"(.*?)\"\\s*(,\\s*\"(..)(..)(..)\")?").matcher(str);
							if (ma.find()) {
								Color c = new Color(Integer.parseInt(ma.group(5),16),Integer.parseInt(ma.group(6),16),Integer.parseInt(ma.group(7),16));
								String cd = getCode2Code(ma.group(2));
								bkset.add(new BKSet(ma.group(1),(cd==null)?(""):(cd),row,col,ma.group(3),c));
							}
						}
						else {
							isEOF = true;
						}
					}
					if (isEOF) {
						bkset.add(new BKSet("","",row,col,"",new Color(0,0,0)));
					}
				}
			}
			reader.close();
			
			// パネルに追加
			for (BKSet bk : bkset) {
				bk.setPreferredSize(new Dimension(wBK,hBK));
			    layout.putConstraint(SpringLayout.NORTH, bk, bk.row*hBK, SpringLayout.NORTH, jPanel);
			    layout.putConstraint(SpringLayout.WEST, bk, bk.column*wBK, SpringLayout.WEST, jPanel);
			    jPanel.add(bk);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void saveBKSet() {
		File f = new File(defFileBK);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(f));
			for (int row=0; row<maxRow; row++) {
				for (int col=0; col<maxCol; col++) {
					int n = row*maxCol+col;
					String code = rowData.get(n)[2];
					String key = getKey2Code(rowData.get(n)[3]);
					writer.write(String.format("\"%s\", \"%s\", \"%s\", \"%s\"\n",
							rowData.get(n)[1],
							(code == null || code.length() == 0)?(""):(code.substring(0,2)),
							(key == null)?(""):(key),
							rowData.get(n)[4].substring(1,7)));
				}
			}
			writer.close();
			
			// パネルから全部削除
			jPanel.removeAll();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
    
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Viewer thisClass = new Viewer();
				thisClass.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				thisClass.setVisible(true);
			}
		});
	}

	/**
	 * This is the default constructor
	 */
	public Viewer() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		
		// 設定ファイルを読み込む
		String recip = "";
		Rectangle ra = new Rectangle();
		File f = new File(defFile);
		if ( f.exists() ) {
			try {
				String str = null;
				int i = 0;
				BufferedReader reader = new BufferedReader(new FileReader(f));
				while ((str = reader.readLine()) != null) {
					switch (i++) {
					case 0:
						recip = str;
						break;
					case 1:
						ra.x = Integer.valueOf(str);
						break;
					case 2:
						ra.y = Integer.valueOf(str);
						break;
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// 鯛ナビと タイニーシンクの設定から
		tainavi.Env env = new tainavi.Env();
		env.load();
		
		RecorderInfoList recInfos = new RecorderInfoList();
		recInfos.load();
		
		ArrayList<taiSync.RecorderInfo> syncrl = taiSync.RecorderInfo.load();
		
		for (RecorderInfo r : recInfos) {
			if ( r.getRecorderIPAddr().equals("") ) {
				continue;
			}
	    	if ( ! r.getRecorderId().contains("RD-") && ! r.getRecorderId().contains("DBR-Z") ) {
	    		continue;
	    	}
	    	
	    	RecorderInfo rn = new RecorderInfo();
	    	if (r.getRecorderIPAddr().equals("127.0.0.1") || r.getRecorderIPAddr().equals("localhost")) {
		    	for (taiSync.RecorderInfo sr : syncrl) {
		    		if (sr.getLocalPort() == Integer.valueOf(r.getRecorderPortNo())) {
		    	    	rn.setRecorderIPAddr(sr.getRecorderIPAddr());
		    	    	rn.setRecorderPortNo(sr.getRecorderPortNo());
				    	rn.setRecorderUser(sr.getRecorderUser());
				    	rn.setRecorderPasswd(sr.getRecorderPasswd());
		    		}
		    	}
	    	}
	    	if (rn.getRecorderIPAddr().equals("")) {
		    	rn.setRecorderIPAddr(r.getRecorderIPAddr());
		    	rn.setRecorderPortNo(r.getRecorderPortNo());
		    	rn.setRecorderUser(r.getRecorderUser());
		    	rn.setRecorderPasswd(r.getRecorderPasswd());
	    	}
	    	rn.setRecorderMacAddr(r.getRecorderMacAddr());
	    	rn.setRecorderBroadcast(r.getRecorderBroadcast());
	    	rn.setRecorderId(r.getRecorderId());
	    	
	    	recorderList.add(rn);
		}
		
		if (env.getFontName() != null) {
			updateFont(env.getFontName(), env.getFontSize());
		}
		
		try {
			final Image image = ImageIO.read(new File("icon/remocon.png"));
			this.setIconImage(image);
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.setResizable(false);
		this.setContentPane(getJTabbedPane());
		this.setTitle("RDリモコン");
		this.pack();
		
		ccwin = new VWColorChooserDialog();
		
		Rectangle ws = this.getBounds();
		ws.x = ra.x;
		ws.y = ra.y;
		this.setBounds(ws);
		
		// 前回選択していたレコーダーを再選択
		if ( ! recip.equals("")) {
			for (int idx=0; idx<recorderList.size(); idx++) {
				if (recorderList.get(idx).getRecorderIPAddr().equals(recip)) {
					jComboBox.setSelectedIndex(idx);
					break;
				}
			}
		}
		
		// ウィンドウを閉じたときの処理
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addWindowListener(new WindowListener() {

			public void windowActivated(WindowEvent e) {}
			public void windowClosed(WindowEvent e) {}
			public void windowDeactivated(WindowEvent e) {}
			public void windowDeiconified(WindowEvent e) {}
			public void windowIconified(WindowEvent e) {}
			public void windowOpened(WindowEvent e) {}

			@Override
			public void windowClosing(WindowEvent e) {
				final Viewer thisClass = (Viewer) e.getSource();
				Rectangle r = thisClass.getBounds();
				
				File f = new File(defFile);
				try {
					BufferedWriter writer = new BufferedWriter(new FileWriter(f));
					if (recinfo != null) {
						writer.write(recinfo.getRecorderIPAddr()+"\n");
					}
					else {
						writer.write("\n");
					}
					writer.write(r.x+"\n");
					writer.write(r.y+"\n");
					writer.write(r.width+"\n");
					writer.write(r.height+"\n");
					writer.close();
				}
				catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	
	private JTabbedPane getJTabbedPane() {
		if (jTabbedPane == null) {
			jTabbedPane = new JTabbedPane();
			
			jTabbedPane.addTab("操作", getJContentPane());
			jTabbedPane.addTab("設定", getJSettingPane());
		}
		return jTabbedPane;
	}
}
