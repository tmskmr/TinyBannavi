package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.border.LineBorder;

import tainavi.TVProgram.ProgGenre;
import tainavi.TVProgram.ProgSubgenre;


/**
 * 予約ダイアログを目的ごとに３ブロックにわけたうちの「録画設定」部分のコンポーネント
 * @since 3.22.2β
 */
public class RecSettingEditorPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public void setDebug(boolean b) { debug = b; }
	private static boolean debug = false;

	private String folderNameWorking = "";

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	private static final String ITEM_YES = "する";
	private static final String ITEM_NO = "しない";

	private static final int PARTS_HEIGHT = 25;
	private static final int SEP_WIDTH = 10;
	private static final int SEP_WIDTH_NARROW = 5;
	private static final int SEP_HEIGHT = 10;
	private static final int SEP_HEIGHT_NALLOW = 5;

	private static final int LABEL_WIDTH = 150;
	private static final int BUTTON_WIDTH = 75;

	private static final int COMBO_WIDTH = 115;
	private static final int COMBO_WIDTH_WIDE = 155;
	private static final int COMBO_HEIGHT = 43;

	private static final int RECORDER_WIDTH = COMBO_WIDTH_WIDE*2+SEP_WIDTH_NARROW;
	private static final int ENCODER_WIDTH = COMBO_WIDTH*2+SEP_WIDTH_NARROW;

	public static final String FOLDER_ID_ROOT = "0";

	private static final String TEXT_SAVEDEFAULT = "<HTML>録画設定を開いた時の枠内のデフォルト値として<BR>現在の値を使用するようにします。<BR><FONT COLOR=#FF0000>※ジャンル別ＡＶ設定があればそちらが優先されます。</FONT></HTML>";

	// ログ関連

	private static final String MSGID = "[録画設定編集] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	private JComboBoxPanel jCBXPanel_recorder = null;
	private JComboBoxPanel jCBXPanel_encoder = null;
	private JLabel jLabel_encoderemptywarn = null;

	private JComboBoxPanel jCBXPanel_genre = null;
	private JComboBoxPanel jCBXPanel_subgenre = null;
	private JComboBoxPanel jCBXPanel_videorate = null;
	private JComboBoxPanel jCBXPanel_audiorate = null;
	private JComboBoxPanel jCBXPanel_folder = null;
	private JComboBoxPanel jCBXPanel_dvdcompat = null;
	private JComboBoxPanel jCBXPanel_device = null;
	private JComboBoxPanel jCBXPanel_aspect = null;
	private JComboBoxPanel jCBXPanel_bvperf = null;
	private JComboBoxPanel jCBXPanel_lvoice = null;
	private JComboBoxPanel jCBXPanel_autodel = null;
	private JComboBoxPanel jCBXPanel_pursues = null;
	private JComboBoxPanel jCBXPanel_xChapter = null;
	private JComboBoxPanel jCBXPanel_msChapter = null;
	private JComboBoxPanel jCBXPanel_mvChapter = null;

	private JLabel jLabel_rectype = null;
	private JButton jButton_load = null;
	private JButton jButton_save = null;

	private JComboBoxPanel jCBXPanel_portable = null;
	private JButton jButton_addFolder = null;
	private JButton jButton_delFolder = null;
	private JButton jButton_savedefault = null;

	private JCheckBoxPanel jCheckBox_Exec = null;

	//
	private RecSettingSelectable recsetsel = null;

	private HDDRecorderList recorders = null;
	private StatusWindow StWin = null;
	private StatusTextArea MWin = null;

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public RecSettingEditorPanel() {

		super();

		setBorder(new LineBorder(Color.BLACK, 1));

		addComponents();

		// 外部要因に左右されないアイテム群の設定
		setGenreItems();

		// 付けたり外したりしないリスナー
		jCBXPanel_genre.addItemListener(f_il_genreSelected);

		jButton_load.addActionListener(f_al_loadAction);
		jButton_save.addActionListener(f_al_saveAction);
		jButton_savedefault.addActionListener(f_al_saveDefaultAction);
		jCBXPanel_audiorate.addItemListener(f_il_recTypeChanged);
		jCBXPanel_msChapter.addItemListener(f_il_marginTopChanged);
		jCBXPanel_mvChapter.addItemListener(f_il_marginBottomChanged);

		// 付けたり外したりするリスナー
		setEnabledListenerAll(true);
	}

	private void addComponents() {

		setLayout(new SpringLayout());

		int lw = ZMSIZE(LABEL_WIDTH);
		int bw = ZMSIZE(BUTTON_WIDTH);
		int cw = ZMSIZE(COMBO_WIDTH);
		int cww = ZMSIZE(COMBO_WIDTH_WIDE);
		int sw = ZMSIZE(SEP_WIDTH);
		int swn = ZMSIZE(SEP_WIDTH_NARROW);
		int shn = ZMSIZE(SEP_HEIGHT_NALLOW);

		int ph = ZMSIZE2(PARTS_HEIGHT);
		int ch = ZMSIZE2(COMBO_HEIGHT);
		int sh = ZMSIZE2(SEP_HEIGHT);

		int lwi  = ZMSIZE(110);
		int cwi = ZMSIZE(150);
		int cwn = ZMSIZE(110);
		int bwn = ZMSIZE(30);
		int cbw = ZMSIZE(75);

		// １行目（レコーダ、エンコーダ）
		int y = 0;
		int x = swn;

		int rw = ZMSIZE(RECORDER_WIDTH);
		int ew = ZMSIZE(ENCODER_WIDTH);
		int sp = ZMSIZE(5);
		add(jCBXPanel_recorder = getComboBox(rw,rw),	rw+sp,	ch, x, y);
		add(jCBXPanel_encoder = getComboBox(ew,ew),		ew+sp,	ch, x+=rw+sp+sw, y);
		add(jLabel_encoderemptywarn = new JLabel(""),	lw,		ph, x+=ew+sp+sw+sp, y+ph);

		jCBXPanel_recorder.getJComboBox().setForeground(Color.BLUE);
		jCBXPanel_encoder.getJComboBox().setForeground(Color.BLUE);

		// ポップアップした時に追加される幅
		jCBXPanel_recorder.addPopupWidth(ZMSIZE(100));
		jCBXPanel_encoder.addPopupWidth(ZMSIZE(100));

		// ２行目（ジャンル、サブジャンル、自動削除、無音部分チャプタ分割）
		y += ch;
		x = swn;
		int spy = y;
		add(jCBXPanel_genre    = getComboBox(lwi,cwi),	cww,	ch, x, y);
		add(jCBXPanel_subgenre = getComboBox(lwi,cwi),	cww,	ch, x+=(cww+sw), y);
		add(jCBXPanel_autodel  = getComboBox(lwi,cwn),	cw,		ch, x+=(cw+sw)*2, y);
		add(jCBXPanel_xChapter = getComboBox(lwi,cwn),	cw,		ch, x+=(cw+sw), y);

		// ３行目（画質、音質、録画優先度、ライン音声選択、マジックチャプタ(シーン)）
		y += ch;
		x = swn;
		add(jCBXPanel_videorate = getComboBox(lwi,cwi),	cww,	ch, x, y);
		add(jCBXPanel_audiorate = getComboBox(lwi,cwn),	cw,		ch, x+=(cww+sw), y);
		add(jCBXPanel_bvperf    = getComboBox(lwi,cwn),	cw,		ch, x+=(cw+sw), y);
		add(jCBXPanel_lvoice    = getComboBox(lwi,cwn),	cw,		ch, x+=(cw+sw), y);
		add(jCBXPanel_msChapter = getComboBox(lwi,cwn),	cw,		ch, x+=(cw+sw), y);

		// ４行目（記録先フォルダ、記録先デバイス、BD/DVD、録画のりしろ、マジックチャプタ(本編)）
		y += ch;
		x = swn;
		add(getJButton_addFolder("新"), bwn, ZMSIZE(17), x+cww-bwn*2, y);
		add(getJButton_delFolder("削"), bwn, ZMSIZE(17), x+cww-bwn,   y);
		add(jCBXPanel_folder    = getComboBox(lwi-10,cwi),cww,	ch, x, y);
		add(jCBXPanel_device    = getComboBox(lwi,cwn),	cw,		ch, x+=(cww+sw), y);
		add(jCBXPanel_dvdcompat = getComboBox(lwi,cwn),	cw,		ch, x+=(cw+sw), y);
		add(jCBXPanel_aspect    = getComboBox(lwi,cwn),	cw,		ch, x+=(cw+sw), y);
		add(jCBXPanel_mvChapter = getComboBox(lwi,cwn),	cw,		ch, x+=(cw+sw), y);
		jCBXPanel_folder.addPopupWidth(ZMSIZE(300));

		// ５行目（持ち出し、番組追従）
		y += ch;
		x = swn;
		add(jCBXPanel_portable = getComboBox(lwi,cwi), 	cww,	ch, x, y);
		x += (cww+sw) + (cw+sw)*3;
		add(jCBXPanel_pursues  = getComboBox(lwi,cwn), 	cw, 	ch, x, y);

		y += ch;

		// 特殊配置（開く、保存、既定化、予約実行）
		x = swn+(cww+sw)+(cw+sw)*4+sw;
		add(jLabel_rectype = new JLabel("ジャンル別の"),	lw, ph, x, spy);
		spy += ph-ZMSIZE(5);
		add(new JLabel("録画設定の選択"),					lw, ph, x, spy);

		add(jButton_load = new JButton("開く"),				bw, ph, x+swn, spy+=ph);
		add(jButton_save = new JButton("保存"),				bw, ph, x+swn, spy+=ph);
		add(jButton_savedefault = new JButton("既定化"),	bw, ph, x+swn, spy+=(ph+sh));

		jButton_savedefault.setToolTipText(TEXT_SAVEDEFAULT);

		add(jCheckBox_Exec = new JCheckBoxPanel("予約実行",cbw,true), cbw, ph, x+swn, spy+=(ph+sh));
		setExecValue(true);

		x += bw+swn*2;

		Dimension d = new Dimension(x,y);
		setPreferredSize(d);
	}

	private int ZMSIZE(int size){ return Env.ZMSIZE(size); }
	private int ZMSIZE2(int size){ return ZMSIZE(size-20) + 20; }

	private JComboBoxPanel getComboBox(int lwidth, int cwidth){
		return new JComboBoxPanel("", lwidth, cwidth);
	}

	private void add(JComponent c, int width, int height, int x, int y) {
		CommonSwingUtils.putComponentOn(this, c, width, height, x, y);
	}

	public void setRecSettingSelector(RecSettingSelectable o) {
		recsetsel = o;
	}


	/*******************************************************************************
	 * アイテムの設定
	 ******************************************************************************/

	/***************************************
	 * 項目ラベルの設定
	 **************************************/

	/**
	 * レコーダが選択されたら各コンポーネントラベルを設定する
	 * @param recorder
	 */
	public void setLabels(HDDRecorder recorder) {

		// 固定ラベル
		setLabel(jCBXPanel_recorder,	null,							"レコーダ");
		setLabel(jCBXPanel_encoder,		null,							"エンコーダ");
		setLabel(jCBXPanel_genre,		null,							"ジャンル");
		setLabel(jCBXPanel_subgenre,	null,							"サブジャンル");
		setLabel(jCBXPanel_pursues,		null,							"番組追従");

		// 可変ラベル
		setLabel(jCBXPanel_videorate,	recorder.getLabel_Videorate(),	"画質");
		setLabel(jCBXPanel_audiorate,	recorder.getLabel_Audiorate(),	"音質");

		setLabel(jCBXPanel_folder,		recorder.getLabel_Folder(),		"記録先フォルダ");
		setLabel(jCBXPanel_device,		recorder.getLabel_Device(),		"記録先デバイス");

 		setLabel(jCBXPanel_bvperf,		recorder.getLabel_BVperf(),		"録画優先度");		// "高ﾚｰﾄ節約"？
		setLabel(jCBXPanel_dvdcompat,	recorder.getLabel_DVDCompat(),	"BD/DVD互換モード");

		setLabel(jCBXPanel_autodel,		recorder.getLabel_Autodel(),	"自動削除");
		setLabel(jCBXPanel_lvoice,		recorder.getLabel_LVoice(),		"ﾗｲﾝ音声選択");
		setLabel(jCBXPanel_aspect,		recorder.getLabel_Aspect(),		"録画のりしろ");		// "DVD記録時画面比"？

		setLabel(jCBXPanel_msChapter,	recorder.getLabel_MsChapter(),	"ﾏｼﾞｯｸﾁｬﾌﾟﾀ(ｼｰﾝ)");	// "DVD/ｼｰﾝﾁｬﾌﾟﾀ分割"？
		setLabel(jCBXPanel_mvChapter,	recorder.getLabel_MvChapter(),	"ﾏｼﾞｯｸﾁｬﾌﾟﾀ(本編)");	// "音多/本編ﾁｬﾌﾟﾀ分割"？
		setLabel(jCBXPanel_xChapter,	recorder.getLabel_XChapter(),	"無音部分ﾁｬﾌﾟﾀ分割");
		setLabel(jCBXPanel_portable,	recorder.getLabel_Portable(),	"持ち出し");
	}

	/**
	 * ジャンル別AV設定か、CH別AV設定かを選ぶ
	 */
	public void setAVCHSetting(boolean enabled) {
		jLabel_rectype.setText(enabled ? "放送局別の" : "ジャンル別の");
	}

	/***************************************
	 * 固定アイテムの設定
	 **************************************/

	/**
	 * 固定のアイテムを設定する
	 * @see #setFlexItems(HDDRecorder, String)
	 */
	public void setFixedItems(HDDRecorderList recorders) {

		setEnabledListenerAll(false);	// リスナー停止

		setRecorderItems(recorders);

		setEnabledListenerAll(true);	// リスナー再開
	}

	/**
	 * レコーダアイテムを設定する
	 * @see #setFixedItems(HDDRecorderList)
	 */
	private void setRecorderItems(HDDRecorderList recorders) {
		String selected = getSelectedRecorderId();
		ArrayList<String> items = new ArrayList<String>();
		for ( HDDRecorder rec : recorders ) {
			if ( rec.isBackgroundOnly() ) {
				continue;	// Googleカレンダープラグインとかははずす
			}

			items.add(rec.getDispName());
		}

		setComboItems(jCBXPanel_recorder, items);

		if ( selected != null ) {
			setSelectedRecorderId(selected);
		}
	}

	/*
	 * レコーダーIDを指定してレコーダーを選択する
	 */
	private String setSelectedRecorderId(String recId){
		if (jCBXPanel_recorder == null)
			return null;

		if (recId == null)
			return recId;

		int num = jCBXPanel_recorder.getItemCount();
		int no = 0;
		for ( HDDRecorder rec : recorders ) {
			if ( rec.isBackgroundOnly() ) {
				continue;	// Googleカレンダープラグインとかははずす
			}

			if (recId.equals(rec.Myself())){
				if (no < num)
					jCBXPanel_recorder.setSelectedIndex(no);
				return rec.Myself();
			}
			no++;
		}

		return null;
	}

	/*
	 * 選択されているレコーダーのIDを取得する
	 */
	private String getSelectedRecorderId(){
		if (jCBXPanel_recorder == null)
			return null;

		int sno = jCBXPanel_recorder.getSelectedIndex();
		if (sno == -1)
			return null;

		int no = 0;
		for ( HDDRecorder rec : recorders ) {
			if ( rec.isBackgroundOnly() ) {
				continue;	// Googleカレンダープラグインとかははずす
			}

			if (no == sno){
				return rec.Myself();
			}
			no++;
		}

		return null;
	}

	/**
	 * ジャンルアイテムを設定する
	 * @see #setFixedItems(HDDRecorderList)
	 */
	private void setGenreItems() {
		ArrayList<String> items = new ArrayList<String>();
		for ( ProgGenre g : ProgGenre.values() ) {
			items.add(g.toString());
		}

		setComboItems(jCBXPanel_genre, items);
	}


	/***************************************
	 * 可変アイテムの設定
	 **************************************/

	/**
	 * レコーダが選択されたらそれにあわせて各コンポーネントアイテムを設定する
	 */
	public void setFlexItems(HDDRecorder recorder, String webChName) {

		setEnabledListenerAll(false);	// リスナー停止

		// エンコーダ
		setComboItems(jCBXPanel_encoder, recorder.getFilteredEncoders(webChName));

		// 設定値
		setComboItems(jCBXPanel_videorate, recorder.getVideoRateList());
		setComboItems(jCBXPanel_audiorate, recorder.getAudioRateList());
		setComboItems(jCBXPanel_folder, recorder.getFolderList());

		setComboItems(jCBXPanel_device, recorder.getDeviceList());
		setComboItems(jCBXPanel_bvperf, recorder.getBVperf());
		setComboItems(jCBXPanel_dvdcompat, recorder.getDVDCompatList());

		setComboItems(jCBXPanel_autodel, recorder.getAutodel());
		setComboItems(jCBXPanel_lvoice, recorder.getLVoice());
		setComboItems(jCBXPanel_aspect, recorder.getAspect());

		setComboItems(jCBXPanel_msChapter, recorder.getMsChapter());
		setComboItems(jCBXPanel_mvChapter, recorder.getMvChapter());
		setComboItems(jCBXPanel_xChapter, recorder.getXChapter());

		setComboItems(jCBXPanel_portable, recorder.getPortable());
		setComboItems(jCBXPanel_pursues, null);

		// フォルダー関係のボタン
		updateFolderList(null);
		updateFolderButtons();

		setEnabledListenerAll(true);	// リスナー再開
	}

	/**
	 * ジャンルが選択されたらそれにあわせてサブジャンルアイテムを設定する
	 * @see #setGenreItems()
	 */
	private void setSubgenreItems(ProgGenre genre) {
		ArrayList<String> items = new ArrayList<String>();
		for ( ProgSubgenre sg : ProgSubgenre.values(genre) ) {
			items.add(sg.toString());
		}

		setComboItems(jCBXPanel_subgenre, items);
	}

	/**
	 * 優先的に使用するチューナーを前に持ってくる
	 */
	public void sortEncoderItems(ArrayList<TextValueSet> preferred) {

		ArrayList<String> tmpList = new ArrayList<String>();
		for ( int i=0; i<jCBXPanel_encoder.getItemCount(); i++ ) {
			tmpList.add((String) jCBXPanel_encoder.getItemAt(i));
		}

		ArrayList<String> items = new ArrayList<String>();
		for ( String enc : tmpList ) {
			for ( TextValueSet tv : preferred ) {
				if ( tv.getText().equals(enc) ) {
					// 見つかったからついかー
					items.add(enc);
					break;
				}
			}
		}
		for ( String enc : items ) {
			tmpList.remove(enc);
		}

		for ( String enc : tmpList ) {
			items.add(enc);
		}

		setComboItems(jCBXPanel_encoder, items);
	}

	/*******************************************************************************
	 * 共通部品的な
	 ******************************************************************************/

	/**
	 * コンボボックスのアイテム登録を行う
	 */
	private <T> int setComboItems(JComboBoxPanel combo, ArrayList<T> items) {

		combo.removeAllItems();

		if ( items == null ) {
			// ここにくるのは番組追従のみかな？
			combo.addItem(ITEM_YES);
			combo.addItem(ITEM_NO);
			combo.setEnabled(true);
			return combo.getItemCount();
		}

		if ( items.size() == 0 ) {
			combo.setEnabled(false);
			return 0;
		}

		// うひー
		for ( T enc : items ) {
			if ( enc.getClass() == TextValueSet.class ) {
				TextValueSet t = (TextValueSet) enc;
				combo.addItem(t.getText());

				if (t.getDefval()) combo.setSelectedIndex(combo.getItemCount()-1);	// デフォルト値があるならば
			}
			else if ( enc.getClass() == String.class ) {
				// レコーダ・エンコーダのみかな？
				combo.addItem((String) enc);
			}
		}
		combo.setEnabled(combo.getItemCount() > 1);
		return combo.getItemCount();
	}

	private void setLabel(JComboBoxPanel combo, String overrideLabel, String defaultLabel) {
		combo.setLabelText((overrideLabel!=null)?overrideLabel:defaultLabel);
	}

	/*******************************************************************************
	 * 外部とのやり取り（設定反映系）
	 ******************************************************************************/

	/***************************************
	 * 設定の一括反映３種
	 **************************************/

	public void setRecorders( HDDRecorderList list ){ recorders = list;	}
	public void setStatusWindow( StatusWindow win ) { StWin = win; }
	public void setStatusTextArea( StatusTextArea win ){ MWin = win; }

	/**
	 * 番組情報によるアイテム選択
	 */
	public void setSelectedValues(ProgDetailList tvd, ReserveList r) {

		setEnabledListenerAll(false);

		// サブジャンルアイテム群はジャンル決定後に埋まる
		setSelectedGenreValues((tvd.genre!=null?tvd.genre.toString():null), (tvd.subgenre!=null?tvd.subgenre.toString():null));

		// チューナー
		setSelectedEncoderValue(r.getTuner());	// encがnullかどうかはメソッドの中で確認するよ

		// 番組追従（これは予約種別[arate]より先に設定しておかないといけない）
		setSelectedValue(jCBXPanel_pursues, r.getPursues() ? ITEM_YES : ITEM_NO);

		// 画質・音質
		setSelectedValue(jCBXPanel_videorate, r.getRec_mode());
		setSelectedValue(jCBXPanel_audiorate, r.getRec_audio());
		setSelectedValue(jCBXPanel_folder, r.getRec_folder());
		// サブジャンルは番組情報から
		setSelectedValue(jCBXPanel_dvdcompat, r.getRec_dvdcompat());
		setSelectedValue(jCBXPanel_device, r.getRec_device());
		updateFolderList(r.getRec_folder());

		// 自動チャプタ関連
		setSelectedValue(jCBXPanel_xChapter, r.getRec_xchapter());
		setSelectedValue(jCBXPanel_msChapter, r.getRec_mschapter());
		setSelectedValue(jCBXPanel_mvChapter, r.getRec_mvchapter());

		// その他
		setSelectedValue(jCBXPanel_aspect, r.getRec_aspect());
		setSelectedValue(jCBXPanel_bvperf, r.getRec_bvperf());
		setSelectedValue(jCBXPanel_lvoice, r.getRec_lvoice());
		setSelectedValue(jCBXPanel_autodel, r.getRec_autodel());

		setSelectedValue(jCBXPanel_portable, r.getRec_portable());

		// 実行ON・OFF
		setExecValue(r.getExec());

		setEnabledListenerAll(true);
	}

	/**
	 * 類似予約情報によるアイテム選択
	 */
	public void setSelectedValues(ReserveList r) {

		setEnabledListenerAll(false);

		// サブジャンルアイテム群はジャンル決定後に埋まる
		setSelectedGenreValues(r.getRec_genre(), r.getRec_genre());

		// チューナー
		setSelectedValue(jCBXPanel_encoder, r.getTuner());

		// 番組追従（これは予約種別[arate]より先に設定しておかないといけない）
		setSelectedValue(jCBXPanel_pursues, r.getPursues() ? ITEM_YES : ITEM_NO);

		// 画質・音質
		setSelectedValue(jCBXPanel_videorate, r.getRec_mode());
		setSelectedValue(jCBXPanel_audiorate, r.getRec_audio());
		setSelectedValue(jCBXPanel_folder, r.getRec_folder());
		setSelectedValue(jCBXPanel_subgenre, r.getRec_subgenre());
		setSelectedValue(jCBXPanel_dvdcompat, r.getRec_dvdcompat());
		setSelectedValue(jCBXPanel_device, r.getRec_device());
		updateFolderList(r.getRec_folder());

		// 自動チャプタ関連
		setSelectedValue(jCBXPanel_xChapter, r.getRec_xchapter());
		setSelectedValue(jCBXPanel_msChapter, r.getRec_mschapter());
		setSelectedValue(jCBXPanel_mvChapter, r.getRec_mvchapter());

		// その他
		setSelectedValue(jCBXPanel_aspect, r.getRec_aspect());
		setSelectedValue(jCBXPanel_bvperf, r.getRec_bvperf());
		setSelectedValue(jCBXPanel_lvoice, r.getRec_lvoice());
		setSelectedValue(jCBXPanel_autodel, r.getRec_autodel());

		setSelectedValue(jCBXPanel_portable, r.getRec_portable());

		// 実行ON・OFF
		setExecValue(r.getExec());

		setEnabledListenerAll(true);
	}

	/**
	 * ジャンル別ＡＶ設定によるアイテム選択
	 */
	public void setSelectedValues(AVs avs) {

		setEnabledListenerAll(false);

		// 画質・音質
		setSelectedValue(jCBXPanel_videorate, avs.getVideorate());
		setSelectedValue(jCBXPanel_audiorate, avs.getAudiorate());
		setSelectedValue(jCBXPanel_folder, avs.getFolder());
		// サブジャンルは確定済み
		setSelectedValue(jCBXPanel_dvdcompat, avs.getDVDCompat());
		setSelectedValue(jCBXPanel_device, avs.getDevice());
		updateFolderList(avs.getFolder());
		updateFolderButtons();

		// 自動チャプタ関連
		setSelectedValue(jCBXPanel_xChapter, avs.getXChapter());
		setSelectedValue(jCBXPanel_msChapter, avs.getMsChapter());
		setSelectedValue(jCBXPanel_mvChapter, avs.getMvChapter());

		// その他
		setSelectedValue(jCBXPanel_aspect, avs.getAspect());
		setSelectedValue(jCBXPanel_bvperf, avs.getBvperf());
		setSelectedValue(jCBXPanel_lvoice, avs.getLvoice());
		setSelectedValue(jCBXPanel_autodel, avs.getAutodel());

		setSelectedValue(jCBXPanel_portable, avs.getPortable());

		setEnabledListenerAll(true);
	}

	/***************************************
	 * 設定の部分反映各種
	 **************************************/

	/**
	 * 実行ON・OFFの強制設定
	 */
	public void setExecValue(boolean b) {
		jCheckBox_Exec.setSelected(b);
		jCheckBox_Exec.setForeground(b ? Color.BLACK : Color.RED);
	}

	/**
	 * レコーダのアイテム選択（中から呼んじゃだめだよ）
	 */
	public String setSelectedRecorderValue(String myself) {
		setEnabledListenerAll(false);
		String s = setSelectedRecorderId(myself);
		setEnabledListenerAll(true);
		return s;
	}

	/**
	 * エンコーダのアイテム選択
	 */
	public String setSelectedEncoderValue(String enc) {

		if ( enc == null ) {
			jCBXPanel_encoder.setSelectedIndex(0);
			jLabel_encoderemptywarn.setText("空きｴﾝｺｰﾀﾞ検索無効");
			jLabel_encoderemptywarn.setForeground(Color.CYAN);
		}
		else if ( enc.length() == 0 ) {
			jCBXPanel_encoder.setSelectedIndex(0);
			jLabel_encoderemptywarn.setText("空きｴﾝｺｰﾀﾞ不足");
			jLabel_encoderemptywarn.setForeground(Color.RED);
		}
		else {
			jCBXPanel_encoder.setSelectedItem(enc);
			jLabel_encoderemptywarn.setText("空きｴﾝｺｰﾀﾞあり");
			jLabel_encoderemptywarn.setForeground(Color.BLUE);
		}

		return (String) jCBXPanel_encoder.getSelectedItem();
	}

	/**
	 * Vrateのアイテム選択（中から呼んじゃだめだよ）
	 */
	public String setSelectedVrateValue(String vrate) {

		if ( vrate == null ) {
			return null;
		}

		return setSelectedValue(jCBXPanel_videorate, vrate);
	}

	/**
	 * ジャンルアイテムの選択
	 */
	private String setSelectedGenreValues(String genre, String subgenre) {

		if ( genre == null || genre.length() == 0 ) {
			genre = ProgGenre.NOGENRE.toString();
		}
		jCBXPanel_genre.setSelectedItem(null);
		jCBXPanel_genre.setSelectedItem(genre);

		if ( subgenre == null || subgenre.length() == 0 ) {
			jCBXPanel_subgenre.setSelectedIndex(0);
		}
		else {
			jCBXPanel_subgenre.setSelectedItem(subgenre);
		}

		return (String) jCBXPanel_genre.getSelectedItem();
	}

	/**
	 * アイテム選択
	 */
	private String setSelectedValue(JComboBoxPanel comp, String value) {

		if ( value != null && value.length() > 0 ) {
			int index = comp.getSelectedIndex();
			comp.setSelectedItem(null);
			comp.setSelectedItem(value);
			String s = (String) comp.getSelectedItem();
			if ( s != null ) {
				return s;
			}

			// 存在しない選択肢が指定されたからもともと選択してた項目を選びなおすわー
			comp.setSelectedIndex(index);
		}
		else if ( comp.getItemCount() > 0 ){
			comp.setSelectedItem(null);
			comp.setSelectedIndex(0);
		}

		String s = (String) comp.getSelectedItem();
		return s;
	}

	/*******************************************************************************
	 * 外部とのやり取り（設定取得系）
	 ******************************************************************************/

	/***************************************
	 * 取得系
	 **************************************/

	/**
	 * 選択値を予約情報に代入する
	 */
	public ReserveList getSelectedValues(ReserveList r) {

		r.setTuner((String) jCBXPanel_encoder.getSelectedItem());

		r.setRec_mode((String) jCBXPanel_videorate.getSelectedItem());
		r.setRec_audio((String) jCBXPanel_audiorate.getSelectedItem());
		r.setRec_folder((String) jCBXPanel_folder.getSelectedItem());
		r.setRec_genre((String) jCBXPanel_genre.getSelectedItem());
		r.setRec_subgenre((String) jCBXPanel_subgenre.getSelectedItem());
		r.setRec_dvdcompat((String) jCBXPanel_dvdcompat.getSelectedItem());
		r.setRec_device((String) jCBXPanel_device.getSelectedItem());

		// 自動チャプタ関連
		r.setRec_xchapter((String) jCBXPanel_xChapter.getSelectedItem());
		r.setRec_mschapter((String) jCBXPanel_msChapter.getSelectedItem());
		r.setRec_mvchapter((String) jCBXPanel_mvChapter.getSelectedItem());

		// その他
		r.setRec_aspect((String) jCBXPanel_aspect.getSelectedItem());
		r.setRec_bvperf((String) jCBXPanel_bvperf.getSelectedItem());
		r.setRec_lvoice((String) jCBXPanel_lvoice.getSelectedItem());
		r.setRec_autodel((String) jCBXPanel_autodel.getSelectedItem());

		// 持ち出し
		r.setRec_portable((String)jCBXPanel_portable.getSelectedItem());

		// 番組追従
		r.setPursues(ITEM_YES == jCBXPanel_pursues.getSelectedItem());

		// 実行ON・OFF
		r.setExec(jCheckBox_Exec.isSelected());

		return r;
	}

	public ReserveList getSelectedValues() {
		return getSelectedValues(new ReserveList());
	}

	/**
	 * 選択値をジャンル別録画設定情報に代入する
	 */
	public AVs getSelectedSetting(AVs c) {

		c.setGenre((String) jCBXPanel_genre.getSelectedItem());

		c.setVideorate((String) jCBXPanel_videorate.getSelectedItem());
		c.setAudiorate((String) jCBXPanel_audiorate.getSelectedItem());
		c.setFolder((String) jCBXPanel_folder.getSelectedItem());
		c.setDVDCompat((String) jCBXPanel_dvdcompat.getSelectedItem());
		c.setDevice((String) jCBXPanel_device.getSelectedItem());

		c.setXChapter((String) jCBXPanel_xChapter.getSelectedItem());
		c.setMsChapter((String) jCBXPanel_msChapter.getSelectedItem());
		c.setMvChapter((String) jCBXPanel_mvChapter.getSelectedItem());

		c.setAspect((String) jCBXPanel_aspect.getSelectedItem());
		c.setBvperf((String) jCBXPanel_bvperf.getSelectedItem());
		c.setLvoice((String) jCBXPanel_lvoice.getSelectedItem());
		c.setAutodel((String) jCBXPanel_autodel.getSelectedItem());

		c.setPortable((String)jCBXPanel_portable.getSelectedItem());

		c.setPursues(ITEM_YES == jCBXPanel_pursues.getSelectedItem());

		return c;
	}

	public AVs getSelectedSetting() {
		return getSelectedSetting(new AVs());
	}

	/**
	 * 選択されているレコーダを取得する
	 */
	private HDDRecorder getSelectedRecorder() {
		String selected = getSelectedRecorderId();

		if (recorders == null)
			return null;

		HDDRecorderList list = recorders.findInstance( selected );
		return (list != null && list.size() > 0) ? list.get(0) : null;
	}

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	/***************************************
	 * 永続的なリスナー
	 **************************************/

	/**
	 * ジャンルを選択したらサブジャンルの選択肢を入れ替える
	 */
	private final ItemListener f_il_genreSelected = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			// サブジャンルのアイテムをリセットする
			String gstr = (String) jCBXPanel_genre.getSelectedItem();
			if ( gstr != null ) {
				setSubgenreItems(ProgGenre.get(gstr));
			}
		}
	};

	/**
	 * EPG予約以外では番組追従が設定できないようにしたいな
	 */
	private final ItemListener f_il_recTypeChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if ( e.getStateChange() != ItemEvent.SELECTED ) {
				return;
			}

			String pgtype = (String) jCBXPanel_audiorate.getSelectedItem();
			if ( pgtype == HDDRecorder.ITEM_REC_TYPE_PROG ) {
				// "ﾌﾟﾗｸﾞﾗﾑ予約"なら触る必要なし
				jCBXPanel_pursues.setSelectedItem(ITEM_NO);
				jCBXPanel_pursues.setEnabled(false);
				checkMarginTop(true);
				checkMarginBottom(true);
			}
			else if ( pgtype == HDDRecorder.ITEM_REC_TYPE_EPG ) {
				jCBXPanel_pursues.setSelectedItem(ITEM_YES);	// EPG予約にするなら追従ありがデフォルトでいいだろ？
				jCBXPanel_pursues.setEnabled(true);
				checkMarginTop(true);
				checkMarginBottom(true);
			}
			else {
				jCBXPanel_pursues.setEnabled(true);
				checkMarginTop(false);
				checkMarginBottom(false);
			}

		}
	};
	private final ItemListener f_il_marginTopChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if ( e.getStateChange() != ItemEvent.SELECTED ) {
				return;
			}

			String pgtype = (String) jCBXPanel_audiorate.getSelectedItem();
			if ( pgtype == HDDRecorder.ITEM_REC_TYPE_EPG || pgtype == HDDRecorder.ITEM_REC_TYPE_PROG ) {
				checkMarginTop(true);
			}
		}
	};
	private final ItemListener f_il_marginBottomChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if ( e.getStateChange() != ItemEvent.SELECTED ) {
				return;
			}

			String pgtype = (String) jCBXPanel_audiorate.getSelectedItem();
			if ( pgtype == HDDRecorder.ITEM_REC_TYPE_EPG || pgtype == HDDRecorder.ITEM_REC_TYPE_PROG ) {
				checkMarginBottom(true);
			}
		}
	};
	private void checkMarginTop(boolean check) {
		_checkMargin(jCBXPanel_msChapter,check);	// 開始マージン０は危ないよね
	}
	private void checkMarginBottom(boolean check) {
		_checkMargin(jCBXPanel_mvChapter,check);	// 終了マージン０は危ないよね
	}
	private void _checkMargin(JComboBoxPanel comp, boolean check) {
		Color c = Color.BLACK;
		try {
			if ( check && comp.getLabelText().startsWith("録画") && Integer.valueOf((String) comp.getSelectedItem()) <= 0) {
				c = Color.RED;
			}
		}
		catch (NumberFormatException ev) {
		}
		comp.setLabelForeground(c);
	}

	/**
	 * ジャンル別ＡＶ設定のロード
	 */
	private final ActionListener f_al_loadAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if ( recsetsel != null ) {
				setEnabledListenerAll(false);

				recsetsel.doSetAVSettings();

				setEnabledListenerAll(true);
			}
		}
	};

	/**
	 * ジャンル別ＡＶ設定のセーブ
	 */
	private final ActionListener f_al_saveAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if ( recsetsel != null ) recsetsel.doSaveAVSettings(false);
		}
	};

	/**
	 * 既定ＡＶ設定のセーブ
	 */
	private final ActionListener f_al_saveDefaultAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if ( recsetsel != null ) recsetsel.doSaveAVSettings(true);
		}
	};


	/***************************************
	 * つけたり外したりするリスナーをつけたり外したりするメソッド
	 **************************************/

	/**
	 * イベントトリガーでアイテムを操作する際に、さらにイベントをキックされてはたまらないのでリスナーを付けたり外したりする
	 */
	private void setEnabledListenerAll(boolean enabled) {
		setEnabledItemListener(jCBXPanel_recorder, il_recorderChanged, enabled);
		setEnabledItemListener(jCBXPanel_encoder, il_encoderChanged, enabled);
		setEnabledItemListener(jCBXPanel_videorate, il_vrateChanged, enabled);
		setEnabledItemListener(jCBXPanel_genre, il_genreChanged, enabled);
		setEnabledItemListener(jCBXPanel_device, il_deviceChanged, enabled);
		setEnabledItemListener(jCBXPanel_folder, il_folderChanged, enabled);
	}
	private void setEnabledItemListener(ItemSelectable comp, ItemListener il, boolean b) {
		comp.removeItemListener(il);
		if ( b ) {
			comp.addItemListener(il);
		}
	}

	/***************************************
	 * つけたり外したりするリスナー群
	 **************************************/

	/**
	 *  レコーダーが選択された
	 */
	private final ItemListener il_recorderChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if ( e.getStateChange() != ItemEvent.SELECTED ) {
				return;
			}

			if ( recsetsel != null ) {
				setEnabledListenerAll(false);	// 停止

				recsetsel.doSelectRecorder(getSelectedRecorderId());

				recsetsel.doSetAVSettings();

				setEnabledListenerAll(true);	// 再開
			}
		}
	};

	/**
	 *  エンコーダが選択された（ので利用可能な画質を選びなおす）
	 */
	private final ItemListener il_encoderChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if ( e.getStateChange() != ItemEvent.SELECTED ) {
				return;
			}

			if ( recsetsel != null ) {
				setEnabledListenerAll(false);	// 停止

				recsetsel.doSelectEncoder((String) jCBXPanel_encoder.getSelectedItem());

				setEnabledListenerAll(true);	// 再開
			}
		}
	};


	/**
	 *  画質が選択された（ので利用可能なエンコーダを選びなおす）
	 */
	private final ItemListener il_vrateChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if ( e.getStateChange() != ItemEvent.SELECTED ) {
				return;
			}

			if ( recsetsel != null ) {
				setEnabledListenerAll(false);	// 停止

				recsetsel.doSelectVrate((String) jCBXPanel_videorate.getSelectedItem());

				setEnabledListenerAll(true);	// 再開
			}
		}
	};


	/**
	 * ジャンルが選択された
	 */
	private final ItemListener il_genreChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() != ItemEvent.SELECTED) {
				return;
			}

			// サブジャンルの選択肢を入れ替える
			setSubgenreItems(ProgGenre.get((String) jCBXPanel_genre.getSelectedItem()));

			// ＡＶ設定変更してーん
			if ( recsetsel != null ) recsetsel.doSetAVSettings();
		}
	};

	/**
	 * デバイスコンボの選択変更時の処理
	 * デバイス情報更新後、タイトル一覧を更新する
	 */
	private final ItemListener il_deviceChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				updateFolderList(null);
				updateFolderButtons();
			}
		}
	};

	/**
	 * フォルダーを追加する
	 */
	private ActionListener al_addFolder = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			// 「指定なし」が選ばれている場合は「追加」とみなし、フォルダ名の初期値は番組タイトルとする
			// それ以外が選ばれている場合はそのフォルダの「変更」とみなし、フォルダ名の初期値は現在の値とする

			VWFolderDialog dlg = new VWFolderDialog();
			CommonSwingUtils.setLocationCenter(jCBXPanel_folder.getParent(), dlg);

			HDDRecorder rec = getSelectedRecorder();

			String device_name = (String)jCBXPanel_device.getSelectedItem();
			String device_id = text2value(rec.getDeviceList(), device_name);
			String title = CommonUtils.substringrb(recsetsel.doGetSelectedTitle(), 80);

			int idx = jCBXPanel_folder.getSelectedIndex();
			String folder_name = (String)jCBXPanel_folder.getSelectedItem();
			String folder_id = text2value(rec.getFolderList(), folder_name);

			String prefix = "[" + device_name + "] ";

			// 変更の場合はフォルダ名が[<device_name>]で始まっている必要がある。
			String nameOld = folder_name;
			if (idx != 0){
				if (!nameOld.startsWith(prefix))
					return;
				// [<device_name>]の部分を取り除く
				nameOld = nameOld.substring(prefix.length());
			}

			dlg.open(idx == 0 ? title : nameOld);
			dlg.setVisible(true);

			if (!dlg.isRegistered())
				return;

			String nameNew = dlg.getFolderName();
			String action = idx == 0 ? "作成" : "更新";
			folderNameWorking = idx == 0 ? nameNew  : "[" + nameOld + "] -> [" + nameNew + "]";

			// フォルダー作成実行
			StWin.clear();
			new SwingBackgroundWorker(false) {
				@Override
				protected Object doWorks() throws Exception {
					StWin.appendMessage(MSGID+"フォルダーを" + action + "します："+folderNameWorking);

					boolean reg = false;
					if (idx != 0)
						reg = rec.UpdateRdFolderName(device_id, folder_id, nameNew);
					else
						reg = rec.CreateRdFolder(device_id, nameNew);
					if (reg){
						MWin.appendMessage(MSGID+"フォルダーを正常に" + action + "できました："+folderNameWorking);
						// [<device_name>]を先頭に付ける
						updateFolderList(prefix + nameNew);
						updateFolderButtons();
					}
					else {
						MWin.appendError(ERRID+"フォルダーの" + action + "に失敗しました："+folderNameWorking);

						if ( ! rec.getErrmsg().equals("")) {
							MWin.appendMessage(MSGID+"[追加情報] "+rec.getErrmsg());
						}
					}

					return null;
				}
				@Override
				protected void doFinally() {
					StWin.setVisible(false);
				}
			}.execute();

			CommonSwingUtils.setLocationCenter(jCBXPanel_folder.getParent(), (Component)StWin);
			StWin.setVisible(true);
		}
	};

	/**
	 * フォルダーを削除する
	 */
	private ActionListener al_delFolder = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			HDDRecorder rec = getSelectedRecorder();

			String device_name = (String)jCBXPanel_device.getSelectedItem();
			String device_id = text2value(rec.getDeviceList(), device_name);

			String folder_name = (String)jCBXPanel_folder.getSelectedItem();
			String folder_id = text2value(rec.getFolderList(), folder_name);

			String prefix = "[" + device_name + "] ";

			// フォルダ名が[<device_name>]で始まっている必要がある。
			String nameOld = folder_name;
			if (!nameOld.startsWith(prefix))
				return;

			// [<device_name>]の部分を取り除く
			folderNameWorking = nameOld.substring(prefix.length());

			// フォルダー削除実行
			StWin.clear();
			new SwingBackgroundWorker(false) {
				@Override
				protected Object doWorks() throws Exception {
					StWin.appendMessage(MSGID+"フォルダーを削除します："+folderNameWorking);

					if (rec.RemoveRdFolder( device_id, folder_id )){
						MWin.appendMessage(MSGID+"フォルダーを正常に削除できました："+folderNameWorking);
						updateFolderList(null);
						updateFolderButtons();
					}
					else {
						MWin.appendError(ERRID+"フォルダーの削除に失敗しました："+folderNameWorking);
					}
					if ( ! rec.getErrmsg().equals("")) {
						MWin.appendMessage(MSGID+"[追加情報] "+rec.getErrmsg());
					}

					return null;
				}
				@Override
				protected void doFinally() {
					StWin.setVisible(false);
				}
			}.execute();

			CommonSwingUtils.setLocationCenter(jCBXPanel_folder.getParent(), (Component)StWin);
			StWin.setVisible(true);
		}
	};

	/**
	 * フォルダーを選択する
	 */
	private final ItemListener il_folderChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				updateFolderButtons();
			}
		}
	};

	/**
	 * フォルダーを選択する
	 */
	private void updateFolderButtons() {
		HDDRecorder rec = getSelectedRecorder();
		boolean b = rec.isFolderCreationSupported();

		int idx = jCBXPanel_folder.getSelectedIndex();
		jButton_addFolder.setEnabled(b);
		jButton_delFolder.setEnabled(b && idx != 0);
		jButton_addFolder.setText(idx == 0 ? "新" : "更");
	}

	/**
	 * フォルダーコンボを更新する
	 * @param sel 更新後選択するフォルダーの名称
	 */
	protected void updateFolderList(String sel){
		String device_name = (String)jCBXPanel_device.getSelectedItem();
		String prefix = "";
		if (device_name != null)
			prefix = "[" + device_name + "]";

		HDDRecorder rec = getSelectedRecorder();
		if (rec == null)
			return;

		JComboBoxPanel combo = jCBXPanel_folder;
		ArrayList<TextValueSet> tvs = rec.getFolderList();

		combo.removeAllItems();
		int idx = 0;
		int no = 0;
		if (tvs != null){
			for ( TextValueSet t : tvs ) {
				if ( t == null || t.getValue() == null || t.getText() == null )
					continue;

				if (! t.getValue().equals(FOLDER_ID_ROOT) && ! t.getValue().equals("-1") &&
					device_name != null && !t.getText().startsWith(prefix))
					continue;

				if (sel != null && t.getText().equals(sel))
					idx = no;
				combo.addItem(t.getText());
				no++;
			}
		}

		if (no > 0)
			combo.setSelectedIndex(idx);
		combo.setEnabled( combo.getItemCount() > 0 );
	}

	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	private JButton getJButton_addFolder(String s) {
		if (jButton_addFolder == null) {
			jButton_addFolder = new JButton(s);

			jButton_addFolder.addActionListener(al_addFolder);
		}
		return jButton_addFolder;
	}

	private JButton getJButton_delFolder(String s) {
		if (jButton_delFolder == null) {
			jButton_delFolder = new JButton(s);

			jButton_delFolder.addActionListener(al_delFolder);
		}
		return jButton_delFolder;
	}

	// 素直にHashMapつかっておけばよかった
	public String text2value(ArrayList<TextValueSet> tvs, String text) {
		for ( TextValueSet t : tvs ) {
			if (t.getText().equals(text)) {
				return(t.getValue());
			}
		}
		return("");
	}
}
