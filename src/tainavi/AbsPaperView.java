package tainavi;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SpringLayout;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.MouseInputListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import tainavi.TVProgram.ProgGenre;
import tainavi.TVProgram.ProgType;
import tainavi.TVProgramIterator.IterationType;
import tainavi.VWMainWindow.MWinTab;


/**
 * 新聞形式タブのクラス
 * @since 3.15.4β　{@link Viewer}から分離
 */
public abstract class AbsPaperView extends JPanel implements TickTimerListener,HDDRecorderListener {

	private static final long serialVersionUID = 1L;

	public static String getViewName() { return "新聞形式"; }

	public void setDebug(boolean b) { debug = b; }
	private static boolean debug = false;


	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/

	protected abstract Env getEnv();
	protected abstract Bounds getBoundsEnv();
	protected abstract PaperColorsMap getPaperColorMap();
	protected abstract ChannelSort getChannelSort();

	protected abstract TVProgramList getTVProgramList();
	protected abstract HDDRecorderList getRecorderList();

	protected abstract StatusWindow getStWin();
	protected abstract StatusTextArea getMWin();

	protected abstract AbsReserveDialog getReserveDialog();
	protected abstract Component getParentComponent();

	protected abstract void ringBeep();

	// クラス内のイベントから呼び出されるもの

	/**
	 * タブが開いた
	 */
	protected abstract void onShown();
	/**
	 * タブが閉じた
	 */
	protected abstract void onHidden();

	/**
	 * マウス右クリックメニューを表示する
	 */
	protected abstract void showPopupMenu(
			final JComponent comp,	final ProgDetailList tvd, final int x, final int y, final String clickedDateTime);

	/**
	 * 予約マーク・予約枠を更新してほしい
	 */
	protected abstract void updateReserveDisplay();

	/**
	 * ピックアップに追加してほしい
	 */
	protected abstract void addToPickup(final ProgDetailList tvd);

	protected abstract boolean isTabSelected(MWinTab tab);
	protected abstract void setSelectedTab(MWinTab tab);

	protected abstract boolean isFullScreen();
	/**
	 * ページャーコンボボックスを更新してほしい
	 */
	protected abstract void setPagerEnabled(boolean b);
	protected abstract int getPagerCount();
	protected abstract int getSelectedPagerIndex();
	protected abstract void setSelectedPagerIndex(int idx);
	protected abstract void setPagerItems(TVProgramIterator pli, int curindex);

	protected abstract String getExtensionMark(ProgDetailList tvd);
	protected abstract String getOptionMark(ProgDetailList tvd);
	protected abstract String getPostfixMark(ProgDetailList tvd);

	/**
	 * ツリーペーンの幅の変更を保存してほしい
	 */
	protected abstract void setDividerEnvs(int loc);

	// 前のページに移動する
	protected abstract void moveToPrevPage();

	// 次のページに移動する
	protected abstract void moveToNextPage();

	/*******************************************************************************
	 * 呼び出し元から引き継いだもの
	 ******************************************************************************/

	// オブジェクト
	private final Env env = getEnv();
	private final Bounds bounds = getBoundsEnv();
	private final PaperColorsMap pColors = getPaperColorMap();
	private final ChannelSort chsort = getChannelSort();

	private final TVProgramList tvprograms = getTVProgramList();
	private final HDDRecorderList recorders = getRecorderList();

	private final StatusWindow StWin = getStWin();			// これは起動時に作成されたまま変更されないオブジェクト
	private final StatusTextArea MWin = getMWin();			// これは起動時に作成されたまま変更されないオブジェクト
	private final AbsReserveDialog rD = getReserveDialog();	// これは起動時に作成されたまま変更されないオブジェクト

	private final Component parent = getParentComponent();	// これは起動時に作成されたまま変更されないオブジェクト

	// メソッド
	private void StdAppendMessage(String message) { System.out.println(CommonUtils.getNow() + message); }
	private void StdAppendError(String message) { System.err.println(CommonUtils.getNow() + message); }
	//private void StWinSetVisible(boolean b) { StWin.setVisible(b); }
	//private void StWinSetLocationCenter(Component frame) { CommonSwingUtils.setLocationCenter(frame, (VWStatusWindow)StWin); }



	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	private final String MSGID = "["+getViewName()+"] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final String DBGID = "[DEBUG]"+MSGID;

	private final int DASHBORDER_LENGTH = 6;	// ダッシュの長さ
	private final int DASHBORDER_SPACE = 4;		// ダッシュの間隔

	private static final String TreeExpRegFile_Paper = "env"+File.separator+"tree_expand_paper.xml";

	private static final int TIMEBAR_START = Viewer.TIMEBAR_START;

	//
	private static final String TUNERLABEL_PICKUP = "PICKUP";

	// 定数ではないが

	/**
	 * 現在時刻追従スクロールで日付がかわったかどうかを確認するための情報を保持する
	 */
	private String prevDT4Now = CommonUtils.getDate529(0,true);
	private String prevDT4Tree = prevDT4Now;

	/**
	 * 番組枠フレームバッファのサイズ
	 */
	private int framebuffersize = 512;


	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// コンポーネント

	private JSplitPane jSplitPane_main = null;
	private JDetailPanel jTextPane_detail = null;
	private JSplitPane jSplitPane_view = null;
	private JPanel jPanel_tree = null;
	private JScrollPane jScrollPane_tree_top = null;
	private JTreeLabel jLabel_tree = null;
	private JScrollPane jScrollPane_tree = null;
	private JTree jTree_tree = null;
	private JScrollPane jScrollPane_space_main = null;
	private JLayeredPane jLayeredPane_space_main_view = null;
	private ArrayList<JTaggedLayeredPane> jLayeredPane_space_main_view_byDate = null;
	private JLayeredPane jLayeredPane_space_main_view_byMakeshift = null;
	private JPanel jPanel_space_top_view = null;
	private JLayeredPane jLayeredPane_space_side_view = null;
	private JViewport vport = null;

	private final JTimeline jLabel_timeline = new JTimeline();
	private final JTBTimeline jLabel_timeline_tb = new JTBTimeline();

	private DefaultMutableTreeNode paperRootNode = null;	// 新聞形式のツリー
	private DefaultMutableTreeNode dateNode = null;
	private DefaultMutableTreeNode dgNode = null;
	private DefaultMutableTreeNode bsNode = null;
	private DefaultMutableTreeNode csNode = null;
	private DefaultMutableTreeNode centerNode = null;
	private DefaultMutableTreeNode passedNode = null;

	private DefaultMutableTreeNode defaultNode = null;

	// コンポーネント以外

	// 番組枠をしまっておくバッファ（newが遅いので一回作ったら捨てない）
	private ArrayList<JTXTButton> frameUsed = new ArrayList<JTXTButton>();			// 画面に表示されている番組枠
	private ArrayList<JTXTButton> frameUnused = new ArrayList<JTXTButton>();		// 未使用の予備
	private ArrayList<JTXTButton> frameUsedByDate = new ArrayList<JTXTButton>();	// 高速描画時の日付別ペーンに表示されている番組枠。高速描画時も、過去ログはframeUsedが使われる

	// 予約枠をしまっておくバッファ（検索用）
	private ArrayList<JRMLabel> reserveBorders = new ArrayList<JRMLabel>();

	// 予約時間枠をしまっておくバッファ
	private ArrayList<JRTLabel> resTimeBorders = new ArrayList<JRTLabel>();

	// ツリーの展開状態の保存場所
	TreeExpansionReg ter = null;

	DefaultMutableTreeNode nowNode = null;

	// 現在放送中のタイマー
	private boolean timer_now_enabled = false;

	private IterationType cur_tuner = null;


	// 予約待機枠と番組枠
	private final DashBorder dborder = new DashBorder(Color.RED,env.getMatchedBorderThickness(),DASHBORDER_LENGTH,DASHBORDER_SPACE);
	private final DashBorder dborderK = new DashBorder(Color.MAGENTA,env.getMatchedBorderThickness(),DASHBORDER_LENGTH,DASHBORDER_SPACE);
	private final LineBorder lborder = new ChippedBorder(Color.BLACK,1);

	private final LineBorder detailInfoLockedBorder = new LineBorder(Color.RED, 1);
	private final LineBorder detailInfoFreeBorder = new LineBorder(new Color(0,0,0,0), 1);

	private float paperHeightZoom = 1.0F;

	private boolean byCenterMode = false;	// １放送局の１週間分を表示するモード化か
	private String startDate = CommonUtils.getDate529(0,  true);	// 放送局別の場合の開始日

	private int dividerLocationOnShown = 0;

	// スクロール関係
	private int buttonPressed = 0;

	// 放送局別表示の日数間隔
	private int byCenterModeDayInterval = 1;

	/*
	 * ページャーのEnable/Disableを更新する
	 */
	private void updatePagerEnabled(){
		setPagerEnabled(env.isPagerEnabled() && !byCenterMode);
	}

	/*
	 * Envの内容を反映する
	 */
	public void reflectEnv(){
		jTree_tree.setRowHeight(jTree_tree.getFont().getSize()+1);
		initDetailHeight();
	}

	/**
	 * 現在時刻線のオブジェクト
	 */
	private class JTimeline extends JLabel {
		public JTimeline(){
			super();
			setBorder(new LineBorder(Color.RED,2));
			setBackground(Color.RED);
			setOpaque(true);
		}
		private static final long serialVersionUID = 1L;
		private int minpos = 0;

		public int setMinpos(int x, int minpos, float multiplier) {
			if ( minpos >= 0 ) {
				this.minpos = minpos;
			}

			int timeline = Math.round(this.minpos*multiplier);
			this.setLocation(x,timeline);

			return timeline;
		}
	}

	/**
	 * 現在時刻のオブジェクト（タイムバー用）
	 */
	private class JTBTimeline extends JLabel {
		public JTBTimeline(){
			super();
			setBorder(new EmptyBorder(0,0,0,0));
			setBackground(Color.RED);
			setOpaque(true);

			label = "";
		}

		public void setTime(GregorianCalendar c){
			switch(env.getTimelineLabelDispMode()){
			case 0:
				label = ""; break;
			case 1:
				label = new SimpleDateFormat("mm").format(c.getTime());
				break;
			case 2:
				label = CommonUtils.getTime(c);
				break;
			case 3:
				label = CommonUtils.getTime529(c);
				break;
			}
		}

		private static final long serialVersionUID = 1L;
		private static final int height = 20;

		private int minpos = 0;
		private BufferedImage image = null;
		private String label;

		@Override
		public void repaint() {
			image = null;
			super.repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			// 初回描画時
			if (image == null) {
				Dimension  d  = this.getSize();
				int w = d.width;
				int h = d.height;
				image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2 = (Graphics2D)image.createGraphics();

				if (env.getTimelineLabelDispMode() == 0){
					// 全体を赤く塗りつぶす
					g2.setColor(Color.RED);
					int xp[] = {-1,    w,  w, -1};
					int yp[] = {h-3, h-3, h+1, h+1};
					g2.fillPolygon(xp, yp, 4);
				}
				else{
					// 書き込む文字列のサイズを算出する
					Font f = this.getFont();
					Font f2 = f.deriveFont(f.getStyle(), 11);
					g2.setFont(f2);
					FontMetrics fm = g2.getFontMetrics();
					Rectangle2D r = fm.getStringBounds(label,g2);
					int wt = (int)r.getWidth();
					int ht = (int)r.getHeight();

					// 全体を赤く塗りつぶす
					g2.setColor(Color.RED);
					int xp[] = {-1, wt+2, wt+5, w, w, -1};
					int yp[] = {h-ht-1, h-ht-1, h-3, h-3, h+1, h+1};
					g2.fillPolygon(xp, yp, 6);

					// 書き込む位置を決定する
					int x = 1;
					int y = h-3;

					// 書き込む
					AttributedString as = new AttributedString(label);
					as.addAttribute(TextAttribute.FOREGROUND, Color.WHITE);
					as.addAttribute(TextAttribute.BACKGROUND, Color.OPAQUE);
					as.addAttribute(TextAttribute.FONT, f2);
					AttributedCharacterIterator ac = as.getIterator();
					g2.drawString(ac, x, y);
				}
			}

			// 反映
			g.drawImage(image, 0, 0, this);
		}

		public int setMinpos(int minpos, float multiplier) {
			if ( minpos >= 0 ) {
				this.minpos = minpos;
			}

			int timeline = Math.round(this.minpos*multiplier);

			Rectangle r = this.getBounds();
			r.x = 0;
			r.y = timeline+3-height;
			r.width  = bounds.getTimebarColumnWidth()+env.getResTimebarWidth();
			r.height = height;
			this.setBounds(r);

			return timeline;
		}

		public void setLabel(String s){
			label = s;
			repaint();
		}
	}

	//
	private class DetailInfo {
		String label;
		String text;
	}

	private ProgDetailList detailInfoData = null;

	private boolean isDetailInfoLocked() { return detailInfoData != null; }

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public AbsPaperView() {

		super();

		this.setLayout(new BorderLayout());
		this.add(getJSplitPane_main(), BorderLayout.CENTER);

		// タブが開いたり閉じたりしたときの処理
		this.addComponentListener(cl_shownhidden);
	}



	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	// 主に他のクラスから呼び出されるメソッド

	public String getFrameBufferStatus() { return String.format("%d/%d",frameUsed.size(),framebuffersize); }

	/**
	 * 現在日時表示にリセット
	 */
	public void jumpToNow() {
		if ( nowNode != null ) {
			this.startDate = CommonUtils.getDate529(0,  true);
			TreePath tp = new TreePath(nowNode.getPath());
			setSelectionPath(null);
			setSelectionPath(tp);
		}
	}

	/**
	 * ツールバーから過去ログへのジャンプ
	 */
	public boolean jumpToPassed(String passed) {

		// タイマーは止める
		stopTimer();

		GregorianCalendar c = CommonUtils.getCalendar(passed);
		String adate = CommonUtils.getDate(c);

		// 指定日付に移動して放送局の位置を確認する
		TVProgramIterator pli = redrawByDateWithCenter(null,adate);
		if ( pli == null ) {
			// どちらにもない
			MWin.appendError(ERRID+"ジャンプ先の日付がみつかりません: "+adate);
			ringBeep();
			return false;
		}

		// 新聞形式に移動
		if ( ! isTabSelected(MWinTab.PAPER) ) {
			setSelectedTab(MWinTab.PAPER);
		}

		return true;
	}

	/**
	 * リスト形式・本体予約一覧からの目的の番組へジャンプ
	 */
	public boolean jumpToBangumi(String center, String startdt) {

		// タイマーは止める
		stopTimer();

		// 日付群
		GregorianCalendar c = CommonUtils.getCalendar(startdt);
		int hour = c.get(Calendar.HOUR_OF_DAY);
		int min  = c.get(Calendar.MINUTE);

		String adate = CommonUtils.getDate(c);
		String adate529 = CommonUtils.getDate529(c,true);


		// 指定日付に移動して放送局の位置を確認する
		TVProgramIterator pli = redrawByDateWithCenter(center,adate529);
		if ( pli == null ) {
			// どちらにもない
			MWin.appendError(ERRID+"ジャンプ先の日付がみつかりません: "+adate529);
			ringBeep();
			return false;
		}

		// 新聞形式に移動
		if ( ! isTabSelected(MWinTab.PAPER) ) {
			setSelectedTab(MWinTab.PAPER);
		}

		/*
		 * マウスカーソル移動
		 */

		// 横の列
		int crindex = pli.getIndex(center);
		if ( crindex == -1 ) {
			MWin.appendError(ERRID+"「"+center+"」は有効な放送局ではありません");
			ringBeep();
			return false;
		}

		int x = 0;
		if ( env.isPagerEnabled() ) {
			int offset = env.getOffsetInPage(crindex);
			x = offset * bounds.getBangumiColumnWidth();
		}
		else {
			x = crindex * bounds.getBangumiColumnWidth();
		}

		// 縦の列
		int y = 0;
		if (adate529.equals(adate)) {
			if (hour < TIMEBAR_START) {
				hour = TIMEBAR_START;
				min = 0;
			}
		}
		else {
			hour += 24;
		}
		y = Math.round((float)((hour-TIMEBAR_START)*60+min)*bounds.getPaperHeightMultiplier()*paperHeightZoom);

		// 新聞面を移動する
		{
			// Viewのサイズ変更をJavaまかせにすると実際に表示されるまで変更されないので明示的に変更しておく
			Dimension dm = vport.getView().getPreferredSize();
			vport.setViewSize(dm);

			// 一旦位置情報をリセットする
			Point pos = new Point(0, 0);
			//vport.setViewPosition(pos);

			Rectangle ra = vport.getViewRect();
			pos.x = x + bounds.getBangumiColumnWidth()/2 - ra.width/2;
			pos.y = y - ra.height/4;

			// ViewのサイズがViewPortのサイズより小さい場合はsetViewPosition()が正しく動作しないので０にする
			if (pos.x < 0 || dm.width < ra.width) {
				pos.x=0;
			}
			else if ((dm.width - ra.width) < pos.x) {
				pos.x = dm.width - ra.width;
			}

			if (pos.y < 0 || dm.height < ra.height)  {
				pos.y=0;
			}
			else if ((dm.height - ra.height) < pos.y) {
				pos.y = dm.height - ra.height;
			}

			vport.setViewPosition(pos);
		}

		// マウスカーソルを移動する
		{
			Point sc = vport.getLocationOnScreen();
			Point pos = vport.getViewPosition();

			Point loc = new Point();
			loc.x = sc.x + (x + bounds.getBangumiColumnWidth()/2) - pos.x;
			loc.y = sc.y + (y + Math.round(5*bounds.getPaperHeightMultiplier()*paperHeightZoom)) - pos.y;

			try {
				Robot robo = new Robot();
				robo.mouseMove(loc.x,loc.y);
				robo = null;
			} catch (AWTException e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	/**
	 * ページャーの選択変更により描画する
	 */
	public void redrawByPager() {
		Point vp = vport.getViewPosition();

		redrawByCurrentSelection();

		vport.setViewPosition(vp);
	}

	/*
	 * 現在の選択状態で再描画する
	 */
	public void redrawByCurrentSelection() {
		JTreeLabel.Nodes node = jLabel_tree.getNode();
		String value = jLabel_tree.getValue();
		if (node == null || value == null)
			return;

		stopTimer();

		switch ( node ) {
		case DATE:
		case TERRA:
		case BS:
		case CS:
			IterationType type = IterationType.ALL;
			switch( node ){
			case DATE:
				type = IterationType.ALL;
				break;
			case TERRA:
				type = IterationType.TERRA;
				break;
			case BS:
				type = IterationType.BS;
				break;
			case CS:
				type = IterationType.CS;
				break;
			default:
				break;
			}
			// 現在日時に移動する
			if ( JTreeLabel.Nodes.NOW.getLabel().equals(value) )
				redrawByNow(type);
			else
				redrawByDate(value, type);
			break;
		case BCAST:
			redrawByCenter(value);
			break;
		case PASSED:
			PassedProgram passed = tvprograms.getPassed();
			if ( passed.loadAllCenters(value) ) {
				attachSyoboPassed(passed);
				redrawByDate(value, IterationType.PASSED);
			}
			else {
				MWin.appendError(ERRID+"過去ログが存在しません: "+value);
				ringBeep();
			}
			break;
		default:
			break;
		}

		updatePagerEnabled();
	}

	/**
	 * 予約待機赤枠の描画（全部）
	 * @see #putReserveBorder(String, String, int)
	 */
	public void updateReserveBorder(String center) {

		// 予約の赤枠を表示する（上：日付別表示中、下：放送局別表示中）

		JTreeLabel.Nodes node = jLabel_tree.getNode();
		String value = jLabel_tree.getValue();

		switch ( node ) {
		case DATE:
		case TERRA:
		case BS:
		case CS:
			{
				// 日付別は４種類ある
				IterationType sTyp;
				switch ( node ) {
				case TERRA:
					sTyp = IterationType.TERRA;
					break;
				case BS:
					sTyp = IterationType.BS;
					break;
				case CS:
					sTyp = IterationType.CS;
					break;
				default:
					sTyp = IterationType.ALL;
					break;
				}

				// "現在日付"を現在日付にする
				String dt = value;
				if ( JTreeLabel.Nodes.NOW.getLabel().equals(dt) ) {
					dt = CommonUtils.getDate529(0,true);
				}
				if (dt == null){
					StWin.appendError(ERRID+"日付がnullです ");
					return;
				}

				TVProgramIterator pli = tvprograms.getIterator().build(chsort.getClst(), sTyp);

				// ページャーが有効なら表示すべきページ番号を取得する
				int colmin = 0;
				int colmax = pli.size();
				if ( env.isPagerEnabled() ) {
					int selectedpage = getSelectedPagerIndex();	// 予約枠の描画なのだから、ページ移動の必要はないはずだ
					if ( selectedpage >= 0 ) {
						colmin = env.getPageOffset(selectedpage);
						colmax = colmin + env.getCentersInPage(selectedpage)-1;
					}
					else {
						StWin.appendError(ERRID+"ページャーコンボボックスが不正です： "+selectedpage);
						return;
					}
				}

				if ( center != null ) {
					// 特定の放送局のみ更新
					int cnt = pli.getIndex(center);
					if ( colmin <= cnt && cnt <= colmax ) {
						int col = cnt - colmin;
						putReserveBorder(dt, center, col);
					}
				}
				else {
					// すべての放送局を更新
					int cnt = -1;
					for ( ProgList pl : pli ) {
						++cnt;
						if ( cnt < colmin ) {
							continue;
						}
						else if ( cnt > colmax ) {
							break;
						}

						int col = cnt - colmin;

						putReserveBorder(dt, pl.Center, col);
					}
				}

				// 予約時間枠の表示
				putResTimeBorder(dt);
			}
			break;

		case BCAST:
			{

				if (center != null && ! center.equals(value)) {
					// 更新の必要はない
					return;
				}
				if (center == null) {
					// 選択中の放送局で更新する
					center = value;
				}

				TVProgramIterator pli = tvprograms.getIterator().build(chsort.getClst(), IterationType.ALL);
				int cnt = tvprograms.getIterator().getIndex(center);
				if ( cnt == -1 ) {
					MWin.appendError(ERRID+"「"+center+"」は有効な放送局ではありません");
				}
				else {
					ProgList pl = pli.getP();
					for (int col=0; col<pl.pdate.size(); col++) {
						// 予約の赤枠を一週間分表示する
						putReserveBorder(pl.pdate.get(col).Date, center, col);
					}
				}
			}
			break;

		default:
			break;
		}
	}

	/**
	 *
	 */
	public void updateBangumiColumns() {
		for (JTXTButton b : frameUsed ) {
			ProgDetailList tvd = b.getInfo();
			if ( tvd.type == ProgType.PROG ) {
				_updPBorder(bounds, b);
			}
		}
	}

	/**
	 * 予約待機赤枠の描画（ツールバーからのトグル操作）
	 */
	public boolean toggleMatchBorder(boolean b) {

		// 状態を保存
		bounds.setShowMatchedBorder(b);

		_updPBorderAll(env, bounds, frameUsed);

		if ( env.getDrawcacheEnable() ) {
			_updPBorderAll(env, bounds, frameUsedByDate);
		}

		return bounds.getShowMatchedBorder();
	}


	/**
	 * 新聞枠の拡縮（ツールバーからの操作）
	 */
	public void setZoom(int n) {
		paperHeightZoom = n * 0.01F;
		updateBounds(env, bounds);
		updateRepaint();
	}

	/*
	 * 前後のノード（日付・放送局）に移動する
	 */
	public void changeNode(boolean down){
		Point vp = vport.getViewPosition();
		Dimension vs = vport.getSize();

		// 前後のノードを取得する
		TreePath path = getNextDatePath(down);
		if (path == null)
			return;

		// 前後のノードに移動する
		setSelectionPath(path);
		jLayeredPane_space_main_view.scrollRectToVisible(new Rectangle(vp, vs));
	}
	/*
	 * 上下に１ページ分スクロールする
	 */
	public void scrollPage(boolean down, boolean nodechange){
		Point vp = vport.getViewPosition();
		Dimension vs = vport.getSize();
		Dimension ps = vport.getView().getPreferredSize();

		// 一番上でかつマイナス方向にスクロールしたら
		if (vp.y == 0 && !down){
			if (!nodechange)
				return;

			// 前日のノードを取得する
			TreePath path = getNextDatePath(false);
			if (path == null)
				return;

			// 前日のノードに移動して一番下にスクロールする
			setSelectionPath(path);
			vp.y = ps.height - vs.height;
		}
		// 一番下でかつプラス方向にスクロールしたら
		else if (vp.y >=  ps.height - vs.height && down){
			if (!nodechange)
				return;

			// 翌日のノードを取得する
			TreePath path = getNextDatePath(true);
			if (path == null)
				return;

			// 翌日のノードに移動して一番上にスクロールする
			setSelectionPath(path);
			vp.y = 0;
		}
		// それ以外の場合はホイール量に応じてスクロールする
		else{
			vp.translate(0, down ? vs.height : -vs.height);
		}

		jLayeredPane_space_main_view.scrollRectToVisible(new Rectangle(vp, vs));
	}
	/**
	 * 新聞ペーンをクリアする。
	 */
	public void clearPanel() {

		// 番組枠の初期化
		/* 移動しました */

		// 予約枠の初期化
		for ( JRMLabel b : reserveBorders) {
			b.setVisible(false);
			jLayeredPane_space_main_view.remove(b);
		}
		reserveBorders.clear();

		// 予約時間枠の初期化
		for ( JRTLabel rb : resTimeBorders) {
			rb.setVisible(false);
			jLayeredPane_space_side_view.remove(rb);
		}
		resTimeBorders.clear();

		// タイムラインの削除
		if (jLabel_timeline != null && jLayeredPane_space_main_view != null) {
			jLayeredPane_space_main_view.remove(jLabel_timeline);
		}
		if (jLabel_timeline_tb != null && jLayeredPane_space_side_view != null) {
			jLayeredPane_space_side_view.remove(jLabel_timeline_tb);
		}

		// 時間枠・日付枠・放送局枠の初期化
		jPanel_space_top_view.removeAll();
		redrawTimebar(jLayeredPane_space_side_view);

		// 選択してないことにする
		//paper.jLabel_tree.setText("");
	}

	/**
	 * <P>新聞ペーンを選択する。
	 * <P>高速描画ＯＮの場合は、主ペーンのほかに複数の日付別ペーンが作成されるのでどれを利用するか選択する。
	 * @param pane : nullの場合、主ペーンを選択する。過去ログは常にnullで。
	 * @see #jLayeredPane_space_main_view_byMakeshift 主ペーン
	 * @see #jLayeredPane_space_main_view_byDate 日付別ペーン
	 */
	private void selectMainView(JLayeredPane pane) {

		// 表示開始位置を記憶する
		Point p = vport.getViewPosition();

		if (pane == null) {
			// 番組枠の初期化
			StdAppendMessage(MSGID+"番組枠描画バッファをリセット: "+frameUsed.size()+"/"+framebuffersize);
			for (int i=frameUsed.size()-1; i>=0; i--) {
				JTXTButton b = frameUsed.remove(i);
				b.setToolTipText(null);
				b.clean();
				frameUnused.add(b);
				//jLayeredPane_space_main_view_byMakeshift.remove(b);	// 削除しちゃダメよ？
			}

			if (jLayeredPane_space_main_view == jLayeredPane_space_main_view_byMakeshift) {
				return;
			}
			jScrollPane_space_main.setViewportView(jLayeredPane_space_main_view = jLayeredPane_space_main_view_byMakeshift);
		}
		else {
			if (jLayeredPane_space_main_view == pane) {
				return;
			}
			jScrollPane_space_main.setViewportView(jLayeredPane_space_main_view = pane);
		}

		// 表示開始位置を戻す
		vport.setViewPosition(p);
	}

	/**
	 * 現在時刻追従スクロールを開始する
	 */
	private void startTimer() {
		timer_now_enabled = true;
	}

	/**
	 * 現在時刻追従スクロールを停止する
	 */
	private boolean stopTimer() {
		jLabel_timeline.setVisible(false);
		jLabel_timeline_tb.setVisible(false);
		return (timer_now_enabled = false);
	}

	/**
	 * サイドツリーの「現在日時」を選択する
	 */
	public void selectTreeDefault() {
		if ( defaultNode != null ) setSelectionPath(new TreePath(defaultNode.getPath()));
	}

	/**
	 * サイドツリーの現在選択中のノードを再度選択して描画しなおす
	 */
	public void reselectTree() {
		String[] names = new String[] { jLabel_tree.getNode().getLabel(), jLabel_tree.getValue() };
		TreeNode[] nodes = ter.getSelectedPath(paperRootNode, names, 0);
		if (nodes != null) {
			TreePath tp = new TreePath(nodes);
			if ( tp != null ) {
				// 表示位置を記憶
				Point vp = vport.getViewPosition(); //= SwingUtilities.convertPoint(vport,0,0,label);
				// ツリー再選択
				setSelectionPath(null);
				setSelectionPath(tp);
				// 表示位置を復帰
				if (vp.x != 0 && vp.y != 0) {
					jLayeredPane_space_main_view.scrollRectToVisible(new Rectangle(vp, vport.getSize()));
				}
			}
		}
	}

	/**
	 * サイドツリーを開く
	 */
	public void setExpandTree() {
		jSplitPane_view.setDividerLocation(bounds.getTreeWidthPaper());
		jScrollPane_tree.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		jScrollPane_tree.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	}

	/**
	 * サイドツリーを閉じる
	 */
	public void setCollapseTree() {
		jSplitPane_view.setDividerLocation(bounds.getMinDivLoc());
		jScrollPane_tree.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		jScrollPane_tree.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	}

	/**
	 * サイドツリーの展開状態を設定ファイルに保存（鯛ナビ終了時に呼び出される）
	 */
	public void saveTreeExpansion() {
		ter.save();
	}

	/**
	 * 画面上部の番組詳細領域の表示のＯＮ／ＯＦＦ
	 */
	public void setDetailVisible(boolean aFlag) {
		jTextPane_detail.setVisible(aFlag);

		if (aFlag){
			int height = bounds.getPaperDetailHeight();

			if (! isTabSelected(MWinTab.PAPER))
				dividerLocationOnShown = height;
			else
				jSplitPane_main.setDividerLocation(height);

			bounds.setPaperDetailHeight(height);
		}
	}

	/**
	 * スクリーンショット用
	 */
	public Component getCenterPane() {
		return jPanel_space_top_view;
	}

	/**
	 * スクリーンショット用
	 */
	public Component getTimebarPane() {
		return jLayeredPane_space_side_view;
	}

	/**
	 * スクリーンショット用
	 */
	public Component getCurrentPane() {
		return jLayeredPane_space_main_view;
	}

	/**
	 * スクリーンショット用
	 */
	public String getCurrentView() {
		return jLabel_tree.getView();
	}

	/**
	 * 高速描画ＯＮの場合に日付別ペーンを一気に全部描画する
	 */
	public void buildMainViewByDate() {

		if (env.getDebug()) System.out.println(DBGID+"CALLED buildMainViewByDate()");

		if (jLayeredPane_space_main_view_byMakeshift == null) {
			jLayeredPane_space_main_view_byMakeshift = new JLayeredPane();
			for (int i=0; i<framebuffersize; i++) {
				JTXTButton b2 = new JTXTButton();
				b2.clean();
				jLayeredPane_space_main_view_byMakeshift.add(b2);
				jLayeredPane_space_main_view_byMakeshift.setLayer(b2, 0);

				// リスナーを設定する
				b2.addMouseListener(ml_risepopup);
				b2.addMouseMotionListener(ml_risepopup);
				frameUnused.add(b2);
			}
			StdAppendMessage(MSGID+"番組枠描画バッファを初期化: "+framebuffersize);
		}

		// ページャー機能とは排他
		if (env.isPagerEnabled() || ! env.getDrawcacheEnable()) {
			jLayeredPane_space_main_view_byDate = null;
			return;
		}

		jLayeredPane_space_main_view_byDate = new ArrayList<JTaggedLayeredPane>();
		frameUsedByDate = new ArrayList<JTXTButton>();
		new SwingBackgroundWorker(true) {

			@Override
			protected Object doWorks() throws Exception {
				//
				int dogDays = (env.getExpandTo8())?(8):(7);
				//
				for ( int y=0; y < dogDays; y++ ) {
					jLayeredPane_space_main_view_byDate.add(new JTaggedLayeredPane());
				}
				for ( int y=0; y < dogDays; y++ ) {
					String day = CommonUtils.getDate529(y*86400,true);

					jLayeredPane_space_main_view_byDate.get(y).setTagstr(day);

					StWin.appendMessage(MSGID+"番組表を構築します："+day);
					redrawByDate(day,IterationType.ALL);
				}
				return null;
			}

			@Override
			protected void doFinally() {
			}
		}.execute();
	}

	/**
	 * ツリーのリスナーを止める
	 */
	private void stopTreeListener() {
		jTree_tree.removeTreeSelectionListener(tsl_nodeselected);
	}

	/**
	 * ツリーのリスナーを動かす
	 */
	private void startTreeListener() {
		jTree_tree.addTreeSelectionListener(tsl_nodeselected);
	}

	/**
	 * 日付でサブノード作成
	 */
	public  void redrawTreeByDate() {

		stopTreeListener();
		TreePath tp = jTree_tree.getSelectionPath();

		_redrawTreeByDate(dateNode);
		_redrawTreeByDate(dgNode);
		_redrawTreeByDate(bsNode);
		_redrawTreeByDate(csNode);

		if (tp != null)
			reselectTree();
		jTree_tree.updateUI();

		startTreeListener();
	}

	private void _redrawTreeByDate(DefaultMutableTreeNode parent) {

		// ★★★ でふぉるとのーど ★★★
		DefaultMutableTreeNode nNode = new DefaultMutableTreeNode(JTreeLabel.Nodes.NOW.getLabel());
		if ( parent == dateNode ) {
			nowNode = nNode;
			defaultNode = nNode;
		}

		parent.removeAllChildren();
		parent.add(nNode);
		int dogDays = (env.getExpandTo8())?(8):(7);
		for ( int i=0; i<dogDays; i++ ) {
			parent.add(new DefaultMutableTreeNode(CommonUtils.getDate529(i*86400, true)));
		}
	}

	/**
	 * 放送局名でサブノード作成
	 */
	public void redrawTreeByCenter() {

		stopTreeListener();
		TreePath tp = jTree_tree.getSelectionPath();

		centerNode.removeAllChildren();
		TVProgramIterator pli = tvprograms.getIterator();
		pli.build(chsort.getClst(), IterationType.ALL);
		for ( ProgList pl : pli ) {
			centerNode.add(new DefaultMutableTreeNode(pl.Center));
		}

		setSelectionPath(tp);
		jTree_tree.updateUI();
		startTreeListener();
	}

	/**
	 * 過去ログ日付でサブノード作成
	 */
	public void redrawTreeByPassed() {

		stopTreeListener();
		TreePath tp = jTree_tree.getSelectionPath();

		passedNode.removeAllChildren();
		if ( env.getUsePassedProgram() ) {
			String[] dd = new PassedProgram().getDateList(env.getPassedLogLimit());
			for ( int i=1; i<dd.length && i<=env.getPassedLogLimit(); i++ ) {
				passedNode.add(new DefaultMutableTreeNode(dd[i]));
			}
		}

		setSelectionPath(tp);
		jTree_tree.updateUI();
		startTreeListener();
	}

	/**
	 *  時刻の列を生成
	 */
	private void redrawTimebar(JLayeredPane jLayeredPane_space_side_view2)
	{
		jLayeredPane_space_side_view2.removeAll();

		float phm60 = 60.0F * bounds.getPaperHeightMultiplier() * paperHeightZoom;

		for (int row=0; row<24; row++) {

			int hour = row+TIMEBAR_START;

			JTimebarLabel b0 = new JTimebarLabel(Integer.toString(hour));

			if ( hour >=6 && hour <= 11 ) {
				b0.setBackground(env.getTimebarColor());
			}
			else if ( hour >=12 && hour <= 17 ) {
				b0.setBackground(env.getTimebarColor2());
			}
			else if ( hour >=18 && hour <= 23 ) {
				b0.setBackground(env.getTimebarColor3());
			}
			else {
				b0.setBackground(env.getTimebarColor4());
			}
			b0.setOpaque(true);
			b0.setBorder(lborder);
			b0.setHorizontalAlignment(JLabel.CENTER);

			b0.setBounds(
					0,
					(int) Math.ceil((float)row*phm60),
					bounds.getTimebarColumnWidth(),
					(int) Math.ceil(phm60));

			jLayeredPane_space_side_view2.add(b0);
		}

		Dimension d = jLayeredPane_space_side_view2.getMaximumSize();
		d.width = bounds.getTimebarColumnWidth()+env.getResTimebarWidth();
		d.height = (int) Math.ceil(24*phm60);
		jLayeredPane_space_side_view2.setPreferredSize(d);
		jLayeredPane_space_side_view2.updateUI();
	}


	/**
	 * 現在日時に移動する
	 * @see #redrawByDate(String, IterationType)
	 */
	private void redrawByNow(final IterationType tuner) {

		// 古いタイマーの削除
		stopTimer();

		// 移動汁！！
		redrawByDate(CommonUtils.getDate529(0,true),tuner);

		// 時間線をひく
		redrawTimeline(true, true);
	}

	/*
	 * 放送局名からプログラムリストを取得する
	 */
	private ProgList getProgListFromCenter(String center){
		for (int np=0; np<tvprograms.size(); np++) {
			TVProgram tvp = tvprograms.get(np);
			if (tvp.getType() != ProgType.PROG)
				continue;

			for (ProgList pl : tvp.getCenters()) {
				if (pl.enabled == true && pl.Center.equals(center))
					return pl;
			}
		}

		return null;
	}

	/*
	 * タイムラインを描画する
	 */
	private void redrawTimeline(boolean vpos, boolean reset){
		if (jLabel_timeline == null | jLabel_timeline_tb == null || vport == null)
			return;

		// 第１階層のノード、第２階層のノード名を取得する
		JTreeLabel.Nodes node1 = jLabel_tree.getNode();
		String node2 = jLabel_tree.getValue();
		if (node2 == null)
			node2 = "";

		// 当日の日付を計算する
		int correct = 0; // 24:00-28:59迄は前日の日付になる
		GregorianCalendar c = CommonUtils.getCalendar(0);
		if ( CommonUtils.isLateNight(c) ) {
			c.add(Calendar.DATE, -1);
			correct += 24;
		}
		String today = CommonUtils.getDate(c);

		// 時間線をひく
		Dimension dm = jLayeredPane_space_main_view.getPreferredSize();
		int width = dm.width;	// 幅のデフォルトはメインビューの幅とする
		int height = 3;
		int x = 0;	// 時間線の左端のX座標

		// 放送局別の場合⇒表示する
		boolean visible = false;
		if (node1 == JTreeLabel.Nodes.BCAST){
			visible = true;

			// 該当放送局のプログラムリストを取得する
			ProgList pl = getProgListFromCenter(node2);
			if (pl != null){
				// 当日のプログラムのインデックスから左端のX座標を求める
				for (int nd=0; nd<pl.pdate.size(); nd++){
					ProgDateList pcl = pl.pdate.get(nd);
					if (pcl.Date.equals(today)){
						x = bounds.getBangumiColumnWidth()*nd;
						break;
					}
				}
			}

			// 幅は一枠分とする
			width = bounds.getBangumiColumnWidth();
		}
		// すべて、地上局、ＢＳ、ＣＳの場合で、当日ないし「現在放送中」のノードの場合⇒表示する
		else if ((node1 == JTreeLabel.Nodes.DATE || node1 == JTreeLabel.Nodes.BS ||
				node1 == JTreeLabel.Nodes.CS || node1 == JTreeLabel.Nodes.TERRA) &&
			(today.equals(node2) || dateNode.getChildAt(0).toString().equals(node2))){
			visible = true;
		}

		jLabel_timeline.setVisible(visible);
		jLabel_timeline_tb.setVisible(visible);

		// タイムラインを表示する場合
		if (visible){
			// タイムラインを初期化する
			jLabel_timeline.setBounds(0, 0, width, height);
			jLayeredPane_space_main_view.add(jLabel_timeline);
			jLayeredPane_space_main_view.setLayer(jLabel_timeline, 2);

			// ビュー上の位置を計算してタイムラインにセットする
			float mul = bounds.getPaperHeightMultiplier()*paperHeightZoom;
			int minpos_new = (c.get(Calendar.HOUR_OF_DAY)-TIMEBAR_START+correct)*60+c.get(Calendar.MINUTE);
			int timeline_vpos = jLabel_timeline.setMinpos(x, minpos_new, mul);

			// タイムバー用のタイムラインを初期化する
			jLayeredPane_space_side_view.add(jLabel_timeline_tb);
			jLayeredPane_space_side_view.setLayer(jLabel_timeline_tb, 2);

			// ビュー上の位置を計算してセットする
			jLabel_timeline_tb.setMinpos(minpos_new, mul);
			jLabel_timeline_tb.setTime(c);

			// ビューを移動する場合
			if (vpos){
				Point vp = vport.getViewPosition();
				Point tp = jLabel_timeline.getLocation();

				// リセットする場合
				if ( reset ) {
					// 初回描画
					Rectangle ra = vport.getViewRect();
					if ( minpos_new >= 30 ) {
						// 05:30以降
						ra.y =  Math.round(timeline_vpos - (float)bounds.getTimelinePosition() * bounds.getPaperHeightMultiplier() * paperHeightZoom);
						vport.setViewPosition(new Point(ra.x, ra.y));
					}
					else {
						// 05:30より前
						if ( ra.y >= 30 ) {
							vport.setViewPosition(new Point(ra.x, 0));
						}
					}
				}
				// スクロールする場合
				else {
					if ( env.getTimerbarScrollEnable() && minpos_new >= 30 ) {
						// 自動更新（05:30まではスクロールしないよ）
						vp.y += (timeline_vpos - tp.y);
						vport.setViewPosition(vp);
					}
				}
			}
		}

		jLabel_timeline.updateUI();
		jLabel_timeline_tb.updateUI();

		// 新しいタイマーの作成（１分ごとに線を移動する）
		startTimer();
	}

	/**
	 * 日付別に表を作成する
	 * @see #_redrawByDateWithCenter(String, String, IterationType)
	 */
	private TVProgramIterator redrawByDate(String date, IterationType tuner) {
		return _redrawByDateWithCenter(null,date,tuner);
	}

	/**
	 * 日付別に表を作成する（ページャーが有効な場合は指定の放送局のあるページを開く）
	 * @see #_redrawByDateWithCenter(String, String, IterationType)
	 */
	private TVProgramIterator redrawByDateWithCenter(String center, String date) {

		// 今日は？
		String ndate529 = CommonUtils.getDate529(0, true);

		//　過去ログかどうか
		IterationType tuner;
		JTreeLabel.Nodes node;
		if ( ndate529.compareTo(date) > 0 ) {
			tuner = IterationType.PASSED;
			node = JTreeLabel.Nodes.PASSED;
		}
		else {
			tuner = IterationType.ALL;
			node = JTreeLabel.Nodes.DATE;
		}

		if ( tuner == IterationType.PASSED ) {
			// 過去ログの取得
			PassedProgram passed = tvprograms.getPassed();
			if ( ! passed.loadAllCenters(date) ) {
				System.err.println(ERRID+"過去ログの取得に失敗しました： "+date);
				return null;
			}
			attachSyoboPassed(passed);

			// 指定した日付のノードがツリーになければ作成する
			addPassedNodeIfNotExist(date);
		}

		// 番組枠描画
		TVProgramIterator pli = _redrawByDateWithCenter(center, date, tuner);

		jLabel_tree.setView(node, date);
		reselectTree();

		return pli;
	}

	/*
	 * 表示対象の過去日のノードがなければ追加する
	 */
	private boolean addPassedNodeIfNotExist(String date){
		if (date == null)
			return false;

		// 過去日ノードを順にチェックする
		int num = passedNode.getChildCount();
		for (int n=0; n<num; n++){
			DefaultMutableTreeNode cnode = (DefaultMutableTreeNode) passedNode.getChildAt(n);

			// ラベルが一致したら
			if (cnode.toString().equals(date))
				return true;
			// 追加する日より過去の日付だったらその前にインサートする
			else if (cnode.toString().compareTo(date) < 0){
				passedNode.insert(new DefaultMutableTreeNode(date), n);
				jTree_tree.updateUI();
				return true;
			}
		}

		// 過去日ノードになかったら追加する
		passedNode.add(new DefaultMutableTreeNode(date));
		jTree_tree.updateUI();
		return true;
	}

	/**
	 * 日付別に表を作成する、の本体
	 * @see #redrawByDate(String, IterationType)
	 * @see #redrawByDateWithCenter(String, String)
	 */
	private TVProgramIterator _redrawByDateWithCenter(String center, String date, IterationType tuner) {

		if (env.getDebug()) System.out.println(DBGID+"CALLED redrawByDate() date="+date+" IterationType="+tuner);

		// ページャーは効くよ
		byCenterMode = false;
		updatePagerEnabled();

		// 古いタイマーの削除
		stopTimer();

		cur_tuner = tuner;

		if (date == null) {
			return null;
		}

		env.setPageBreakEnabled(tuner == IterationType.ALL || tuner == IterationType.PASSED);

		// イテレータ－
		TVProgramIterator pli = tvprograms.getIterator().build(chsort.getClst(),tuner);

		// パネルの初期化
		clearPanel();

		// 新聞ペーンの選択
		boolean drawPrograms = true;
		if ( tuner != IterationType.ALL || env.isPagerEnabled() || ! env.getDrawcacheEnable() ) {
			selectMainView(null);
		}
		else {
			// 描画速度優先の場合
			boolean nopane = true;
			for ( JTaggedLayeredPane tlp : jLayeredPane_space_main_view_byDate ) {
				if ( tlp.getTagstr().equals(date) ) {
					selectMainView(tlp);
					if ( tlp.getComponentCountInLayer(0) > 0 ) {
						// 描画済みなら再度の描画は不要
						drawPrograms = false;
					}
					nopane = false;
					break;
				}
			}
			if ( nopane ) {
				// 該当日付のPaneがない場合
				selectMainView(null);
			}
		}

		// ページ制御
		int colmin = 0;
		int colmax = pli.size();
		if ( env.isPagerEnabled() ) {

			int selectedpage = getSelectedPagerIndex();

			if ( center == null ) {
				// とび先の指定がないのでもともと選択されていたページを再度選択したい
				if ( selectedpage == -1 ) {
					// なんか、選択されてないよ？
					selectedpage = 0;
				}
				else {
					int maxindex = env.getPageIndex(pli.size()-1);
					if ( selectedpage > maxindex ) {
						// ページ数かわったら、インデックスがはみだしちゃった
						selectedpage = 0;
					}
				}
			}
			else {
				// 特定の日付の特定の放送局を表示したい
				int crindex = pli.getIndex(center);
				if ( crindex ==  -1 ) {
					// ここに入ったらバグ
					MWin.appendError(ERRID+"「"+center+"」は有効な放送局ではありません");
					ringBeep();
					crindex = 0;
				}
				selectedpage = env.getPageIndex(crindex);
			}

			// 開始位置・終了位置・局数
			colmin = env.getPageOffset(selectedpage);
			colmax = colmin + env.getCentersInPage(selectedpage)-1;

			// ページャーコンボボックスの書き換え
			setPagerItems(pli,env.getPageIndex(colmin));
			pli.rewind();

			// ページャーは有効だよ
			//setPagerEnabled(true);
		}

		if (env.getDebug()) System.out.println(DBGID+"[描画開始] ch_start="+colmin+" ch_end="+colmax+" ch_size="+pli.size());

		// 番組の枠表示用
		dborder.setDashColor(env.getMatchedBorderColor());
		dborder.setThickness(env.getMatchedBorderThickness());
		dborderK.setDashColor(env.getMatchedKeywordBorderColor());
		dborderK.setThickness(env.getMatchedBorderThickness());

		// 番組表時の共通設定
		updateFonts(env);

		// 局列・番組表を作成
		jPanel_space_top_view.setLayout(null);
		int cnt = -1;
		int col = -1;
		for ( ProgList pl : pli ) {

			++cnt;

			if ( cnt < colmin ) {
				continue;
			}
			else if ( cnt > colmax ) {
				break;
			}

			col = cnt-colmin;

			//TVProgram tvp = tvprograms.get(pli.getSiteId());

			//if (env.getDebug()) System.out.println(DBGID+"[描画中] "+pl.Center+" min="+colmin+" max="+colmax+" cnt="+cnt+" col="+col+" siteid="+siteid);

			// 局列
			JLabel b1 = new JLabel(pl.Center);
			b1.setOpaque(true);
			b1.setBackground(pl.BgColor);
			b1.setBorder(lborder);
			b1.setHorizontalAlignment(JLabel.CENTER);
			b1.setBounds(bounds.getBangumiColumnWidth()*col, 0, bounds.getBangumiColumnWidth(), bounds.getBangumiColumnHeight());
			b1.addMouseListener(cnMouseAdapter);
			jPanel_space_top_view.add(b1);

			// 予約の赤枠を表示する
			if (tuner != IterationType.PASSED) {
				putReserveBorder(date, pl.Center, col);
			}

			// 番組表
			if (drawPrograms == true) {
				putBangumiColumns(pl, col, date);
			}
		}

		// 予約時間枠の表示
		putResTimeBorder(date);

		++col; // 描画後にパネルサイズの変更にも使う

		if ( ! env.getDrawcacheEnable()) {
			// 番組枠描画バッファサイズの上限を確認する
			if (framebuffersize < frameUsed.size()) {
				framebuffersize = frameUsed.size();
				StdAppendMessage(MSGID+"番組枠描画バッファの上限を変更: "+frameUsed.size()+"/"+framebuffersize);
			}
		}

		// ページサイズを変更する
		//jPanel_space_top_view.setPreferredSize(new Dimension(bounds.getTimebarColumnWidth()+cnt*bounds.getBangumiColumnWidth(),bounds.getBangumiColumnHeight()));
		jPanel_space_top_view.setPreferredSize(new Dimension(col*bounds.getBangumiColumnWidth(),bounds.getBangumiColumnHeight()));
		jPanel_space_top_view.updateUI();

		jLayeredPane_space_main_view.setPreferredSize(new Dimension(bounds.getBangumiColumnWidth()*col,Math.round(24*60*bounds.getPaperHeightMultiplier()*paperHeightZoom)));

		// 時間線をひく
		redrawTimeline(false, false);

		if (env.getDebug()) System.out.println(DBGID+"END redrawByDate() date="+date+" IterationType="+tuner);

		env.setPageBreakEnabled(true);

		pli.rewind();
		return pli;
	}

	/**
	 * 放送局別に表を作成する
	 */
	private void redrawByCenter(String center)
	{
		// 古いタイマーの削除
		stopTimer();

		// ページャーは効かないよ
		byCenterMode = true;
		updatePagerEnabled();

		// パネルの初期化
		clearPanel();
		selectMainView(null);

		TreePath path = jTree_tree.getSelectionPath();
		if (path == null || path.getPathCount() != 3)
			return;

		String today = CommonUtils.getDate529(0,  true);
		// 開始日が当日以降であれば現在の表を作成する
		if (this.byCenterModeDayInterval == 1 && startDate.compareTo(today) >= 0){
			startDate = today;
			redrawByCenterCurrent(center);
		}
		// 開始日が過去日であれば過去ログから表作成する
		else{
			redrawByCenterPassed(center);
		}

		// 番組枠描画バッファサイズの上限を確認する
		if (framebuffersize < frameUsed.size()) {
			framebuffersize = frameUsed.size();
			StdAppendMessage(MSGID+"番組枠描画バッファの上限を変更: "+frameUsed.size()+"/"+framebuffersize);
		}
	}

	/*
	 * 放送局別に現在の表を作成する
	 */
	private void redrawByCenterCurrent(String center){
		for (int a=0; a<tvprograms.size(); a++) {
			//
			TVProgram tvp = tvprograms.get(a);
			//
			if (tvp.getType() != ProgType.PROG) {
				continue;
			}
			//
			for (ProgList pl : tvp.getCenters()) {
				if (pl.enabled == true && pl.Center.equals(center)) {
					// 日付ヘッダを描画する
					for (int centerid=0; centerid<pl.pdate.size(); centerid++)
					{
						ProgDateList pcl = pl.pdate.get(centerid);

						JTXTLabel b1 = new JTXTLabel();
						GregorianCalendar c = CommonUtils.getCalendar(pcl.Date);
						if ( c != null ) {
							String date = CommonUtils.getDate(c);
							b1.setValue(date);
							b1.setText(date.substring(5));
							b1.setOpaque(true);
							if ( HolidayInfo.IsHoliday(pcl.Date)){
								b1.setBackground(new Color(255,90,90));
							}
							else if ( c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ) {
								b1.setBackground(new Color(90,90,255));
							}
							else if ( c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ) {
								b1.setBackground(new Color(255,90,90));
							}
							else {
								b1.setBackground(new Color(180,180,180));
							}
						}
						b1.setBorder(lborder);
						b1.setHorizontalAlignment(JLabel.CENTER);
						b1.setBounds(bounds.getBangumiColumnWidth()*centerid, 0, bounds.getBangumiColumnWidth(), bounds.getBangumiColumnHeight());
						b1.addMouseListener(tbMouseAdapter);
						jPanel_space_top_view.add(b1);
					}
					//jPanel_space_top_view.setPreferredSize(new Dimension(bounds.getTimebarColumnWidth()+bounds.getBangumiColumnWidth()*tvprogram.getPlist().get(x).pcenter.size(),bounds.getBangumiColumnHeight()));
					jPanel_space_top_view.setPreferredSize(new Dimension(bounds.getBangumiColumnWidth()*pl.pdate.size(),bounds.getBangumiColumnHeight()));
					jPanel_space_top_view.updateUI();


					// 番組枠の表示
					{
						putBangumiColumns(pl, -1, null);
					}

					// 予約枠の表示
					for (int progid=0; progid<pl.pdate.size(); progid++) {
						putReserveBorder(pl.pdate.get(progid).Date, pl.Center, progid);
					}

					//
					jLayeredPane_space_main_view.setPreferredSize(new Dimension(tvp.getCenters().get(0).pdate.size()*bounds.getBangumiColumnWidth(),Math.round(24*60*bounds.getPaperHeightMultiplier()*paperHeightZoom)));
					//jScrollPane_space_main.updateUI();

					break;
				}
			}
		}

		// 時間線をひく
		redrawTimeline(false, false);

	}

	/*
	 * 放送局別に過去ログから表を作成する
	 */
	private void redrawByCenterPassed(String center){
		// 開始日を調整する
		String today = CommonUtils.getDate529(0, true);

		GregorianCalendar ct = CommonUtils.getCalendar(today);
		ct.add(GregorianCalendar.DAY_OF_MONTH, 7-(env.getDatePerPassedPage()-1)*this.byCenterModeDayInterval);
		String dateMax = CommonUtils.getDate(ct);

		while (startDate.compareTo(dateMax) > 0){
			ct = CommonUtils.getCalendar(startDate);
			ct.add(GregorianCalendar.DAY_OF_MONTH, -this.byCenterModeDayInterval);
			startDate = CommonUtils.getDate(ct);
		}

		// 過去ログの取得
		PassedProgram tvp = tvprograms.getPassed();

		// 開始日以降で指定日数分の番組情報を取得する
		ProgList pl = tvp.loadByCenterDates(startDate, env.getDatePerPassedPage(), this.byCenterModeDayInterval, center);
		if (pl == null)
			return;

		attachSyoboPassedProgList(pl);

		// 未来分の番組情報をマージする
		GregorianCalendar c2 = CommonUtils.getCalendar(startDate);
		for (int n=0; n<env.getDatePerPassedPage(); n++){
			String date = CommonUtils.getDate(c2);
			if (date.compareTo(today) >= 0){
				ProgDateList pcl = getProgDateListByCenterAndDate(center, date);
				if (pcl != null)
					pl.pdate.add(pcl);
			}

			c2.add(Calendar.DAY_OF_MONTH, this.byCenterModeDayInterval);
		}

		// 日付ヘッダを描画する
		for (int centerid=0; centerid<pl.pdate.size(); centerid++)	{
			ProgDateList pcl = pl.pdate.get(centerid);

			JTXTLabel b1 = new JTXTLabel();
			GregorianCalendar c = CommonUtils.getCalendar(pcl.Date);
			if ( c != null ) {
				String date = CommonUtils.getDate(c);
				b1.setValue(date);
				b1.setText(date.substring(5));
				b1.setOpaque(true);
				if ( HolidayInfo.IsHoliday(pcl.Date)){
					b1.setBackground(new Color(255,90,90));
				}
				else if ( c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ) {
					b1.setBackground(new Color(90,90,255));
				}
				else if ( c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ) {
					b1.setBackground(new Color(255,90,90));
				}
				else {
					b1.setBackground(new Color(180,180,180));
				}
			}
			b1.setBorder(lborder);
			b1.setHorizontalAlignment(JLabel.CENTER);
			b1.setBounds(bounds.getBangumiColumnWidth()*centerid, 0, bounds.getBangumiColumnWidth(), bounds.getBangumiColumnHeight());
			b1.addMouseListener(tbMouseAdapter);
			jPanel_space_top_view.add(b1);
		}

		jPanel_space_top_view.setPreferredSize(new Dimension(bounds.getBangumiColumnWidth()*pl.pdate.size(),bounds.getBangumiColumnHeight()));
		jPanel_space_top_view.updateUI();

		putBangumiColumns(pl, -1, null);

		jLayeredPane_space_main_view.setPreferredSize(new Dimension(pl.pdate.size()*bounds.getBangumiColumnWidth(),Math.round(24*60*bounds.getPaperHeightMultiplier()*paperHeightZoom)));
		//jScrollPane_space_main.updateUI();
	}

	/**
	 * 指定された放送局の番組一覧を取得する
	 *
	 * @param center	対象の放送局
	 * @return			該当の番組一覧
	 */
	private ProgList getProgListByCenter(String center){
		for (int a=0; a<tvprograms.size(); a++) {
			//
			TVProgram tvp = tvprograms.get(a);
			//
			if (tvp.getType() != ProgType.PROG) {
				continue;
			}
			//
			for (ProgList pl : tvp.getCenters()) {
				if (pl.enabled == true && pl.Center.equals(center)) {
					return pl;
				}
			}
		}

		return null;
	}

	/**
	 * 指定された放送局の本日の番組一覧を取得する
	 *
	 * @param center	対象の放送局
	 * @return			該当の番組一覧
	 */
	private ProgDateList getProgDateListByCenterAndDate(String center, String date){
		ProgList pl = getProgListByCenter(center);
		if (pl == null)
			return null;

		// 日付ヘッダを描画する
		for (int centerid=0; centerid<pl.pdate.size(); centerid++)	{
			ProgDateList pcl = pl.pdate.get(centerid);

			if (pcl.Date.equals(date))
				return pcl;
		}

		return null;
	}

	/**
	 * 予約待機赤枠の描画（個々の枠）
	 * @see #updateReserveBorder(String)
	 */
	private void putReserveBorder(String date, String Center, int q) {
		if (date == null){
			return;
		}

		// 古いマークの削除（一見取りこぼしがあるように見えるが無問題）
		for (int i=reserveBorders.size()-1; i>=0; i--) {
			JRMLabel rb = reserveBorders.get(i);
			if ( rb.getDate().equals(date) && rb.getCenter().equals(Center) ) {
				rb.setVisible(false);
				jLayeredPane_space_main_view.remove(rb);
				reserveBorders.remove(i);
			}
		}

		// 座標系
		JRMLabel.setColumnWidth(bounds.getBangumiColumnWidth());
		JRMLabel.setHeightMultiplier(bounds.getPaperHeightMultiplier() * paperHeightZoom);

		// 表示範囲
		GregorianCalendar cal = CommonUtils.getCritCalendar(date);
		String topDateTime = CommonUtils.getDateTime(cal);
		cal.add(Calendar.DATE, 1);
		String bottomDateTime = CommonUtils.getDateTime(cal);

		//
		String passedCritDateTime = CommonUtils.getCritDateTime(env.getDisplayPassedReserve());

		// ツールバーで選択されている実レコーダ
		String myself = ( env.getEffectComboToPaper() ) ? (getSelectedMySelf()) : (null);

		// 予約枠の描画
		drawReserveBorders(date, Center, q, topDateTime, bottomDateTime, passedCritDateTime, myself);

		// ピックアップ枠の描画
		drawPickupBorders(date, Center, q, topDateTime, bottomDateTime, passedCritDateTime, TUNERLABEL_PICKUP);
	}
	private void drawReserveBorders(String date, String Center, int q, String topDateTime, String bottomDateTime, String passedCritDateTime, String myself) {
		if ( myself == HDDRecorder.SELECTED_PICKUP ) {
			return;
		}
		for ( HDDRecorder recorder : getSelectedRecorderList() ) {
			for ( ReserveList r : recorder.getReserves()) {

				// 「実行のみ表示」で無効な予約は表示しない
				if ( (env.getDisplayOnlyExecOnEntry() || !bounds.getShowOffReserve()) && ! r.getExec() ) {
					continue;
				}

				// 放送局名の確認
				if ( r.getCh_name() == null ) {
					if ( r.getChannel() == null ) {
						// CHコードすらないのはバグだろう
						System.err.println(ERRID+"予約情報にCHコードが設定されていません。バグの可能性があります。 recid="+recorder.Myself()+" chname="+r.getCh_name());
					}
					continue;
				}

				// 描画本体
				if (r.getCh_name().equals(Center)) {

					// 開始終了日時リストを生成する
					ArrayList<String> starts = new ArrayList<String>();
					ArrayList<String> ends = new ArrayList<String>();
					CommonUtils.getStartEndList(starts, ends, r);

					// 予約枠を描画する
					for ( int j=0; j<starts.size(); j++ ) {
						if ( passedCritDateTime.compareTo(ends.get(j)) > 0 ) {
							// 過去情報の表示が制限されている場合
							continue;
						}

						drawBorder(date,Center,topDateTime,bottomDateTime,starts.get(j),ends.get(j),r.getRec_min(),r.getTuner(),recorder.getColor(r.getTuner()),r.getExec(),q);
					}
				}
			}
		}
	}
	private void drawPickupBorders(String date, String Center, int q, String topDateTime, String bottomDateTime, String passedCritDateTime, String tuner) {
		for ( ProgList pl : tvprograms.getPickup().getCenters() ) {
			if ( ! pl.Center.equals(Center) ) {
				continue;
			}
			for ( ProgDateList pcl : pl.pdate ) {
				for ( ProgDetailList tvd : pcl.pdetail ) {
					if ( passedCritDateTime.compareTo(tvd.endDateTime) > 0 ) {
						// 過去情報の表示が制限されている場合
						continue;
					}

					drawBorder(date,Center,topDateTime,bottomDateTime,tvd.startDateTime,tvd.endDateTime,tvd.recmin,tuner,env.getPickedColor(),false,q);
				}
			}
		}
	}
	private void drawBorder(String date, String Center, String topDateTime, String bottomDateTime, String startDateTime, String endDateTime, String recmin, String tuner, String bordercol, boolean exec, int col) {
		drawBorder(date, Center, topDateTime, bottomDateTime, startDateTime, endDateTime, Integer.valueOf(recmin), tuner, CommonUtils.str2color(bordercol), exec, col);
	}
	private void drawBorder(String date, String Center, String topDateTime, String bottomDateTime, String startDateTime, String endDateTime, int recmin, String tuner, Color bordercol, boolean exec, int col) {

		GregorianCalendar ca = CommonUtils.getCalendar(startDateTime);
		int ahh = ca.get(Calendar.HOUR_OF_DAY);
		int amm = ca.get(Calendar.MINUTE);

		int row = 0;
		int length = 0;
		if (topDateTime.compareTo(startDateTime) <= 0 && startDateTime.compareTo(bottomDateTime) < 0) {
			// 開始時刻が表示範囲内にある
			row = ahh - TIMEBAR_START;
			if (row < 0) {
				row += 24;
			}
			row = row*60 + amm;
			length = recmin;
		}
		else if (startDateTime.compareTo(topDateTime) < 0 && topDateTime.compareTo(endDateTime) < 0) {
			//　表示開始位置が番組の途中にある
			GregorianCalendar ct = CommonUtils.getCalendar(topDateTime);
			int add = ca.get(Calendar.DAY_OF_MONTH) == ct.get(Calendar.DAY_OF_MONTH) ? 0 : -1;

			row = 0;
			length = recmin - (TIMEBAR_START*60 - add*24*60 - ahh*60 - amm);
		}
		else {
			return;
		}

		{
			// 重複予約の場合のエンコーダマーク表示位置の調整
			int rc = 0;
			//int rw = 0;
			for (int k=0; k<reserveBorders.size(); k++) {
				JRMLabel rb = reserveBorders.get(k);
				if ( rb.getDate().equals(date) && rb.getCenter().equals(Center) ) {
					int drow = rb.getVRow() - row;
					int dlen = rb.getVHeight() - length;
					if ( rb.getVColumn() == col && ((drow == 0 && dlen == 0) || ((drow == 1 || drow == -1) && (dlen == 0 || dlen == -1 || dlen == 1))) ) {
						rc++;
					}
				}
			}

			// 予約マーク追加
			JRMLabel rb = new JRMLabel();

			if (rc == 0) {
				rb.setVerticalAlignment(JLabel.BOTTOM);
				rb.setHorizontalAlignment(JLabel.RIGHT);
			}
			else if (rc == 1) {
				rb.setVerticalAlignment(JLabel.BOTTOM);
				rb.setHorizontalAlignment(JLabel.LEFT);
			}
			else {
				rb.setVerticalAlignment(JLabel.TOP);
				rb.setHorizontalAlignment(JLabel.RIGHT);
			}

			// エンコーダの区別がないものは"■"を表示する
			rb.setEncBackground(bordercol);
			rb.setBorder(new LineBorder(bordercol,4));
			if ( tuner != null && tuner.equals(TUNERLABEL_PICKUP) ) {
				rb.setEncForeground(env.getPickedFontColor());
			}
			else if ( exec ) {
				rb.setEncForeground(env.getExecOnFontColor());
			}
			else {
				rb.setEncForeground(env.getExecOffFontColor());
			}
			if (tuner == null || tuner.equals("")) {
				rb.setEncoder("■");
			}
			else {
				rb.setEncoder(tuner);
			}

			// 検索用情報
			rb.setDate(date);
			rb.setCenter(Center);
			rb.setExec(exec);

			jLayeredPane_space_main_view.add(rb);
			jLayeredPane_space_main_view.setLayer(rb,1);
			rb.setVBounds(col, row, 1, length);
			rb.setVisible(true);

			reserveBorders.add(rb);
		}
	}

	/**
	 * 番組枠の表示
	 * @param cnt -1:放送局別表示、>=0:日付表示
	 */
	private void putBangumiColumns(ProgList pl, int cnt, String date) {
		int ymax = pl.pdate.size();
		int col = -1;
		for ( int dateid=0; dateid < ymax; dateid++ ) {
			ProgDateList pcl = pl.pdate.get(dateid);

			if ( cnt >= 0 ) {
				if ( ! pcl.Date.equals(date) ) {
					// 日付表示の場合は１列のみ描画
					continue;
				}

				col = cnt;
			}
			else if ( cnt == -1 ) {
				col++;
			}

			int row = 0;
			int pEnd = 0;
			int zmax = pcl.pdetail.size();
			for ( int progid=0; progid<zmax; progid++ ) {
				ProgDetailList tvd = pcl.pdetail.get(progid);
				if ( progid != 0 ) {
					// ２つめ以降は開始時刻から計算
					String[] st = tvd.start.split(":",2);
					if ( st.length == 2 ) {
						int ahh = Integer.valueOf(st[0]);
						int amm = Integer.valueOf(st[1]);
						if ( CommonUtils.isLateNight(ahh) ) {
							ahh += 24;
						}
						row = (ahh-TIMEBAR_START)*60+amm;
					}
					else {
						// 「番組情報がありません」は前の番組枠のお尻に
						row = pEnd;
					}
				}
				else {
					// その日の最初のエントリは5:00以前の場合もあるので強制０スタート
					row = 0;
				}

				// 番組枠描画
				putBangumiColumnSub(tvd, row, col);

				// 「番組情報がありません」用に保存
				pEnd = row + tvd.length;
			}
		}
	}
	private void putBangumiColumnSub(ProgDetailList tvd, int row, int col) {

		// 新規生成か既存流用かを決める
		JTXTButton b2 = null;
		if (jLayeredPane_space_main_view == jLayeredPane_space_main_view_byMakeshift && ! frameUnused.isEmpty()) {
			b2 = frameUnused.remove(frameUnused.size()-1);
			//b2.setVisible(true);	// JTXTButton.clear()内でsetVisible(false)しているので
		}
		else {
			// 生成する
			b2 = new JTXTButton();
			jLayeredPane_space_main_view.add(b2);
			jLayeredPane_space_main_view.setLayer(b2, 0);

			// リスナーを設定する
			b2.addMouseListener(ml_risepopup);
			b2.addMouseMotionListener(ml_risepopup);
		}
		if (jLayeredPane_space_main_view == jLayeredPane_space_main_view_byMakeshift) {
			frameUsed.add(b2);
		}
		else {
			// 裏描画は十分遅いのでb2をUnusedキャッシュには入れず都度生成で構わない
			frameUsedByDate.add(b2);
		}

		// 情報設定
		b2.setInfo(tvd);

		JTXTButton.setColumnWidth(bounds.getBangumiColumnWidth());
		JTXTButton.setHeightMultiplier(bounds.getPaperHeightMultiplier() * paperHeightZoom);

		b2.setBackground(pColors.get(tvd.genre));
		_updPBorder(bounds, b2);

		// 配置を決定する
		b2.setVBounds(col,row,1,tvd.length);

		// ツールチップを付加する
		if (Env.TheEnv.getTooltipEnable() && ! tvd.title.isEmpty() && ! tvd.start.isEmpty())
			b2.setToolTipText("dummy");
	}


	/**
	 * 予約時間赤枠の描画
	 */
	private void putResTimeBorder(String date) {
		if (date == null){
			return;
		}

		// 古いマークの削除
		for ( JRTLabel rb : resTimeBorders ) {
			rb.setVisible(false);
			jLayeredPane_space_side_view.remove(rb);
		}
		resTimeBorders.clear();

		// 座標系
		JRTLabel.setColumnWidth(env.getResTimebarWidth());
		JRTLabel.setHeightMultiplier(bounds.getPaperHeightMultiplier() * paperHeightZoom);

		// 表示範囲
		GregorianCalendar cal = CommonUtils.getCalendar(String.format("%s %02d:00",date.substring(0,10),TIMEBAR_START));
		String startDateTime = CommonUtils.getDateTime(cal);
		cal.add(Calendar.HOUR_OF_DAY, 24);
		String endDateTime = CommonUtils.getDateTime(cal);

		// 基準日
		String critDateTime = CommonUtils.getCritDateTime(env.getDisplayPassedReserve());

		// ツールバーで選択されている実レコーダ
		String myself = ( env.getEffectComboToPaper() ) ? (getSelectedMySelf()) : (null);

		if ( myself == null || myself.length() > 0 ) {

			// ピックアップはここに入らない
			HDDRecorderList recs = recorders.findInstance(myself);
			ResTimeList list = new ResTimeList();

			for ( HDDRecorder recorder : recs ){
				//System.err.println(DBGID+recorder.Myself());

				for ( ReserveList r : recorder.getReserves()) {
					// Exec == ON ?
					if ((env.getDisplayOnlyExecOnEntry() || !bounds.getShowOffReserve()) && ! r.getExec()) {
						//StdAppendMessage("@Exec = OFF : "+r.getTitle());
						continue;
					}

					// 開始終了日時リストを生成する
					ArrayList<String> starts = new ArrayList<String>();
					ArrayList<String> ends = new ArrayList<String>();
					CommonUtils.getStartEndList(starts, ends, r);

					// 予約枠を描画する
					for (int j=0; j<starts.size(); j++) {
						if (critDateTime.compareTo(ends.get(j)) <= 0) {
							list.mergeResTimeItem("",  starts.get(j), ends.get(j), r, recorder);
						}
					}
				}

				putResTimeBorderItems(recorder, startDateTime, endDateTime, list);
				list.clear();
			}

			jLayeredPane_space_side_view.updateUI();
		}
	}

	/*
	 * ResTimeItemオブジェクト配列を時間帯ビューに追加する
	 */
	private void putResTimeBorderItems(HDDRecorder rec, String startDateTime, String endDateTime, ResTimeList list){
		int tnum = rec.getTunerNum();

		for (ResTimeItem item : list){
			// 予約マーク追加
			JRTLabel rb = new JRTLabel();

			String start = item.getStart();
			String end = item.getEnd();
			int count = item.getCount();
			int cno = count > tnum ? 4 : count == tnum ? 3 : count > 1 ? 2 : 1;
			rb.setColorNo(cno);
			Color c =
				cno == 4 ? env.getResTimebarColor4() :
				cno == 3 ? env.getResTimebarColor3() :
				cno == 2 ? env.getResTimebarColor2() : env.getResTimebarColor1();
			rb.setBackground(c);
			rb.setBorder(new LineBorder(c, env.getResTimebarWidth()/2+1));

			int row = 0;
			int length = 0;
			GregorianCalendar cs = CommonUtils.getCalendar(start);
			GregorianCalendar ce = CommonUtils.getCalendar(end);
			int ahh = cs.get(Calendar.HOUR_OF_DAY);
			int amm = cs.get(Calendar.MINUTE);
			int zhh = ce.get(Calendar.HOUR_OF_DAY);
			int zmm = ce.get(Calendar.MINUTE);

			if (startDateTime.compareTo(start) <= 0 && start.compareTo(endDateTime) < 0) {
				row =  ahh - TIMEBAR_START;
				if (row < 0) {
					row += 24;
				}
				row = row*60 + amm;
				length = (int)(ce.getTimeInMillis() - cs.getTimeInMillis())/1000/60;
			}
			else if (start.compareTo(startDateTime) < 0 && startDateTime.compareTo(end) < 0) {
				//
				row = 0;
				length = (zhh*60 + zmm) - TIMEBAR_START*60;
			}
			else {
				continue;
			}

			jLayeredPane_space_side_view.add(rb);

			rb.setVBounds(bounds.getTimebarColumnWidth(), row, length);
			rb.setToolTipText("<html><table>" + item.getTooltip() + "</table></html>");
			rb.setVisible(true);

			resTimeBorders.add(rb);
		}
	}

	/**
	 * 現在時刻線の位置を変える
	 */
	private int setTimelinePos(boolean reset) {
		if ( vport != null && jLabel_timeline != null && jLabel_timeline.isVisible() ) {

			int correct = 0; // 24:00-28:59迄は前日の日付になる
			GregorianCalendar c = CommonUtils.getCalendar(0);
			if ( CommonUtils.isLateNight(c) ) {
				c.add(Calendar.DATE, -1);
				correct += 24;
			}

			Point vp = vport.getViewPosition();
			Point tp = jLabel_timeline.getLocation();

			// ビュー上の位置
			float mul = bounds.getPaperHeightMultiplier()*paperHeightZoom;
			int minpos_new = (c.get(Calendar.HOUR_OF_DAY)-TIMEBAR_START+correct)*60+c.get(Calendar.MINUTE);
			int timeline_vpos = jLabel_timeline.setMinpos(0, minpos_new, mul);

			jLabel_timeline_tb.setMinpos(minpos_new, mul);
			jLabel_timeline_tb.setTime(c);

			// ビューポートの位置
			if ( reset ) {
				// 初回描画
				Rectangle ra = vport.getViewRect();
				if ( minpos_new >= 30 ) {
					// 05:30以降
					ra.y =  Math.round(timeline_vpos - (float)bounds.getTimelinePosition() * bounds.getPaperHeightMultiplier() * paperHeightZoom);
					vport.setViewPosition(new Point(ra.x, ra.y));
				}
				else {
					// 05:30より前
					if ( ra.y >= 30 ) {
						vport.setViewPosition(new Point(ra.x, 0));
					}
				}
			}
			else {
				if ( env.getTimerbarScrollEnable() && minpos_new >= 30 ) {
					// 自動更新（05:30まではスクロールしないよ）
					vp.y += (timeline_vpos - tp.y);
					vport.setViewPosition(vp);
				}
			}

			jLabel_timeline.updateUI();
			jLabel_timeline_tb.updateUI();

			return minpos_new;
		}

		return -1;
	}

	/**
	 * 番組枠のクリック位置を日時に変換する
	 */
	private String getClickedDateTime(ProgDetailList tvd, int clikedY) {

		String clickedDateTime = null;

		if ( clikedY >= 0 && tvd.start.length() != 0 ) {
			// 新聞形式ならクリック位置の日時を算出する
			GregorianCalendar cala = CommonUtils.getCalendar(tvd.startDateTime);
			cala.add(Calendar.MINUTE, Math.round(((float)clikedY)/(bounds.getPaperHeightMultiplier()*paperHeightZoom)));
			clickedDateTime = CommonUtils.getDateTime(cala);
		}

		return clickedDateTime;
	}

	/**
	 * 番組詳細欄に情報を設定する
	 * @param tvd
	 * @param locking
	 */
	private void setDetailInfo(ProgDetailList tvd, boolean locking) {
		if ( locking ) {
			detailInfoData = tvd;
			jTextPane_detail.setBorder(detailInfoLockedBorder);
		}
		else {
			detailInfoData = null;
			jTextPane_detail.setBorder(detailInfoFreeBorder);
		}
		if ( tvd != null ) {
			jTextPane_detail.setLabel(tvd.accurateDate + " " + tvd.start, tvd.end, tvd.title,
					env.getShowMarkOnDetailArea() ? tvd.extension_mark+tvd.prefix_mark+tvd.newlast_mark : "");
			jTextPane_detail.setText(tvd.detail + "\n" + tvd.getAddedDetail());
		}
	}

	/*
	 * 詳細欄関係
	 */
	/*
	 * 詳細欄の高さを初期化する
	 */
	private void initDetailHeight(){
		if (bounds.getDetailRows() > 0){
			resetDetailHeight();
		}
		else{
			int dh = bounds.getPaperDetailHeight();
			if (dh > 1)
				setDetailHeight(dh);
			else
				resetDetailHeight();
		}
	}

	/*
	 * 詳細欄の高さを設定する
	 */
	private void setDetailHeight(int height){
		if (jTextPane_detail == null)
			return;

		jSplitPane_main.setDividerLocation(height);
		bounds.setPaperDetailHeight(height);
	}

	/*
	 * 詳細欄の高さをリセットする
	 */
	private void resetDetailHeight(){
		if (jTextPane_detail == null)
			return;

		int rows = bounds.getDetailRows();
		int height = jTextPane_detail.getHeightFromRows(rows > 0 ? rows : 4);

		setDetailHeight(height);
	}

	/*
	 * 放送局別１週間おき表示で前の日に移動する
	 */
	public void moveToPrevDateIndex(){
		if (!isPrevDateIndexEnabled())
			return;

		PassedProgram passed = tvprograms.getPassed();

		String date = passed.getPrevDate(startDate, 1);
		if (date == null)
			return;

		startDate = date;

		// 表を描画し直す
		redrawByCurrentSelection();
	}

	/*
	 * 放送局別１週間おき表示で前の日に移動可能かを返す
	 */
	public boolean isPrevDateIndexEnabled(){
		PassedProgram passed = tvprograms.getPassed();

		String date = passed.getPrevDate(startDate, 1);

		return date != null;
	}

	/*
	 * 放送局別１週間おき表示で次の日に移動する
	 */
	public void moveToNextDateIndex(){
		if (!isNextDateIndexEnabled())
			return;

		PassedProgram passed = tvprograms.getPassed();

		String date = passed.getNextDate(startDate, 1);
		if (date == null)
			date = CommonUtils.getDate529(0, true);

		startDate = date;

		// 表を描画し直す
		redrawByCurrentSelection();
	}

	/*
	 * 放送局別１週間おき表示で次の日に移動可能かを返す
	 */
	public boolean isNextDateIndexEnabled(){
		String today = CommonUtils.getDate529(0, true);
		if (this.byCenterModeDayInterval == 1){
			return (startDate.compareTo(today) < 0);
		}

		GregorianCalendar ct = CommonUtils.getCalendar(today);
		ct.add(GregorianCalendar.DAY_OF_MONTH, 7);
		String dateMax = CommonUtils.getDate(ct);

		ct = CommonUtils.getCalendar(startDate);
		if (ct == null)
			return false;
		ct.add(GregorianCalendar.DAY_OF_MONTH, (env.getDatePerPassedPage()-1)*this.byCenterModeDayInterval+1);
		String date = CommonUtils.getDate(ct);

		return (date.compareTo(dateMax) <= 0);
	}

	/*
	 * 放送局別１週間おき表示で前の日に移動する
	 */
	public void moveToPrevDatePage(){
		PassedProgram passed = tvprograms.getPassed();

		String date = passed.getPrevDate(startDate, env.getDatePerPassedPage()*this.byCenterModeDayInterval);
		if (date == null)
			return;

		startDate = date;

		// 表を描画し直す
		redrawByCurrentSelection();
	}

	/*
	 * 放送局別で前のページに移動可能かを返す
	 */
	public boolean isPrevDatePageEnabled(){
		PassedProgram passed = tvprograms.getPassed();

		String date = passed.getPrevDate(startDate, env.getDatePerPassedPage()*this.byCenterModeDayInterval);

		return date != null;
	}

	/*
	 * 放送局別で次のページに移動する
	 */
	public void moveToNextDatePage(){
		if (!isNextDatePageEnabled())
			return;

		PassedProgram passed = tvprograms.getPassed();

		String date = passed.getNextDate(startDate, env.getDatePerPassedPage()*this.byCenterModeDayInterval);
		if (date == null)
			date = CommonUtils.getDate529(0, true);

		startDate = date;

		// 表を描画し直す
		redrawByCurrentSelection();
	}

	/*
	 * 放送局別で次のページに移動可能かを返す
	 */
	public boolean isNextDatePageEnabled(){
		String today = CommonUtils.getDate529(0, true);
		if (this.byCenterModeDayInterval == 1){
			return (startDate.compareTo(today) < 0);
		}

		GregorianCalendar ct = CommonUtils.getCalendar(today);
		ct.add(GregorianCalendar.DAY_OF_MONTH, 7);
		String dateMax = CommonUtils.getDate(ct);

		ct = CommonUtils.getCalendar(startDate);
		if (ct == null)
			return false;
		ct.add(GregorianCalendar.DAY_OF_MONTH, env.getDatePerPassedPage()*this.byCenterModeDayInterval);
		String date = CommonUtils.getDate(ct);

		return (date.compareTo(dateMax) <= 0);
	}

	/*
	 * 放送局別の表示モードかを返す
	 */
	public boolean isByCenterMode(){
		TreePath path = jTree_tree.getSelectionPath();
		if (path == null || path.getPathCount() < 2)
			return false;

		return path.getPathComponent(1) == centerNode;
	}

	/**
	 * しょぼかるの番組情報を過去番組表に反映する
	 *
	 * @param passed	対象の過去番組表
	 */
	private void attachSyoboPassed(PassedProgram passed) {
		for ( ProgList tvpl : passed.getCenters() ) {
			attachSyoboPassedProgList(tvpl);
		}
	}

	/**
	 * しょぼかるの番組情報を放送局別過去番組表に反映する
	 *
	 * @param tvpl	対象の過去番組表
	 */
	private void attachSyoboPassedProgList(ProgList tvpl) {
		TVProgram syobo = tvprograms.getSyobo();
		if (syobo == null) {
			return;
		}

		if ( ! tvpl.enabled) {
			return;
		}

		for ( ProgList svpl : syobo.getCenters() ) {
			if ( ! tvpl.Center.equals(svpl.Center)) {
				continue;
			}
			for ( ProgDateList tvc : tvpl.pdate ) {
				ProgDateList mSvc = null;
				for ( ProgDateList svc : svpl.pdate ) {
					if (tvc.Date.equals(svc.Date) ) {
						mSvc = svc;
						break;
					}
				}
				if (mSvc == null)
					continue;

				// しょぼかる側に該当する日付があるのでマッチング。アニメと映画と音楽
				for ( ProgDetailList tvd : tvc.pdetail ) {
					if ( ! tvd.isEqualsGenre(ProgGenre.ANIME, null) &&
						 ! tvd.isEqualsGenre(ProgGenre.MOVIE, null) &&
						 ! tvd.isEqualsGenre(ProgGenre.MUSIC, null) ) {
						continue;
					}

					for ( ProgDetailList svd : mSvc.pdetail ) {
						if ( tvd.start.equals(svd.start) ) {
							tvd.linkSyobo = svd.link;
							break;
						}
					}
				}
			}

			break;
		}
	}

	/*******************************************************************************
	 * ハンドラ―メソッド
	 ******************************************************************************/

	/**
	 * ツールバーでレコーダの選択イベントが発生
	 */
	@Override
	public void valueChanged(HDDRecorderSelectionEvent e) {
		if (debug) System.out.println(DBGID+"recorder selection rised");

		// 選択中のレコーダ情報を保存する
		src_recsel = (HDDRecorderSelectable) e.getSource();

		// 予約枠を書き換える
		updateReserveBorder(null);
	}

	private String getSelectedMySelf() {
		return ( src_recsel!=null ? src_recsel.getSelectedMySelf() : null );
	}

	private HDDRecorderList getSelectedRecorderList() {
		return ( src_recsel!=null ? src_recsel.getSelectedList() : null );
	}

	private HDDRecorderSelectable src_recsel;


	/**
	 * レコーダ情報の変更イベントが発生
	 */
	@Override
	public void stateChanged(HDDRecorderChangeEvent e) {
		// テーブルをリフレッシュする処理

	}



	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	/**
	 * 現在時刻追従スクロール
	 */
	@Override
	public void timerRised(TickTimerRiseEvent e) {

		String curDT = CommonUtils.getDate529(0,true);

		// 日付が変わったらツリーを書き換える
		if ( prevDT4Tree != null && ! prevDT4Tree.equals(curDT) ) {
			// 日付が変わったらツリーを書き換える
			redrawTreeByDate();
			redrawTreeByPassed();

			// 前回実行日
			prevDT4Tree = curDT;
		}

		if ( timer_now_enabled ) {
			// 表示中のノード
			JTreeLabel.Nodes node = jLabel_tree.getNode();
			String value = jLabel_tree.getValue();
			if (node == null || value == null)
				return;

			boolean byCenter = (node == JTreeLabel.Nodes.DATE || node == JTreeLabel.Nodes.BS ||
					node == JTreeLabel.Nodes.CS || node == JTreeLabel.Nodes.TERRA);
			boolean now = byCenter && dateNode.getChildAt(0).toString().equals(value);

			if (prevDT4Now != null && ! prevDT4Now.equals(curDT)){
				// 日付切り替え
				StdAppendError(MSGID+"日付が変わったので番組表を切り替えます("+CommonUtils.getDateTime(0)+")");

				// 前日以前の番組情報を削除する
				for ( TVProgram tvp : tvprograms ) {
					tvp.refresh();
				}

				// 放送局別表示の開始日を１日進める
				GregorianCalendar c = CommonUtils.getCalendar(startDate, 3600*24);
				if (c != null)
					startDate = CommonUtils.getDate(c);

				if (now)
					redrawByNow(cur_tuner);
				else if (byCenter && !value.equals("")){
					c = CommonUtils.getCalendar(value, 3600*24);
					if (c != null)
						redrawByDateWithCenter(null, CommonUtils.getDate(c));
				}
				else
					redrawByCurrentSelection();
			}
			else if (now){
				// 現在時刻線に合わせたスクロール
				setTimelinePos(false);
			}
			else {
				// 現在時刻戦の移動
				redrawTimeline(false, false);
			}

			// 前回実行日
			prevDT4Now = curDT;
		}
	}

	/**
	 * タブを開いたり閉じたりしたときに動くリスナー
	 */
	private ComponentListener cl_shownhidden = new ComponentAdapter() {
		@Override
		public void componentShown(ComponentEvent e) {

			// 前日以前の番組情報を削除する
			for ( TVProgram tvp : tvprograms ) {
				tvp.refresh();
			}

			// 終了した予約を整理する
			for ( HDDRecorder recorder : recorders ) {
				recorder.removePassedReserves();
			}

			// 他のコンポーネントと連動
			onShown();

			if (dividerLocationOnShown != 0){
				jSplitPane_main.setDividerLocation(dividerLocationOnShown);
				dividerLocationOnShown = 0;
			}

			updatePagerEnabled();
		}

		@Override
		public void componentHidden(ComponentEvent e) {

			onHidden();

			setPagerEnabled(false);
		}
	};

	/**
	 * 番組枠につけるマウス操作のリスナー
	 */
	private final MouseInputListener ml_risepopup = new MouseInputListener() {
		//
		private final Cursor defCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
		private final Cursor hndCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
		private final Point pp = new Point();

		private Color bgcolor = null;

		@Override
		public void mouseClicked(MouseEvent e) {

			// ポインタの位置
			Point p = e.getPoint();

			// ポインタの乗っている番組
			JTXTButton b = (JTXTButton) e.getSource();
			ProgDetailList tvd = b.getInfo();

			if (e.getButton() == MouseEvent.BUTTON3) {
				if (e.getClickCount() == 1) {
					// 右シングルクリックでメニューの表示
					String clicked = getClickedDateTime(tvd, e.getY());
					showPopupMenu(b, tvd, p.x, p.y, clicked);
				}
			}
			else if (e.getButton() == MouseEvent.BUTTON1) {
				if ( e.getClickCount() == 1 ) {
					// 番組詳細表示のロック or 解除（トグル）
					setDetailInfo(tvd, tvd != detailInfoData);
				}
				else if (e.getClickCount() == 2) {
					// 過去ログは閲覧のみ
					if (tvd.type == ProgType.PASSED) {
						MWin.appendMessage(MSGID+"過去ログでダブルクリックは利用できません");
						ringBeep();
						return;
					}

					// 左ダブルクリックで予約ウィンドウを開く
					openReserveDialog(tvd);
				}
			}
			else if (e.getButton() == MouseEvent.BUTTON2) {
				// ピックアップに追加
				addToPickup(tvd);
			}
		}

		private void openReserveDialog(ProgDetailList tvd) {

			// レコーダが登録されていない場合はなにもしない
			if (recorders.size() == 0) {
				return;
			}

			// ダイアログの位置指定
			CommonSwingUtils.setLocationCenter(parent,rD);

			// サブタイトルを番組追跡の対象から外す
			boolean succeeded = false;
			if ( ! env.getSplitEpno() && env.getTraceOnlyTitle() ) {
				succeeded = rD.open(tvd,tvd.title,TraceKey.defaultFazzyThreshold);
			}
			else {
				succeeded = rD.open(tvd);
			}

			if (succeeded) {
				rD.setVisible(true);
			}
			else {
				rD.dispose();
			}

			if (rD.isSucceededReserve()) {
				updateReserveDisplay();
				updateReserveBorder(tvd.center);
			}
		}

		/**
		 * 詳細情報の自動表示
		 */
		@Override
		public void mouseEntered(MouseEvent e) {

			JTXTButton b = (JTXTButton) e.getSource();
			ProgDetailList tvd = b.getInfo();

			if ( env.getEnableHighlight() ) {
				bgcolor = ((JTXTButton)e.getSource()).getBackground();
				((JTXTButton)e.getSource()).setBackground(env.getHighlightColor());
			}

			if ( ! isDetailInfoLocked() ) {
				setDetailInfo(tvd, false);
			}

		}

		@Override
		public void mouseExited(MouseEvent e) {
			if ( env.getEnableHighlight() ) {
				((JTXTButton)e.getSource()).setBackground(bgcolor);
			}
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			Point cp = e.getLocationOnScreen();
			Point vp = vport.getViewPosition(); //= SwingUtilities.convertPoint(vport,0,0,label);
			vp.translate(pp.x-cp.x, pp.y-cp.y);
			jLayeredPane_space_main_view.scrollRectToVisible(new Rectangle(vp, vport.getSize()));
			pp.setLocation(cp);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			pp.setLocation(e.getLocationOnScreen());
			jLayeredPane_space_main_view.setCursor(hndCursor);

			buttonPressed = e.getButton();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			buttonPressed = 0;

			jLayeredPane_space_main_view.setCursor(defCursor);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
		}
	};

	private final MouseListener ml_detailInfoClick = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				if (e.getClickCount() == 2) {
					// 番組詳細表示のロック解除
					setDetailInfo(null, false);
				}
			}
		}
	};

	/*
	 * ツリービューでパスを選択する
	 */
	private void setSelectionPath(TreePath path){
		if (jTree_tree != null){
			jTree_tree.setSelectionPath(path);
			if (path != null)
				jTree_tree.scrollPathToVisible(path);
		}
	}
	/*
	 * 表示中の日付の前後の日付のパスを取得する
	 */
	private final TreePath getNextDatePath(boolean next){
		// 表示中のノード
		JTreeLabel.Nodes node = jLabel_tree.getNode();
		String value = jLabel_tree.getValue();
		if (node == null || value == null)
			return null;

		DefaultMutableTreeNode pnode = null;
		int offset = next ? 1 : -1;

		switch(node){
		case DATE:
			pnode = dateNode;
			break;
		case TERRA:
			pnode = dgNode;
			break;
		case BS:
			pnode = bsNode;
			break;
		case CS:
			pnode = csNode;
			break;
		case BCAST:
			pnode = centerNode;
			break;
		case PASSED:
			pnode = passedNode;
			offset = next ? -1 : 1;	// 過去ログのみ逆方向に移動する
			break;
			// それ以外のノードでは何もしない
		default:
			return null;
		}

		String date = value;
		String today = JTreeLabel.Nodes.NOW.getLabel();

		// 特殊なケース（今日⇔昨日）
		DefaultMutableTreeNode pdateNode = passedNode.getChildCount() > 0 ?
			(DefaultMutableTreeNode)passedNode.getFirstChild() : null;
		if (node == JTreeLabel.Nodes.DATE &&
				(dateNode.getChildAt(1).toString().equals(date) || dateNode.getChildAt(0).toString().equals(date)) && !next){
			if (pdateNode == null)
				return null;

			return new TreePath(pdateNode.getPath());
		}
		else if (node == JTreeLabel.Nodes.PASSED && pdateNode != null && pdateNode.toString().equals(date) && next){
			DefaultMutableTreeNode nnode = (DefaultMutableTreeNode)dateNode.getChildAt(1);
			if (nnode == null)
				return null;

			return new TreePath(nnode.getPath());

		}

		// 日付ノードを順にチェックする
		int num = pnode.getChildCount();
		for (int n=0; n<num; n++){
			DefaultMutableTreeNode cnode = (DefaultMutableTreeNode) pnode.getChildAt(n);

			// ラベルが一致したら
			if (cnode.toString().equals(date)){
				// NOWの場合は当日と同じ扱いにする
				if (cnode.toString().equals(today)){
					n++;
					cnode = cnode.getNextNode();
				}

				// 前後のノードを取得する
				if (n+offset < 0 || n+offset >= num)
					return null;
				DefaultMutableTreeNode nnode = (DefaultMutableTreeNode)pnode.getChildAt(n+offset);
				// 前後のノードがないかNOWの場合は何もしない
				if (nnode == null || nnode.toString().equals(today))
					return null;

				// 前後のノードのパスを返す
				return 	new TreePath(nnode.getPath());

			}
		}

		// 不正なノードだった場合
		return null;
	}

	/**
	 * メインペインにつけるマウスホイールのリスナー
	 */
	private final MouseWheelListener mwListner = new MouseWheelListener() {
		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			int wr = e.getWheelRotation();

			Point vp = vport.getViewPosition();
			Dimension vs = vport.getSize();
			Dimension ps = vport.getView().getPreferredSize();

			// 日付/放送局切替モード
			boolean nodeMode = e.isControlDown() || (buttonPressed != 0 && buttonPressed == env.getMouseButtonForNodeSwitch());
			// ページ切替モード
			boolean pageMode = buttonPressed != 0 && buttonPressed == env.getMouseButtonForPageSwitch();
			// 日付/放送局自動切替モード
			boolean autoMode = e.isShiftDown() || (buttonPressed != 0 && buttonPressed == env.getMouseButtonForNodeAutoPaging());

			// 日付/放送局切替モードの場合はノードを移動する
			if (nodeMode){
				// 前後のノードを取得する
				TreePath path = getNextDatePath(wr > 0);
				if (path == null)
					return;

				// 前後のノードに移動する
				setSelectionPath(path);
			}
			// 日付/放送局自動切替モードの場合はページを切り替える
			else if (pageMode){
				if (wr > 0)
					moveToNextPage();
				else
					moveToPrevPage();
			}
			// データの高さがビューポートより小さい場合は何もしない
			else if (ps.height <= vs.height)
				return;
			// 一番上でかつマイナス方向にホイールを回したら
			else if (vp.y == 0 && wr < 0){
				// 前日のノードを取得する
				TreePath path = getNextDatePath(false);
				if (path == null || !autoMode)
					return;

				// 前日のノードに移動して一番下にスクロールする
				setSelectionPath(path);
				vp.y = ps.height - vs.height;
			}
			// 一番下でかつプラス方向にホイールを回したら
			else if (vp.y ==  ps.height - vs.height && wr > 0){
				// 翌日のノードを取得する
				TreePath path = getNextDatePath(true);
				if (path == null || !autoMode)
					return;

				// 翌日のノードに移動して一番上にスクロールする
				setSelectionPath(path);
				vp.y = 0;
			}
			// それ以外の場合はホイール量に応じてスクロールする
			else{
				vp.translate(0, wr*75);
			}

			jLayeredPane_space_main_view.scrollRectToVisible(new Rectangle(vp, vs));
		}
	};

	/**
	 * サイドツリーにつけるリスナー（ツリーの展開状態を記憶する）
	 */
	private final TreeExpansionListener tel_nodeexpansion = new TreeExpansionListener() {

		@Override
		public void treeExpanded(TreeExpansionEvent event) {
			ter.reg();
		}

		@Override
		public void treeCollapsed(TreeExpansionEvent event) {
			ter.reg();
		}
	};

	/**
	 * サイドツリーにつけるリスナー（クリックで描画実行）
	 */
	private final TreeSelectionListener tsl_nodeselected = new TreeSelectionListener() {
		@Override
		public void valueChanged(TreeSelectionEvent e){
			TreePath path = jTree_tree.getSelectionPath();
			if (path == null)
				return;

			if (env.getDebug()) System.out.println(DBGID+"SELECTED treeSelListner "+path);

			// ３層目が選択されていない場合は無視する
			if (path.getPathCount() != 3)
				return;

			// ２層目と３層目を取得してラベルにセットする
			JTreeLabel.Nodes node = JTreeLabel.Nodes.getNode(path.getPathComponent(1).toString());
			String value = path.getLastPathComponent().toString();

			jLabel_tree.setView(node, value);

			// セットした選択状態で再描画する
			redrawByCurrentSelection();
		}
	};

	/**
	 * フルスクリーン時にツリーを隠したりするの
	 */
	private final MouseListener ml_treehide = new MouseAdapter() {
		public void mouseEntered(MouseEvent e) {
			if (isFullScreen()) {
				setExpandTree();
				//StdAppendMessage("Show tree (N)");
			}
		}
		public void mouseExited(MouseEvent e) {
			if (isFullScreen()) {
				setCollapseTree();
				//StdAppendMessage("Hide tree (N)");
			}
		}
	};

	/**
	 * 放送局名につけるリスナー（ダブルクリックで一週間表示にジャンプ）
	 */
	private final MouseAdapter cnMouseAdapter = new MouseAdapter() {

		private Color bgcolor = null;

		public void mouseExited(MouseEvent e) {
			((JLabel)e.getSource()).setBackground(bgcolor);
		}
		public void mouseEntered(MouseEvent e) {
			bgcolor = ((JLabel)e.getSource()).getBackground();
			((JLabel)e.getSource()).setBackground(CommonUtils.getSelBgColor(bgcolor));
		}

		public void mouseClicked(MouseEvent e) {
			JTreeLabel.Nodes node = jLabel_tree.getNode();
			String value = jLabel_tree.getValue();
			if (node == null || value == null)
				return;

			// 右ダブルクリックで局表示に切り替え
			String center = ((JLabel)e.getSource()).getText();

			if (e.getButton() == MouseEvent.BUTTON1) {
				if (e.getClickCount() == 2) {
					byCenterModeDayInterval = 1;

					// 過去ログノードの場合、選択されているノードを開始日とする
					if (node == JTreeLabel.Nodes.PASSED)
						startDate = value;
					else
						startDate = CommonUtils.getDate529(0, true);

					StdAppendMessage(MSGID+"一局表示に切り替え："+center);
					jLabel_tree.setView(JTreeLabel.Nodes.BCAST, center);
					reselectTree();
				}
			}
			else if (e.getButton() == MouseEvent.BUTTON3){
				JPopupMenu pop = new JPopupMenu();
				// ポインタの位置
				Point p = e.getPoint();

				JMenuItem menuItem = new JMenuItem(String.format("放送局別表示へ切り替え【%s】", center));
				Font f = menuItem.getFont();
				menuItem.setFont(f.deriveFont(f.getStyle()|Font.BOLD));
				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						byCenterModeDayInterval = 1;

						// 過去ログノードの場合、選択されているノードを開始日とする
						if (node == JTreeLabel.Nodes.PASSED)
							startDate = value;
						else
							startDate = CommonUtils.getDate529(0, true);

						jLabel_tree.setView(JTreeLabel.Nodes.BCAST, center);
						reselectTree();
					}
				});
				pop.add(menuItem);

				menuItem = new JMenuItem(String.format("放送局別表示（１週おき）へ切り替え【%s】", center));
				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						byCenterModeDayInterval = 7;

						// 過去ログノードの場合、選択されているノードを開始日とする
						GregorianCalendar c = CommonUtils.getCalendar(value);
						if (c != null)
							startDate = value;
						else
							startDate = CommonUtils.getDate529(0, true);

						jLabel_tree.setView(JTreeLabel.Nodes.BCAST, center);
						reselectTree();
					}
				});
				pop.add(menuItem);

				pop.show((Component)e.getSource(), p.x, p.y);
			}
		}
	};

	/**
	 * 日付枠につけるリスナー（ダブルクリックで放送局別表示にジャンプ）
	 */
	private final MouseAdapter tbMouseAdapter = new MouseAdapter() {
		private Color bgcolor = null;
		//
		public void mouseExited(MouseEvent e) {
			((JTXTLabel)e.getSource()).setBackground(bgcolor);
		}
		public void mouseEntered(MouseEvent e) {
			bgcolor = ((JTXTLabel)e.getSource()).getBackground();
			((JTXTLabel)e.getSource()).setBackground(CommonUtils.getSelBgColor(bgcolor));
		}

		//
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				if (e.getClickCount() == 2) {
					JTreeLabel.Nodes node = jLabel_tree.getNode();
					String value = jLabel_tree.getValue();
					if (node == null || value == null)
						return;

					String center = value;
					String date = ((JTXTLabel)e.getSource()).getValue();

					StdAppendMessage(MSGID+"日付表示に切り替え："+date);

					String today = CommonUtils.getDate529(0,  true);

					// 過去日の場合、過去ログの日付を選択する
					if (date.compareTo(today) < 0){
						// 指定した日付のノードがツリーになければ作成する
						addPassedNodeIfNotExist(date);
					}

					redrawByDateWithCenter(center, date);
				}
			}
			else if (e.getButton() == MouseEvent.BUTTON3){
				JPopupMenu pop = new JPopupMenu();
				// ポインタの位置
				Point p = e.getPoint();
				String date = ((JTXTLabel)e.getSource()).getValue();

				JMenuItem menuItem = new JMenuItem(String.format("日付表示へ切り替え【%s】", date) );
				Font f = menuItem.getFont();
				menuItem.setFont(f.deriveFont(f.getStyle()|Font.BOLD));
				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JTreeLabel.Nodes node = jLabel_tree.getNode();
						String value = jLabel_tree.getValue();
						if (node == null || value == null)
							return;

						String center = value;

						StdAppendMessage(MSGID+"日付表示に切り替え："+date);

						String today = CommonUtils.getDate529(0,  true);

						// 過去日の場合、過去ログの日付を選択する
						if (date.compareTo(today) < 0){
							// 指定した日付のノードがツリーになければ作成する
							addPassedNodeIfNotExist(date);
						}

						redrawByDateWithCenter(center, date);
					}
				});
				pop.add(menuItem);

				if (byCenterModeDayInterval == 7){
					menuItem = new JMenuItem(String.format("放送局別表示へ切り替え【%s】", date));
					menuItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							byCenterModeDayInterval = 1;

							GregorianCalendar c = CommonUtils.getCalendar(date);
							if (c != null)
								startDate = date;
							else
								startDate = CommonUtils.getDate529(0, true);

							reselectTree();
						}
					});
					pop.add(menuItem);

					pop.addSeparator();
					GregorianCalendar c = CommonUtils.getCalendar(date);
					c.add(Calendar.DATE, -1);

					menuItem = new JMenuItem(String.format("前の曜日へ【%s】", CommonUtils.getWeekDayString(c.get(Calendar.DAY_OF_WEEK))));
					menuItem.setEnabled(isPrevDateIndexEnabled());
					menuItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							moveToPrevDateIndex();
						}
					});
					pop.add(menuItem);

					c = CommonUtils.getCalendar(date);
					c.add(Calendar.DATE, 1);

					menuItem = new JMenuItem(String.format("次の曜日へ【%s】", CommonUtils.getWeekDayString(c.get(Calendar.DAY_OF_WEEK))));
					menuItem.setEnabled(isNextDateIndexEnabled());
					menuItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							moveToNextDateIndex();
						}
					});
					pop.add(menuItem);
				}
				else{
					menuItem = new JMenuItem(String.format("放送局別表示（１週おき）へ切り替え【%s】", date));
					menuItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							byCenterModeDayInterval = 7;

							GregorianCalendar c = CommonUtils.getCalendar(date);
							if (c != null)
								startDate = date;
							else
								startDate = CommonUtils.getDate529(0, true);

							reselectTree();
						}
					});
					pop.add(menuItem);
				}

				pop.show((Component)e.getSource(), p.x, p.y);
			}
		}
	};



	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	private JDetailPanel getJTextPane_detail() {
		if (jTextPane_detail == null) {
			jTextPane_detail = new JDetailPanel();
			jTextPane_detail.setBorder(detailInfoFreeBorder);
			jTextPane_detail.addMouseListener(ml_detailInfoClick);
		}
		return jTextPane_detail;
	}

	private JSplitPane getJSplitPane_main() {
		if ( jSplitPane_main == null ) {

			jSplitPane_main = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

			jSplitPane_main.setTopComponent(getJTextPane_detail());
			jSplitPane_main.setBottomComponent(getJSplitPane_view());

			initDetailHeight();

			jSplitPane_main.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
					@Override
			        public void propertyChange(PropertyChangeEvent pce) {
						if (jTextPane_detail.isVisible())
							bounds.setPaperDetailHeight(jSplitPane_main.getDividerLocation());
					}
				});
		}

		return jSplitPane_main;
	}

	private JSplitPane getJSplitPane_view() {
		if ( jSplitPane_view == null ) {

			jSplitPane_view = new JSplitPane() {

				private static final long serialVersionUID = 1L;

				@Override
				public void setDividerLocation(int loc) {
					setDividerEnvs(loc);
					super.setDividerLocation(loc);
				}
			};

			jSplitPane_view.setLeftComponent(getJPanel_tree());
			jSplitPane_view.setRightComponent(getJScrollPane_space_main());
			setExpandTree();
		}
		return jSplitPane_view;
	}

	private JPanel getJPanel_tree() {
		if (jPanel_tree == null) {
			jPanel_tree = new JPanel();

			jPanel_tree.setLayout(new BorderLayout());
			jPanel_tree.add(getJScrollPane_tree_top(), BorderLayout.PAGE_START);
			jPanel_tree.add(getJScrollPane_tree(), BorderLayout.CENTER);
		}
		return jPanel_tree;
	}

	//
	private JScrollPane getJScrollPane_tree_top() {
		if (jScrollPane_tree_top == null) {
			jScrollPane_tree_top = new JScrollPane();
			jScrollPane_tree_top.setViewportView(getJLabel_tree());
			jScrollPane_tree_top.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
			jScrollPane_tree_top.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		}
		return jScrollPane_tree_top;
	}
	private JTreeLabel getJLabel_tree() {
		if (jLabel_tree == null) {
			jLabel_tree = new JTreeLabel();

			Dimension d = jLabel_tree.getMaximumSize();
			d.height = bounds.getBangumiColumnHeight();
			jLabel_tree.setPreferredSize(d);
			jLabel_tree.setOpaque(true);
			jLabel_tree.setBackground(Color.WHITE);
		}
		return jLabel_tree;
	}

	private JScrollPane getJScrollPane_tree() {
		if (jScrollPane_tree == null) {
			jScrollPane_tree = new JScrollPane();

			jScrollPane_tree.setViewportView(getJTree_tree());
		}
		return jScrollPane_tree;
	}

	/**
	 * ツリーの作成
	 */
	private JTree getJTree_tree() {
		if (jTree_tree == null) {

			// ツリーの作成
			jTree_tree = new JTree(){
			  @Override public String getToolTipText(MouseEvent e) {
				  return CommonUtils.GetTreeTooltip(jScrollPane_tree, jTree_tree, e);
			  }
			};
			jTree_tree.setToolTipText("dummy");

			jTree_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			jTree_tree.setCellRenderer(new TreeCellRenderer());	// 検索結果が存在するノードの色を変える
			jTree_tree.setRootVisible(env.getRootNodeVisible());
			jTree_tree.setRowHeight(jTree_tree.getFont().getSize()+1);

			// ノードの作成
			jTree_tree.setModel(new DefaultTreeModel(getTreeNodes()));

			// ツリーの展開状態の復帰
			undoTreeExpansion();

			// ツリーの開閉時に状態を保存する
			jTree_tree.addTreeExpansionListener(tel_nodeexpansion);

			// フルスクリーンの時に使う（新聞形式のツリーを自動的に隠す）
			jTree_tree.addMouseListener(ml_treehide);
		}
		return jTree_tree;
	}

	/*
	 * ツリービューの土曜日と日曜日の色を変える
	 */
	private class TreeCellRenderer extends DefaultTreeCellRenderer {

		private static final long serialVersionUID = 1L;

		private DefaultMutableTreeNode tnode = null;
		private boolean tnode_selected;
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean selected, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {

			Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
			// 'instanceof'は使っていいものやらわるいものやら
			tnode = value instanceof DefaultMutableTreeNode ? (DefaultMutableTreeNode) value : null;
			tnode_selected = selected;
			return c;
		}

		@Override
		public Color getForeground() {
			String label = tnode != null ? tnode.toString() : null;

			if (!env.getWeekEndColoring() || label == null)
				;
			else if (HolidayInfo.IsHoliday(label))
				return tnode_selected ? Color.CYAN : Color.RED;
			else if (label.endsWith("(土)"))
				return tnode_selected ? Color.YELLOW : Color.BLUE;
			else if (label.endsWith("(日)"))
				return tnode_selected ? Color.CYAN : Color.RED;

			return super.getForeground();
		}
	}

	/**
	 * ツリーのノード作成
	 */
	private DefaultMutableTreeNode getTreeNodes() {

		paperRootNode = new DefaultMutableTreeNode(JTreeLabel.Nodes.ROOT.getLabel());

		dateNode	= new DefaultMutableTreeNode(JTreeLabel.Nodes.DATE.getLabel());
		dgNode		= new DefaultMutableTreeNode(JTreeLabel.Nodes.TERRA.getLabel());
		bsNode		= new DefaultMutableTreeNode(JTreeLabel.Nodes.BS.getLabel());
		csNode		= new DefaultMutableTreeNode(JTreeLabel.Nodes.CS.getLabel());
		centerNode	= new DefaultMutableTreeNode(JTreeLabel.Nodes.BCAST.getLabel());
		passedNode	= new DefaultMutableTreeNode(JTreeLabel.Nodes.PASSED.getLabel());

		paperRootNode.add(dateNode);
		paperRootNode.add(dgNode);
		paperRootNode.add(bsNode);
		paperRootNode.add(csNode);
		paperRootNode.add(centerNode);
		paperRootNode.add(passedNode);

		// 子の描画
		redrawTreeByDate();
		redrawTreeByCenter();
		redrawTreeByPassed();

		return paperRootNode;
	}

	private void undoTreeExpansion() {

		// 展開状態の復帰
		stopTreeListener();

		// 展開状態の記憶域の初期化
		ter = new TreeExpansionReg(jTree_tree, TreeExpRegFile_Paper);
		try {
			ter.load();
		}
		catch (Exception e) {
			MWin.appendError(ERRID+"ツリー展開情報の解析で問題が発生しました");
			e.printStackTrace();
		}

		// 状態を復元する
		ArrayList<TreePath> tpa = ter.get();
		for ( TreePath path : tpa ) {
			jTree_tree.expandPath(path);
		}

		startTreeListener();
	}

	private JScrollPane getJScrollPane_space_main() {
		if (jScrollPane_space_main == null) {
			jScrollPane_space_main = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			jScrollPane_space_main.getVerticalScrollBar().setUnitIncrement(bounds.getBangumiColumnHeight());
			jScrollPane_space_main.getHorizontalScrollBar().setUnitIncrement(bounds.getBangumiColumnWidth());

			//jScrollPane_space_main.setViewportView(getJLayeredPane_space_main_view());
			jScrollPane_space_main.setColumnHeaderView(getJPanel_space_top_view());
			jScrollPane_space_main.setRowHeaderView(getjLayeredPane_space_side_view());
			jScrollPane_space_main.addMouseWheelListener(mwListner);
			jScrollPane_space_main.setWheelScrollingEnabled(false);

			vport = jScrollPane_space_main.getViewport();
		}
		return jScrollPane_space_main;
	}

	private JPanel getJPanel_space_top_view() {
		if (jPanel_space_top_view == null) {
			jPanel_space_top_view = new JPanel();
			jPanel_space_top_view.setLayout(new SpringLayout());
		}
		return jPanel_space_top_view;
	}

	private JLayeredPane getjLayeredPane_space_side_view() {
		if (jLayeredPane_space_side_view == null) {
//			jLayeredPane_space_side_view = new JPanel();
			jLayeredPane_space_side_view = new JLayeredPane();
			jLayeredPane_space_side_view.setLayout(null);
		}
		return jLayeredPane_space_side_view;
	}


	/*
	 * 以下は、pcwinから呼び出されるメソッドをまとめたもの
	 */

	// 時間枠のコンポーネント
	public Component[] getTimebarComponents() {
		return jLayeredPane_space_side_view.getComponents();
	}

	// 背景色ほかの変更
	public void updateColors(Env ec,PaperColorsMap pc) {
		_updPColors(ec, pc, frameUsed);

		if ( env.getDrawcacheEnable() ) {
			_updPColors(ec, pc, frameUsedByDate);
		}

		// マウスオーバー時のハイライト
		/* no proc. */

		// タイムバーの色
		for ( Component c : getTimebarComponents() ) {
			if ( c instanceof JTimebarLabel ) {
				int j = Integer.valueOf(((JTimebarLabel) c).getTs());
				if ( j >=6 && j <= 11 ) {
					c.setBackground(ec.getTimebarColor());
				}
				else if ( j >=12 && j <= 17 ) {
					c.setBackground(ec.getTimebarColor2());
				}
				else if ( j >=18 && j <= 23 ) {
					c.setBackground(ec.getTimebarColor3());
				}
				else {
					c.setBackground(ec.getTimebarColor4());
				}
			}
		}

		// 予約タイムバーの色
		for ( JRTLabel rb : resTimeBorders ) {
			int cno = rb.getColorNo();
			Color c =
				cno == 4 ? ec.getResTimebarColor4() :
				cno == 3 ? ec.getResTimebarColor3() :
				cno == 2 ? ec.getResTimebarColor2() : ec.getResTimebarColor1();
			rb.setBackground(c);
			rb.setBorder(new LineBorder(c, ec.getResTimebarWidth()/2+1));
		}
	}

	// サイズの変更
	public void updateBounds(Env ec, Bounds bc) {

		int maxCol = jPanel_space_top_view.getComponentCount();
		float maxRow = 24*60;

		float phm = bc.getPaperHeightMultiplier() * paperHeightZoom ;

		int vieww = maxCol * bc.getBangumiColumnWidth();
		int viewh = (int) Math.ceil(maxRow * phm);

		// 変更前のビューの位置
		Point vp = vport.getViewPosition();
		float vh = vport.getView().getPreferredSize().height;

		// タイムバーのサイズ変更
		{
			int h = (int) Math.ceil(60.0F*phm);
			int row = 0;
			for ( Component b0 : jLayeredPane_space_side_view.getComponents() ) {
				if (b0 == jLabel_timeline_tb || resTimeBorders.contains(b0))
					continue;
				b0.setBounds(0,(int) Math.ceil((float)row*phm),bc.getTimebarColumnWidth(),h);
				row += 60;
			}

			Dimension d = jLayeredPane_space_side_view.getPreferredSize();
			d.width = bc.getTimebarColumnWidth() + ec.getResTimebarWidth();
			d.height = viewh;
			jLayeredPane_space_side_view.setPreferredSize(d);
		}

		// 放送局名(or日付)のサイズ変更
		{
			for ( int col=0; col<jPanel_space_top_view.getComponentCount(); col++ ) {
				Component b1 = jPanel_space_top_view.getComponent(col);
				b1.setBounds(
						bc.getBangumiColumnWidth() * col,
						0,
						bc.getBangumiColumnWidth(),
						bc.getBangumiColumnHeight());
			}
			Dimension d = jPanel_space_top_view.getPreferredSize();
			d.width = vieww;
			jPanel_space_top_view.setPreferredSize(d);
		}

		// 各番組枠のサイズ変更・検索マッチ枠の表示変更
		{
			{
				_updPBounds(bc, frameUsed);
				_updPBorderAll(ec, bc, frameUsed);

				Dimension d = jLayeredPane_space_main_view.getPreferredSize();
				d.width = vieww;
				d.height = viewh;
				jLayeredPane_space_main_view.setPreferredSize(d);
			}

			if ( ec.getDrawcacheEnable() ) {
				_updPBounds(bc, frameUsedByDate);
				_updPBorderAll(ec, bc, frameUsedByDate);

				for ( JLayeredPane pane : jLayeredPane_space_main_view_byDate ) {
					Dimension d = pane.getPreferredSize();
					d.width = vieww;
					d.height = viewh;
					pane.setPreferredSize(d);
				}
			}
		}

		// 予約枠・ピックアップ枠のサイズ変更＆色変更
		{
			JRMLabel.setColumnWidth(bc.getBangumiColumnWidth());
			JRMLabel.setHeightMultiplier(phm);

			for ( JRMLabel rb : reserveBorders ) {

				rb.reVBounds();

				if ( rb.getEncoder().equals(TUNERLABEL_PICKUP) ) {
					rb.setEncBackground(ec.getPickedColor());
					rb.setEncForeground(ec.getPickedFontColor());
					rb.setBorder(new LineBorder(ec.getPickedColor(),4));
				}
				else if ( rb.getExec() ) {
					rb.setEncForeground(ec.getExecOnFontColor());
				}
				else {
					rb.setEncForeground(ec.getExecOffFontColor());
				}
				rb.repaint();
			}
		}

		// 予約時間枠の位置変更
		{
			JRTLabel.setColumnWidth(ec.getResTimebarWidth());
			JRTLabel.setHeightMultiplier(phm);

			for ( JRTLabel rb : resTimeBorders ) {
				rb.reVBounds();
				rb.repaint();
			}
		}

		// 現在時刻線の位置変更
		setTimelinePos(false);

		// 枠のサイズを更新したのでupdateUI()
		jScrollPane_space_main.updateUI();

		// ビューの位置調整
		vp.y = (int)Math.ceil(maxRow * (float)vp.y * phm / vh);
		vport.setViewPosition(vp);

	}

	// フォントの変更
	public void updateFonts(Env ec) {
		JTXTButton.setShowStart(ec.getShowStart());
		JTXTButton.setSplitEpno(ec.getSplitEpno());
		JTXTButton.setShowDetail(ec.getShowDetail());
		JTXTButton.setDetailTab(ec.getDetailTab());

		JTXTButton.setTitleFont(ec.getTitleFont());
		JTXTButton.setTitleFontStyle(ec.getTitleFontStyle());
		JTXTButton.setDetailFont(ec.getDetailFont());
		JTXTButton.setDetailFontStyle(ec.getDetailFontStyle());
		JTXTButton.setTitleFontSize(ec.getTitleFontSize());
		JTXTButton.setTitleFontColor(ec.getTitleFontColor());
		JTXTButton.setDetailFontSize(ec.getDetailFontSize());
		JTXTButton.setDetailFontColor(ec.getDetailFontColor());
		JTXTButton.setAAHint(ec.getPaperAAMode().getHint());
	}

	// 再描画？
	public void updateRepaint() {
		_updPRepaint(frameUsed);

		if ( env.getDrawcacheEnable() ) {
			_updPRepaint(frameUsedByDate);
		}
	}

	// 以下共通部品

	private void _updPColors(Env ec, PaperColorsMap pc, ArrayList<JTXTButton> fa) {
		for ( JTXTButton b2 : fa ) {
			b2.setBackground(pc.get(b2.getInfo().genre));
		}
	}

	private void _updPBounds(Bounds bc, ArrayList<JTXTButton> fa) {

		JTXTButton.setColumnWidth(bc.getBangumiColumnWidth());
		JTXTButton.setHeightMultiplier(bc.getPaperHeightMultiplier() * paperHeightZoom);

		for ( JTXTButton b2 :  fa ) {
			b2.reVBounds();
		}
	}

	private void _updPBorderAll(Env ec, Bounds bc, ArrayList<JTXTButton> fa) {
		dborder.setDashColor(ec.getMatchedBorderColor());
		dborder.setThickness(ec.getMatchedBorderThickness());
		dborderK.setDashColor(ec.getMatchedKeywordBorderColor());
		dborderK.setThickness(ec.getMatchedBorderThickness());
		for ( JTXTButton b2 :  fa ) {
			_updPBorder(bc, b2);
		}
	}

	private void _updPBorder(Bounds bc, JTXTButton b) {
		if ( bc.getShowMatchedBorder() && b.isStandby() ) {
			if ( b.isStandbyByTrace() ) {
				if ( b.getBorder() != dborder )
					b.setBorder(dborder);
			}
			else {
				// 番組追跡はキーワード検索に優先する
				if ( b.getBorder() != dborder && b.getBorder() != dborderK )
					b.setBorder(dborderK);
			}
		}
		else {
			if ( b.getBorder() != lborder )
				b.setBorder(lborder);
		}
	}

	private void _updPRepaint(ArrayList<JTXTButton> fa) {
		for ( JTXTButton b2 :  fa ) {
			b2.forceRepaint();
		}
	}

}
