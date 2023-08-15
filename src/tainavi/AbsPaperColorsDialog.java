package tainavi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import tainavi.Env.AAMode;
import tainavi.JTXTButton.FontStyle;
import tainavi.TVProgram.ProgGenre;


abstract class AbsPaperColorsDialog extends JEscCancelDialog {

	private static final long serialVersionUID = 1L;

	private static boolean debug = false;


	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/

	protected abstract Env getEnv();
	protected abstract Bounds getBoundsEnv();
	protected abstract PaperColorsMap getPaperColorMap();

	protected abstract VWColorChooserDialog getCCWin();

	protected abstract void updatePaperColors(Env ec, PaperColorsMap pc);
	protected abstract void updatePaperFonts(Env ec);
	protected abstract void updatePaperBounds(Env ec, Bounds bc);
	protected abstract void updatePaperRepaint();


	/*******************************************************************************
	 * 呼び出し元から引き継いだもの
	 ******************************************************************************/

	private final Env origenv = getEnv();
	private final Bounds origbnd = getBoundsEnv();
	private final PaperColorsMap origpc = getPaperColorMap();

	private final VWColorChooserDialog ccwin = getCCWin();

	private final Env tmpenv = new Env();
	private final Bounds tmpbnd = new Bounds();
	private final PaperColorsMap tmppc = new PaperColorsMap();


	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	private static final int STEPBY = 10;

	private static final int SEP_WIDTH = 10;
	private static final int SEP_WIDTH_NARROW = 2;
	private static final int SEP_HEIGHT = 5;
	private static final int SEP_HEIGHT_NARROW = 2;

	//private static final int PARTS_WIDTH = 900;
	private static final int PARTS_HEIGHT = 25;

	private static final int LABEL_WIDTH = 125;
	private static final int ITEM_WIDTH = 250;
	private static final int TITLE_WIDTH = LABEL_WIDTH+ITEM_WIDTH;

	private static final int BUTTON_WIDTH = 100;

	private static int PANEL_WIDTH = LABEL_WIDTH+ITEM_WIDTH+SEP_WIDTH*2;
	private static int PANEL_HEIGHT = 0;

	private static final int TABLE_WIDTH = PANEL_WIDTH-SEP_WIDTH*2;
	private static final int TABLE_HEIGHT = 260;

	private static final int STYLETABLE_HEIGHT = 80;

	private static final int TIMEBAR_WIDTH = TABLE_WIDTH/4;


	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	private JPanel jPanel = null;

	private JTabbedPane jTabbedPane = null;

	private JPanel jPanel_buttons = null;
	private JButton jButton_preview = null;
	private JButton jButton_update = null;
	private JButton jButton_cancel = null;

	// ジャンル別背景色のタブ
	private JPanel jPanel_pColors = null;
	private JScrollPane jScrollPane_list = null;
	private JNETable jTable_list = null;
	private DefaultTableModel jTableModel_list = null;
	private JCCLabel jLabel_timebar = null;
	private JCCLabel jLabel_timebar2 = null;
	private JCCLabel jLabel_timebar3 = null;
	private JCCLabel jLabel_timebar4 = null;
	private JCCLabel jLabel_restimebar1 = null;
	private JCCLabel jLabel_restimebar2 = null;
	private JCCLabel jLabel_restimebar3 = null;
	private JCCLabel jLabel_restimebar4 = null;
	private JCheckBoxPanel jCBP_highlight = null;
	private JCCLabel jLabel_highlight = null;

	// フォント設定のタブ
	private JPanel jPanel_fonts = null;
	private JCheckBoxPanel jCBP_showStart = null;
	private JComboBoxPanel jCBX_titleFont = null;
	private JSliderPanel jSP_titleFontSize = null;
	private JCCLabel jLabel_titleFontColor = null;
	private JScrollPane jScrollPane_titleFontStyle = null;
	private JCheckBoxPanel jCBP_showDetail = null;
	private JComboBoxPanel jCBX_detailFont = null;
	private JSliderPanel jSP_detailFontSize = null;
	private JCCLabel jLabel_detailFontColor = null;
	private JScrollPane jScrollPane_detailFontStyle = null;
	private JSliderPanel jSP_detailTab = null;
	private JComboBoxPanel jCBX_aaMode = null;

	// サイズのタブ
	private JPanel jPanel_bounds = null;
	private JSliderPanel jSP_width = null;
	private JSliderPanel jSP_height = null;
	private JSliderPanel jSP_timebarPosition = null;
	private JSliderPanel jSP_restimebar = null;
	private JCCLabel jLabel_execon = null;
	private JCCLabel jLabel_execoff = null;
	private JCCLabel jLabel_pickup = null;
	private JCCLabel jLabel_pickupFont = null;
	private JCCLabel jLabel_matchedBorderColor = null;
	private JCCLabel jLabel_matchedKeywordBorderColor = null;
	private JSliderPanel jSP_matchedBorderThickness = null;
	//private JCheckBoxPanel jCBP_lightProgramView = null;


	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public AbsPaperColorsDialog() {
		//
		super();
		//
		this.setModal(true);
		//
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				doCancel();
			}
		});

		this.setContentPane(getJPanel());
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);	// 閉じるときはキャンセルボタンを使ってクレ

		this.pack();
		this.setResizable(false);

		this.setTitle("新聞形式の表示設定");
	}


	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	private void doPreview() {
		getColors(tmpenv,tmppc);
		getFonts(tmpenv);
		getBounds(tmpenv,tmpbnd);

		updatePaperColors(tmpenv,tmppc);
		updatePaperFonts(tmpenv);
		updatePaperBounds(tmpenv,tmpbnd);

		updatePaperRepaint();
	}

	private void doUpdate() {
		getColors(origenv,origpc);
		getFonts(origenv);
		getBounds(origenv,origbnd);

		updatePaperFonts(origenv);
		updatePaperColors(origenv,origpc);
		updatePaperBounds(origenv,origbnd);

		updatePaperRepaint();

		origpc.save();
		origenv.save();
		origbnd.save();

		setVisible(false);
	}

	@Override
	protected void doCancel() {
		updatePaperColors(origenv,origpc);
		updatePaperFonts(origenv);
		updatePaperBounds(origenv,origbnd);

		updatePaperRepaint();

		setVisible(false);
	}

	/*
	 * メソッド
	 */

	//
	@Override
	public void setVisible(boolean b) {
		if (b) {
			if (debug) {
				for ( ProgGenre key : origpc.keySet() ) {
					System.err.println("[DEBUG] before orig papercolorsmap "+key+"="+origpc.get(key));
				}
			}
			FieldUtils.deepCopy(tmpenv, origenv);
			FieldUtils.deepCopy(tmpbnd, origbnd);
			FieldUtils.deepCopy(tmppc, origpc);
			setColors();
			setFonts();
			setBounds();
		}
		else {
			if (debug) {
				for ( ProgGenre key : origpc.keySet() ) {
					System.err.println("[DEBUG] after orig papercolorsmap "+key+"="+origpc.get(key));
				}
			}
		}
		super.setVisible(b);
	}

	//
	private void getColors(Env toe, PaperColorsMap top) {
		for ( int row=0; row<jTable_list.getRowCount(); row++ ) {
			TVProgram.ProgGenre g = (ProgGenre) jTable_list.getValueAt(row, 0);
			Color c = CommonUtils.str2color((String) jTable_list.getValueAt(row, 1));
			top.put(g, c);
		}
		toe.setTimebarColor(jLabel_timebar.getChoosed());
		toe.setTimebarColor2(jLabel_timebar2.getChoosed());
		toe.setTimebarColor3(jLabel_timebar3.getChoosed());
		toe.setTimebarColor4(jLabel_timebar4.getChoosed());

		toe.setResTimebarColor1(jLabel_restimebar1.getChoosed());
		toe.setResTimebarColor2(jLabel_restimebar2.getChoosed());
		toe.setResTimebarColor3(jLabel_restimebar3.getChoosed());
		toe.setResTimebarColor4(jLabel_restimebar4.getChoosed());

		toe.setEnableHighlight(jCBP_highlight.isSelected());
		toe.setHighlightColor(jLabel_highlight.getChoosed());
	}

	//
	private void getFonts(Env to) {
		to.setShowStart(jCBP_showStart.isSelected());
		to.setTitleFont((String) jCBX_titleFont.getSelectedItem());
		to.setTitleFontSize(jSP_titleFontSize.getValue());
		to.setTitleFontColor(jLabel_titleFontColor.getChoosed());
		to.setTitleFontStyle(getFontStyles((JNETable) jScrollPane_titleFontStyle.getViewport().getView()));
		to.setShowDetail(jCBP_showDetail.isSelected());
		to.setDetailFont((String) jCBX_detailFont.getSelectedItem());
		to.setDetailFontSize(jSP_detailFontSize.getValue());
		to.setDetailFontColor(jLabel_detailFontColor.getChoosed());
		to.setDetailFontStyle(getFontStyles((JNETable) jScrollPane_detailFontStyle.getViewport().getView()));
		to.setDetailTab(jSP_detailTab.getValue());
		to.setPaperAAMode((AAMode) jCBX_aaMode.getSelectedItem());
	}
	private ArrayList<JTXTButton.FontStyle> getFontStyles(JNETable jt) {
		ArrayList<JTXTButton.FontStyle> fsa = new ArrayList<JTXTButton.FontStyle>();
		for ( int row=0; row<jt.getRowCount(); row++ ) {
			if ( (Boolean)jt.getValueAt(row, 0) ) {
				fsa.add((FontStyle) jt.getValueAt(row, 1));
			}
		}
		return fsa;
	}

	//
	private void getBounds(Env toe, Bounds tob) {
		tob.setBangumiColumnWidth(jSP_width.getValue());
		tob.setPaperHeightMultiplier(jSP_height.getValue()*(float)STEPBY/(float)60);
		tob.setTimelinePosition(jSP_timebarPosition.getValue());
		toe.setResTimebarWidth(jSP_restimebar.getValue());
		toe.setExecOnFontColor(jLabel_execon.getChoosed());
		toe.setExecOffFontColor(jLabel_execoff.getChoosed());
		toe.setPickedColor(jLabel_pickup.getChoosed());
		toe.setPickedFontColor(jLabel_pickupFont.getChoosed());
		toe.setMatchedBorderColor(jLabel_matchedBorderColor.getChoosed());
		toe.setMatchedKeywordBorderColor(jLabel_matchedKeywordBorderColor.getChoosed());
		toe.setMatchedBorderThickness(jSP_matchedBorderThickness.getValue());
		//
		tob.setShowMatchedBorder(origbnd.getShowMatchedBorder());
	}



	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	private JPanel getJPanel() {
		if (jPanel == null) {
			jPanel = new JPanel();
			jPanel.setLayout(new BorderLayout());
			jPanel.add(getJTabbedPane(), BorderLayout.CENTER);
			jPanel.add(getJPanel_buttons(), BorderLayout.PAGE_END);
		}
		return jPanel;
	}

	//
	private JTabbedPane getJTabbedPane() {
		if (jTabbedPane == null) {
			jTabbedPane = new JTabbedPane();
			jTabbedPane.add(getJPanel_pColors(),"背景色",0);
			jTabbedPane.add(getJPanel_fonts(),"テキスト",1);
			jTabbedPane.add(getJPanel_bounds(),"その他",2);
		}
		return jTabbedPane;
	}

	//
	private JPanel getJPanel_buttons() {
		if (jPanel_buttons == null) {
			jPanel_buttons = new JPanel();

			jPanel_buttons.setLayout(new SpringLayout());

			int sw = ZMSIZE(SEP_WIDTH);
			int bw = ZMSIZE(BUTTON_WIDTH);

			int ph = ZMSIZE(PARTS_HEIGHT);
			int sh = ZMSIZE(SEP_HEIGHT);

			int pw = ZMSIZE(PANEL_WIDTH);

			int y = sh;
			int x = (pw - (bw*3+sw*2))/2;
			CommonSwingUtils.putComponentOn(jPanel_buttons, getJButton_preview("ﾌﾟﾚﾋﾞｭｰ"), bw, ph, x, y);
			CommonSwingUtils.putComponentOn(jPanel_buttons, getJButton_update("登録"), bw, ph, x+=bw+sw, y);
			CommonSwingUtils.putComponentOn(jPanel_buttons, getJButton_cancel("ｷｬﾝｾﾙ"), bw, ph, x+=bw+sw, y);

			y += ph+sh;

			jPanel_buttons.setPreferredSize(new Dimension(pw, y));
		}
		return jPanel_buttons;
	}
	private JButton getJButton_preview(String s) {
		if (jButton_preview == null) {
			jButton_preview = new JButton();
			jButton_preview.setText(s);

			jButton_preview.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					doPreview();
				}
			});
		}
		return jButton_preview;
	}
	private JButton getJButton_update(String s) {
		if (jButton_update == null) {
			jButton_update = new JButton();
			jButton_update.setText(s);

			jButton_update.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					doUpdate();
				}
			});
		}
		return jButton_update;
	}
	private JButton getJButton_cancel(String s) {
		if (jButton_cancel == null) {
			jButton_cancel = new JButton();
			jButton_cancel.setText(s);

			jButton_cancel.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					doCancel();
				}
			});
		}
		return jButton_cancel;
	}


	/*
	 * ジャンル別背景色のタブ
	 */

	private JPanel getJPanel_pColors() {
		if (jPanel_pColors == null) {
			jPanel_pColors = new JPanel();

			jPanel_pColors.setLayout(new SpringLayout());

			int tw = ZMSIZE(TITLE_WIDTH);
			int lw = ZMSIZE(LABEL_WIDTH);
			int iw = ZMSIZE(ITEM_WIDTH);
			int sw = ZMSIZE(SEP_WIDTH);
			int swn = ZMSIZE(SEP_WIDTH_NARROW);
			int tbw = ZMSIZE(TIMEBAR_WIDTH);
			int pw = ZMSIZE(PANEL_WIDTH);

			int ph = ZMSIZE(PARTS_HEIGHT);
			int sh = ZMSIZE(SEP_HEIGHT);
			int shn = ZMSIZE(SEP_HEIGHT_NARROW);
			int th = ZMSIZE(TABLE_HEIGHT);

			int y = shn;
			int x = sw;

			addc(new JTitleLabel("ジャンル別背景色"), tw, ph, swn, y);
			y += (ph+shn);
			addc(getJScrollPane_list(), tw, th, sw, y);

			y += th+sh;
			addc(new JTitleLabel("タイムバーの色"), tw, ph, swn, y);
			y += (ph+shn);
			addc(jLabel_timebar  = new JCCLabel("6～11",  origenv.getTimebarColor(), true,this,ccwin), tbw, ph, x, y);
			addc(jLabel_timebar2 = new JCCLabel("12～17", origenv.getTimebarColor2(),true,this,ccwin), tbw, ph, x+=tbw, y);
			addc(jLabel_timebar3 = new JCCLabel("18～23", origenv.getTimebarColor3(),true,this,ccwin), tbw, ph, x+=tbw, y);
			addc(jLabel_timebar4 = new JCCLabel("24～5",  origenv.getTimebarColor4(),true,this,ccwin), tbw, ph, x+=tbw, y);

			y += (ph+sh);
			addc(new JTitleLabel("予約バーの色"), tw, ph, swn, y);
			y += (ph+shn);
			x = sw;
			addc(jLabel_restimebar1 = new JCCLabel("１",	origenv.getResTimebarColor1(),true,this,ccwin), tbw, ph, x, y);
			addc(jLabel_restimebar2 = new JCCLabel("２",	origenv.getResTimebarColor2(),true,this,ccwin), tbw, ph, x+=tbw, y);
			addc(jLabel_restimebar3 = new JCCLabel("最大",	origenv.getResTimebarColor3(),true,this,ccwin), tbw, ph, x+=tbw, y);
			addc(jLabel_restimebar4 = new JCCLabel("＞最大",origenv.getResTimebarColor4(),true,this,ccwin), tbw, ph, x+=tbw, y);

			y += (ph+sh);
			addc(new JTitleLabel("マウスオーバー時のハイライト色"), tw, ph, swn, y);
			y += (ph+shn);
			addc(jCBP_highlight = new JCheckBoxPanel("有効",lw/2), lw, ph, sw, y);
			addc(jLabel_highlight = new JCCLabel("ハイライト",origenv.getHighlightColor(),true,this,ccwin), iw, ph, lw+sw, y);

			y += (ph+sh*2);

			if (PANEL_HEIGHT < y) PANEL_HEIGHT = y;

			jPanel_pColors.setPreferredSize(new Dimension(pw, PANEL_HEIGHT));
		}
		return jPanel_pColors;
	}

	private void addc(JComponent c, int width, int height, int x, int y) {
		CommonSwingUtils.putComponentOn(jPanel_pColors, c, width, height, x, y);
	}


	private void setColors() {
		//
		for (int i=jTableModel_list.getRowCount()-1; i>=0; i--) {
			jTableModel_list.removeRow(i);
		}
		for (TVProgram.ProgGenre g : TVProgram.ProgGenre.values()) {
			Object[] data = {
					g,
					CommonSwingUtils.getColoredString(origpc.get(g),"色見本")
			};
			jTableModel_list.addRow(data);
		}
		jTable_list.updateUI();
		//
		jLabel_timebar.setChoosed(origenv.getTimebarColor());
		jLabel_timebar2.setChoosed(origenv.getTimebarColor2());
		jLabel_timebar3.setChoosed(origenv.getTimebarColor3());
		jLabel_timebar4.setChoosed(origenv.getTimebarColor4());

		jLabel_restimebar1.setChoosed(origenv.getResTimebarColor1());
		jLabel_restimebar2.setChoosed(origenv.getResTimebarColor2());
		jLabel_restimebar3.setChoosed(origenv.getResTimebarColor3());
		jLabel_restimebar4.setChoosed(origenv.getResTimebarColor4());

		jCBP_highlight.setSelected(origenv.getEnableHighlight());
		jLabel_highlight.setChoosed(origenv.getHighlightColor());
	}

	private JScrollPane getJScrollPane_list() {
		if (jScrollPane_list == null) {
			jScrollPane_list = new JScrollPane();
			jScrollPane_list.setViewportView(getJTable_list());
			jScrollPane_list.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
			jScrollPane_list.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		}
		return(jScrollPane_list);
	}
	private JNETable getJTable_list() {
		if (jTable_list == null) {
			//
			String[] colname = {"ジャンル", "色"};
			int[] colwidth = {ZMSIZE(TABLE_WIDTH-100),ZMSIZE(100)};
			//
			jTableModel_list = new DefaultTableModel(colname, 0);
			jTable_list = new JNETable(jTableModel_list, false);
//			jTable_list.setRowHeight(ZMSIZE(jTable_list.getRowHeight()));
			jTable_list.setAutoResizeMode(JNETable.AUTO_RESIZE_OFF);
			DefaultTableColumnModel columnModel = (DefaultTableColumnModel)jTable_list.getColumnModel();
			TableColumn column = null;
			for (int i = 0 ; i < columnModel.getColumnCount() ; i++){
				column = columnModel.getColumn(i);
				column.setPreferredWidth(colwidth[i]);
			}
			//
			TableCellRenderer colorCellRenderer = new VWColorCellRenderer();
			jTable_list.getColumn("色").setCellRenderer(colorCellRenderer);
			//
			final JDialog jd = this;
			jTable_list.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (SwingUtilities.isLeftMouseButton(e)) {
						//
						JTable t = (JTable) e.getSource();
						Point p = e.getPoint();

						int col = t.convertColumnIndexToModel(t.columnAtPoint(p));
						if (col == 1) {
							int row = t.convertRowIndexToModel(t.rowAtPoint(p));

							ccwin.setColor(CommonUtils.str2color((String) t.getValueAt(row,1)));
							CommonSwingUtils.setLocationCenter(jd,ccwin);
							ccwin.setVisible(true);

							if (ccwin.getSelectedColor() != null ) {
								//
								tmppc.put((TVProgram.ProgGenre) t.getValueAt(row,0), ccwin.getSelectedColor());
								//
								t.setValueAt(CommonSwingUtils.getColoredString(ccwin.getSelectedColor(),"色見本"), row, 1);
							}
						}
					}
				}
			});
		}
		return(jTable_list);
	}



	/*
	 * フォントのタブ
	 */

	/**
	 * フォントの選択肢を設定
	 */
	public void setFontList(VWFont vwfont) {
		jCBX_titleFont.removeAllItems();
		jCBX_detailFont.removeAllItems();
		for ( String fn : vwfont.getNames() ) {
			jCBX_titleFont.addItem(fn);
			jCBX_detailFont.addItem(fn);

			//if (debug) System.err.println("[DEBUG] font name="+fn);
		}
	}
	private JPanel getJPanel_fonts() {
		if (jPanel_fonts == null) {
			jPanel_fonts = new JPanel();

			jPanel_fonts.setLayout(new SpringLayout());

			int tw = ZMSIZE(TITLE_WIDTH);
			int lw = ZMSIZE(LABEL_WIDTH);
			int iw = ZMSIZE(ITEM_WIDTH);
			int sw = ZMSIZE(SEP_WIDTH);
			int swn = ZMSIZE(SEP_WIDTH_NARROW);
			int pw = ZMSIZE(PANEL_WIDTH);

			int ph = ZMSIZE(PARTS_HEIGHT);
			int sh = ZMSIZE(SEP_HEIGHT);
			int shn = ZMSIZE(SEP_HEIGHT_NARROW);
			int sth = ZMSIZE(STYLETABLE_HEIGHT);

			int y = shn;
			addf(new JTitleLabel("開始時刻欄の設定"), tw, ph, shn, y);

			y += (ph+shn);
			addf(jCBP_showStart = new JCheckBoxPanel("表示する",lw), tw, ph, sw, y);
			//jCBP_showStart.addActionListener(fal);

			y += (ph+sh);
			addf(new JTitleLabel("番組名のフォント設定"), tw, ph, swn, y);

			y += (ph+shn);
			addf(jCBX_titleFont = new JComboBoxPanel("フォント",lw,iw,true), lw+iw, ph, sw, y);

			y += (ph+shn);
			addf(jSP_titleFontSize = new JSliderPanel("サイズ",lw,ZMSIZE(6),ZMSIZE(24),iw), lw+iw, ph, sw, y);

			y += (ph+shn);
			addf(new JLabel("文字色"), lw, ph, sw, y);
			addf(jLabel_titleFontColor = new JCCLabel("番組名", origenv.getTitleFontColor(),false,this,ccwin), iw, ph, sw+lw, y);

			y += (ph+shn);
			addf(new JLabel("スタイル"), lw, ph, sw, y);
			addf(jScrollPane_titleFontStyle = getJScrollPane_fontstyle(), iw, sth, sw+lw, y);

			y += sth+sh;
			addf(new JTitleLabel("番組詳細のフォント設定"), tw, ph, swn, y);

			y += (ph+shn);
			addf(jCBP_showDetail = new JCheckBoxPanel("表示する",lw), tw+iw, ph, sw, y);

			y += (ph+shn);
			addf(jCBX_detailFont = new JComboBoxPanel("フォント",lw,iw,true), lw+iw, ph, sw, y);

			y += (ph+shn);
			addf(jSP_detailFontSize = new JSliderPanel("サイズ",lw,ZMSIZE(6),ZMSIZE(24),iw), lw+iw, ph, sw, y);

			y += (ph+shn);
			addf(new JLabel("文字色"), lw, ph, sw, y);
			addf(jLabel_detailFontColor = new JCCLabel("番組詳細", origenv.getDetailFontColor(),false,this,ccwin), iw, ph, sw+lw, y);

			y += (ph+shn);
			addf(new JLabel("スタイル"), lw, ph, sw, y);
			addf(jScrollPane_detailFontStyle = getJScrollPane_fontstyle(), iw, sth, sw+lw, y);

			y += sth+shn;
			addf(jSP_detailTab = new JSliderPanel("左余白",lw,0,ZMSIZE(24),iw), lw+iw, ph, sw, y);

			y += (ph+sh);

			if (PANEL_HEIGHT < y) PANEL_HEIGHT = y;

			jPanel_fonts.setPreferredSize(new Dimension(pw, PANEL_HEIGHT));
		}
		return jPanel_fonts;
	}

	private void addf(JComponent c, int width, int height, int x, int y) {
		CommonSwingUtils.putComponentOn(jPanel_fonts, c, width, height, x, y);
	}

	private void setFonts() {
		//
		jCBP_showStart.setSelected(origenv.getShowStart());
		//
		if ( ! origenv.getTitleFont().equals("") ) {
			jCBX_titleFont.setSelectedItem(origenv.getTitleFont());
		}
		else if ( ! origenv.getFontName().equals("") ) {
			jCBX_titleFont.setSelectedItem(origenv.getFontName());
		}
		jSP_titleFontSize.setValue(origenv.getTitleFontSize());
		jLabel_titleFontColor.setChoosed(origenv.getTitleFontColor());
		setFontStyles((JNETable) jScrollPane_titleFontStyle.getViewport().getView(), origenv.getTitleFontStyle());
		//
		jCBP_showDetail.setSelected(origenv.getShowDetail());
		if ( ! origenv.getDetailFont().equals("") ) {
			jCBX_detailFont.setSelectedItem(origenv.getDetailFont());
		}
		else if ( ! origenv.getFontName().equals("") ) {
			jCBX_detailFont.setSelectedItem(origenv.getFontName());
		}
		jSP_detailFontSize.setValue(origenv.getDetailFontSize());
		jLabel_detailFontColor.setChoosed(origenv.getDetailFontColor());
		setFontStyles((JNETable) jScrollPane_detailFontStyle.getViewport().getView(), origenv.getDetailFontStyle());
		jSP_detailTab.setValue(origenv.getDetailTab());
		jCBX_aaMode.setSelectedItem(origenv.getPaperAAMode());
	}
	private void setFontStyles(JNETable jt, ArrayList<JTXTButton.FontStyle> fsa) {
		for ( int row=0; row<jt.getRowCount(); row++ ) {
			jt.setValueAt(false, row, 0);
			for ( JTXTButton.FontStyle fs : fsa ) {
				if ( fs == jt.getValueAt(row, 1) ) {
					jt.setValueAt(true, row, 0);
					break;
				}
			}
		}
	}

	private JScrollPane getJScrollPane_fontstyle() {
		JScrollPane jScrollPane = new JScrollPane();
		jScrollPane.setViewportView(getJTable_fontstyle());
		jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		return jScrollPane;
	}
	private JNETable getJTable_fontstyle() {

		// ヘッダの設定
		String[] colname = {"ﾁｪｯｸ", "スタイル"};
		int[] colwidth = {ZMSIZE(50),ZMSIZE(ITEM_WIDTH-50)};

		//
		DefaultTableModel model = new DefaultTableModel(colname, 0);
		JNETable jTable = new JNETable(model, false) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
					return (column == 0);
			}
		};
//		jTable.setRowHeight(ZMSIZE(jTable.getRowHeight()));
		jTable.setAutoResizeMode(JNETable.AUTO_RESIZE_OFF);
		DefaultTableColumnModel columnModel = (DefaultTableColumnModel)jTable.getColumnModel();
		TableColumn column = null;
		for (int i = 0 ; i < columnModel.getColumnCount() ; i++){
			column = columnModel.getColumn(i);
			column.setPreferredWidth(colwidth[i]);
		}

		// にゃーん
		jTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// エディタに手を入れる
		DefaultCellEditor editor = new DefaultCellEditor(new JCheckBox() {

			private static final long serialVersionUID = 1L;

			@Override
			public int getHorizontalAlignment() {
				return JCheckBox.CENTER;
			}
		});
		jTable.getColumn("ﾁｪｯｸ").setCellEditor(editor);
		// レンダラに手を入れる
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus, int row, int column) {
				//
				JCheckBox cBox = new JCheckBox();
				cBox.setHorizontalAlignment(JCheckBox.CENTER);
				//
				Boolean b = (Boolean)value;
				cBox.setSelected(b.booleanValue());
				//
				if (isSelected) {
					cBox.setBackground(table.getSelectionBackground());
				}
				else {
					cBox.setBackground(table.getBackground());
				}
				return cBox;
			}
		};
		jTable.getColumn("ﾁｪｯｸ").setCellRenderer(renderer);

		//
		for ( JTXTButton.FontStyle fs : JTXTButton.FontStyle.values() ) {
			Object[] data = { false,fs };
			model.addRow(data);
		}
		return jTable;
	}

	/*
	 * サイズのタブ
	 */

	private JPanel getJPanel_bounds() {
		if (jPanel_bounds == null) {
			jPanel_bounds = new JPanel();

			jPanel_bounds.setLayout(new SpringLayout());

			int tw = ZMSIZE(TITLE_WIDTH);
			int lw = ZMSIZE(LABEL_WIDTH);
			int iw = ZMSIZE(ITEM_WIDTH);
			int sw = ZMSIZE(SEP_WIDTH);
			int swn = ZMSIZE(SEP_WIDTH_NARROW);
			int pw = ZMSIZE(PANEL_WIDTH);

			int ph = ZMSIZE(PARTS_HEIGHT);
			int sh = ZMSIZE(SEP_HEIGHT);
			int shn = ZMSIZE(SEP_HEIGHT_NARROW);

			int y = shn;
			addb(new JTitleLabel("サイズの設定"), tw, ph, swn, y);

			y += (ph+shn);
			addb(jSP_width = new JSliderPanel("幅",lw,ZMSIZE(50),ZMSIZE(300),iw), lw+iw, ph, sw, y);

			y += (ph+shn);
			addb(jSP_height = new JSliderPanel("高さ(pt/H)",lw,ZMSIZE(30),ZMSIZE(600),ZMSIZE(STEPBY),iw), lw+iw, ph, sw, y);

			y += (ph+shn);
			addb(jSP_timebarPosition = new JSliderPanel("現在時刻線(分)",lw,1,ZMSIZE(180),iw), lw+iw, ph, sw, y);

			y += (ph+shn);
			addb(jSP_restimebar= new JSliderPanel("予約バー幅",lw,0,ZMSIZE(12),iw), lw+iw, ph, sw, y);

			y += (ph+sh);
			addb(new JTitleLabel("予約枠の設定"), tw, ph, swn, y);

			y += (ph+shn);
			addb(jLabel_execon = new JCCLabel("実行ONの文字色",origenv.getExecOnFontColor(),false,this,ccwin), iw, ph, sw+lw, y);

			y += (ph+shn);
			addb(jLabel_execoff = new JCCLabel("実行OFFの文字色",origenv.getExecOffFontColor(),false,this,ccwin), iw, ph, sw+lw, y);

			y += (ph+sh);
			addb(new JTitleLabel("ピックアップ枠の設定"), tw, ph, swn, y);

			y += (ph+shn);
			addb(jLabel_pickup = new JCCLabel("ピックアップの枠色",origenv.getPickedColor(),true,this,ccwin), iw, ph, sw+lw, y);

			y += (ph+shn);
			addb(jLabel_pickupFont = new JCCLabel("ピックアップの文字色",origenv.getPickedFontColor(),false,this,ccwin), iw, ph, sw+lw, y);

			y += (ph+sh);
			addb(new JTitleLabel("予約待機枠の設定"), tw, ph, swn, y);

			y += (ph+shn);
			addb(jSP_matchedBorderThickness = new JSliderPanel("太さ",lw,1,16,iw), lw+iw, ph, sw, y);

			y += (ph+shn);
			addb(jLabel_matchedBorderColor = new JCCLabel("予約待機枠(番組追跡)",origenv.getMatchedBorderColor(),true,this,ccwin), iw, ph, sw+lw, y);

			y += (ph+shn);
			addb(jLabel_matchedKeywordBorderColor = new JCCLabel("予約待機枠(ｷｰﾜｰﾄﾞ検索)",origenv.getMatchedKeywordBorderColor(),true,this,ccwin), iw, ph, sw+lw, y);

			y += (ph+sh);
			addb(new JTitleLabel("フォントのアンチエイリアス設定"), tw, ph, swn, y);

			y += (ph+shn);
			addb(jCBX_aaMode = new JComboBoxPanel("アンチエイリアス",lw,iw,true), lw+iw, ph, sw, y);
			for ( AAMode aam : AAMode.values() ) {
				jCBX_aaMode.addItem(aam);
			}

			y += (ph+sh);

			if (PANEL_HEIGHT < y) PANEL_HEIGHT = y;

			jPanel_bounds.setPreferredSize(new Dimension(pw, PANEL_HEIGHT));
		}
		return jPanel_bounds;
	}

	private void addb(JComponent c, int width, int height, int x, int y) {
		CommonSwingUtils.putComponentOn(jPanel_bounds, c, width, height, x, y);
	}

	private void setBounds() {
		jSP_width.setValue(origbnd.getBangumiColumnWidth());
		jSP_height.setValue(Math.round(origbnd.getPaperHeightMultiplier()*(float)60/(float)STEPBY));
		jSP_timebarPosition.setValue(origbnd.getTimelinePosition());
		jSP_restimebar.setValue(origenv.getResTimebarWidth());
		jLabel_execon.setChoosed(origenv.getExecOnFontColor());
		jLabel_execon.setBackground(Color.RED);
		jLabel_execoff.setChoosed(origenv.getExecOffFontColor());
		jLabel_execoff.setBackground(Color.RED);
		jLabel_pickup.setChoosed(origenv.getPickedColor());
		jLabel_pickupFont.setChoosed(origenv.getPickedFontColor());
		jLabel_pickupFont.setBackground(Color.RED);
		jLabel_matchedBorderColor.setChoosed(origenv.getMatchedBorderColor());
		jLabel_matchedKeywordBorderColor.setChoosed(origenv.getMatchedKeywordBorderColor());
		jSP_matchedBorderThickness.setValue(origenv.getMatchedBorderThickness());
		/*
		if ( ! origenv.getShowStart() && ! origenv.getShowDetail() ) {
			jCBP_lightProgramView.setSelected(true);
		}
		else {
			jCBP_lightProgramView.setSelected(false);
		}
		*/
	}

	/*
	 * フォントサイズを考慮した長さを取得する
	 */
	private int ZMSIZE(int size){ return origenv.ZMSIZE(size); }

	/*******************************************************************************
	 * 独自部品
	 ******************************************************************************/

	private class JTitleLabel extends JLabel {

		private static final long serialVersionUID = 1L;

		public JTitleLabel(String s) {
			super(s);
			this.setForeground(Color.RED);
			//this.setFont(this.getFont().deriveFont(this.getFont().getStyle()|Font.BOLD));
		}
	}

}
