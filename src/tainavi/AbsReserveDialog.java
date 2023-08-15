package tainavi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;

import tainavi.HDDRecorder.RecType;
import tainavi.TVProgram.ProgGenre;
import tainavi.TVProgram.ProgOption;
import tainavi.TVProgram.ProgSubgenre;
import tainavi.TitleEditorPanel.TimeVal;


// ソースがもうグチャグチャでございます.

/**
 * 予約ダイアログのクラス
 * @since 3.15.4β　ReserveDialogからクラス名変更
 * @version 3.22.2β コンポーネントを、番組情報部・録画設定部、類似予約部の３つに分離（このまま突き進めばロジックとSwingコンポーネントを分離できるんじゃないかしら？）
 */
abstract class AbsReserveDialog extends JEscCancelDialog implements
HDDRecorderListener,RecordExecutable,RecSettingSelectable,LikeReserveSelectable,TitleEditorSelectable {

	private static final long serialVersionUID = 1L;

	public static String getViewName() { return "予約ダイアログ"; }

	public void setDebug(boolean b) { debug = b; }
	private static boolean debug = false;


	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/

	protected abstract Env getEnv();
	protected abstract TVProgramList getTVProgramList();
	protected abstract HDDRecorderList getRecorderList();

	protected abstract AVSetting getAVSetting();
	protected abstract CHAVSetting getCHAVSetting();

	protected abstract StatusWindow getStWin();
	protected abstract StatusTextArea getMWin();

	protected abstract Component getParentComponent();

	protected abstract void ringBeep();

	// クラス内のイベントから呼び出されるもの
	protected abstract LikeReserveList findLikeReserves(ProgDetailList tvd, String keyword, int threshold);


	/*******************************************************************************
	 * 呼び出し元から引き継いだもの
	 ******************************************************************************/

	private final Env env = getEnv();
	private final HDDRecorderList recorders = getRecorderList();

	private final AVSetting avslist = getAVSetting();
	private final CHAVSetting chavslist = getCHAVSetting();

	private final StatusWindow StWin = getStWin();			// これは起動時に作成されたまま変更されないオブジェクト
	private final StatusTextArea MWin = getMWin();			// これは起動時に作成されたまま変更されないオブジェクト

	private final Component parent = getParentComponent();	// これは起動時に作成されたまま変更されないオブジェクト

	private final GetEventId geteventid = new GetEventId();	// 番組IDの取得


	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	// ログ関連

	private static final String MSGID = "["+getViewName()+"] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;


	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// コンポーネント

	private JPanel jContentPane_rsv = null;

	private TitleEditorPanel jPane_title = null;				// 番組設定
	private RecSettingEditorPanel jPane_recsetting = null;		// 録画設定
	private LikeReserveEditorPanel  jPane_likersv = null;		// 類似予約
	private OverlapReserveViewPanel jPane_overlap = null;


	/*
	 * その他
	 */

	/**
	 * 初期化漏れが怖いのでまとめて内部クラスとした。
	 */
	private class Vals {

		// 検索した類似予約を保持する
		LikeReserveList hide_likersvlist = null;

		// 類似予約抽出条件（タイトル）
		String keyword = "";

		// 類似予約抽出条件（あいまい度）
		int threshold = 0;

		// 実行のON/OFFのみの更新かどうか
		boolean isUpdateOnlyExec = false;

		// 予約する番組情報
		HDDRecorder hide_recorder = null;
		ProgDetailList hide_tvd = null;
		AVs hide_avs = null;

		// 録画設定の選択イベントにより設定される値
		HDDRecorder selected_recorder = null;
	}

	private Vals vals = null;

	/**
	 * 予約操作が成功したかどうかを返す。
	 */
	public boolean isSucceededReserve() { return doneReserve; }

	private boolean doneReserve = false;


	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public AbsReserveDialog(int x, int y) {

		super();

		setModal(true);
		setContentPane(getJContentPane_rsv());

		// タイトルバーの高さも考慮する必要がある
		Dimension d = getJContentPane_rsv().getPreferredSize();
		pack();
		setBounds(
				x,
				y,
				d.width+(this.getInsets().left+this.getInsets().right),
				d.height+(this.getInsets().top+this.getInsets().bottom));
		setResizable(false);

		setTitle(getViewName());

		// とりあえず起動時のみ設定可能
		jPane_recsetting.setDebug(env.getDebug());

		addWindowListener(wl_opened);

		// 番組ID取得時に設定画面で入力したUserAgentを使用する
		geteventid.setUserAgent(env.getUserAgent());
	}


	/*******************************************************************************
	 * ダイアログオープン
	 ******************************************************************************/

	/***************************************
	 * 番組情報からのオープン２種＋α
	 **************************************/

	/**
	 * 類似予約抽出条件なしオープン
	 * @see #doSelectLikeReserve(int)
	 */
	public boolean open(ProgDetailList tvd) {
		return open(tvd,null,0);
	}

	/**
	 *  類似予約抽出条件ありオープン
	 * @see #doSelectLikeReserve(int)
	 */
	public boolean open(ProgDetailList tvd, String keywordVal, int thresholdVal) {

		// 予約は行われてないよー
		doneReserve = false;

		if (recorders.size() == 0) {
			return false;	// レコーダがひとつもないのはやばい
		}
		if (tvd.start.equals("")) {
			return false;	// これは「番組情報がありません」だろう
		}

		// 初期パラメータの保存場所
		if (vals == null) vals = new Vals();

		// 選択中のレコーダ
		String myself = getSelectedMySelf();				// ツールバーで選択されているのはどれかな？
		HDDRecorder myrec = getSelectedRecorderList().get(0);	// 先頭を選んでおけばおけ

		vals.hide_recorder = myrec;	// 隠しパラメータ
		vals.hide_tvd = tvd;		// 隠しパラメータ

		// ダイアログオープン時に自動で取得する
		if ( env.getAutoEventIdComplete() ) {
			tvd.progid = getEventIdOnOpen(tvd);
		}

		// 類似予約抽出条件
		if ( thresholdVal > 0 ) {
			vals.keyword = keywordVal;		// 隠しパラメータ
			vals.threshold = thresholdVal;	// 隠しパラメータ
		}

		// 類似予約情報
		ReserveList myrsv = null;
		int myrsvidx = -1;
		LikeReserveList likersvlist = findLikeReserves(tvd, vals.keyword, vals.threshold);	// 類似予約リストの作成
		if ( env.getGivePriorityToReserved() ) {
			// 類似予約が優先される
			LikeReserveItem likersv = findClosestLikeReserve(likersvlist, myself);		// 類似予約の絞り込み
			if ( likersv != null ) {
				myrsvidx = likersvlist.getClosestIndex();
				myrsv = likersv.getRsv();
				myrec = likersv.getRec();
			}
		}

		vals.hide_likersvlist = likersvlist;	// 隠しパラメータ

		// ジャンル別ＡＶ設定の確認（該当するものがあれば）
		AVs myavs = null;
		if ( myrsv == null ) {
			// 類似予約がないか、あっても優先されない場合
			myavs = findAVs(tvd.genre.toString(), tvd.center, myrec.getRecorderId());
		}
		else {
			MWin.appendMessage(MSGID+"画質・音質を類似予約から継承します.");
		}

		vals.hide_avs = myavs;	// 隠しパラメータ

		// 類似予約リストのアイテム設定
		setLikeRsvItems(likersvlist);

		// 初期値の選択
		if ( env.getGivePriorityToReserved() && myrsv != null ) {
			// それっぽい類似予約を選択する
			jPane_likersv.setRowSelection(myrsvidx);
		}
		else {
			// 番組情報から選択する
			jPane_likersv.setRowSelection(LikeReserveEditorPanel.LIKERSVTABLE_NONE);
		}

		jPane_title.setSelector(this);

		return true;
	}

	/**
	 * 隣接予約の編集
	 */
	public boolean open(ProgDetailList tvd, LikeReserveItem likersv) {

		// 予約は行われてないよー
		doneReserve = false;

		if (recorders.size() == 0) {
			return false;	// レコーダがひとつもないのはやばい
		}
		if (tvd.start.equals("")) {
			return false;	// これは「番組情報がありません」だろう
		}

		// 初期パラメータの保存場所
		if (vals == null) vals = new Vals();

		// 選択中のレコーダ
		HDDRecorder myrec = likersv.getRec();
//		String myself = myrec.Myself();

		vals.hide_recorder = myrec;	// 隠しパラメータ
		vals.hide_tvd = tvd;		// 隠しパラメータ

		// ダイアログオープン時に自動で取得する
		if ( env.getAutoEventIdComplete() ) {
			tvd.progid = getEventIdOnOpen(tvd);
		}

		LikeReserveList likersvlist = new LikeReserveList();
		likersvlist.add(likersv);

		vals.hide_likersvlist = likersvlist;	// 隠しパラメータ

		MWin.appendMessage(MSGID+"画質・音質を類似予約から継承します.");

		// 類似予約リストのアイテム設定
		setLikeRsvItems(likersvlist);

		// 初期値の選択（類似予約の一個目を選択）
		jPane_likersv.setRowSelection(LikeReserveEditorPanel.LIKERSVTABLE_DEFAULT);

		// 番組情報は番組情報から得た情報を優先する　←変な日本語
		jPane_title.setSelectedValues(tvd);

		// 各コンポーネントの強制状態変更
		jPane_title.setEnabledRecordButton(false);	// 新規ボタンは操作不能に
		jPane_likersv.setEnabledTable(false);		// 類似予約は選択不能に

		return true;
	}

	/***************************************
	 * 予約情報からのオープン１種
	 **************************************/

	/**
	 * 実行のON/OFFだけしか操作しない場合に呼び出す（画面にウィンドウは表示しない）
	 * ※これがあるので、各openでは vals != null チェックの必要がある
	 */
	public boolean open(String myself, String rsvId) {

		return open(myself, rsvId, null);

	}

	/**
	 * 本体予約一覧からのオープン、または予約ＯＮ／ＯＦＦメニュー
	 * @see #doSelectLikeReserve(int)
	 */
	public boolean open(String myself, String rsvId, Boolean onlyupdateexec) {

		// 予約は行われてないよー
		doneReserve = false;

		HDDRecorderList myrecs = recorders.findInstance(myself);
		if ( myrecs.size() == 0 ) {
			return false;	// ここに来たらバグ
		}
		HDDRecorder myrec = myrecs.get(0);

		ReserveList myrsv = myrec.getReserveList(rsvId);
		if ( myrsv == null ) {
			MWin.appendError(ERRID+"更新すべき予約情報が見つかりません: "+myself+", "+rsvId);
			ringBeep();
			return false;	// ここに来たらバグ
		}
		if ( myrsv.getCh_name() == null ) {
			MWin.appendError(ERRID+"予約情報の放送局名が不正です: "+myrsv.getStartDateTime()+", "+myrsv.getTitle());
			ringBeep();
			return false;
		}

		// 初期パラメータの保存場所
		if (vals == null) vals = new Vals();

		// 予約情報から番組情報を組み立てる
		ProgDetailList tvd = getProgDetails(myrsv);

		vals.hide_recorder = myrec;	// 隠しパラメータ
		vals.hide_tvd = tvd;		// 隠しパラメータ

		// 予約情報（類似予約の一個目として設定）
		LikeReserveList likersvlist = new LikeReserveList();
		likersvlist.add(new LikeReserveItem(myrec, myrsv, 0));

		vals.hide_likersvlist = likersvlist;	// 隠しパラメータ

		// 類似予約リストのアイテム設定
		setLikeRsvItems(likersvlist);

		// 初期値の選択（類似予約の一個目を選択）
		jPane_likersv.setRowSelection(LikeReserveEditorPanel.LIKERSVTABLE_DEFAULT);

		// 各コンポーネントの強制状態変更
		jPane_title.setEnabledRecordButton(false);	// 新規ボタンは操作不能に
		jPane_likersv.setEnabledTable(false);		// 類似予約は選択不能に

		/*
		 *  予約ON・OFFのみの実行かなー？
		 */
		if ( onlyupdateexec != null ) {
			vals.isUpdateOnlyExec = true;
			jPane_recsetting.setExecValue(onlyupdateexec);
		}

		return true;
	}

	/***************************************
	 * オープン用部品
	 **************************************/

	/**
	 * 類似予約リストの取得
	 */
	private LikeReserveItem findClosestLikeReserve(LikeReserveList lrl, String myself) {

		if ( lrl.size() == 0 ) {
			// 類似予約がない
			return null;
		}

		LikeReserveItem lr = lrl.getClosest(myself);
		if ( lr == null ) {
			// 類似予約があってもコンボボックスで選択したレコーダのものがない
			if (debug) System.out.println(DBGID+"類似予約に選択中のレコーダのものはなかった： "+myself);
			return null;
		}

		// 選択中のレコーダの類似予約があった
		return lr;
	}

	/**
	 * ジャンル別ＡＶ設定の取得
	 */
	private AVs findAVs(String key_genre, String key_webChName, String recId) {

		String selected_key = key_genre;
		AVSetting xavslist = avslist;
		if ( env.getEnableCHAVsetting() ) {
			selected_key = key_webChName;
			xavslist = chavslist;
		}

		AVs myavs = xavslist.getSelectedAVs(selected_key, recId);
		if ( myavs != null ) {
			if ( myavs.getGenre() != null ) {
				MWin.appendMessage(MSGID+"画質・音質を自動設定します： "+recId+" & "+myavs.getGenre());
			}
			else {
				MWin.appendMessage(MSGID+"画質・音質にデフォルト設定を適用します： "+recId);
			}
		}
		else {
			MWin.appendMessage(MSGID+"画質・音質の自動設定候補がありません： "+recId+" & "+selected_key);
		}

		return myavs;
	}

	/**
	 * 番組情報から予約情報を生成する
	 */
	private ReserveList getReserveList(HDDRecorder recorder, ProgDetailList tvd, TimeVal tVal, String enc) {

		ReserveList r = new ReserveList();

		// 開始・終了時刻
		if ( tvd != null ) {
			r.setTitle(tvd.title);
			r.setCh_name(tvd.center);
			GregorianCalendar ca = CommonUtils.getCalendar(tVal!=null ? tVal.startDateTime : tvd.startDateTime);
			GregorianCalendar cz = CommonUtils.getCalendar(tVal!=null ? tVal.endDateTime : tvd.endDateTime);
			r.setAhh(String.format("%02d",ca.get(Calendar.HOUR_OF_DAY)));
			r.setAmm(String.format("%02d",ca.get(Calendar.MINUTE)));
			r.setZhh(String.format("%02d",cz.get(Calendar.HOUR_OF_DAY)));
			r.setZmm(String.format("%02d",cz.get(Calendar.MINUTE)));
		}

		// チューナー
		r.setTuner(enc);

		// 画質・音質
		r.setRec_mode(getDefaultText(recorder, recorder.getVideoRateList()));
		r.setRec_audio(getDefaultText(recorder, recorder.getAudioRateList()));
		r.setRec_folder(getDefaultText(recorder, recorder.getFolderList()));

		r.setRec_dvdcompat(getDefaultText(recorder, recorder.getDVDCompatList()));
		r.setRec_device(getDefaultText(recorder, recorder.getDeviceList()));

		// 自動チャプタ関連
		r.setRec_xchapter(getDefaultText(recorder, recorder.getXChapter()));
		r.setRec_mschapter(getDefaultText(recorder, recorder.getMsChapter()));
		r.setRec_mvchapter(getDefaultText(recorder, recorder.getMvChapter()));

		// その他
		r.setRec_aspect(getDefaultText(recorder, recorder.getAspect()));
		r.setRec_bvperf(getDefaultText(recorder, recorder.getBVperf()));
		r.setRec_lvoice(getDefaultText(recorder, recorder.getLVoice()));
		r.setRec_autodel(getDefaultText(recorder, recorder.getAutodel()));

		r.setExec(true);

		return r;
	}

	private String getDefaultText(HDDRecorder myrec, ArrayList<TextValueSet> tvs) {
		TextValueSet t = myrec.getDefaultSet(tvs);
		if ( t != null ) {
			return t.getText();
		}
		if ( tvs.size() > 0 ) {
			return tvs.get(0).getText();
		}
		return null;
	}

	/**
	 * 予約情報から番組情報を生成する
	 */
	private ProgDetailList getProgDetails(ReserveList myrsv) {

		ProgDetailList tvd = new ProgDetailList();

		tvd.title = myrsv.getTitle();
		tvd.detail = myrsv.getDetail();

		tvd.center = myrsv.getCh_name();

		{
			String nextdate = CommonUtils.getNextDate(myrsv);
			GregorianCalendar ca = CommonUtils.getCalendar(nextdate);

			ca.add(Calendar.HOUR_OF_DAY, Integer.valueOf(myrsv.getAhh())-ca.get(Calendar.HOUR_OF_DAY));
			ca.add(Calendar.MINUTE, Integer.valueOf(myrsv.getAmm())-ca.get(Calendar.MINUTE));
			tvd.startDateTime = CommonUtils.getDateTime(ca);
			tvd.start = CommonUtils.getTime(ca);

			ca.add(Calendar.MINUTE, Integer.valueOf(myrsv.getRec_min()));
			tvd.endDateTime = CommonUtils.getDateTime(ca);
			tvd.end = CommonUtils.getTime(ca);
		}

		tvd.progid = myrsv.getContentId();

		tvd.genre = ProgGenre.get(myrsv.getRec_genre());
		tvd.subgenre = ProgSubgenre.get(tvd.genre,myrsv.getRec_subgenre());

		// 特殊
		tvd.accurateDate = null;
		tvd.dontoverlapdown = true;

		return tvd;
	}

	/***************************************
	 * 番組情報部の設定
	 **************************************/

	private TimeVal setTitleItems(HDDRecorder myrec, ProgDetailList tvd, LikeReserveList lrl, boolean atrsvlst) {

		jPane_title.setTitleItems(tvd, lrl, env.getUseAutocomplete());
		jPane_title.setChItem(myrec, tvd);

		TimeVal tVal = getTimeValue(tvd);
		jPane_title.setTimeValue(tVal);
		jPane_title.setDateItems(tvd, tVal);

		return tVal;
	}

	/***************************************
	 * 録画設定部の設定
	 **************************************/

	private void setRecSettingItems(HDDRecorderList reclst, HDDRecorder myrec, ProgDetailList tvd) {

		jPane_recsetting.setLabels(myrec);					// 項目ラベル
		jPane_recsetting.setFixedItems(reclst);				// 固定アイテム
		jPane_recsetting.setFlexItems(myrec,tvd.center);	// 可変アイテム
	}

	/***************************************
	 * 類似予約部の設定
	 **************************************/

	private void setLikeRsvItems(LikeReserveList lrl) {
		jPane_likersv.setListItems(lrl);
	}


	/*******************************************************************************
	 * ほげほげ
	 ******************************************************************************/

	/**
	 * 延長警告などを加味した録画開始・終了日時を算出する
	 */
	private TimeVal getTimeValue(ProgDetailList tvd) {

		TimeVal tVal = new TimeVal();

		GregorianCalendar ca = CommonUtils.getCalendar(tvd.startDateTime);
		GregorianCalendar cz = CommonUtils.getCalendar(tvd.endDateTime);

		if ( tvd.accurateDate != null ) {
			// のりしろ処理（開始時刻）
			if ( env.getOverlapUp() ) {
				// 開始１分前倒し
				ca.add(Calendar.MINUTE, -1);

				tVal.margined = true;
			}

			// のりしろ処理（終了時刻）
			if ( env.getOverlapDown() ) {
				// 終了１分延長
				cz.add(Calendar.MINUTE, +1);
			}
			else if (
					env.getOverlapDown2() &&
					! tvd.dontoverlapdown &&			// NHKは縮めない
					! (env.getNoOverlapDown2Sp() && tvd.option.contains(ProgOption.SPECIAL))	// OVAとかは縮めない
					) {
				// 終了１分前倒し
				cz.add(Calendar.MINUTE, -1);

				tVal.clipped = true;
			}

			// 延長警告処理
			int spoexlen = Integer.valueOf(env.getSpoexLength());
			if ( tvd.extension == true && spoexlen > 0 ) {
				// 指定時間分延長
				cz.add(Calendar.MINUTE, +spoexlen);

				tVal.spoex = true;
				tVal.spoexlen = spoexlen;
			}
		}

		tVal.date = CommonUtils.getDate(ca);

		tVal.ahh = ca.get(Calendar.HOUR_OF_DAY);
		tVal.amm = ca.get(Calendar.MINUTE);
		tVal.zhh = cz.get(Calendar.HOUR_OF_DAY);
		tVal.zmm = cz.get(Calendar.MINUTE);

		tVal.startDateTime = CommonUtils.getDateTime(ca);
		tVal.endDateTime = CommonUtils.getDateTime(cz);

		return tVal;
	}


	/*******************************************************************************
	 * ネットから番組IDを取得する
	 ******************************************************************************/

	/**
	 * ダイアログオープン時の番組ID取得処理
	 */
	private String getEventIdOnOpen(ProgDetailList tvd) {

		if (debug) System.err.println(DBGID+"ダイアログ表示時の自動番組ID取得 id=\""+tvd.progid+"\"");

		if ( ContentIdEDCB.isValid(tvd.progid) ) {
			if (debug) System.err.println(DBGID+"番組ID取得済み");
			return tvd.progid;
		}
		if ( ! env.getAutoEventIdComplete() ) {
			if (debug) System.err.println(DBGID+"番組ID自動取得OFF");
			return tvd.progid;	// 有効な値かもしれないし、nullかもしれない
		}

		// キャッシュに情報を持っていないか探す
		String content_id = getContentId(tvd, false);
		if ( content_id != null ) {
			if (debug) System.err.println(DBGID+"番組IDキャッシュ有効");
			return content_id;
		}

		// ネットに探しに行く
		return doGetEventId();
	}


	/*******************************************************************************
	 * 自動エンコーダ選択と裏番組抽出
	 ******************************************************************************/

	/**
	 * これはグラフで描画するようになるまでの仮置き
	 * @param urabanlist
	 */
	private void showUrabanList(ReserveList myrsv, ArrayList<ReserveList> urabanlist) {

		jPane_overlap.putOverlap(myrsv, urabanlist);

		if ( ! debug ) {
			return;
		}

		if ( urabanlist == null ) {
			return;
		}
		String MID = MSGID+"[裏番組チェック] ";
		System.out.println(MID+"----------");
		if ( urabanlist.size() > 0 ) {
			for ( ReserveList ura : urabanlist ) {
				System.out.println(String.format("%s裏番組あり: %s:%s-%s:%s, %-10s, %-12s, %s", MID, ura.getAhh(), ura.getAmm(), ura.getZhh(), ura.getZmm(), ura.getTuner(), ura.getCh_name(), ura.getTitle()));
			}
		}
		else {
			// 裏番組がない場合に分かりにくかったので追加
			System.out.println(MID+"裏番組はありません");
		}
	}


	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	/**
	 * ダイアログを開いたときは
	 */
	private final WindowListener wl_opened = new WindowAdapter() {
		@Override
		public void windowClosing(WindowEvent e) {

			if (debug) System.out.println(DBGID+"wl_opened/windowClosing "+((vals!=null)?(vals.toString()):("")));

			resetWhenWindowClosed();

			((AbsReserveDialog) e.getSource()).dispose();
		}

		@Override
		public void windowOpened(WindowEvent e) {
			if (debug) System.out.println(DBGID+"wl_opened/windowOpened");
			//　開いたときは、タイトル入力エリアにフォーカスを移します
//			jComboBox_title.requestFocusInWindow();
		}
	};

	private void resetWhenWindowClosed() {

		if (debug) System.out.println(DBGID+"resetWhenWindowClosed "+((vals!=null)?(vals.toString()):("")));

		if (vals == null) return;

		// リセット
		vals = null;
	}


	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	private JPanel getJContentPane_rsv() {
		if (jContentPane_rsv == null) {
			jContentPane_rsv = new JPanel();

			jContentPane_rsv.setLayout(new BorderLayout());

			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			jContentPane_rsv.add(panel,BorderLayout.CENTER);
			jContentPane_rsv.add(getJPane_overlap(),BorderLayout.EAST);

			panel.add(getJPane_title(),BorderLayout.NORTH);
			panel.add(getJPane_recsetting(),BorderLayout.CENTER);
			panel.add(getJPane_likersv(),BorderLayout.SOUTH);
		}
		return jContentPane_rsv;
	}

	/**
	 * 番組情報のエリア
	 */
	private JPanel getJPane_title() {
		if ( jPane_title == null ) {
			jPane_title = new TitleEditorPanel();

			jPane_title.setRecordExecuter(this);	// 予約実行ボタン押下時のコールバックの設定
		}
		return jPane_title;
	}

	/**
	 * 録画設定のエリア
	 */
	private JPanel getJPane_recsetting() {
		if ( jPane_recsetting == null ) {
			jPane_recsetting = new RecSettingEditorPanel();

			jPane_recsetting.setRecSettingSelector(this);	// アイテム選択時のコールバックの設定
			jPane_recsetting.setRecorders( recorders );
			jPane_recsetting.setStatusWindow( StWin );
			jPane_recsetting.setStatusTextArea( MWin );
		}
		return jPane_recsetting;
	}

	/**
	 * 類似予約のエリア
	 */
	private JScrollPane getJPane_likersv() {
		if ( jPane_likersv == null ) {
			jPane_likersv = new LikeReserveEditorPanel();

			jPane_likersv.setLikeReserveSelector(this);	// 予約実行ボタン押下時のコールバックの設定
		}
		return jPane_likersv;
	}

	/**
	 * 重複予約のエリア
	 */
	private JScrollPane getJPane_overlap() {
		if ( jPane_overlap == null ) {
			jPane_overlap = new OverlapReserveViewPanel();
			jPane_overlap.setBorder(new LineBorder(Color.BLACK));
		}
		return jPane_overlap;
	}

	/*******************************************************************************
	 * ハンドラ―メソッドの実装
	 ******************************************************************************/

	/**
	 * ツールバーでレコーダの選択イベントが発生
	 */
	@Override
	public void valueChanged(HDDRecorderSelectionEvent e) {
		if (debug) System.out.println(DBGID+"recorder selection rised");

		// 選択中のレコーダ情報を保存する
		src_recsel = (HDDRecorderSelectable) e.getSource();
	}

	/**
	 * ツールバーの操作によって選択されたレコーダのIDを取得する
	 */
	private String getSelectedMySelf() {
		return ( src_recsel!=null ? src_recsel.getSelectedMySelf() : null );
	}

	/**
	 * ツールバーの操作によって選択されたレコーダのプラグインインスタンスリストを取得する
	 */
	private HDDRecorderList getSelectedRecorderList() {
		return ( src_recsel!=null ? src_recsel.getSelectedList() : null );
	}

	private HDDRecorderSelectable src_recsel;


	/**
	 * ツールバーでレコーダ情報の変更イベントが発生
	 */
	@Override
	public void stateChanged(HDDRecorderChangeEvent e) {
		// 処理はいらんな…
	}


	/*******************************************************************************
	 * コールバックメソッドの実装（番組情報）
	 ******************************************************************************/

	/***************************************
	 * 予約ボタンが押された時の処理
	 **************************************/

	/**
	 * 新規登録を行う
	 */
	@Override
	public void doRecord() {

		if (debug) System.out.println(DBGID+"doRecord "+vals.toString());

		// 新規処理
		final ReserveList newRsv = new ReserveList();

		jPane_title.getSelectedValues(newRsv);			// タイトル
		jPane_recsetting.getSelectedValues(newRsv);		// 録画設定

		if ( newRsv.getRec_audio() == HDDRecorder.ITEM_REC_TYPE_EPG &&
				(newRsv.getContentId() == null || newRsv.getContentId().length() == 0) ) {
			ringBeep();
			JOptionPane.showConfirmDialog(this, "EPG予約では番組IDが必要になります。", "警告", JOptionPane.CLOSED_OPTION);
			return;
		}

		newRsv.setId(null);								// PostRdEntry()中で取得するのでここはダミー
		newRsv.setUpdateOnlyExec(false);				// 新規ONLYなのでfalse固定

		final HDDRecorder recorder = vals.selected_recorder;

		// 予約実行
		StWin.clear();
		new SwingBackgroundWorker(false) {

			@Override
			protected Object doWorks() throws Exception {

				StWin.appendMessage(MSGID+"予約を登録します："+newRsv.getTitle());

				if ( recorder.PostRdEntry(newRsv) ) {

					MWin.appendMessage(MSGID+"正常に登録できました："+newRsv.getTitle()+"("+newRsv.getCh_name()+")");
					doneReserve = true;

					// カレンダーに登録する
					if ( recorder.getUseCalendar() && newRsv.getExec() ) {

						for ( HDDRecorder calendar : recorders.findInstance(RecType.CALENDAR) ) {

							StWin.appendMessage(MSGID+"カレンダーに予約情報を登録します");

							if ( ! calendar.PostRdEntry(newRsv)) {
								MWin.appendError(ERRID+"[カレンダー] "+calendar.getErrmsg());
								ringBeep();
							}
						}
					}
				}
				else {
					MWin.appendError(ERRID+"登録に失敗しました："+newRsv.getTitle()+"("+newRsv.getCh_name()+")");
				}

				if ( ! recorder.getErrmsg().equals("")) {
					MWin.appendMessage(MSGID+"[追加情報] "+recorder.getErrmsg());
					ringBeep();
				}

				return null;
			}

			@Override
			protected void doFinally() {
				//CommonUtils.milSleep(0);
				StWin.setVisible(false);
			}
		}.execute();

		CommonSwingUtils.setLocationCenter(parent, (Component)StWin);
		StWin.setVisible(true);

		resetWhenWindowClosed();
		dispose();
	}

	/**
	 * 更新を行う
	 */
	@Override
	public void doUpdate() {

		if (debug) System.out.println(DBGID+"doUpdate "+vals.toString());

		LikeReserveItem likersv = vals.hide_likersvlist.getSelected();
		if ( likersv == null ) {
			// ==0なら更新対象の予約情報がないっつーことで処理できない
			return;
		}

		// 更新処理
		final ReserveList oldRsv = likersv.getRsv();
		final ReserveList newRsv = oldRsv.clone();
		final HDDRecorder recorder = likersv.getRec();

		jPane_title.getSelectedValues(newRsv);				// タイトル
		jPane_recsetting.getSelectedValues(newRsv);			// 録画設定

		// XXX old.valueはload()で取得した値の場合定数とは異なる文字列になって、 == で比較できなくなるな多分
		if ( (newRsv.getRec_audio() == HDDRecorder.ITEM_REC_TYPE_EPG || newRsv.getRec_audio() == HDDRecorder.ITEM_REC_TYPE_PROG) &&
				! newRsv.getRec_audio().equals(oldRsv.getRec_audio()) ) {
			ringBeep();
			JOptionPane.showConfirmDialog(this, String.format("%s予約を%s予約には変更できません。",oldRsv.getRec_audio(),newRsv.getRec_audio()), "警告", JOptionPane.CLOSED_OPTION);
			return;
		}

		newRsv.setId(oldRsv.getId());						// 更新では引き継ぐ
		newRsv.setUpdateOnlyExec(vals.isUpdateOnlyExec);	// 実行ON・OFFのみかもしんない

		// 更新実行
		StWin.clear();
		new SwingBackgroundWorker(false) {

			@Override
			protected Object doWorks() throws Exception {

				StWin.appendMessage(MSGID+"予約を更新します："+newRsv.getTitle());

				if ( recorder.UpdateRdEntry(oldRsv, newRsv) ) {

					// 成功したよ
					MWin.appendMessage(MSGID+"正常に更新できました："+oldRsv.getTitle()+"("+oldRsv.getCh_name()+")");
					doneReserve = true;

					// カレンダーを更新する
					if ( recorder.getUseCalendar() ) {
						for ( HDDRecorder calendar : recorders.findInstance(RecType.CALENDAR) ) {

							StWin.appendMessage(MSGID+"カレンダーの予約情報を更新します");

							if ( ! calendar.UpdateRdEntry(oldRsv, (newRsv.getExec())?(newRsv):(null))) {
								MWin.appendError(ERRID+"[カレンダー] "+calendar.getErrmsg());
								ringBeep();
							}
						}
					}
				}
				else {
					MWin.appendError(ERRID+"更新に失敗しました："+oldRsv.getTitle()+"("+oldRsv.getCh_name()+")");
				}

				if ( ! recorder.getErrmsg().equals("")) {
					MWin.appendMessage(MSGID+"[追加情報] "+recorder.getErrmsg());
					ringBeep();
				}
				return null;
			}

			@Override
			protected void doFinally() {
				StWin.setVisible(false);
			}
		}.execute();

		StWin.setVisible(true);

		resetWhenWindowClosed();
		dispose();
	}

	/**
	 * ダイアログを閉じる
	 */
	@Override
	public void doCancel() {
		resetWhenWindowClosed();
		dispose();
	}

	/***************************************
	 * 番組ID取得ボタンが押された時の処理
	 **************************************/

	/**
	 * 番組IDを取得する
	 */
	public String doGetEventId() {

		final ProgDetailList tvd = vals.hide_tvd;

		StWin.clear();

		new SwingBackgroundWorker(false) {

			@Override
			protected Object doWorks() throws Exception {
				TatCount tc = new TatCount();
				StWin.appendMessage(MSGID+"番組IDを取得します");

				String content_id = getContentId(tvd, true);
				if ( content_id == null ) {
					StWin.appendError(ERRID+String.format("番組IDの取得に失敗しました。所要時間： %.2f秒",tc.end()));
				}
				else {
					StWin.appendMessage(MSGID+String.format("番組IDを取得しました。所要時間： %.2f秒",tc.end()));
				}

				return null;
			}

			@Override
			protected void doFinally() {
				StWin.setVisible(false);
			}
		}.execute();

		CommonSwingUtils.setLocationCenter(AbsReserveDialog.this.isVisible() ? AbsReserveDialog.this : parent, (Component) StWin);
		StWin.setVisible(true);

		return tvd.progid;
	}

	private String getContentId(ProgDetailList tvd, boolean force) {

		String content_id = null;

		if ( ContentIdDIMORA.isValid(tvd.progid) ) {
			ContentIdDIMORA.decodeContentId(tvd.progid);
			String chid = ContentIdDIMORA.getChId();
			content_id = getContentIdById(chid, tvd.startDateTime, force);
		}
		else {
			content_id = getContentIdByName(tvd.center, tvd.startDateTime, force);
		}
		if ( content_id != null ) {
			tvd.progid = content_id;
			tvd.setContentIdStr();
		}

		return content_id;
	}

	/**
	 * 番組表に存在する放送局IDで
	 */
	private String getContentIdById(String chid, String startdatetime, boolean force) {

		Integer evid = geteventid.getEvId(chid, startdatetime, force);

		if ( force && evid == null ) {
			MWin.appendError(ERRID+"番組ID取得でエラーが発生しました： "+chid+", "+startdatetime);
			ringBeep();
			return null;	// 一発死に
		}
		else if ( ! force && evid == null ) {
			System.out.println(MSGID+"キャッシュにヒットしませんでした： "+chid+", "+startdatetime);
			return null;
		}
		else if ( evid == -1 ) {
			MWin.appendError(ERRID+"番組IDが取得できませんでした： "+chid+", "+startdatetime);
			return null;
		}

		ContentIdDIMORA.decodeChId(chid);
		String content_id = ContentIdDIMORA.getContentId(evid);

		MWin.appendMessage(MSGID+"番組IDを取得しました(byId)： "+content_id);

		return content_id;
	}

	/**
	 * レコーダに登録された放送局IDで
	 */
	private String getContentIdByName(String chname, String startdatetime, boolean force) {

		String chid = null;
		String chidEDCB = null;
		String chidREGZA = null;

		Integer evid = null;

		// 登録済みのレコーダプラグインを全部チェックしてみる
		for ( HDDRecorder rec : recorders ) {
			if ( rec.isBackgroundOnly() ) {
				continue;
			}

			chidEDCB = chidREGZA = chid = null;
			Integer tmpEvid = null;

			String chcode = rec.getChCode().getCH_WEB2CODE(chname);
			if ( chcode == null ) {
				System.err.println(ERRID+"「Web番組表の放送局名」を「放送局コード」に変換できません： "+rec.getRecorderId()+" "+chname);
				continue;
			}

			chidEDCB = chid = ContentIdEDCB.getChId(chcode);
			if ( chid == null ) {
				chidREGZA = chid = ContentIdREGZA.getChId(chcode);
				if ( chid == null ) {
					System.err.println(ERRID+"番組IDの取得に未対応のレコーダです： "+rec.getRecorderId());
					continue;
				}
			}

			if (debug) System.out.println(MSGID+"番組IDを取得します： "+rec.getRecorderId());

			tmpEvid = geteventid.getEvId(chid, startdatetime, force);

			if (evid == null) evid = tmpEvid;

			if ( force && tmpEvid == null ) {
				MWin.appendError(ERRID+"番組ID取得でエラーが発生しました： "+chid+", "+startdatetime);
				ringBeep();
				return null;	// 一発死に
			}
			else if ( ! force && tmpEvid == null ) {
				System.out.println(MSGID+"キャッシュにヒットしませんでした： "+chid+", "+startdatetime);
				return null;
			}
			else if ( tmpEvid == -1 ) {
				System.err.println(ERRID+"番組IDが取得できませんでした： "+chid+", "+startdatetime);
				continue;
			}

			break;
		}

		if ( evid == null ) {
			MWin.appendError(ERRID+"【致命的エラー】放送局IDを持つレコーダプラグインが存在しません");
			ringBeep();
			return null;
		}
		else if ( evid == -1 ) {
			MWin.appendError(ERRID+"【警告】番組IDの取得に失敗しました。開始時刻の移動や、まだ番組表サイトに情報が用意されていない場合などが考えられます。");
			ringBeep();
			return null;
		}

		String content_id;
		if ( chidREGZA != null ) {
			content_id = ContentIdREGZA.getContentId(chidREGZA, evid);
		}
		else {
			content_id = ContentIdEDCB.getContentId(chidEDCB, evid);
		}

		MWin.appendMessage(MSGID+"番組IDを取得しました(byName)： "+content_id);

		return content_id;
	}

	/*******************************************************************************
	 * コールバックメソッドの実装（録画設定）
	 ******************************************************************************/

	/***************************************
	 * 選択系
	 **************************************/

	/**
	 * 開始時刻、終了時刻が変わったので裏番組を更新する
	 */
	public boolean notifyTimeChange(){
		ReserveList myrsv = jPane_title.getSelectedValues();

		HDDRecorder myrec = vals.selected_recorder;
		if ( myrec == null ) {
			return false;
		}

		// 選択
		String enc = myrec.getEmptyEncorder(myrsv.getCh_name(), myrsv.getStartDateTime(), myrsv.getEndDateTime(), null, null);
		myrsv.setTuner(enc);
		jPane_recsetting.setSelectedEncoderValue(enc);

		showUrabanList(myrsv, myrec.getUrabanList());

		return true;
	}

	/**
	 * レコーダが選択されたのでテキトーな録画設定を選ぶ
	 */
	public boolean doSelectRecorder(String myself) {

		System.out.println(DBGID+"選択されたレコーダ: "+myself);

		HDDRecorderList myrecs = recorders.findInstance(myself);
		if ( myrecs.size() == 0 ) {
			return false;
		}
		HDDRecorder myrec = myrecs.get(0);

		// 今回選択されたレコーダを保存する
		vals.selected_recorder = myrec;

		// 番組情報のアイテム設定
		ProgDetailList tvd = vals.hide_tvd;
		TimeVal tVal = getTimeValue(tvd);
		jPane_title.setTimeValue(tVal);
		jPane_title.setDateItems(tvd, tVal);

		// 録画設定のアイテム設定
		setRecSettingItems(recorders, myrec, tvd);

		// 選択
		String enc = myrec.getEmptyEncorder(tvd.center, tVal.startDateTime, tVal.endDateTime, null, null);
		ReserveList myrsv = getReserveList(myrec, tvd, tVal, enc);
		jPane_recsetting.setSelectedValues(tvd, myrsv);

		showUrabanList(myrsv, myrec.getUrabanList());

		return true;
	}

	/**
	 * エンコーダが選択されたのでテキトーな録画設定を選ぶ(RD用)
	 */
	public boolean doSelectEncoder(String encoder) {

		if ( encoder == null ) {
			return false;
		}

		HDDRecorder myrec = vals.hide_recorder;
		if ( myrec == null ) {
			return false;
		}

		// チューナーにあった画質を探そう
		String vrate = myrec.getPreferredVrate_VARDIA(encoder);
		if ( vrate == null ) {
			// 対象外だわ
			return true;
		}

		jPane_recsetting.setSelectedVrateValue(vrate);
		return true;
	}

	/**
	 * 画質が選択されたのでテキトーな録画設定を選ぶ（）
	 */
	public boolean doSelectVrate(String vrate) {

		if ( vrate == null ) {
			return false;
		}

		HDDRecorder myrec = vals.hide_recorder;
		if ( myrec == null ) {
			return false;
		}

		// 画質にあったチューナーのリストを探してコンボボックスを並べ替える
		ArrayList<TextValueSet> tuners = myrec.getPreferredTuners_VARDIA(vrate);
		if ( tuners == null ) {
			// 対象外だわ
			return true;
		}
		jPane_recsetting.sortEncoderItems(tuners);

		// 画質にあったチューナーを探そう
		ReserveList myrsv = jPane_title.getSelectedValues();
		String tuner = myrec.getEmptyEncorder(myrsv.getCh_name(), myrsv.getStartDateTime(), myrsv.getEndDateTime(), null, vrate);

		jPane_recsetting.setSelectedEncoderValue(tuner);
		return true;
	}

	/***************************************
	 * ボタン系
	 **************************************/

	/**
	 *
	 */
	public boolean doSetAVSettings() {

		ReserveList r = jPane_title.getSelectedValues();	// 放送局名の取得
		String webChName = r.getCh_name();
		if ( webChName == null ) {
			System.out.println(ERRID+"放送局名が不正.");
			return false;
		}

		jPane_recsetting.getSelectedValues(r);				// ジャンルの取得
		ProgGenre genre = ProgGenre.get(r.getRec_genre());
		if ( genre == null ) {
			return false;
		}

		HDDRecorder myrec = vals.selected_recorder;			// レコーダ情報の取得
		if ( myrec == null ) {
			System.out.println(ERRID+"レコーダが未選択.");
			return false;
		}
		String recid = myrec.getRecorderId();

		// ジャンル別ＡＶ設定の取得
		AVs myavs = findAVs(genre.toString(), webChName, recid);
		if ( myavs == null ) {
			return false;
		}

		// ジャンル別AV設定から追加で選択する
		jPane_recsetting.setSelectedValues(myavs);

		return true;
	}

	/**
	 *
	 */
	public boolean doSaveAVSettings(boolean savedefault) {

		ReserveList r = jPane_title.getSelectedValues();	// 放送局名の取得
		String webChName = r.getCh_name();
		if ( webChName == null ) {
			System.out.println(ERRID+"放送局名が不正.");
			return false;
		}

		HDDRecorder myrec = vals.selected_recorder;			// レコーダ情報の取得
		if ( myrec == null ) {
			System.out.println(ERRID+"レコーダが未選択.");
			return false;
		}
		String recid = myrec.getRecorderId();

		AVs c = jPane_recsetting.getSelectedSetting();		// AV設定の取得
		c.setRecorderId(recid);

		String key_item = c.getGenre();
		AVSetting xavslist = avslist;
		if ( env.getEnableCHAVsetting() ) {
			key_item = webChName;
			xavslist = chavslist;	// CHをキーに
		}

		if ( savedefault ) {
			key_item = null;		// デフォルトだよう
		}

		c.setGenre(key_item);		// 入れなおしだよう

		xavslist.add(recid, key_item, c);
		xavslist.save();

		MWin.appendMessage(MSGID+"画質・音質等の設定を保存しました："+recid+" & "+((key_item!=null)?(key_item):("デフォルト")));

		return true;
	}


	/*******************************************************************************
	 * コールバックメソッドの実装（類似予約）
	 ******************************************************************************/

	/***************************************
	 * 類似予約が選択された時の処理
	 **************************************/

	/**
	 * 類似予約が選択されたので処理をしてほしい
	 */
	@Override
	public void doSelectLikeReserve(int row) {
		if ( row == LikeReserveEditorPanel.LIKERSVTABLE_NONE ) {
			doSelectLikeReserveByProg();
		}
		else {
			doSelectLikeReserveByReserve(row);
		}
	}

	// 番組情報で置き換え
	private boolean doSelectLikeReserveByProg() {

		HDDRecorder myrec = vals.hide_recorder;
		ProgDetailList tvd = vals.hide_tvd;
		LikeReserveList likersvlist = vals.hide_likersvlist;
		AVs myavs = vals.hide_avs;
		TimeVal tVal = getTimeValue(tvd);

		// 初期化
		setTitleItems(myrec, tvd, likersvlist, false);
		setRecSettingItems(recorders, myrec, tvd);
		jPane_title.setDateItems(tvd, tVal);

		// 選択
		{
			// 番組情報の選択
			jPane_title.setSelectedValues(tvd);

			jPane_title.setTimeValue(tVal);

			// RDだと画質でエンコーダの種類が絞られちまうンですよ
			String vrate = myavs != null ? myavs.getVideorate() : null;

			// 録画設定の選択
			setSelectedRecorder(myrec);
			jPane_recsetting.setFlexItems(myrec, tvd.center);

			String enc = myrec.getEmptyEncorder(tvd.center, tVal.startDateTime, tVal.endDateTime, null, vrate);
			ReserveList myrsv = getReserveList(myrec, tvd, tVal, enc);
			jPane_recsetting.setSelectedValues(tvd, myrsv);
			showUrabanList(myrsv, myrec.getUrabanList());

			if ( myavs != null ) {
				// ジャンル別AV設定から追加で選択する
				jPane_recsetting.setSelectedValues(myavs);
			}
		}

		// 予約ボタンの状態設定
		jPane_title.setEnabledRecordButton(true);
		jPane_title.setEnabledUpdateButton(false);

		return true;
	}

	// 類似予約情報で置き換え
	private boolean doSelectLikeReserveByReserve(int row) {

		LikeReserveList likersvlist = vals.hide_likersvlist;
		LikeReserveItem likersv = likersvlist.setSelectedIndex(row);
		ReserveList myrsv = likersv.getRsv();

		HDDRecorderList myrecs = new HDDRecorderList();
		HDDRecorder myrec = likersv.getRec();
		myrecs.add(myrec);

		ProgDetailList tvd = vals.hide_tvd;
		TimeVal tVal = getTimeValue(tvd);

		// 初期化
		setTitleItems(myrec, tvd, likersvlist, true);
		setRecSettingItems(myrecs, myrec, tvd);
		jPane_title.setDateItems(tvd, tVal);
		//jPane_recsetting.setFlexItems(myrec, tvd.center);

		//　選択
		{
			// 番組情報の選択
			jPane_title.setSelectedValues(myrsv);
			jPane_title.setTimeValue(tVal);

			// 録画設定の選択
//			myrec.getEmptyEncorder(tvd.center, myrsv.getStartDateTime(), myrsv.getEndDateTime(), myrsv, null);	// 裏番組チェックのみ
			myrec.getEmptyEncorder(tvd.center, tVal.startDateTime, tVal.endDateTime, myrsv, null);	// 裏番組チェックのみ
			setSelectedRecorder(myrec);

			ReserveList modrsv = getReserveList(myrec, tvd, tVal, myrsv.getTuner());
			showUrabanList(modrsv, myrec.getUrabanList());

			jPane_recsetting.setSelectedValues(myrsv);
		}

		// 予約ボタンの状態設定
		jPane_title.setEnabledRecordButton(true);
		jPane_title.setEnabledUpdateButton(true);

		return true;
	}

	/*
	 * タイトルを返す
	 */
	public String doGetSelectedTitle(){
		return jPane_title.getSelectedTitle();
	}
	/*******************************************************************************
	 * 直し残し
	 ******************************************************************************/

	/**
	 * レコーダの選択をしなおしたらコンボボックスアイテムの入れ替えも実行すること
	 * @see RecSettingEditorPanel#setFlexItems(HDDRecorder, String)
	 */
	private HDDRecorder setSelectedRecorder(HDDRecorder myrec) {
		if ( jPane_recsetting.setSelectedRecorderValue(myrec.Myself()) != null ) {
			vals.selected_recorder = myrec;
			return myrec;
		}
		return null;
	}

	/**
	 *  <P>指定したレコーダによってフォルダを変える
	 *  <P>うーん、folderを他の用途に転用してるけど問題おきないかな？
	 */
	private void setSelectedFolder() {
/*
		// タイトルに連動
		if ( env.getAutoFolderSelect() ) {
			String titlePop = TraceProgram.replacePop((String) jComboBox_title.getSelectedItem());
			for (int i=0; i<jCBXPanel_folder.getItemCount(); i++) {
				String folderPop = TraceProgram.replacePop(((String) jCBXPanel_folder.getItemAt(i)).replaceFirst("^\\[(HDD|USB)\\] ",""));
				if (folderPop.equals(titlePop)) {
					jCBXPanel_folder.setSelectedIndex(i);
					return;
				}
			}
		}

		// デバイス名に連動
		int defaultFolderIdx = -1;
		int defaultHDDFolderIdx = -1;
		int defaultDVDFolderIdx = -1;
		for (int i=0; i<jCBXPanel_folder.getItemCount(); i++ ) {
			String folderName = (String) jCBXPanel_folder.getItemAt(i);
			if (folderName.indexOf("指定なし") != -1) {
				if (defaultFolderIdx == -1) {
					defaultFolderIdx = i;
				}
				if (folderName.startsWith("[HDD] ")) {
					defaultHDDFolderIdx = i;
				}
				else if (folderName.startsWith("[USB] ")) {
					defaultDVDFolderIdx = i;
				}
			}
		}
		if (jCBXPanel_device.getItemCount() > 0) {
			if (((String) jCBXPanel_device.getSelectedItem()).equals("HDD")) {
				if (defaultHDDFolderIdx != -1) {
					jCBXPanel_folder.setSelectedIndex(defaultHDDFolderIdx);
					return;
				}
			}
			else {
				if (defaultDVDFolderIdx != -1) {
					jCBXPanel_folder.setSelectedIndex(defaultDVDFolderIdx);
					return;
				}
			}
		}
		if (defaultFolderIdx != -1) {
			jCBXPanel_folder.setSelectedIndex(defaultFolderIdx);
		}
*/
	}

}
