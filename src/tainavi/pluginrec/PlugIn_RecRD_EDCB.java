package tainavi.pluginrec;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import taiSync.ReserveInfo;
import tainavi.AutoReserveInfo;
import tainavi.AutoReserveInfoList;
import tainavi.ChannelCode;
import tainavi.CommonUtils;
import tainavi.ContentIdDIMORA;
import tainavi.ContentIdEDCB;
import tainavi.ContentIdREGZA;
import tainavi.Env;
import tainavi.HDDRecorder;
import tainavi.HDDRecorderUtils;
import tainavi.RecordedInfo;
import tainavi.ReserveList;
import tainavi.TextValueSet;
import tainavi.TraceProgram;
import tainavi.TVProgram.ProgGenre;
import tainavi.TVProgram.ProgSubgenre;


/**
 * 
 */
public class PlugIn_RecRD_EDCB extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	@Override
	public PlugIn_RecRD_EDCB clone() {
		return (PlugIn_RecRD_EDCB) super.clone();
	}
	
	private static final String thisEncoding = "MS932";
	private static final String xmlEncoding = "utf8";
	
	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/

	@Override
	public String getRecorderId() { return "EpgDataCap_Bon"; }
	@Override
	public RecType getType() { return RecType.RECORDER; }
	
	// 自動予約を編集できる
	@Override
	public boolean isEditAutoReserveSupported() { return true; }
	// 録画結果一覧を取得できる
	@Override
	public boolean isRecordedListSupported() { return true; }
	// 自動エンコーダ選択は禁止
	@Override
	public boolean isAutoEncSelectEnabled() { return false; }
	// 繰り返し予約はできない
	@Override
	public boolean isRepeatReserveSupported() { return false; }
	// 番組追従が可能
	@Override
	public boolean isPursuesEditable() { return true; }
	// タイトル自動補完はできない
	@Override
	public boolean isAutocompleteSupported() { return false; }
	// chvalueを使っていいよ
	@Override
	public boolean isChValueAvailable() { return true; }
	// CHコードは入力しなくていい
	@Override
	public boolean isChCodeNeeded() { return false; }

	/*******************************************************************************
	 * 予約ダイアログなどのテキストのオーバーライド
	 ******************************************************************************/
	
	@Override
	public String getLabel_Audiorate() { return "予約方法"; }
	@Override
	public String getLabel_Folder() { return "録画ﾓｰﾄﾞ"; }
	@Override
	public String getLabel_Device() { return "指定ｻｰﾋﾞｽ対象"; }
	@Override
	public String getLabel_XChapter() { return "復帰後再起動"; }		// [mv->x]
	@Override
	public String getLabel_MsChapter() { return "録画開始(秒前)"; }	// [x->ms]
	@Override
	public String getLabel_MvChapter() { return "録画終了(秒後)"; }	// [ms->mv]
	@Override
	public String getLabel_DVDCompat() { return "連続録画動作"; }
	@Override
	public String getLabel_LVoice() { return "ぴったり録画"; }
	@Override
	public String getLabel_Aspect() { return "録画後動作"; }
	@Override
	public String getLabel_Videorate() { return "プリセット"; }
	@Override
	public String getLabel_Autodel() { return "録画優先度"; }
	@Override
	public String getLabel_BVperf() { return "ﾜﾝｾｸﾞを別ﾌｧｲﾙに出力"; }
	
	@Override
	public String getChDatHelp() { return
			"「レコーダの放送局名」は、予約一覧取得が正常に完了していれば設定候補がコンボボックスで選択できるようになります。"+
			"";
	}
	
	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	private static final String ITEM_REC_MODE_DISABLE	= "無効";
	
	private static final String ITEM_JUST_ENABLE		= "する";

	private static final String ITEM_PRESETID_REG		= "予約時";

	private static final String ITEM_MARGINE_DEFAULT	= "デフォルト";
	
	private static final String ITEM_SERVMOCE_DEFAULT	= "デフォルト";
	private static final String ITEM_SERVMOCE_SUBT		= "+字幕";
	private static final String ITEM_SERVMOCE_CARO		= "+ｶﾙｰｾﾙ";
	private static final String ITEM_SERVMOCE_SUBTCARO	= "+字幕&ｶﾙｰｾﾙ";
	private static final String ITEM_SERVMOCE_NONE		= "なし";
	
	private static final String ITEM_YES = "する";
	private static final String ITEM_NO = "しない";

	private static final String VALUE_REC_MODE_DISABLE  = "5";

	private static final String VALUE_YES  = "1";
	private static final String VALUE_NO  = "0";
	
	private static final String VALUE_TRACE_DISABLE		= "0";
	private static final String VALUE_TRACE_ENABLE		= "1";

	private static final String VALUE_PRESETID_REG		= "65535";
	
	private static final String VALUE_MARGINE_DEFAULT	= "FF";
	
	private static final String VALUE_SERVMOCE_DEFAULT	= "FF";
	private static final String VALUE_SERVMOCE_SUBT		= "0-";
	private static final String VALUE_SERVMOCE_CARO		= "-0";
	private static final String VALUE_SERVMOCE_SUBTCARO	= "00";
	private static final String VALUE_SERVMOCE_NONE		= "--";
	
	private static final String RETVAL_KEY_RECFOLDERCOUNT		= "recFolderCount";
	private static final String RETVAL_KEY_R_RECFOLDER			= "RrecFolder";
	private static final String RETVAL_KEY_R_WRITEPLUGIN		= "RwritePlugIn";
	private static final String RETVAL_KEY_R_RECNAMEPLUGIN		= "RrecNamePlugIn";
	
	private static final String RETVAL_KEY_PARTIALFOLDERCOUNT	= "partialFolderCount";
	private static final String RETVAL_KEY_P_RECFOLDER			= "PrecFolder";
	private static final String RETVAL_KEY_P_WRITEPLUGIN		= "PwritePlugIn";
	private static final String RETVAL_KEY_P_RECNAMEPLUGIN		= "PrecNamePlugIn";

	private static final String TEXT_NANSHICHO_HEADER = "臨)";
	
	// EPG予約の確認範囲は前後４時間まで
	private static final long likersvrange = 4L*3600L*1000L;

	// HTTPリトライ回数
	private static final int retryMax = 3;

	// ログ関連
	
	private final String MSGID = "["+getRecorderId()+"] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final String DBGID = "[DEBUG]"+MSGID;
	
	
	/*******************************************************************************
	 * CHコード設定、エラーメッセージ
	 ******************************************************************************/
	
	@Override
	public ChannelCode getChCode() {
		return cc;
	}
	
	private ChannelCode cc = new ChannelCode(getRecorderId());
	
	@Override
	public String getErrmsg() {
		return(errmsg);
	}
	
	private String errmsg = "";
	
	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	private String rsvedFile = null;

	/*******************************************************************************
	 * 素のTVSではコードが読めなくなるのでラッピングしてみる
	 ******************************************************************************/

	private ArrayList<TextValueSet> getListPresetID()			{ return vrate; }		// プリセット
	private String getTextPresetID(ReserveList r)				{ return r.getRec_mode(); }
	private void setTextPresetID(ReserveList r, String text)	{ r.setRec_mode(text); }
	
	private ArrayList<TextValueSet> getListRecMode()			{ return folder; }		// 録画モード
	private String getTextRecMode(ReserveList r)				{ return r.getRec_folder(); }
	private void setTextRecMode(ReserveList r, String text)		{ r.setRec_folder(text); }
	
	private ArrayList<TextValueSet> getListPriority()			{ return autodel; }		// 優先度
	private String getTextPriority(ReserveList r)				{ return r.getRec_autodel(); }
	private void setTextPriority(ReserveList r, String text)	{ r.setRec_autodel(text); }
	
	private ArrayList<TextValueSet> getListPittariFlag()		{ return lvoice; }		// ぴったり（？）録画
	private String getTextPittariFlag(ReserveList r)			{ return r.getRec_lvoice(); }
	private void setTextPittariFlag(ReserveList r, String text)	{ r.setRec_lvoice(text); }
	
	private ArrayList<TextValueSet> getListSuspendMode()		{ return aspect; }	// 録画後動作
	private String getTextSuspendMode(ReserveList r)			{ return r.getRec_aspect(); }
	private void setTextSuspendMode(ReserveList r, String text)	{ r.setRec_aspect(text); }
	
	private ArrayList<TextValueSet> getListRebootFlag()			{ return xchapter; }	// 復帰後再起動する [mv->x]
	private String getTextRebootFlag(ReserveList r)				{ return r.getRec_xchapter(); }
	private void setTextRebootFlag(ReserveList r, String text)	{ r.setRec_xchapter(text); }
	
	private ArrayList<TextValueSet> getListStartMargine()		{ return mschapter; }	// 録画マージン(開始) [x->ms]
	private String getTextStartMargine(ReserveList r)			{ return r.getRec_mschapter(); }
	private void setTextStartMargine(ReserveList r, String text){ r.setRec_mschapter(text); }
	
	private ArrayList<TextValueSet> getListEndMargine()			{ return mvchapter; }	// 録画マージン(終了) [ms->mv]
	private String getTextEndMargine(ReserveList r)				{ return r.getRec_mvchapter(); }
	private void setTextEndMargine(ReserveList r, String text)	{ r.setRec_mvchapter(text); }
	
	private ArrayList<TextValueSet> getListContinueRecFlag()	{ return dvdcompat; }		// 連続録画動作
	private String getTextContinueRecFlag(ReserveList r)		{ return r.getRec_dvdcompat(); }
	private void setTextContinueRecFlag(ReserveList r, String text)	{ r.setRec_dvdcompat(text); }
	
	private ArrayList<TextValueSet> getListServiceMode()		{ return device; }		// 指定サービス対象データ
	private String getTextServiceMode(ReserveList r)			{ return r.getRec_device(); }
	private void setTextServiceMode(ReserveList r, String text)	{ r.setRec_device(text); }
	
	private ArrayList<TextValueSet> getListTunerID()			{ return encoder; }		// 使用チューナー強制指定
	private String getTextTunerID(ReserveList r)				{ return r.getTuner(); }
	private void setTextTunerID(ReserveList r, String text)		{ r.setTuner(text); }
	
	private ArrayList<TextValueSet> getListPartialRecFlag()		{ return bvperf; }		// 別ファイルに同時出力する
	private String getTextPartialRecFlag(ReserveList r)			{ return r.getRec_bvperf(); }
	private void setTextPartialRecFlag(ReserveList r, String text)	{ r.setRec_bvperf(text); }
	
	private ArrayList<TextValueSet> getListRecType()			{ return arate; }		// 予約方式　★鯛ナビ独自
	private String getTextRecType(ReserveList r)				{ return r.getRec_audio(); }
	private void setTextRecType(ReserveList r, String text)		{ r.setRec_audio(text); }
	
	private ArrayList<TextValueSet> getListChValue()			{ return chvalue; }		// CH番号　★鯛ナビ独自
	//private String getTextChValue(ReserveList r)				{ return null; }
	
	private ArrayList<TextValueSet> getListChType()				{ return chtype; }		// CH種別　★鯛ナビ独自
	//private String getTextChType(ReserveList r)					{ return null; }
	
	private void setListPresetID(ArrayList<TextValueSet> tvs)		{ ArrayList<TextValueSet> o = getListPresetID();		o.clear(); o.addAll(tvs); }
	private void setListRecMode(ArrayList<TextValueSet> tvs)		{ ArrayList<TextValueSet> o = getListRecMode();			o.clear(); o.addAll(tvs); }
	private void setListPriority(ArrayList<TextValueSet> tvs)		{ ArrayList<TextValueSet> o = getListPriority();		o.clear(); o.addAll(tvs); }
	private void setListPittariFlag(ArrayList<TextValueSet> tvs)	{ ArrayList<TextValueSet> o = getListPittariFlag();		o.clear(); o.addAll(tvs); }
	private void setListSuspendMode(ArrayList<TextValueSet> tvs)	{ ArrayList<TextValueSet> o = getListSuspendMode();		o.clear(); o.addAll(tvs); }
	//private void setListRebootFlag(ArrayList<TextValueSet> tvs)		{ ArrayList<TextValueSet> o = getListRebootFlag();		o.clear(); o.addAll(tvs); }
	//private void setListStartMargine(ArrayList<TextValueSet> tvs)	{ ArrayList<TextValueSet> o = getListStartMargine();	o.clear(); o.addAll(tvs); }
	//private void setListEndMargine(ArrayList<TextValueSet> tvs)		{ ArrayList<TextValueSet> o = getListEndMargine();		o.clear(); o.addAll(tvs); }
	//private void setListContinueRecFlag(ArrayList<TextValueSet> tvs){ ArrayList<TextValueSet> o = getListContinueRecFlag();	o.clear(); o.addAll(tvs); }
	private void setListTunerID(ArrayList<TextValueSet> tvs)		{ ArrayList<TextValueSet> o = getListTunerID();			o.clear(); o.addAll(tvs); }
	//private void setListPartialRecFlag(ArrayList<TextValueSet> tvs)	{ ArrayList<TextValueSet> o = getListPartialRecFlag();	o.clear(); o.addAll(tvs); }
	//private void setListRecType(ArrayList<TextValueSet> tvs)		{ ArrayList<TextValueSet> o = getListRecType();			o.clear(); o.addAll(tvs); }
	private void setListChValue(ArrayList<TextValueSet> tvs)		{ ArrayList<TextValueSet> o = getListChValue();			o.clear(); o.addAll(tvs); }
	private void setListChType(ArrayList<TextValueSet> tvs)			{ ArrayList<TextValueSet> o = getListChType();			o.clear(); o.addAll(tvs); }

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
	
	public PlugIn_RecRD_EDCB() {
		setIPAddr("127.0.0.1(サンプル)");
		setPortNo("5510(サンプル)");
		setUser("IDとPASSはダミーで結構です");
		setPasswd("********");
	}

	/*******************************************************************************
	 * チャンネルリモコン機能
	 ******************************************************************************/
	
	@Override
	public boolean ChangeChannel(String Channel) {
		// 何もない
		return false;
	}
	
	/*
	@Override
	public void wakeup() {
	}
	*/
	
	@Override
	public void shutdown() {
	}

	
	/*******************************************************************************
	 * レコーダーから予約一覧を取得する
	 ******************************************************************************/
	
	@Override
	public boolean GetRdSettings(boolean force) {
		
		System.out.println("レコーダの各種設定情報を取得します.");
		
		errmsg = "";
		
		/*
		 *  CHコード設定
		 */
		
		cc.load(force);
		replaceChNames(cc);		// これは予約一覧取得からの場合は無駄な処理になるが、GetRdSettings単体呼び出しなら意味がある
		
		/*
		 *  選択肢集団
		 */
		
		final String presetIdTFile	= String.format("%s%srs_%s.%s.%s_%s.xml", "env", File.separator, getRecorderId(), "presetId", getIPAddr(), getPortNo());
		final String recModeTFile	= String.format("%s%srs_%s.%s.%s_%s.xml", "env", File.separator, getRecorderId(), "recMode", getIPAddr(), getPortNo());
		final String prioTFile		= String.format("%s%srs_%s.%s.%s_%s.xml", "env", File.separator, getRecorderId(), "priority", getIPAddr(), getPortNo());
		final String pittariTFile	= String.format("%s%srs_%s.%s.%s_%s.xml", "env", File.separator, getRecorderId(), "pittari", getIPAddr(), getPortNo());
		final String suspModeTFile	= String.format("%s%srs_%s.%s.%s_%s.xml", "env", File.separator, getRecorderId(), "suspendMode", getIPAddr(), getPortNo());
		final String tunerIdTFile	= String.format("%s%srs_%s.%s.%s_%s.xml", "env", File.separator, getRecorderId(), "tunerId", getIPAddr(), getPortNo());
		final String chTypeTFile	= String.format("%s%srs_%s.%s.%s_%s.xml", "env", File.separator, getRecorderId(), "chType", getIPAddr(), getPortNo());
		final String chValueTFile	= String.format("%s%srs_%s.%s.%s_%s.xml", "env", File.separator, getRecorderId(), "chValue", getIPAddr(), getPortNo());

		// ハードコーディングな選択肢の面々
		setSettingNoYes(getListRebootFlag());
		setSettingRecMargin(getListStartMargine());
		setSettingRecMargin(getListEndMargine());
		setSettingServiceMode(getListServiceMode());
		setSettingNoYes(getListContinueRecFlag());
		setSettingNoYes(getListPartialRecFlag());
		setSettingRecType(getListRecType());
	
		if ( force == false ) {
			/*
			 *  キャッシュから読み出し
			 */
			setListPresetID(TVSload(presetIdTFile));
			setListRecMode(TVSload(recModeTFile));
			setListPriority(TVSload(prioTFile));
			setListPittariFlag(TVSload(pittariTFile));
			setListSuspendMode(TVSload(suspModeTFile));
			setListTunerID(TVSload(tunerIdTFile));
			setListChValue(TVSload(chValueTFile));
			setListChType(TVSload(chTypeTFile));
		}
		else {
			/*
			 *  レコーダから読み出し
			 */
			
			// おまじない
			Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
			
			// (1)録画設定の取得
			{
				reportProgress("録画設定を取得します.");
				String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/addprogres.html",null);
				//String hdr = d[0];
				String res = d[1];
				
				if ( res == null ) {
					errmsg = "レコーダーが反応しません";
					return false;
				}
				
				ArrayList<TextValueSet> tvs = new ArrayList<TextValueSet>();
				ArrayList<TextValueSet> tvs2 = new ArrayList<TextValueSet>();
				
				// (1-1)プリセット
				setSettingEtc(tvs,"presetID",0,res);
				add2tvs(tvs, ITEM_PRESETID_REG, VALUE_PRESETID_REG);	// なんつーか
				TVSsave(tvs, presetIdTFile);
				setListPresetID(tvs);
	
				// (1-2)録画モード
				setSettingEtc(tvs,"recMode",0,res);
				for ( TextValueSet o : tvs ) {
					if ( o.getText().equals(ITEM_REC_MODE_DISABLE) ) {
						tvs.remove(o);
						break;
					}
				}
				TVSsave(tvs, recModeTFile);
				setListRecMode(tvs);
				
				// (1-12)録画優先度
				setSettingEtc(tvs, "priority", 0, res);
				TVSsave(tvs, prioTFile);
				setListPriority(tvs);
				
				// (1-11)ぴったり
				setSettingEtc(tvs,"pittariFlag",0,res);
				TVSsave(tvs, pittariTFile);
				setListPittariFlag(tvs);
				
				// (1-6)録画後動作
				setSettingEtc(tvs,"suspendMode",0,res);
				TVSsave(tvs, suspModeTFile);
				setListSuspendMode(tvs);
				
				// (1-4)エンコーダ
				setSettingEtc(tvs,"tunerID",0,res);
				TVSsave(tvs, tunerIdTFile);
				setListTunerID(tvs);
				
				// (1-5)チャンネル
				setSettingChCodeValue(tvs,tvs2,"serviceID",res);
				TVSsave(tvs, chValueTFile);
				TVSsave(tvs2, chTypeTFile);
				setListChValue(tvs);
				setListChType(tvs);
			}
		}
		
		// ちゃんと設定を取得できているよね？
		return (getListTunerID().size()>0 && getListChValue().size()>0 && getListPresetID().size()>0);
	}
	
	/**
	 *	レコーダーから予約一覧を取得する 
	 */
	@Override
	public boolean GetRdReserve(boolean force) {
	
		System.out.println("レコーダから予約一覧を取得します("+force+")");
		
		errmsg = "";
		
		// 予約情報キャッシュ保存先ファイル名
		rsvedFile = String.format("%s%s%s.%s_%s_%s.xml", "env", File.separator, "reserved", getIPAddr(), getPortNo(), getRecorderId());

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		File f = new File(rsvedFile);
		if ( force == false && f.exists() ) {
			// キャッシュから読み出し（予約一覧）
			setReserves(ReservesFromFile(rsvedFile));
			
			// ３種の入れ替え（しばらくしたら削除する）
			{
				boolean modified = false;
				for ( ReserveList r : getReserves() ) {
					if ( ITEM_YES.equals(r.getRec_mvchapter()) || ITEM_NO.equals(r.getRec_mvchapter()) ) {
						String s = r.getRec_mvchapter();
						r.setRec_mvchapter(r.getRec_mschapter());
						r.setRec_mschapter(r.getRec_xchapter());
						r.setRec_xchapter(s);
						
						modified = true;
					}
				}
				if ( modified ) {
					ReservesToFile(getReserves(), rsvedFile);	// キャッシュに保存
				}
			}
			
			replaceChNames(cc);
			if (getDebug()) ShowReserves(getReserves());

			return true;
		}
		
		// レコーダから読み出し（予約一覧）
		ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();
		if ( ! getRsvListAPI(newReserveList) ) {
			return(false);
		}
		
		// 既存予約一覧からの情報引き継ぎ
		copyAttributesAllList(getReserves(), newReserveList);
		
		// 保存
		setReserves(newReserveList);
		ReservesToFile(getReserves(), rsvedFile);	// キャッシュに保存
		
		// 録画済みフラグを立てる（録画結果一覧→予約一覧）
		setRecordedFlag();
		
		if (getDebug()) ShowReserves(getReserves());
		
		return true;
	}
	
	/**
	 * CHコードを置き換えよう（EDCBの場合はCODE2WEB）
	 */
	private void replaceChNames(ChannelCode cc) {
		for ( ReserveList r : getReserves() ) {
			r.setCh_name(cc.getCH_CODE2WEB(r.getChannel()));
		}
	}
	
	/**
	 *	レコーダーから自動予約一覧を取得する 
	 */
	@Override
	public boolean GetRdAutoReserve(boolean force) {
	
		System.out.println("レコーダから自動予約一覧を取得します("+force+")");
		
		errmsg = "";
		
		AutoReserveInfoList newList = new AutoReserveInfoList(Env.envDir,null,getRecorderId(),getIPAddr(),getPortNo());
		
		if ( ! force && newList.exists() ) {
			// 既存のファイルがあれば読み出す
			if ( newList.load() ) {
				setAutoReserves(newList);
				return true;
			}
			
			// ★★★ログだせよ！
			
			return false;	// 読めなかったよ
		}
		
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		// リモートから取得
		if ( ! getAutoReserveList(newList) ) {
			if ( newList.exists() ) {
				// 過去に取得したことがあるなら通信エラーとして処理
				return false;
			}
			else {
				// 過去に取得したことがあるないのでスキップ扱い（にしないと移行時に録画結果一覧が表示されないよね）
				return true;
			}
		}
		
		setAutoReserves(newList);
		
		if ( ! newList.save() ) {
			// ★★★ログだせよ！
		}
		
		return true;
	}
	
	/**
	 *	レコーダーから録画結果一覧を取得する 
	 * @see #GetRdSettings(boolean)
	 */
	@Override
	public boolean GetRdRecorded(boolean force) {
		
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		System.out.println("レコーダから録画結果一覧を取得します("+force+")： "+getRecorderId()+"("+getIPAddr()+":"+getPortNo()+")");
		
		String recedFile = String.format("%s%s%s.%s_%s_%s.xml", "env", File.separator, "recorded", getIPAddr(), getPortNo(), getRecorderId());
		
		// 既存のログをチェック
		ArrayList<RecordedInfo> newRecordedList = RecordedFromFile(recedFile);
		
		File f = new File(recedFile);
		if ( ! force && f.exists() ) {
			
			// キャッシュから読み出し（録画結果一覧）
			setRecorded(newRecordedList);
			if (getDebug()) ShowRecorded(getRecorded());
	
			// 録画済みフラグを立てる（録画結果一覧→予約一覧）
			setRecordedFlag();
		
			return true;
		}
		
		if ( ! getRecedList(newRecordedList) ) {
			return false;
		}
		if ( ! getRecedDetailAll(newRecordedList) ) {
			return false;
		}
		setRecorded(newRecordedList);				// 置き換え
		RecordedToFile(getRecorded(), recedFile);	// キャッシュに保存
		
		// 録画済みフラグを立てる（録画結果一覧→予約一覧）
		setRecordedFlag();
		
		if (getDebug()) ShowRecorded(getRecorded());

		return true;
	}
	
	
	/*------------------------------------------------------------------------------
	 * 予約一覧の取得
	 *------------------------------------------------------------------------------/
	
	/**
	 * 予約一覧＋詳細の取得【API版】
	 */
	private boolean getRsvListAPI(ArrayList<ReserveList> newReserveList) {
		
		//　RDから予約一覧を取り出す
		String response="";
		{
			reportProgress("予約一覧を取得します.");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/api/EnumReserveInfo",null,xmlEncoding);
			response = d[1];
			
			if (response == null) {
				errmsg = "レコーダーが反応しません";
				return(false);
			}
		}
		
		Matcher ma = Pattern.compile("<reserveinfo>(.+?)</reserveinfo>", Pattern.DOTALL).matcher(response);
		while ( ma.find() ) {
			String id = null;
			String title = "";
			GregorianCalendar cal = null;
			Integer hh = null;
			Integer mm = null;
			Integer length = null;
			Integer onid = null;
			Integer tsid = null;
			Integer sid = null;
			Integer evid = null;		// 65536:プログラム予約
			String comment = null;
			Integer overlapMode = null;
			
			Integer recMode = null;
			Integer priority = null;
			Integer tuijyuuFlag = null;
			Integer serviceMode = null;
			Integer pittariFlag = null;
			Integer suspendMode = null;
			Integer rebootFlag = null;
			Integer useMargineFlag = null;
			Integer startMargine = null;
			Integer endMargine = null;
			Integer continueRecFlag = null;
			Integer partialRecFlag = null;
			Integer tunerID = null;
			
			String batFilePath = null;
			
			int recFolderCount = 0;
			ArrayList<String> rRecFolder = new ArrayList<String>();
			ArrayList<String> rWritePlugIn = new ArrayList<String>();
			ArrayList<String> rRecNamePlugIn = new ArrayList<String>();
			
			int partialFolderCount = 0;
			ArrayList<String> pRecFolder = new ArrayList<String>();
			ArrayList<String> pWritePlugIn = new ArrayList<String>();
			ArrayList<String> pRecNamePlugIn = new ArrayList<String>();
			
			ArrayList<String> recFileNameList = new ArrayList<String>();

			Matcher mb = Pattern.compile("<(.+?)>(.+?)</\\1>", Pattern.DOTALL).matcher(ma.group(1));
			while ( mb.find() ) {
				/*
				 * 解析
				 */
				
				// 基本情報
				if ( mb.group(1).equals("ID") ) {
					id = mb.group(2);
				}
				else if ( mb.group(1).equals("title") ) {
					title = CommonUtils.unEscape(mb.group(2));
				}
				else if ( mb.group(1).equals("startDate") ) {
					Matcher mc = Pattern.compile("^(\\d+)/(\\d+)/(\\d+)").matcher(mb.group(2));
					if ( mc.find() ) {
						cal = CommonUtils.getCalendar(String.format("%04d/%02d/%02d",Integer.valueOf(mc.group(1)),Integer.valueOf(mc.group(2)),Integer.valueOf(mc.group(3))));
					}
				}
				else if ( mb.group(1).equals("startTime") ) {
					Matcher mc = Pattern.compile("^(\\d+):(\\d+)").matcher(mb.group(2));
					if ( mc.find() ) {
						hh = Integer.valueOf(mc.group(1));
						mm = Integer.valueOf(mc.group(2));
					}
				}
				else if ( mb.group(1).equals("duration") ) {
					length = Integer.valueOf(mb.group(2))/60;
				}
				else if ( mb.group(1).equals("ONID") ) {
					onid = Integer.valueOf(mb.group(2));
				}
				else if ( mb.group(1).equals("TSID") ) {
					tsid = Integer.valueOf(mb.group(2));
				}
				else if ( mb.group(1).equals("SID") ) {
					sid = Integer.valueOf(mb.group(2));
				}
				else if ( mb.group(1).equals("eventID") ) {
					evid = Integer.valueOf(mb.group(2));
				}
				else if ( mb.group(1).equals("comment") ) {
					comment = CommonUtils.unEscape(mb.group(2));
				}
				else if ( mb.group(1).equals("overlapMode") ) {
					overlapMode = Integer.valueOf(mb.group(2));
				}
				else if ( mb.group(1).equals("recsetting") ) {
					// 録画設定
					Matcher mc = Pattern.compile("<(.+?)>(.+?)</\\1>", Pattern.DOTALL).matcher(mb.group(2));
					while ( mc.find() ) {
						if ( mc.group(1).equals("recMode") ) {
							recMode = Integer.valueOf(mc.group(2));
						}
						else if ( mc.group(1).equals("priority") ) {
							priority = Integer.valueOf(mc.group(2));
						}
						else if ( mc.group(1).equals("tuijyuuFlag") ) {
							tuijyuuFlag = Integer.valueOf(mc.group(2));
						}
						else if ( mc.group(1).equals("serviceMode") ) {
							serviceMode = Integer.valueOf(mc.group(2));
						}
						else if ( mc.group(1).equals("pittariFlag") ) {
							pittariFlag = Integer.valueOf(mc.group(2));
						}
						else if ( mc.group(1).equals("batFilePath") ) {
							batFilePath = mc.group(2);
						}
						else if ( mc.group(1).equals("suspendMode") ) {
							suspendMode = Integer.valueOf(mc.group(2));
						}
						else if ( mc.group(1).equals("rebootFlag") ) {
							rebootFlag = Integer.valueOf(mc.group(2));
						}
						else if ( mc.group(1).equals("useMargineFlag") ) {
							useMargineFlag = Integer.valueOf(mc.group(2));
						}
						else if ( mc.group(1).equals("startMargine") ) {
							startMargine = Integer.valueOf(mc.group(2));
						}
						else if ( mc.group(1).equals("endMargine") ) {
							endMargine = Integer.valueOf(mc.group(2));
						}
						else if ( mc.group(1).equals("continueRecFlag") ) {
							continueRecFlag = Integer.valueOf(mc.group(2));
						}
						else if ( mc.group(1).equals("partialRecFlag") ) {
							partialRecFlag = Integer.valueOf(mc.group(2));
						}
						else if ( mc.group(1).equals("tunerID") ) {
							tunerID = Integer.valueOf(mc.group(2));
						}
						else if ( mc.group(1).equals("recFolderList") ) {
							Matcher md = Pattern.compile("<(.+?)>(.+?)</\\1>", Pattern.DOTALL).matcher(mc.group(2));
							while ( md.find() ) {
								if ( md.group(1).equals("recFolderInfo") ) {
									recFolderCount++;
									Matcher me = Pattern.compile("<(.+?)>(.*?)</\\1>", Pattern.DOTALL).matcher(md.group(2));
									while ( me.find() ) {
										if ( me.group(1).equals("recFolder") ) {
											rRecFolder.add(me.group(2));
										}
										else if ( me.group(1).equals("writePlugIn") ) {
											rWritePlugIn.add(me.group(2));
										}
										else if ( me.group(1).equals("recNamePlugIn") ) {
											rRecNamePlugIn.add(me.group(2));
										}
									}
								}
							}
						}
						else if ( mc.group(1).equals("partialRecFolder") ) {
							Matcher md = Pattern.compile("<(.+?)>(.+?)</\\1>", Pattern.DOTALL).matcher(mc.group(2));
							while ( md.find() ) {
								if ( md.group(1).equals("recFolderInfo") ) {
									partialFolderCount++;
									Matcher me = Pattern.compile("<(.+?)>(.*?)</\\1>", Pattern.DOTALL).matcher(md.group(2));
									while ( me.find() ) {
										if ( me.group(1).equals("recFolder") ) {
											pRecFolder.add(me.group(2));
										}
										else if ( me.group(1).equals("writePlugIn") ) {
											pWritePlugIn.add(me.group(2));
										}
										else if ( me.group(1).equals("recNamePlugIn") ) {
											pRecNamePlugIn.add(me.group(2));
										}
									}
								}
							}
						}
					}
				}
				else if ( mb.group(1).equals("recFileNameList") ) {
					Matcher mc = Pattern.compile("<(.+?)>(.+?)</\\1>", Pattern.DOTALL).matcher(mb.group(2));
					while ( mc.find() ) {
						if ( mc.group(1).equals("name") ) {
							recFileNameList.add(mc.group(2));
						}
					}
				}
			}
			
			/*
			 * 生成
			 */
			
			ReserveList r = new ReserveList();

			r.setId(getUniqId(id));
			r.setTitle(title);
			r.setTitlePop(TraceProgram.replacePop(title));

			String chid = ContentIdEDCB.getChId(onid, tsid, sid);
			String edcbid = ContentIdEDCB.getContentId(chid, evid);
			String contentid = ContentIdEDCB.stripMark(edcbid);

			{
				StringBuilder dt = new StringBuilder("■予約状況：");
				if ( evid != 0 && evid != 65535 ) {
					// EPG予約
					r.setContentId(edcbid);
					setTextRecType(r,ITEM_REC_TYPE_EPG);
					//
					if ( comment != null && comment.contains("EPG自動予約") ) {
						dt.append(comment);
						r.setAutoreserved(true);
					}
					else {
						dt.append("EPG手動予約");
						r.setAutoreserved(false);
					}
				}
				else {
					// プログラム予約
					r.setContentId(null);
					setTextRecType(r,ITEM_REC_TYPE_PROG);
					//
					dt.append("プログラム予約");
					r.setAutoreserved(false);
				}
				dt.append("\n");
				r.setDetail(dt.toString());		// 初期化
			}
			
			r.setChannel(String.valueOf(Long.decode("0x"+chid)));
			r.setCh_name(cc.getCH_CODE2WEB(r.getChannel()));
			
			r.setRec_pattern(CommonUtils.getDate(cal));
			r.setRec_pattern_id(HDDRecorder.RPTPTN_ID_BYDATE);
			cal.set(Calendar.HOUR_OF_DAY, hh);
			cal.set(Calendar.MINUTE, mm);
			r.setAhh(String.format("%02d", cal.get(Calendar.HOUR_OF_DAY)));
			r.setAmm(String.format("%02d", cal.get(Calendar.MINUTE)));
			cal.add(Calendar.MINUTE,length);
			r.setZhh(String.format("%02d", cal.get(Calendar.HOUR_OF_DAY)));
			r.setZmm(String.format("%02d", cal.get(Calendar.MINUTE)));
			r.setRec_nextdate(CommonUtils.getNextDate(r));
			r.setRec_min(CommonUtils.getRecMin(r.getAhh(), r.getAmm(), r.getZhh(), r.getZmm()));
			getStartEndDateTime(r);
			
			// 予約実行とチューナー不足
			{
				String text = value2text(getListRecMode(),String.valueOf(recMode));
				setTextRecMode(r, text);
				if ( text.equals("") ) {
					r.setExec(false);
					r.setTunershort(false);
				}
				else {
					r.setExec(true);
					r.setTunershort(overlapMode!=0);
				}
			}
			
			// プリセット（予約一覧からとれる情報は常に「予約時」）
			setTextPresetID(r, value2text(getListPresetID(),VALUE_PRESETID_REG));
			
			// チューナー
			setTextTunerID(r, value2text(getListTunerID(),String.valueOf(tunerID)));
			
			// 録画後動作
			setTextSuspendMode(r, value2text(getListSuspendMode(),String.valueOf(suspendMode)));

			// 追従
			r.setPursues(VALUE_YES.equals(String.valueOf(tuijyuuFlag)));
			
			// ぴったり
			setTextPittariFlag(r, value2text(getListPittariFlag(),String.valueOf(pittariFlag)));
			
			// 録画優先度
			setTextPriority(r, value2text(getListPriority(),String.valueOf(priority)));
			
			// 連続録画動作
			if ( VALUE_YES.equals(String.valueOf(continueRecFlag)) ) {
				setTextContinueRecFlag(r, ITEM_YES);
			}
			else {
				setTextContinueRecFlag(r, ITEM_NO);
			}
			
			// 復帰後動作
			if ( VALUE_YES.equals(String.valueOf(rebootFlag)) ) {
				setTextRebootFlag(r, ITEM_YES);
			}
			else {
				setTextRebootFlag(r, ITEM_NO);
			}
			
			// 指定サービス対象データ
			if ( serviceMode == 0x00 ) {
				setTextServiceMode(r, ITEM_SERVMOCE_DEFAULT);
			}
			else if ( serviceMode == 0x01 ) {
				setTextServiceMode(r, ITEM_SERVMOCE_NONE);
			}
			else if ( serviceMode == 0x11 ) {
				setTextServiceMode(r, ITEM_SERVMOCE_SUBT);
			}
			else if ( serviceMode == 0x21 ) {
				setTextServiceMode(r, ITEM_SERVMOCE_CARO);
			}
			else if ( serviceMode == 0x31 ) {
				setTextServiceMode(r, ITEM_SERVMOCE_SUBTCARO);
			}
			
			// 録画マージン
			if ( useMargineFlag == 0 ) {
				setTextStartMargine(r, ITEM_MARGINE_DEFAULT);
				setTextEndMargine(r, ITEM_MARGINE_DEFAULT);
			}
			else {
				setTextStartMargine(r, String.valueOf(startMargine));
				setTextEndMargine(r, String.valueOf(endMargine));
			}
			
			// 別ファイルに同時出力する
			if ( VALUE_YES.equals(String.valueOf(partialRecFlag)) ) {
				setTextPartialRecFlag(r, ITEM_YES);
			}
			else {
				setTextPartialRecFlag(r, ITEM_NO);
			}
			
			// 録画後実行bat
			if ( batFilePath != null ) {
				StringBuilder dt = new StringBuilder(r.getDetail());
				dt.append("■録画後実行bat：");
				dt.append(batFilePath);
				dt.append("\n");
				r.setDetail(dt.toString());
			}
			
			// オプショナル
			r.getHidden_params().put(RETVAL_KEY_RECFOLDERCOUNT, String.valueOf(recFolderCount));
			if ( recFolderCount > 0 && rRecFolder.size() >= recFolderCount && rWritePlugIn.size() >= recFolderCount && rRecNamePlugIn.size() >= recFolderCount ) {
				StringBuilder dt = new StringBuilder(r.getDetail());
				StringBuilder rf = new StringBuilder();
				StringBuilder wp = new StringBuilder();
				StringBuilder rp = new StringBuilder();
				for ( int i=0; i<recFolderCount; i++ ) {
					dt.append("■録画フォルダ・出力PlugIn・ファイル名PlugIn(");
					dt.append(String.valueOf(i+1));
					dt.append(")：");
					dt.append(rRecFolder.get(i));
					dt.append(" / ");
					dt.append(rWritePlugIn.get(i));
					dt.append(" / ");
					dt.append(rRecNamePlugIn.get(i));
					dt.append("\n");
					
					rf.append(rRecFolder.get(i));
					rf.append("\t");
					
					wp.append(rWritePlugIn.get(i));
					wp.append("\t");
					
					rp.append(rRecNamePlugIn.get(i));
					rp.append("\t");
				}
				r.setDetail(dt.toString());
				r.getHidden_params().put(RETVAL_KEY_R_RECFOLDER, rf.toString());
				r.getHidden_params().put(RETVAL_KEY_R_WRITEPLUGIN, wp.toString());
				r.getHidden_params().put(RETVAL_KEY_R_RECNAMEPLUGIN, rp.toString());
			}
			
			r.getHidden_params().put(RETVAL_KEY_PARTIALFOLDERCOUNT, String.valueOf(partialFolderCount));
			if ( partialFolderCount > 0 && pRecFolder.size() >= recFolderCount && pWritePlugIn.size() >= recFolderCount && pRecNamePlugIn.size() >= recFolderCount ) {
				StringBuilder dt = new StringBuilder(r.getDetail());
				StringBuilder rf = new StringBuilder();
				StringBuilder wp = new StringBuilder();
				StringBuilder rp = new StringBuilder();
				for ( int i=0; i<recFolderCount; i++ ) {
					dt.append("■[1SEG] 録画フォルダ・出力PlugIn・ファイル名PlugIn(");
					dt.append(String.valueOf(i+1));
					dt.append(")：");
					dt.append(pRecFolder.get(i));
					dt.append(" / ");
					dt.append(pWritePlugIn.get(i));
					dt.append(" / ");
					dt.append(pRecNamePlugIn.get(i));
					dt.append("\n");
					
					rf.append(pRecFolder.get(i));
					rf.append("\t");
					
					wp.append(pWritePlugIn.get(i));
					wp.append("\t");
					
					rp.append(pRecNamePlugIn.get(i));
					rp.append("\t");
				}
				r.setDetail(dt.toString());
				r.getHidden_params().put(RETVAL_KEY_P_RECFOLDER, rf.toString());
				r.getHidden_params().put(RETVAL_KEY_P_WRITEPLUGIN, wp.toString());
				r.getHidden_params().put(RETVAL_KEY_P_RECNAMEPLUGIN, rp.toString());
			}
			
			// 予定ファイル名
			if ( recFileNameList.size() > 0 ) {
				StringBuilder dt = new StringBuilder(r.getDetail());
				for ( int i=0; i<recFileNameList.size(); i++ ) {
					dt.append("■予定ファイル名(");
					dt.append(String.valueOf(i+1));
					dt.append(")：");
					dt.append(recFileNameList.get(i));
				}
				dt.append("\n");
				r.setDetail(dt.toString());
			}
			
			// 番組ID
			if ( evid != 0 && evid != 65535 ) {
				StringBuilder dt = new StringBuilder(r.getDetail());
				dt.append("■番組ID：");
				dt.append(contentid);
				dt.append("\n");
				r.setDetail(dt.toString());
			}
			
			/*
			 *  既存予約一覧からの情報引き継ぎ
			 */
			//XcopyAttributes(r, getReserves());
			
			/*
			 * APIではジャンルがとれない
			 */
			r.setRec_genre(ProgGenre.NOGENRE.toString());
			r.setRec_subgenre(ProgSubgenre.NOGENRE_ETC.toString());
			
			/*
			 * 追加
			 */
			newReserveList.add(r);
		}
		
		return true;
	}

	/**
	 * 予約詳細の取得【HTML版】（番組IDキー）
	 */
	private boolean getRsvDetailByContentId(ReserveList r, int cnt) {
		String pstr = genPoststrEPGA(r);
		String url = "http://"+getIPAddr()+":"+getPortNo()+"/epginfo.html?"+pstr;
		System.out.println("URL: "+url);
		return getRsvDetail(r, url, cnt);
	}
	
	/**
	 * 予約詳細の取得【HTML版】（予約IDキー）
	 */
	private boolean getRsvDetailByReserveId(ReserveList r, int cnt) {
		String url = "http://"+getIPAddr()+":"+getPortNo()+"/reserveinfo.html?id="+getRsvId(r.getId());
		System.out.println("URL: "+url);
		return getRsvDetail(r, url, cnt);
	}
	
	/**
	 * 予約詳細の取得【HTML版】（共通）
	 */
	private boolean getRsvDetail(ReserveList r, String url, int cnt) {
		
		reportProgress("+予約詳細を取得します("+cnt+")");

		String[] d = reqGET(url,null);
		if ( d[1] == null ) {
			return false;
		}
		
		return decodeRsvDetail(r, d[1], url);
	}

	/**
	 * 予約詳細のデコード【HTML版】
	 */
	private boolean decodeRsvDetail(ReserveList r, String res, String url) {

		Matcher mb = null;
		
		try {
			// 予約ID
			mb = Pattern.compile("<form method=\"POST\" action=\"reservedel\\.html\\?id=(\\d+?)\">").matcher(res);
			if ( mb.find() ) {
				r.setId(getUniqId(mb.group(1)));
			}
			
			// イベントIDと予約方式
			int evid = -1;
			mb = Pattern.compile("EventID:\\d+\\(0x(.+)\\)").matcher(res);
			if ( mb.find() ) {
				evid = Integer.decode("0X"+mb.group(1));
				setTextRecType(r,ITEM_REC_TYPE_EPG);
			}
			else {
				setTextRecType(r,ITEM_REC_TYPE_PROG);
			}

			// EPG予約場合のみの処理
			if ( getTextRecType(r).equals(ITEM_REC_TYPE_EPG) ) {
				// 放送局
				int onid = -1;
				mb = Pattern.compile("OriginalNetworkID:\\d+\\(0x(.+)\\)").matcher(res);
				if ( mb.find() ) {
					onid = Integer.decode("0X"+mb.group(1));
				}
				int tsid = -1;
				mb = Pattern.compile("TransportStreamID:\\d+\\(0x(.+)\\)").matcher(res);
				if ( mb.find() ) {
					tsid = Integer.decode("0X"+mb.group(1));
				}
				int sid = -1;
				mb = Pattern.compile("ServiceID:\\d+\\(0x(.+)\\)").matcher(res);
				if ( mb.find() ) {
					sid = Integer.decode("0X"+mb.group(1));
				}
				if ( onid == -1 || tsid == -1 || sid == -1 ) {
					System.err.println("放送局IDが取得できませんでした: "+url);
				}
				else {
					String chid = String.valueOf(Long.decode("0x"+ContentIdEDCB.getChId(onid,tsid,sid)));
					r.setCh_name(cc.getCH_CODE2WEB(chid));
					r.setChannel(chid);
					
					r.setContentId(ContentIdEDCB.getContentId(onid,tsid,sid,evid));
				}
				
				// (1-X) 開始・終了日時と、番組詳細、タイトル
				// ★一覧の開始・終了日時は、登録した際の開始・終了日時のままなので詳細を参照して修正が必要
				mb = Pattern.compile("<HR>番組情報<HR>\\s*(\\d\\d\\d\\d/\\d\\d/\\d\\d\\(.\\)) (\\d\\d):(\\d\\d)～(\\d\\d):(\\d\\d)<BR>.+?<BR>\\s*(.*?)<BR>.*?<BR>\\s*(.+?)\\s*<HR>録画設定<HR>",Pattern.DOTALL).matcher(res);
				if ( mb.find() ) {
					GregorianCalendar cal = CommonUtils.getCalendar(mb.group(1));
					r.setRec_pattern(CommonUtils.getDate(cal));
					r.setRec_pattern_id(HDDRecorder.RPTPTN_ID_BYDATE);
					
					r.setStartDateTime(CommonUtils.getDateTime(cal));
					r.setRec_nextdate(r.getStartDateTime());
					r.setAhh(mb.group(2));
					r.setAmm(mb.group(3));
					r.setZhh(mb.group(4));
					r.setZmm(mb.group(5));
					
					r.setRec_nextdate(CommonUtils.getNextDate(r));
					r.setRec_min(CommonUtils.getRecMin(r.getAhh(), r.getAmm(), r.getZhh(), r.getZmm()));
					getStartEndDateTime(r);
					
					r.setTitle(CommonUtils.unEscape(mb.group(6)));
					r.setTitlePop(TraceProgram.replacePop(r.getTitle()));
					
					r.setDetail(CommonUtils.unEscape(mb.group(7)).replaceAll("<BR>", ""));
				}
			}
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		
		// (1-1)プリセット
		setTextPresetID(r, getSelectedSetting("presetID",res));
		
		// (1-3)録画モード
		{
			String rec_mode = getSelectedSetting("recMode",res);
			if ( rec_mode != null && rec_mode.equals(ITEM_REC_MODE_DISABLE) ) {
				// "無効"は「予約実行」で扱うので
				r.setExec(false);
				setTextRecMode(r,getListRecMode().get(0).getText());
				// チューナー不足警告（リスト取得時に立ててるので落とす）
				r.setTunershort(false);
			}
			else {
				setTextRecMode(r,rec_mode);
			}
		}
		
		// (1-4)エンコーダ
		setTextTunerID(r, getSelectedSetting("tunerID",res));
		
		// (1-6)録画後動作
		setTextSuspendMode(r, getSelectedSetting("suspendMode",res));
		
		// (1-10)追従
		r.setPursues(ITEM_YES.equals(getSelectedSetting("tuijyuuFlag",res)));
		
		// (1-11)ぴったり
		setTextPittariFlag(r, getSelectedSetting("pittariFlag",res));
		
		// (1-12)録画優先度
		setTextPriority(r, getSelectedSetting("priority",res));
		
		// (1-13)連続録画動作
		if ( getCheckedSetting("continueRecFlag", res) != null ) {
			setTextContinueRecFlag(r, ITEM_YES);
		}
		else {
			setTextContinueRecFlag(r, ITEM_NO);
		}
		
		// (1-14)復帰後再起動
		if ( getCheckedSetting("rebootFlag", res) != null ) {
			setTextRebootFlag(r, ITEM_YES);
		}
		else {
			setTextRebootFlag(r, ITEM_NO);
		}
		
		// (1-15) 指定サービス対象データ
		if ( getCheckedSetting("serviceMode", res) != null ) {
			setTextServiceMode(r, ITEM_SERVMOCE_DEFAULT);
		}
		else {
			boolean b1 = "0".equals(getCheckedSetting("serviceMode_1", res));
			boolean b2 = "0".equals(getCheckedSetting("serviceMode_2", res));
			if ( b1 && b2 ) {
				setTextServiceMode(r, ITEM_SERVMOCE_SUBTCARO);
			}
			else if ( b1 ) {
				setTextServiceMode(r, ITEM_SERVMOCE_SUBT);
			}
			else if ( b2 ) {
				setTextServiceMode(r, ITEM_SERVMOCE_CARO);
			}
			else {
				setTextServiceMode(r, ITEM_SERVMOCE_NONE);
			}
		}
		
		// (1-16) 録画マージン
		{
			if ( getCheckedSetting("useDefMargineFlag", res) != null ) {
				setTextStartMargine(r, ITEM_MARGINE_DEFAULT);
				setTextEndMargine(r, ITEM_MARGINE_DEFAULT);
			}
			else {
				setTextStartMargine(r, getEditedSetting("startMargine", res));
				setTextEndMargine(r, getEditedSetting("endMargine", res));
			}
		}
		
		// (1-17) 別ファイルに同時出力する
		if ( getCheckedSetting("partialRecFlag", res) != null ) {
			setTextPartialRecFlag(r, ITEM_YES);
		}
		else {
			setTextPartialRecFlag(r, ITEM_NO);
		}
		
		String batFilePath = null;
		ArrayList<String> rRecFolder = new ArrayList<String>();
		ArrayList<String> rWritePlugIn = new ArrayList<String>();
		ArrayList<String> rRecNamePlugIn = new ArrayList<String>();
		ArrayList<String> pRecFolder = new ArrayList<String>();
		ArrayList<String> pWritePlugIn = new ArrayList<String>();
		ArrayList<String> pRecNamePlugIn = new ArrayList<String>();
		
		// 録画後実行bat
		mb = Pattern.compile("録画後実行bat.*?:\\s*(.*?)<BR>",Pattern.DOTALL).matcher(res);
		if ( mb.find() ) {
			batFilePath = mb.group(1);
		}
		
		// オプショナル
		{
			// 録画フォルダ等
			Matcher mc = Pattern.compile("録画フォルダ.*?(<TABLE.*?</TABLE>)\\s*<input type=hidden name=\"recFolderCount\" value=\"(\\d+?)\">",Pattern.DOTALL).matcher(res);
			if ( ! mc.find() ) {
				errmsg = "情報が見つかりません："+RETVAL_KEY_RECFOLDERCOUNT;
			}
			else {
				r.getHidden_params().put(RETVAL_KEY_RECFOLDERCOUNT, mc.group(2));
				
				Matcher md = Pattern.compile("<TR>(.*?)</TR>",Pattern.DOTALL).matcher(mc.group(1));
				int idx = 0;
				while ( md.find() ) {
					if ( idx++ == 0 ) {
						continue;
					}
					Matcher me = Pattern.compile("<TD>(.*?)</TD>",Pattern.DOTALL).matcher(md.group(1));
					for ( int i=0; i<3 && me.find(); i++ ) {
						switch (i) {
						case 0:
							rRecFolder.add(me.group(1));
							break;
						case 1:
							rWritePlugIn.add(me.group(1));
							break;
						case 2:
							rRecNamePlugIn.add(me.group(1));
							break;
						}
					}
				}
			}
			
			// 録画フォルダ等（ワンセグ）
			mc = Pattern.compile("partialRecFlag.*?(<TABLE.*?</TABLE>)\\s*<input type=hidden name=\"partialFolderCount\" value=\"(\\d+?)\">",Pattern.DOTALL).matcher(res);
			if ( ! mc.find() ) {
				errmsg = "情報が見つかりません："+RETVAL_KEY_PARTIALFOLDERCOUNT;
			}
			else {
				r.getHidden_params().put(RETVAL_KEY_PARTIALFOLDERCOUNT, mc.group(2));
				
				
				Matcher md = Pattern.compile("<TR>(.*?)</TR>",Pattern.DOTALL).matcher(mc.group(1));
				int idx = 0;
				while ( md.find() ) {
					if ( idx++ == 0 ) {
						continue;
					}
					Matcher me = Pattern.compile("<TD>(.*?)</TD>",Pattern.DOTALL).matcher(md.group(1));
					for ( int i=0; i<3 && me.find(); i++ ) {
						switch (i) {
						case 0:
							pRecFolder.add(me.group(1));
							break;
						case 1:
							pWritePlugIn.add(me.group(1));
							break;
						case 2:
							pRecNamePlugIn.add(me.group(1));
							break;
						}
					}
				}
			}
		}
		
		{
			StringBuilder dt = new StringBuilder("■予約状況：");
			if ( r.getContentId() != null ) {
				dt.append("EPG予約（自動手動不明→予約一覧再取得を行ってください）");
			}
			else {
				dt.append("プログラム予約");
			}
			dt.append("\n");
			r.setDetail(dt.toString());
		}
		
		if ( batFilePath != null && batFilePath.length() > 0 ) {
			StringBuilder dt = new StringBuilder(r.getDetail());
			dt.append("■録画後実行bat：");
			dt.append(batFilePath);
			dt.append("\n");
			r.setDetail(dt.toString());
		}
		
		if ( rRecFolder.size() > 0 ) {
			StringBuilder dt = new StringBuilder(r.getDetail());
			StringBuilder rf = new StringBuilder();
			StringBuilder wp = new StringBuilder();
			StringBuilder rp = new StringBuilder();
			for ( int i=0; i<rRecFolder.size(); i++ ) {
				dt.append("■録画フォルダ・出力PlugIn・ファイル名PlugIn(");
				dt.append(String.valueOf(i+1));
				dt.append(")：");
				dt.append(rRecFolder.get(i));
				dt.append(" / ");
				dt.append(rWritePlugIn.get(i));
				dt.append(" / ");
				dt.append(rRecNamePlugIn.get(i));
				dt.append("\n");
				
				rf.append(rRecFolder.get(i));
				rf.append("\t");
				
				wp.append(rWritePlugIn.get(i));
				wp.append("\t");
				
				rp.append(rRecNamePlugIn.get(i));
				rp.append("\t");
			}
			r.setDetail(dt.toString());
			r.getHidden_params().put(RETVAL_KEY_R_RECFOLDER, rf.toString());
			r.getHidden_params().put(RETVAL_KEY_R_WRITEPLUGIN, wp.toString());
			r.getHidden_params().put(RETVAL_KEY_R_RECNAMEPLUGIN, rp.toString());
		}
		
		if ( pRecFolder.size() > 0 ) {
			StringBuilder dt = new StringBuilder(r.getDetail());
			StringBuilder rf = new StringBuilder();
			StringBuilder wp = new StringBuilder();
			StringBuilder rp = new StringBuilder();
			for ( int i=0; i<rRecFolder.size(); i++ ) {
				dt.append("■[1SEG] 録画フォルダ・出力PlugIn・ファイル名PlugIn(");
				dt.append(String.valueOf(i+1));
				dt.append(")：");
				dt.append(pRecFolder.get(i));
				dt.append(" / ");
				dt.append(pWritePlugIn.get(i));
				dt.append(" / ");
				dt.append(pRecNamePlugIn.get(i));
				dt.append("\n");
				
				rf.append(pRecFolder.get(i));
				rf.append("\t");
				
				wp.append(pWritePlugIn.get(i));
				wp.append("\t");
				
				rp.append(pRecNamePlugIn.get(i));
				rp.append("\t");
			}
			r.setDetail(dt.toString());
			r.getHidden_params().put(RETVAL_KEY_P_RECFOLDER, rf.toString());
			r.getHidden_params().put(RETVAL_KEY_P_WRITEPLUGIN, wp.toString());
			r.getHidden_params().put(RETVAL_KEY_P_RECNAMEPLUGIN, rp.toString());
		}
		
		// 番組ID
		if ( r.getContentId() != null && r.getContentId().length() > 0 ) {
			StringBuilder dt = new StringBuilder(r.getDetail());
			dt.append("■番組ID：");
			dt.append(ContentIdEDCB.stripMark(r.getContentId()));
			dt.append("\n");
			r.setDetail(dt.toString());
		}

		return true;
	}
	private String getSelectedSetting(String key, String res) {
		Matcher mb = Pattern.compile("<select name=\""+key+"\">[\\s\\S]*?<option value=\"([^\"]+?)\"\\s*selected>(.+?)\n").matcher(res);
		if (mb.find()) {
			return mb.group(2);
		}
		return null;
	}
	private String getEditedSetting(String key, String res) {
		Matcher mb = Pattern.compile("<input type=text name=\""+key+"\" value=\"([^\"]+?)\">").matcher(res);
		if (mb.find()) {
			return mb.group(1);
		}
		return null;
	}
	private String getCheckedSetting(String key, String res) {
		Matcher mb = Pattern.compile("<input type=checkbox name=\""+key+"\" value=\"([^\"]+?)\" checked>").matcher(res);
		if (mb.find()) {
			return mb.group(1);
		}
		return null;
	}
	
	
	/*------------------------------------------------------------------------------
	 * 自動予約一覧の取得
	 *------------------------------------------------------------------------------/
	
	/**
	 * 自動予約一覧を取得する
	 */
	private boolean getAutoReserveList(AutoReserveInfoList newAutoReserveList) {
		if ( ! getAutorsvList(newAutoReserveList) ) {
			// 一覧が取得できなかった
			return false;
		}
		if ( ! getAutorsvDetailAll(newAutoReserveList) ) {
			// 詳細が取得できなかった
			return false;
		}
		return true;
	}
	
	/**
	 * 自動予約一覧を取得する（リスト部）
	 */
	private boolean getAutorsvList(AutoReserveInfoList newAutoReserveList) {
		
		int maxpage = 1;			// 初期値は"1"！
		String firstResp = null;	// ２回読み出したくない

		String url = "http://"+getIPAddr()+":"+getPortNo()+"/autoaddepg.html&page=";
		
		{
			String uri = url+"0";
			reportProgress("自動予約一覧のページ数を取得します: "+uri);
			String[] d = reqGET(uri,null);
			if (d[1] == null) {
				errmsg = "レコーダーが反応しません";
				return false;
			}
			
			firstResp = d[1];
			
			// maxpageの計算が入る
			Matcher ma = Pattern.compile("\"autoaddepg\\.html\\?page=").matcher(firstResp);
			while ( ma.find() ) {
				++maxpage;
			}
		}
		
		for ( int i=0; i<maxpage; i++ ) {
			
			String uri = url+String.valueOf(i);
			
			reportProgress(String.format("+自動予約一覧を取得します(%d/%d): %s",(i+1),maxpage,uri));
			
			String response;
			if ( i == 0 ) {
				response = firstResp;
			}
			else {
				// あとで
				String[] d = reqGET(uri,null);
				if (d[1] == null) {
					errmsg = "レコーダーが反応しません";
					return false;
				}
				
				response = d[1];
			}
			
			Matcher ma = Pattern.compile("<TR>(.+?)</TR>",Pattern.DOTALL).matcher(response);
			while ( ma.find() ) {
				// 入れ物用意
				AutoReserveInfo c = new AutoReserveInfo();
				
				// 分解
				Matcher mb = Pattern.compile("<TD>(.*?)</TD>",Pattern.DOTALL).matcher(ma.group(1));
				for ( int n=0; mb.find(); n++ ) {
					switch (n) {
					case 0:	// title
						c.setLabel(CommonUtils.unEscape(mb.group(1)));
						break;
					case 5:	// id
						Matcher mc = Pattern.compile("\"autoaddepginfo\\.html\\?id=(.+)\"",Pattern.DOTALL).matcher(mb.group(1));
						if ( mc.find() ) {
							c.setId(mc.group(1));
						}
						break;
					case 3:	// channel
						//c.getChannels().add(CommonUtils.unEscape(mb.group(1)));
						break;
					case 1:	// mark
					case 2:	// genre
					case 4:	// service
					default:
						break;
					}
				}
				if ( c.getId() == null ) {
					c = null;
					continue;	// 情報がみつからなかった
				}
				
				newAutoReserveList.add(c);
			}
		}
		
		return true;
	}
	
	/**
	 * 自動予約一覧を取得する（詳細部）
	 */
	private boolean getAutorsvDetailAll(AutoReserveInfoList newAutoReserveList) {
		
		String url = "http://"+getIPAddr()+":"+getPortNo()+"/autoaddepginfo.html?id=";
		String pstr = "presetID=65535&dataID=";
		
		int cnt = 0;
		for ( AutoReserveInfo c : newAutoReserveList ) {

			++cnt;
			
			reportProgress(String.format("自動予約詳細を取得します(%d/%d)",cnt,newAutoReserveList.size()));
			String[] d = reqPOST(url+c.getId(), pstr+c.getId(), null);
			if (d[1] == null) {
				errmsg = "レコーダーが反応しません";
				return false;
			}
			
			if ( ! decodeAutorsvDetail(c, d[1]) ) {
				// デコードできなかった
				System.err.println("★★★　スクリプト解析まで仮置き　★★★　");
				CommonUtils.printStackTrace();
			}
		}
		
		return true;
	}
	
	/**
	 * 自動予約詳細をデコードする
	 */
	private boolean decodeAutorsvDetail(AutoReserveInfo c, String str) {
		
		String[] data = str.split("<HR>録画設定<HR>");
		if ( data.length != 2 ) {
			return false;
		}

		// キーワード設定部
		{
			// テキストボックスの場合
			{
				Matcher mb = Pattern.compile("<input type=\"text\" name=\"(.+?)\" value=\"(.*?)\" size=", Pattern.DOTALL).matcher(data[0]);
				while ( mb.find() ) {
					if ( mb.group(1).equals("andKey") ) {
						c.setLabel(CommonUtils.unEscape(mb.group(2)));
						c.setKeyword(c.getLabel());
					}
					else if ( mb.group(1).equals("notKey") ) {
						c.setExKeyword(CommonUtils.unEscape(mb.group(2)));
					}
					else if ( mb.group(1).equals("dateList") ) {
						
					}
					else if ( mb.group(1).equals("chkRecDay") ) {
						try {
							c.setRecordedCheckTerm(Integer.valueOf(mb.group(2)));
							continue;
						}
						catch ( NumberFormatException e) {
							e.printStackTrace();
						}
						c.setRecordedCheckTerm(6);		// デフォルトバリュー
					}
				}
			}
			
			// コンボボックスの場合
			{
				Matcher mb = Pattern.compile("<select name=\"(.+?)\" .*?size=\\d+?>(.*?)</select>", Pattern.DOTALL).matcher(data[0]);
				while ( mb.find() ) {
					if ( mb.group(1).equals("contentList") ) {
						// ジャンル
					}
					else if ( mb.group(1).equals("serviceList") ) {
						
						// 放送局
						Matcher mc = Pattern.compile("<option value=\"(\\d+?)\" selected>", Pattern.DOTALL).matcher(mb.group(2));
						while ( mc.find() ) {
							String chCode = mc.group(1);
							c.getChCodes().add(chCode);
							
							// code->name変換は setAutoReserves() 内で行う
						}
					}
					
				}
			}
		}
		
		// 録画設定部
		{
			ReserveInfo r = new ReserveInfo();
			decodeRsvDetail(r, data[1], null);
			c.setRecSetting(r);
			
			c.setExec(r.getExec());
		}
		
		return true;
	}
	
	
	/*------------------------------------------------------------------------------
	 * 録画結果一覧の取得
	 *------------------------------------------------------------------------------/
	 *
	/**
	 *	録画結果一覧を取得する
	 */
	private boolean getRecedList(ArrayList<RecordedInfo> newRecordedList) {
		
		String critDate = null;
		if ( newRecordedList.size() > 0 ) {
			// 最新の情報の前日分までチェックする
			GregorianCalendar cal = CommonUtils.getCalendar(newRecordedList.get(0).getDate());
			cal.add(Calendar.DATE, -1);
			critDate = CommonUtils.getDate(cal);
		}
		else {
			// 既存情報が無ければ上限まで
			critDate = CommonUtils.getDate(CommonUtils.getCalendar(-86400*getRecordedSaveScope()));
		}
		
		if (getDebug()) System.out.println(DBGID+"録画結果の取り込みはここまで： "+critDate);
		
		//　RDから予約一覧を取り出す
		String response="";
		{
			reportProgress("録画結果一覧を取得します.(1)");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/recinfo.html",null);
			response = d[1];
			
			if (response == null) {
				errmsg = "レコーダーが反応しません";
				return false;
			}
		}

		// ３０件を超えるとページが増える
		int maxpage = 0;
		Matcher ma = Pattern.compile("\"recinfo.html\\?page=(\\d+)\"").matcher(response);
		while ( ma.find() ) {
			int page = Integer.valueOf(ma.group(1));
			if ( maxpage < page ) {
				maxpage = page;
			}
		}
		
		for ( int page = 0; page<=maxpage; page++ ) {
			if ( page > 0 ) {
				reportProgress(String.format("録画結果一覧を取得します.(%d)",page+1));
				String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/recinfo.html?page="+page,null,thisEncoding);
				response = d[1];
			}
			if ( decodeRecedList(newRecordedList, response, critDate) <= 0) {
				// おわったぽ
				break;
			}
		}

		return true;
	}
	
	/**
	 * 録画一覧のデコード
	 */
	private int decodeRecedList(ArrayList<RecordedInfo> newRecordedList, String response, String critDate) {
		
		int cnt = 0;
		
		//
		Matcher mc = Pattern.compile("<TABLE BORDER=\"1\">(.+?)</TABLE>",Pattern.DOTALL).matcher(response);
		if (mc.find()) {
			Matcher ma = Pattern.compile("<TR( BGCOLOR=#......)?><TD>(\\d\\d\\d\\d/\\d\\d/\\d\\d)\\(.\\) (\\d\\d):(\\d\\d)～(\\d\\d):(\\d\\d)</TD><TD>(.+?)</TD>.*?<A HREF=\"recinfodesc\\.html\\?id=(\\d+?)\">詳細</A></TD></TR>\\s*?<TR( BGCOLOR=#......)?><TD COLSPAN=\"2\">(.*?)</TD></TR>\\s*?<TR( BGCOLOR=#......)?><TD COLSPAN=\"2\">(.*?)</TD></TR>",Pattern.DOTALL).matcher(mc.group(1));
			while ( ma.find() ) {
				
				// 日付を調べる
				GregorianCalendar cal = CommonUtils.getCalendar(ma.group(2));
				String date = CommonUtils.getDate(cal);
				
				// 既存の情報に追いついたので取得終了
				if ( date.compareTo(critDate) < 0 ) {
					return -1;
				}
				
				// 個々のデータを取り出す
				RecordedInfo entry = new RecordedInfo();

				entry.setDate(date);
				
				entry.setAhh(ma.group(3));
				entry.setAmm(ma.group(4));
				entry.setZhh(ma.group(5));
				entry.setZmm(ma.group(6));
				
				long lenL = CommonUtils.getCompareDateTime(entry.getDate()+" "+entry.getZhh()+":"+entry.getZmm(), entry.getDate()+" "+entry.getAhh()+":"+entry.getAmm());
				if ( lenL < 0 ) {
					lenL += 86400000;
				}
				int len =  (int) (lenL / 60000L);
				entry.setLength(len);
				
				// 放送局（仮）
				String recChName = ma.group(7);
				String chid = cc.getCH_REC2CODE(recChName);
				if ( chid == null ) {
					// 難視聴対策局対策
					String nan = TEXT_NANSHICHO_HEADER+recChName;
					chid = cc.getCH_REC2CODE(nan);
					if ( chid != null ) {
						recChName = nan;
					}
				}
				
				if ( chid == null ) {
					// CHコードにできなければ、HTMLから取得した放送局名をそのまま使う
					entry.setChannel(null);
					entry.setCh_name(recChName);
				}
				else {
					entry.setChannel(chid);
					String ch_name = cc.getCH_CODE2WEB(chid);
					if ( ch_name == null ) {
						// CHコード設定がうまくないようですよ？
						entry.setCh_name(ma.group(7));
					}
					else {
						entry.setCh_name(ch_name);
					}
				}
				
				entry.setCh_orig(recChName);
				
				// ID
				entry.setId(ma.group(8));
				
				// 予約名
				entry.setTitle(CommonUtils.unEscape(ma.group(10)).replaceAll("<BR>", ""));
				
				// 録画結果
				entry.setResult(ma.group(12));
				entry.setSucceeded(entry.getResult().matches("^(録画終了|開始時間が変更されました)$"));
				
				// その他
				
				addRecorded(newRecordedList, entry);
				
				++cnt;
			}
		}
		
		return cnt;
	}

	/**
	 * 録画詳細の取得
	 */
	private boolean getRecedDetailAll(ArrayList<RecordedInfo> newRecordedList) {
		// 詳細情報を取得する
		int i=0;
		for ( RecordedInfo entry : newRecordedList ) {
			
			if ( entry.getId() == null ) {
				// 過去ログかな…？
				continue;
			}
			
			++i;
			
			reportProgress("+録画結果詳細を取得します("+i+")");
			
			String response="";
			{
				String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/recinfodesc.html?id="+entry.getId(),null);
				response = d[1];
				
				if (response == null) {
					errmsg = "レコーダーが反応しません";
					return false;
				}
			}
			
			Matcher ma = Pattern.compile("ドロップ：(\\d+)", Pattern.DOTALL).matcher(response);
			if ( ma.find() ) {
				entry.setDrop(Integer.valueOf(ma.group(1)));
			}
			
			ma = Pattern.compile("Drop:\\s*(\\d+)\\s*Scramble:\\s*(\\d+)\\s*MPEG2", Pattern.DOTALL).matcher(response);
			while ( ma.find() ) {
				entry.setDrop_mpeg(entry.getDrop_mpeg()+Integer.valueOf(ma.group(1)));
			}
			
			ma = Pattern.compile("(<HR>番組情報<HR>.*</PRE>)", Pattern.DOTALL).matcher(response);
			if ( ma.find() ) {
				entry.setDetail(CommonUtils.unEscape(ma.group(1).replaceAll("<BR>", "").replaceAll("</?PRE>", "").replaceAll("<HR>", "====").replaceAll("\n\n+", "\n\n")/*.replaceFirst("^([\\s\\S]*?)(====エラーログ====[\\s\\S]*?)$","$2\n\n$1")*/));
			}
			
			// 放送局（訂正）
			Matcher md = Pattern.compile("OriginalNetworkID:(\\d+).*?TransportStreamID:(\\d+).*?ServiceID:(\\d+)",Pattern.DOTALL).matcher(response);
			if ( md.find() ) {
				// 詳細情報からCHコードが取得できるなら
				String chid = String.valueOf(Long.decode("0x"+ContentIdEDCB.getChId(Integer.valueOf(md.group(1)),Integer.valueOf(md.group(2)),Integer.valueOf(md.group(3)))));
				entry.setChannel(chid);
				String ch_name = cc.getCH_CODE2WEB(chid);
				if ( ch_name != null ) {
					entry.setCh_name(ch_name);
				}
			}
			

		}
		return true;
	}

	
	/*******************************************************************************
	 * 新規予約
	 ******************************************************************************/
	
	@Override
	public boolean PostRdEntry(ReserveList reqr)
	{
		
		ArrayList<ReserveList> tmprl = getReserves();
		
		boolean b = _PostRdEntry(reqr);
		
		// 予約一覧が更新されていたら、本体から取得できない情報は引き継ぐ
		if ( getReserves() != tmprl ) {
			copyAttributesAllList(tmprl, getReserves());
		}
		
		// 成功しても失敗してもキャッシュが更新されている可能性があるので保存し直す
		ReservesToFile(getReserves(), rsvedFile);
		setRecordedFlag();
		
		return b;
	}
	private boolean _PostRdEntry(ReserveList reqr) 
	{
		//
		System.out.println("Run: PostRdEntry("+reqr.getTitle()+")");
		
		errmsg = "";

		// 放送局と日付のチェック
		if ( ! PrePostCheck(reqr) ) {
			return(false);
		}
		
		// プリセット
		if ( reqr.getRec_audio().equals(ITEM_PRESETID_REG) ) {
			errmsg = "新規予約では次の指定はできません：プリセット＝"+ITEM_PRESETID_REG;
			return false;
		}
		
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		String header;
		String response;
		
		int onid = -1;
		int tsid = -1;
		int sid = -1;
		int evid = -1;
		
		boolean rec_type_epg = true;
		if ( getTextRecType(reqr) != null && getTextRecType(reqr).equals(ITEM_REC_TYPE_EPG) ) {
			if ( ContentIdEDCB.isValid(reqr.getContentId()) ) {
				ContentIdEDCB.decodeContentId(reqr.getContentId());
				onid = ContentIdEDCB.getOnId();
				tsid = ContentIdEDCB.getTSId();
				sid = ContentIdEDCB.getSId();
				evid = ContentIdEDCB.getEvId();
			}
			else if ( ContentIdDIMORA.isValid(reqr.getContentId()) ) {
				ContentIdDIMORA.decodeContentId(reqr.getContentId());
				evid = ContentIdDIMORA.getEvId();
				// 後続処理あり
			}
			else if ( ContentIdREGZA.isValid(reqr.getContentId()) ) {
				ContentIdREGZA.decodeContentId(reqr.getContentId());
				evid = ContentIdREGZA.getEvId();
				// 後続処理あり
			}
			else {
				errmsg = "番組表に予約IDがないためEPG予約は利用できません。EDCB番組表を利用するかプログラム予約を行ってください。";
				return false;
			}
			
			if ( evid != -1 && onid == -1 ) {
				// Dimora、REGZA形式の場合 onid と tsid が取得できないので自前の情報で補完
				ContentIdEDCB.decodeChId(ContentIdEDCB.getChId(cc.getCH_WEB2CODE(reqr.getCh_name())));
				onid = ContentIdEDCB.getOnId();
				tsid = ContentIdEDCB.getTSId();
				sid = ContentIdEDCB.getSId();
				// EDCB形式で置き換え
				reqr.setContentId(ContentIdEDCB.getContentId(onid, tsid, sid, evid));
			}
		}
		else {
			rec_type_epg = false;
		}
		
		ReserveList newr = null;
		
		// 予約IDが含まれていたら削る
		reqr.setId("");
		
		/*
		 *  EPG予約
		 */
		if ( rec_type_epg ) {
			
			int cntMax = 3;
			int cnt = 1;
			
			
			// 番組情報ページを開く
			{
				reportProgress(String.format("EPG予約を実行します(%d/%d).",cnt++,cntMax));
				
				ReserveList tmpr = new ReserveList();
				tmpr.setContentId(reqr.getContentId());
				setTextPresetID(tmpr, getTextPresetID(reqr));
				if ( ! getRsvDetailByContentId(tmpr,0) ) {
					errmsg = "予約ページが開けません。";
					return(false);
				}
				
				if ( tmpr.getId() != null && tmpr.getId().length() > 0 ) {
					System.out.println("重複予約が実行されます.");
				}
			}
			
			// 予約登録を実行する
			{		
				reportProgress(String.format("追加を実行します(%d/%d).",cnt++,cntMax));
				
				String pstr = genPoststrEPGB(reqr);
				String uri = "http://"+getIPAddr()+":"+getPortNo()+"/"+String.format("reserveadd.html?onid=%d&tsid=%d&sid=%d&evid=%d",onid,tsid,sid,evid);;
				
				System.err.println("URL: "+uri);
				
				header = response = null;
				for (int i=0; i<retryMax; i++) {
					String[] d = reqPOST(uri, pstr, null);
					header = d[0];
					response = d[1];
					if ( header != null && response == null ) {
						reportProgress("コネクションがリセットされました。リトライします。");
						CommonUtils.milSleep(1000);
					}
					else {
						break;
					}
				}
				if ( header == null || response == null ) {
					errmsg = "レコーダーが反応しません。";
					return(false);
				}
			}
			
			// EPG予約の場合はすぐわかる（はずだった→重複予約だとわからない）
			{
				reportProgress(String.format("新しい予約IDを取得します(%d/%d).",cnt++,cntMax));
				
				newr = getRsvOnEdcbTR(reqr);
				if ( newr == null ) {
					return(false);
				}
				
				// 本体から取得できない情報は引き継ぐ
				copyAttributesMethod(reqr, newr);
				
				// 一発ヒットしたものはキャッシュ上にないので載せる
				if ( ! getReserves().contains(newr) ) {
					getReserves().add(newr);
				}
				
				reportProgress("+新しい予約ID： "+newr.getId());
				
				if ( isModified(reqr, newr) ) {
					errmsg = "EDCB番組表からの情報で内容が変更されました: "+newr.getStartDateTime()+"～"+newr.getZhh()+":"+newr.getZmm()+" "+newr.getTitle();
				}
			}
		}
		
		/*
		 * プログラム予約 
		 */
		else {
			
			int cntMax = 3;
			int cnt = 1;
			
			// プログラム予約の制限のチェック
			if ( ! PreProgResCheck(reqr) ) {
				return(false);
			}
			
			// 番組IDが含まれていたら削る
			reqr.setContentId(null);
			
			// 予約情報の一部を確定する
			{		
				reportProgress(String.format("プログラム予約を実行します(%d/%d).",cnt++,cntMax));
				
				String pstr = genPoststrDTA(reqr);
				String uri = "http://"+getIPAddr()+":"+getPortNo()+"/addprogres.html";
				
				System.err.println("URL: "+uri+"?"+pstr);
				
				header = response = null;
				for (int i=0; i<retryMax; i++) {
					String[] d = reqPOST(uri, pstr, null);
					header = d[0];
					response = d[1];
					if ( header != null && response == null ) {
						reportProgress("コネクションがリセットされました。リトライします。");
						CommonUtils.milSleep(1000);
					}
					else {
						break;
					}
				}
				if ( header == null || response == null ) {
					errmsg = "レコーダーが反応しません。";
					return(false);
				}

				Matcher ma = Pattern.compile("<form method=\"POST\" action=\"reservepgadd.html\">").matcher(response);
				if ( ! ma.find() ) {
					errmsg = "予約に失敗しました。";
					return(false);
				}
				
				ma = Pattern.compile("<input type=hidden name=\"recFolderCount\" value=\"(\\d+?)\">").matcher(response);
				if ( ! ma.find() ) {
					errmsg = "予約に失敗しました。";
					return(false);
				}
				reqr.getHidden_params().put(RETVAL_KEY_RECFOLDERCOUNT, ma.group(1));
				
				ma = Pattern.compile("<input type=hidden name=\"partialFolderCount\" value=\"(\\d+?)\">").matcher(response);
				if ( ! ma.find() ) {
					errmsg = "予約に失敗しました。";
					return(false);
				}
				reqr.getHidden_params().put(RETVAL_KEY_PARTIALFOLDERCOUNT, ma.group(1));
			}
			
			// 予約登録を実行する
			{		
				reportProgress(String.format("追加を実行します(%d/%d).",cnt++,cntMax));
				
				String pstr = genPoststrDTB(reqr);
				String uri = "http://"+getIPAddr()+":"+getPortNo()+"/reservepgadd.html";
				
				System.err.println("URL: "+uri+"?"+pstr);
				
				header = response = null;
				for (int i=0; i<retryMax; i++) {
					String[] d = reqPOST(uri, pstr, null);
					header = d[0];
					response = d[1];
					if ( header != null && response == null ) {
						reportProgress("コネクションがリセットされました。リトライします。");
						CommonUtils.milSleep(1000);
					}
					else {
						break;
					}
				}
				if ( header == null || response == null ) {
					errmsg = "レコーダーが反応しません。";
					return(false);
				}
			}
			
			// EDCBに追加された予約IDを検索する　★EDCBは再起動すると予約IDが振り直しになるので必要！！
			{
				reportProgress(String.format("新しい予約IDを番号を取得します(%d/%d).",cnt++,cntMax));
				
				reqr.setId(""); // 予約番号未定のためダミー
				newr = getRsvOnEdcbDT(reqr);
				if ( newr == null ) {
					errmsg = "予約IDがみつかりません。";
					return(false);
				}
				
				// 本体から取得できない情報は引き継ぐ
				copyAttributesMethod(reqr, newr);

				// 一発ヒットしたものはキャッシュ上にないので載せる
				if ( ! getReserves().contains(newr) ) {
					getReserves().add(newr);
				}
				
				reportProgress("+新しい予約ID： "+newr.getId());
			}
		}
		
		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に登録できました。");
		
		return(true);
		
		// 長いよ！分割しる！！
	}

	
	/*******************************************************************************
	 * 予約更新
	 ******************************************************************************/
	
	/**
	 * 予約を更新する（入口）
	 */
	@Override
	public boolean UpdateRdEntry(ReserveList cacher, ReserveList reqr)
	{
		ArrayList<ReserveList> tmprl = getReserves();
		
		boolean b = _UpdateRdEntry(cacher, reqr);
		
		// 予約一覧が更新されていたら、本体から取得できない情報は引き継ぐ
		if ( getReserves() != tmprl ) {
			copyAttributesAllList(tmprl, getReserves());
		}
		
		// 成功しても失敗してもキャッシュが更新されている可能性があるので保存し直す
		ReservesToFile(getReserves(), rsvedFile);
		setRecordedFlag();
		
		return b;
	}
	
	/**
	 * 予約を更新する（本体）
	 */
	private boolean _UpdateRdEntry(ReserveList cacher, ReserveList reqr)
	{
		//
		System.out.println("Run: UpdateRdEntry()");
		
		errmsg = "";
		
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		// 放送局と日付のチェック
		if ( ! PrePostCheck(reqr) ) {
			return(false);
		}
		
		if ( ! getTextRecType(cacher).equals(getTextRecType(reqr)) ) {
			errmsg = String.format("異なる予約方式への更新は行えません（%s->%s）",getTextRecType(cacher),getTextRecType(reqr));
			return false;
		}
		
		boolean rec_type_epg = true;
		if ( getTextRecType(reqr) != null && getTextRecType(reqr).equals(ITEM_REC_TYPE_EPG) ) {
			if ( ContentIdEDCB.isValid(reqr.getContentId()) ) {
				// 正しい番組IDのようですね
			}
			else {
				errmsg = "番組表に予約IDがないためEPG予約は利用できません。EDCB番組表を利用するかプログラム予約を行ってください。";
				return false;
			}
		}
		else {
			rec_type_epg = false;
			// 番組IDが含まれていたら削る
			reqr.setContentId(null);
		}
		
		if ( rec_type_epg ) {
			// EPG予約は普通に更新
			if (getDebug()) System.err.println("[DEBUG] Update EPG RSV");
			return UpdateRdEntryTR(cacher,reqr);
		}
		else {
			
			// プログラム予約の制限のチェック
			if ( ! PreProgResCheck(reqr)) {
				return(false);
			}
			
			// プログラム予約
			if ( isEqualsDate(cacher, reqr) && cacher.getTitle().equals(reqr.getTitle()) ) {
				// 開始・終了日時とタイトルに変更がない場合は更新
				if (getDebug()) System.err.println("[DEBUG] Update PROG RSV");
				return UpdateRdEntryDT2(cacher,reqr);
			}
			else {
				// 開始・終了日時とタイトルに変更がある場合は削除して追加
				if (getDebug()) System.err.println("[DEBUG] Refresh PROG RSV");
				return UpdateRdEntryDT1(cacher,reqr);
			}
		}
	}
	
	/**
	 * EPG予約を更新する
	 */
	private boolean UpdateRdEntryTR(ReserveList cacher, ReserveList reqr) {
		
		//　EDCBで更新する
		int cnt = 1;
		int cntMax = 2;
		
		ReserveList oldr = null;	// 本体にあった既存の予約情報
		ReserveList newr = null;	// 更新後に取得しなおした新しい予約情報
		
		// 予約IDの検索
		{
			reportProgress(String.format("更新するEPG予約の予約IDを取得します(%d/%d).",cnt++,cntMax));
			
			// 予約IDを取得する
			oldr = getRsvOnEdcbTR(cacher);
			if ( oldr == null ) {
				return false;
			}
			
			reportProgress("+更新される予約ID： "+oldr.getId());
		}
		
		// 予約更新
		{		
			reportProgress(String.format("更新を実行します(%d/%d).",cnt++,cntMax));
			
			newr = _UpdateRdEntrySub(oldr, reqr);
			if ( newr == null  ) {
				return false;
			}
			
			// 本体から取得できない情報は引き継ぐ
			copyAttributesMethod(reqr, newr);
			
			// 一発ヒットした場合は予約リストの更新が必要。再取得している場合は不要
			int idx = getReserves().indexOf(cacher);
			if ( idx >= 0 ) {
				getReserves().set(idx,newr);
			}
		}
		
		if ( isModified(cacher, newr) ) {
			// ピンピンうるさいので実行OFFの予約の場合はだんまりで
			errmsg = "EDCB番組表からの情報で内容が変更されました: "+newr.getStartDateTime()+"～"+newr.getZhh()+":"+newr.getZmm()+" "+newr.getTitle();
		}
		
		return true;
	}
	
	/**
	 * プログラム予約を更新する（開始・終了日時とタイトルに変更がない場合は更新）
	 */
	private boolean UpdateRdEntryDT2(ReserveList cacher, ReserveList reqr) {
		
		//　EDCBで更新する
		int cnt = 1;
		int cntMax = 2;
		
		ReserveList oldr = null;	// 本体にあった既存の予約情報
		ReserveList newr = null;	// 更新後に取得しなおした新しい予約情報
		
		// EDCBの更新すべき予約IDを検索する　★EDCBは再起動すると予約IDが振り直しになるので必要！！
		{
			reportProgress(String.format("更新対象の予約を探します(%d/%d).",cnt++,cntMax));
			
			oldr = getRsvOnEdcbDT(cacher);
			if ( oldr == null ) {
				errmsg = "予約一覧に更新対象が見つかりません。";
				return false;
			}
			
			// 予約IDのみ更新
			reportProgress("+更新される予約ID： "+oldr.getId());
		}
		
		// 予約更新
		{
			reportProgress(String.format("プログラム予約を更新します(%d/%d).",cnt++,cntMax));

			newr = _UpdateRdEntrySub(oldr, reqr);
			if ( newr == null  ) {
				return false;
			}
			
			// 本体から取得できない情報は引き継ぐ
			copyAttributesMethod(reqr, newr);
			
			// 一発ヒットした場合は予約リストの更新が必要。再取得している場合は不要
			int idx = getReserves().indexOf(cacher);
			if ( idx >= 0 ) {
				getReserves().set(idx,newr);
			}
		}
		
		return true;
	}
	
	/**
	 * プログラム予約を更新する（開始・終了日時とタイトルに変更がある場合は削除して追加）
	 */
	private boolean UpdateRdEntryDT1(ReserveList cacher, ReserveList reqr) {
		
		reportProgress("プログラム予約を登録し直します.");
		
		// 削除して
		if ( RemoveRdEntry(cacher.getId()) == null ) {
			return(false);
		}
		
		// 追加する
		if ( ! PostRdEntry(reqr) ) {
			errmsg += "予約が削除されたので登録しなおしてください。";
		}
		
		return(true);
	}

	/**
	 * 予約更新の共通部分
	 */
	private ReserveList _UpdateRdEntrySub(ReserveList oldr, ReserveList reqr) {
		
		String pstr = genPoststrDTB(reqr);
		String uri = "http://"+getIPAddr()+":"+getPortNo()+"/reservechg.html?id="+getRsvId(oldr.getId());
		
		System.err.println("URL: "+uri);
		
		String header = null;
		String response = null;
		for (int i=0; i<retryMax; i++) {
			String[] d = reqPOST(uri, pstr, null);
			header = d[0];
			response = d[1];
			if ( header != null && response == null ) {
				reportProgress("コネクションがリセットされました。リトライします。");
				CommonUtils.milSleep(1000);
			}
			else {
				break;
			}
		}
		if ( header == null || response == null ) {
			errmsg = "レコーダーが反応しません。";
			return null;
		}
		if ( ! response.contains("予約を変更しました") ) {
			errmsg = "更新に失敗しました。";
			return null;
		}
		
		// 更新後の情報を再取得
		ReserveList newr = reqr.clone();
		newr.setId(oldr.getId());
		if ( ! getRsvDetailByReserveId(newr,0) ) {
			errmsg = "更新後の情報を取得できませんでした。";
			return null;
		}

		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に更新できました。");
		return newr;
	}
	
	
	/*******************************************************************************
	 * 予約削除
	 ******************************************************************************/
	
	@Override
	public ReserveList RemoveRdEntry(String delid) {
		
		System.out.println("Run: RemoveRdEntry()");
		
		errmsg = "";
		
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		ReserveList delr = null;	// キャッシュ上の削除対象
		ReserveList oldr = null;	// レコーダ上の削除対象
		
		// キャッシュから削除対象を探す
		for ( ReserveList reserve : getReserves() )  {
			if (reserve.getId().equals(delid)) {
				delr = reserve;
				break;
			}
		}
		if (delr == null) {
			return(null);
		}
		
		// EDCBから削除対象を探す　★EDCBは再起動すると予約IDが振り直しになるので必要！！
		reportProgress(String.format("削除する予約IDを取得します."));
		
		if ( ContentIdEDCB.isValid(delr.getContentId()) ) {
			oldr = getRsvOnEdcbTR(delr);
		}
		else {
			oldr = getRsvOnEdcbDT(delr);
		}
		if ( oldr == null ) {
			errmsg = "レコーダ上に削除対象が見つかりません。";
			return(null);
		}
		
		reportProgress("+削除される予約ID： "+oldr.getId());
		
		//　EDCBから削除する
		String header;
		String response;

		{		
			reportProgress("削除を実行します.");
			String uri = "http://"+getIPAddr()+":"+getPortNo()+"/reservedel.html?id="+getRsvId(oldr.getId());
			
			System.err.println("URL: "+uri);
			
			header = response = null;
			for (int i=0; i<retryMax; i++) {
				String[] d = reqPOST(uri, "", null);
				header = d[0];
				response = d[1];
				if ( header != null && response == null ) {
					reportProgress("コネクションがリセットされました。リトライします。");
					CommonUtils.milSleep(1000);
				}
				else {
					break;
				}
			}
			if ( header == null || response == null ) {
				errmsg = "レコーダーが反応しません。";
				return(null);
			}
			if ( ! response.contains("予約を削除しました") ) {
				errmsg = "削除に失敗しました。";
				return(null);
			}
		}
		
		// 予約リストを更新
		getReserves().remove(delr);
		
		// キャッシュに保存（削除なので録画済みフラグは操作しなくてよい）
		ReservesToFile(getReserves(), rsvedFile);
		//setRecordedFlag();
		
		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に削除できました。");
		
		return delr;
	}
	
	
	/*******************************************************************************
	 * 予約用の部品
	 ******************************************************************************/
	
	/**
	 * 放送局と日付のチェック
	 */
	private boolean PrePostCheck(ReserveList r) {
		// 放送局
		r.setChannel(cc.getCH_WEB2CODE(r.getCh_name()));
		if ( r.getChannel() == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}
		
		// 予約パターンID
		// 次回予定日
		// 録画長
		// 開始日時・終了日時
		r.setRec_pattern_id(getRec_pattern_Id(r.getRec_pattern()));
		r.setRec_nextdate(CommonUtils.getNextDate(r));
		r.setRec_min(CommonUtils.getRecMin(r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm()));
		getStartEndDateTime(r);
		if ( r.getRec_pattern_id() != 11 ) {
			errmsg = "日付指定しか利用出来ません。";
			System.out.println(errmsg);
			return(false) ;
		}
		
		return true;
	}
	
	/**
	 * プログラム予約の制限のチェック
	 */
	private boolean PreProgResCheck(ReserveList r) {
		if (
				getTextPittariFlag(r).equals(ITEM_JUST_ENABLE)
			) {
			errmsg = "プログラム予約では次の指定はできません：ぴったり録画＝"+ITEM_JUST_ENABLE;
			return(false) ;
		}
		
		if (
				r.getPursues()
			) {
			errmsg = "プログラム予約では次の指定はできないので規定値に変更しました：番組追従＝"+ITEM_YES;
			r.setPursues(false);
			//return(false) ;
		}
		
		return true;
	}
	
	/**
	 * EDCB予約一覧から操作対象を探すのです（EPG予約版）
	 * @see #isEqualsTR(ReserveList, ReserveList, int)
	 * @see #getRsvOnEdcbDT(ReserveList)
	 */
	private ReserveList getRsvOnEdcbTR(ReserveList origr) {
		
		// 予約IDを試してみる（EDCBがリブートしていなければこれでＯＫなはず）
		if ( origr.getId() != null && origr.getId().length() > 0 ) {
			ReserveList newr = origr.clone();
			if ( getRsvDetailByReserveId(newr,0) && isEqualsTR(origr, newr, -1) ) {
				//System.out.println("+-一致する予約です（予約ID直接）： "+newr.getId());
				return newr;
			}
		}
		
		// 番組IDを試してみる（重複予約されていなければこれで大丈夫なはず）
		if ( origr.getContentId() != null && ContentIdEDCB.isValid(origr.getContentId()) ) {
			ReserveList newr = origr.clone();
			if ( getRsvDetailByContentId(newr,0) && isEqualsTR(origr, newr, 0) ) {
				//System.out.println("+-一致する予約です（番組ID直接）： "+newr.getId());
				return newr;
			}
		}

		// 一覧取得しなおし(3.17.5b)
		ArrayList<ReserveList> rl = new ArrayList<ReserveList>();
		if ( ! getRsvListAPI(rl) ) {
			errmsg = "予約一覧の取得に失敗しました。";
			return null;
		}

		// 置き換えていいよ
		setReserves(rl);

		// 一覧から該当する予約を探す
		int idx=1;
		for ( ReserveList newr : rl ) {
			if ( isEqualsTR(origr, newr, idx++) ) {
				return newr;
			}
		}
		
		errmsg = "予約IDがみつかりません。";
		return null;
	}
	
	/**
	 * 同じEPG予約かどうかしらべる
	 */
	private boolean isEqualsTR(ReserveList origr, ReserveList newr, Integer idx) {
		
		// 放送局が一緒で、開始日時が近いかどうか（旧情報vs一覧）
		if ( ! isLikesRsvOnList(origr, newr) ) {
			return false;
		}
		
		// EPG予約かどうか（詳細）
		if ( ! ContentIdEDCB.isValid(newr.getContentId()) ) {
			if ( idx != null ) System.out.println("+-そっくりですがEPG予約ではありません： "+newr.getId());
			return false;
		}
		
		// おなじ番組かどうか（旧情報vs一覧）
		if ( ! origr.getContentId().equals(newr.getContentId()) ) {
			if ( idx != null ) System.out.println("+-そっくりですが別の番組です： "+newr.getId());
			return false;
		}
		
		// チューナー指定が等しいかどうか（旧情報vs一覧）
		if ( ! origr.getTuner().equals(newr.getTuner()) ) {
			if ( idx != null ) System.out.println("+-そっくりですが異なる予約です（使用するチューナーが異なります）： "+newr.getId());
			return false;
		}
		
		// 時間移動も確認しておくか
		if ( isEqualsDate(origr, newr) ) {
			if ( idx != null ) {
				if ( idx == -1 ) {
					System.out.println("+-一致する予約です（予約ID直接）： "+newr.getId());
				}
				else if ( idx == 0 ) {
					System.out.println("+-一致する予約です（番組ID直接）： "+newr.getId());
				}
				else {
					System.out.println("+-一致する予約です（間接）["+idx+"]： "+newr.getId());
				}
			}
		}
		else {
			System.out.println("+-一致する予約のようです（時間が移動しています）["+idx+"]： "+newr.getId());
		}
		
		return true;
	}
	
	/**
	 * EDCB予約一覧から操作対象を探すのです（プログラム予約版）<BR>
	 * ※一発ヒットしたものはキャッシュに乗らないので注意
	 * @see #isEqualsDT(ReserveList, ReserveList, int)
	 * @see #getRsvOnEdcbTR(ReserveList)
	 */
	private ReserveList getRsvOnEdcbDT(ReserveList origr) {
		
		// 予約IDを試してみる（EDCBがリブートしていなければこれでＯＫなはず）
		if ( origr.getId() != null && origr.getId().length() > 0 ) {
			ReserveList newr = origr.clone();
			if ( getRsvDetailByReserveId(newr,0) && isEqualsDT(origr, newr, 0) ) {
				//System.out.println("+-一致する予約です（番組ID直接）： "+newr.getId());
				return newr;
			}
		}

		// 予約IDは固定ではないので再度取得しなおさないと(3.17.5b変更)
		ArrayList<ReserveList> rl = new ArrayList<ReserveList>();
		if ( ! getRsvListAPI(rl) ) {
			errmsg = "予約一覧の取得に失敗しました。";
			return null;
		}

		// 置き換えていいよ
		setReserves(rl);
		
		// 一覧から該当する予約を探す
		int idx=0;
		for ( ReserveList newr : rl ) {
			if ( isEqualsDT(origr, newr, idx++) ) {
				return newr;
			}
		}
		
		errmsg = "予約IDが見つかりません。";
		return null;
	}
	
	/**
	 * 同じプログラム予約かどうかしらべる
	 */
	private boolean isEqualsDT(ReserveList origr, ReserveList newr, Integer idx) {
		
		// 放送局がいっしょで、開始日時・終了時刻が等しいかどうか（旧情報vs一覧）
		if ( ! isEqualsRsvOnList(origr, newr) ) {
			return false;
		}
		
		// プログラム予約かどうか
		if ( ContentIdEDCB.isValid(newr.getContentId()) ) {
			if ( idx != null ) System.out.println("+-そっくりですがプログラム予約ではありません： "+newr.getId());
			return false;
		}
		
		// チューナー指定が等しいかどうか（旧情報vs一覧）
		if ( ! origr.getTuner().equals(newr.getTuner()) ) {
			if ( idx != null ) System.out.println("+-そっくりですが異なる予約です（使用するチューナーが異なります）： "+newr.getId());
			return false;
		}
		
		if ( idx != null ) {
			if ( idx == 0 ) {
				System.out.println("+-一致する予約です（予約ID直接）： "+newr.getId());
			}
			else {
				System.out.println("+-一致する予約です（間接）["+idx+"]： "+newr.getId());
			}
		}
		
		return true;
	}

	/**
	 * 古い予約一覧から情報を引き継ぐ
	 */
	private void copyAttributesAllList( ArrayList<ReserveList> oldrl, ArrayList<ReserveList> newrl ) {
		
		// 予約一覧の再取得があった
		for ( ReserveList newr : newrl ) {
			if ( newr.getContentId() != null ) {
				for ( ReserveList oldr : oldrl ) {
					if ( oldr.getContentId() != null && isEqualsTR(oldr, newr, null) ) {
						copyAttributesMethod(oldr, newr);
					}
				}
			}
			else {
				for ( ReserveList oldr : oldrl ) {
					if ( oldr.getContentId() == null && isEqualsDT(oldr, newr, null) ) {
						copyAttributesMethod(oldr, newr);
					}
				}
			}
		}
		
	}
	private void copyAttributesMethod( ReserveList oldr, ReserveList newr ) {
		newr.setRec_genre(oldr.getRec_genre());
		newr.setRec_subgenre(oldr.getRec_subgenre());
		setTextPresetID(newr, getTextPresetID(oldr));
	}
	
	
	/* ここまで */
	
	
	

	
	
	/*******************************************************************************
	 * 非公開メソッド
	 ******************************************************************************/

	//
	private String genPoststrEPGA(ReserveList r) {
		StringBuilder sb = new StringBuilder();
		try {
			sb.append("presetID="+text2value(getListPresetID(),getTextPresetID(r))+"&");
			sb.append("onid="+Integer.decode("0x"+r.getContentId().substring(5,9))+"&");
			sb.append("tsid="+Integer.decode("0x"+r.getContentId().substring(9,13))+"&");
			sb.append("sid="+Integer.decode("0x"+r.getContentId().substring(13,17))+"&");
			sb.append("evid="+Integer.decode("0x"+r.getContentId().substring(17,21)));
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	private String genPoststrEPGB(ReserveList r) {
		StringBuilder sb = new StringBuilder();
		sb.append(genPoststrEPGA(r)+"&");
		sb.append(genPoststrCom(r));
		return sb.toString();
	}
	
	//
	private String genPoststrDTCom(ReserveList r) {
		StringBuilder sb = new StringBuilder();
		
		// 日付指定しか対応してないが…
		GregorianCalendar cal = CommonUtils.getCalendar(r.getRec_nextdate().substring(0,10));
		sb.append("sdy="+cal.get(Calendar.YEAR)+"&");
		sb.append("sdm="+(cal.get(Calendar.MONTH)+1)+"&");
		sb.append("sdd="+cal.get(Calendar.DATE)+"&");
		sb.append("sth="+Integer.valueOf(r.getAhh())+"&");
		sb.append("stm="+Integer.valueOf(r.getAmm())+"&");
		//
		if ( r.getAhh().compareTo(r.getZhh()) > 0 ) {
			cal.add(Calendar.DATE, 1);
		}
		sb.append("edy="+cal.get(Calendar.YEAR)+"&");
		sb.append("edm="+(cal.get(Calendar.MONTH)+1)+"&");
		sb.append("edd="+cal.get(Calendar.DATE)+"&");
		sb.append("eth="+Integer.valueOf(r.getZhh())+"&");
		sb.append("etm="+Integer.valueOf(r.getZmm()));
		
		return sb.toString();
	}
	private String genPoststrDTA(ReserveList r) {
		StringBuilder sb = new StringBuilder();
		try {
			//sb.append("presetID="+text2value(getListPresetID(),getTextPresetID(r))+"&");
			sb.append("serviceID="+cc.getCH_WEB2CODE(r.getCh_name())+"&");
			sb.append("pgname="+URLEncoder.encode(r.getTitle(),thisEncoding)+"&");
			sb.append(genPoststrDTCom(r));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}
	private String genPoststrDTB(ReserveList r) {
		StringBuilder sb = new StringBuilder();
		try {
			sb.append("serviceID="+cc.getCH_WEB2CODE(r.getCh_name())+"&");
			sb.append("pgname="+URLEncoder.encode(r.getTitle(),thisEncoding)+"&");
			sb.append(genPoststrDTCom(r)+"&");
			sb.append(genPoststrCom(r));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}
	
	//			
	private String genPoststrCom(ReserveList r) {
		// EPG・プログラム予約共通
		StringBuilder sb = new StringBuilder();
		try {
			sb.append("presetID="+text2value(getListPresetID(),getTextPresetID(r))+"&");
			if ( r.getExec() ) {
				sb.append("recMode="+text2value(getListRecMode(),getTextRecMode(r))+"&");
			}
			else {
				sb.append("recMode="+VALUE_REC_MODE_DISABLE+"&");
			}
			if ( r.getPursues() ) {
				sb.append("tuijyuuFlag="+VALUE_TRACE_ENABLE+"&");
			}
			else {
				sb.append("tuijyuuFlag="+VALUE_TRACE_DISABLE+"&");
			}
			sb.append("priority="+text2value(getListPriority(),getTextPriority(r))+"&");
			sb.append("pittariFlag="+text2value(getListPittariFlag(),getTextPittariFlag(r))+"&");
			sb.append("suspendMode="+text2value(getListSuspendMode(),getTextSuspendMode(r))+"&");
			if ( getTextStartMargine(r).equals(ITEM_MARGINE_DEFAULT) || getTextEndMargine(r).equals(ITEM_MARGINE_DEFAULT) ) {
				sb.append("useDefMargineFlag=1&");
				sb.append("startMargine=0&");
				sb.append("endMargine=0&");
			}
			else {
				sb.append("startMargine="+text2value(getListStartMargine(),getTextStartMargine(r))+"&");
				sb.append("endMargine="+text2value(getListEndMargine(),getTextEndMargine(r))+"&");
			}
			{
				String s = getTextServiceMode(r);
				if ( s.equals(ITEM_SERVMOCE_DEFAULT) ) {
					sb.append("serviceMode=0&");
				}
				else {
					if ( s.equals(ITEM_SERVMOCE_SUBT) || s.equals(ITEM_SERVMOCE_SUBTCARO) ) {
						sb.append("serviceMode_1=0&");
					}
					if ( s.equals(ITEM_SERVMOCE_CARO) || s.equals(ITEM_SERVMOCE_SUBTCARO) ) {
						sb.append("serviceMode_2=0&");
					}
				}
			}
			sb.append("tunerID="+text2value(getListTunerID(),getTextTunerID(r))+"&");
			if ( getTextContinueRecFlag(r).equals(ITEM_YES) ) {
				sb.append("continueRecFlag=1&");
			}
			if ( getTextRebootFlag(r).equals(ITEM_YES) ) {
				sb.append("rebootFlag=1&");
			}
			if ( getTextPartialRecFlag(r).equals(ITEM_YES) ) {
				sb.append("partialRecFlag=1&");
			}
			
			// オプショナル
			{
				String val = r.getHidden_params().get(RETVAL_KEY_RECFOLDERCOUNT);
				sb.append("recFolderCount=");
				sb.append((val!=null)?(val):("0"));
				sb.append("&");
				
				val = r.getHidden_params().get(RETVAL_KEY_PARTIALFOLDERCOUNT);
				sb.append("partialFolderCount=");
				sb.append((val!=null)?(val):("0"));
			}
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}
	
	//
	private void setSettingRecMargin(ArrayList<TextValueSet> tvs) {
		tvs.clear();
		add2tvs(tvs,ITEM_MARGINE_DEFAULT,VALUE_MARGINE_DEFAULT);
		for (int i=0; i<=90; i++) {
			add2tvs(tvs,String.valueOf(i),String.valueOf(i));
		}
	}
	private void setSettingServiceMode(ArrayList<TextValueSet> tvs) {
		tvs.clear();
		add2tvs(tvs,ITEM_SERVMOCE_DEFAULT,VALUE_SERVMOCE_DEFAULT);
		add2tvs(tvs,ITEM_SERVMOCE_SUBT,VALUE_SERVMOCE_SUBT);
		add2tvs(tvs,ITEM_SERVMOCE_CARO,VALUE_SERVMOCE_CARO);
		add2tvs(tvs,ITEM_SERVMOCE_SUBTCARO,VALUE_SERVMOCE_SUBTCARO);
		add2tvs(tvs,ITEM_SERVMOCE_NONE,VALUE_SERVMOCE_NONE);
	}
	private void setSettingNoYes(ArrayList<TextValueSet> tvs) {
		tvs.clear();
		add2tvs(tvs,ITEM_NO,VALUE_NO);
		add2tvs(tvs,ITEM_YES,VALUE_YES);
	}
	private void setSettingRecType(ArrayList<TextValueSet> tvs) {
		tvs.clear();
		add2tvs(tvs,ITEM_REC_TYPE_EPG,VALUE_REC_TYPE_EPG);
		add2tvs(tvs,ITEM_REC_TYPE_PROG,VALUE_REC_TYPE_PROG);
	}
	//
	private void setSettingChCodeValue(ArrayList<TextValueSet> tvsValue, ArrayList<TextValueSet> tvsType, String key, String res) {
		
		HashMap<String, String> typ = new HashMap<String, String>();
		typ.put("地デジ", "uvd");
		typ.put("BS", "bsd");
		typ.put("","csd");
		
		tvsValue.clear();
		tvsType.clear();
		Matcher mb = Pattern.compile("<select name=\""+key+"\">([\\s\\S]+?)</select>").matcher(res);
		if (mb.find()) {
			Matcher mc = Pattern.compile("<option value=\"([^\"]*?)\"(\\s*selected\\s*)?>(.+?)\\((.+?)\\)\\n").matcher(mb.group(1));
			while (mc.find()) {
				// ワンセグは対象外
				if (mc.group(4).equals("ワンセグ")) {
					continue;
				}
				
				String chname = mc.group(3);
				Long chid = 0L;
				
				// 難視聴対策放送
				try {
					chid = Long.valueOf(mc.group(1));
					Long tsid = (chid & 0x0000FFFF0000L);
					if ( tsid == 0x000043100000L || tsid == 0x000043110000L ) {
						chname = TEXT_NANSHICHO_HEADER+chname;
						System.out.println("[DEBUG] "+chname);
					}
				}
				catch (NumberFormatException e) {
					continue;
				}
				
				// 重複する放送局名は対象外
				boolean dup = false;
				for ( TextValueSet t : tvsValue ) {
					if ( t.getText().equals(chname) ) {
						dup = true;
					}
				}
				if (dup) {
					continue;
				}
				
				// 放送局ID順でソートして追加
				{
					int n = 0;
					for ( ; n<tvsValue.size(); n++ ) {
						if ( Long.valueOf(tvsValue.get(n).getValue()) > Long.valueOf(chid) ) {
							break;
						}
					}
					add2tvs(n,tvsValue,chname,mc.group(1));
				}

				// 地デジでもBSでもないものは全部CSあつかい
				{
					String val = typ.get(mc.group(4));
					if (val == null) {
						val = typ.get("");
					}
					add2tvs(tvsType,chname,val);
				}
			}
		}
		
		System.err.println("=== CHコード一覧 for EDCB ===");
		System.err.println("放送局　：　\"レコーダの放送局名\",\"放送局コード\"");
		System.err.println("=============================");
		
		for ( TextValueSet tv : tvsValue ) {
			System.err.println(String.format("%-20s : \"%s\",\"%s\"",tv.getText(),tv.getValue(),tv.getValue()));
		}
		System.err.println("=============================");
	}
	//
	protected void setSettingEtc(ArrayList<TextValueSet> tvs, String key, int typ, String res) {
		tvs.clear();
		Matcher mb = Pattern.compile("<select name=\""+key+"\">([\\s\\S]+?)</select>").matcher(res);
		if (mb.find()) {
			Matcher mc = Pattern.compile("<option value=\"([^\"]*?)\"(\\s*selected\\s*)?>(.*?)\\n").matcher(mb.group(1));
			while (mc.find()) {
				TextValueSet t = add2tvs(tvs,mc.group(3),mc.group(1));
				if (mc.group(2) != null) {
					t.setDefval(true);
				}
			}
		}
	}
	
	// 予約情報同志を比較する
	private boolean isModified(ReserveList o, ReserveList r) {
		return ! (isEqualsDate(o, r) && o.getTitle().equals(r.getTitle()));
	}
	private boolean isEqualsRsvOnDetail(ReserveList o, ReserveList r) {
		// ここまで一緒なら同じ予約情報だろ
		return (isEqualsRsvOnList(o, r) && o.getTuner().equals(r.getTuner()));
	}
	private boolean isEqualsRsvOnList(ReserveList o, ReserveList r) {
		// ここまで一緒なら同じ予約情報だろ
		return (isEqualsDate(o, r) && o.getChannel().equals(r.getChannel()));
	}
	private boolean isEqualsDate(ReserveList o, ReserveList r) {
		return (o.getStartDateTime().equals(r.getStartDateTime()) && o.getEndDateTime().equals(r.getEndDateTime()));
	}
	private boolean isLikesRsvOnList(ReserveList o, ReserveList r) {
		// ここまで一緒なら親戚の予約情報だろ
		return (isLikesDate(o, r) && o.getChannel().equals(r.getChannel()));
	}
	private boolean isLikesDate(ReserveList o, ReserveList r) {
		return (CommonUtils.getDiffDateTime(o.getStartDateTime(), r.getStartDateTime()) < likersvrange);
	}
}
