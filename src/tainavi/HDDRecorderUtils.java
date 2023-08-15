package tainavi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * {@link HDDRecorder}インタフェース をインプルメントしたレコーダプラグインのクラスで利用できる、共有部品の集合です。
 * @version 3.15.4β クラス名を RecorderUtils から HDDRecorderUtils に変更しました。
 */
public class HDDRecorderUtils implements HDDRecorder,Cloneable {

	/*******************************************************************************
	 * ディープコピーが意外と大変
	 ******************************************************************************/

	public HDDRecorderUtils clone() {
		try {
			HDDRecorderUtils ru = (HDDRecorderUtils) super.clone();
			FieldUtils.deepCopy(ru, this); // ディープコピーするよ
			return ru;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}

	/*******************************************************************************
	 * オプション確認
	 ******************************************************************************/

	@Override
	public boolean isReserveListSupported() { return true; }
	@Override
	public boolean isThereAdditionalDetails() { return false; }
	@Override
	public boolean isEditAutoReserveSupported() { return false; }
	@Override
	public boolean isRecordedListSupported() { return false; }
	@Override
	public boolean isTitleListSupported() { return false; }
	@Override
	public boolean isRepeatReserveSupported() { return true; }
	@Override
	public boolean isPursuesEditable() { return false; }
	@Override
	public boolean isAutocompleteSupported() { return true; }
	@Override
	public boolean isChangeChannelSupported() { return ChangeChannel(null); }
	@Override
	public boolean isBackgroundOnly() { return false; }
	@Override
	public boolean isChValueAvailable() { return false; }
	@Override
	public boolean isChCodeNeeded() { return true; }
	@Override
	public boolean isRecChNameNeeded() { return true; }
	@Override
	public boolean isBroadcastTypeNeeded() { return false; }
	@Override
	public boolean isAutoEncSelectEnabled() { return true; }
	@Override
	public boolean isPortableSupported() { return false; }
	@Override
	public boolean isFolderCreationSupported() { return false; }
	@Override
	public boolean isThereTitleDetails() { return false; }
	@Override
	public String getChDatHelp() { return ""; }

	// フリーテキストによるオプション指定
	@Override
	public boolean setOptString(String s) { return true; }		// ダミー
	@Override
	public String getOptString() { return null; }	// ダミー


	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	public static final String NULL_ENCODER = "■";

	// メッセージID
	private static final String MSGID = "[レコーダ共通] ";
	private static final String DBGID = "[DEBUG]"+MSGID;
	private static final String ERRID = "[ERROR]"+MSGID;


	/*******************************************************************************
	 * メンバ変数関連
	 ******************************************************************************/

	// デバッグログを出力するか
	public void setDebug(boolean b) { debug = b; }
	protected boolean getDebug() { return debug; }
	private boolean debug = false;

	// 終了時刻と開始時刻が重なる番組を重複扱いするか
	public void setAdjNotRep(boolean b) { adjnotrep = b; }
	private boolean getAdjNotRep() { return adjnotrep; }
	private boolean adjnotrep = false;

	// カレンダー連携を行うかどうか
	public void setUseCalendar(boolean b) { usecalendar = b; }
	public boolean getUseCalendar() { return usecalendar; }
	private boolean usecalendar = true;

	// チャンネル操作を行うかどうか
	public void setUseChChange(boolean b) { usechchange = b; }
	public boolean getUseChChange() { return usechchange; }
	private boolean usechchange = true;

	// 録画完了チェックの範囲
	public void setRecordedCheckScope(int n) { recordedCheckScope = n; }
	protected int getRecordedCheckScope() { return recordedCheckScope; }
	private int recordedCheckScope = 14;

	// 録画結果一覧の保存期間
	public void setRecordedSaveScope(int n) { recordedSaveScope = n; }
	protected int getRecordedSaveScope() { return recordedSaveScope; }
	private int recordedSaveScope = 90;

	// HTTPアクセス時のUser-Agent
	public void setUserAgent(String s) { userAgent = s; }
	//public String getUserAgent() { return userAgent; }
	private String userAgent = "";


	/*******************************************************************************
	 * レコーダーの固有情報
	 ******************************************************************************/

	public String Myself() { return(getIPAddr()+":"+getPortNo()+":"+getRecorderId()); }
	public boolean isMyself(String myself) { return Myself().equals(myself); }

	protected ArrayList<TextValueSet> vrate = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> arate = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> folder = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> encoder = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> dvdcompat = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> device = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> channel = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> xchapter = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> mschapter = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> mvchapter = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> chvalue = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> chtype = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> genre = new ArrayList<TextValueSet>();

	protected ArrayList<TextValueSet> aspect = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> bvperf = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> lvoice = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> autodel = new ArrayList<TextValueSet>();
	protected ArrayList<TextValueSet> portable = new ArrayList<TextValueSet>();

	public ArrayList<TextValueSet> getVideoRateList() { return(vrate); }
	public ArrayList<TextValueSet> getAudioRateList() { return(arate); }
	public ArrayList<TextValueSet> getFolderList() { return(folder); }
	public ArrayList<TextValueSet> getEncoderList() { return(encoder); }
	public ArrayList<TextValueSet> getDVDCompatList() { return(dvdcompat); }
	public ArrayList<TextValueSet> getDeviceList() { return(device); }
	public ArrayList<TextValueSet> getXChapter() { return(xchapter); }
	public ArrayList<TextValueSet> getMsChapter() { return(mschapter); }
	public ArrayList<TextValueSet> getMvChapter() { return(mvchapter); }
	public ArrayList<TextValueSet> getChValue() { return(chvalue); }
	public ArrayList<TextValueSet> getChType() { return(chtype); }

	public ArrayList<TextValueSet> getAspect() { return(aspect); }
	public ArrayList<TextValueSet> getBVperf() { return(bvperf); }
	public ArrayList<TextValueSet> getLVoice() { return(lvoice); }
	public ArrayList<TextValueSet> getAutodel() { return(autodel); }
	public ArrayList<TextValueSet> getPortable(){ return(portable); }

	public String getLabel_Videorate() { return null; }
	public String getLabel_Audiorate() { return null; }
	public String getLabel_Folder() { return null; }
	public String getLabel_Device() { return null; }
	public String getLabel_DVDCompat() { return null; }
	public String getLabel_XChapter() { return null; }
	public String getLabel_MsChapter() { return null; }
	public String getLabel_MvChapter() { return null; }
	public String getLabel_Aspect() { return null; }
	public String getLabel_BVperf() { return null; }
	public String getLabel_LVoice() { return null; }
	public String getLabel_Autodel() { return null; }
	public String getLabel_Portable(){ return null; }

	/*******************************************************************************
	 * レコーダー操作のための固有情報
	 ******************************************************************************/

	public String getRecorderId() { return "THIS IS DUMMY METHOD"; }

	private String recorderIPAddr = "";
	private String recorderPortNo = "";
	private String recorderUser = "";
	private String recorderPasswd = "";
	private String recorderMacAddr = "";
	private String recorderBroadcast = "";
	private int recorderTunerNum = 0;
	private String recorderName = "";

	public String getIPAddr() { return recorderIPAddr; }
	public void setIPAddr(String s) { recorderIPAddr = s; }
	public String getPortNo() { return recorderPortNo; }
	public void setPortNo(String s) { recorderPortNo = s; }
	public String getUser() { return recorderUser; }
	public void setUser(String s) { recorderUser = s; }
	public String getPasswd() { return recorderPasswd; }
	public void setPasswd(String s) { recorderPasswd = s; }
	public String getMacAddr() { return recorderMacAddr; }
	public void setMacAddr(String s) { recorderMacAddr = s; }
	public String getBroadcast() { return recorderBroadcast; }
	public void setBroadcast(String s) { recorderBroadcast = s; }
	public int getTunerNum() { return recorderTunerNum; }
	public void setTunerNum(int n) { recorderTunerNum = n; }
	public String getDispName(){ return recorderName.equals("") ? Myself() : recorderName; }
	public String getName(){ return recorderName; }
	public void setName(String s) { recorderName = s; }

	// エンコーダ別の配色を取得する
	public String getColor(String tuner) {
		//
		if (tuner != null && tuner.length() != 0) {
			int idx = 0;
			for (TextValueSet e : getEncoderList()) {
				if (tuner.equals(e.getText())) {
					if (recorderColorList.size() > idx) {
						return recorderColorList.get(idx);
					}
					break;
				}
				idx++;
			}
		}
		// 適当なエンコーダが見つからない場合は既定値
		return recorderColorList.get(0);
	}

	// Envからもらってきたものを分解して保持する
	public void setColor(String s) {
		//
		recorderColorList.clear();
		//
		Matcher ma = null;
		ma = Pattern.compile("^#......$").matcher(s);
		if (ma.find()) {
			recorderColorList.add(s);
		}
		else {
			ma = Pattern.compile("(#......);").matcher(s);
			while (ma.find()) {
				recorderColorList.add(ma.group(1));
			}
		}
		return;
	}

	public ArrayList<String> getColors() { return recorderColorList; }

	private ArrayList<String> recorderColorList = new ArrayList<String>();

	/**
	 * 特定の予約を決め打ちで
	 */
	public ReserveList getReserveList(String rsvId) {
		for ( ReserveList rsv : RESERVES ) {
			if ( rsv.getId() != null && rsv.getId().equals(rsvId) ) {
				return rsv;
			}
		}
		return null;
	}

	/**
	 * 持っている予約をすべて…吐き出させるっ…！
	 */
	public ArrayList<ReserveList> getReserves() { return RESERVES; }

	private ArrayList<ReserveList> RESERVES = new ArrayList<ReserveList>();

	/**
	 * 自動予約一覧
	 */
	public AutoReserveInfoList getAutoReserves() { return AUTORESERVES; }
	private AutoReserveInfoList AUTORESERVES = new AutoReserveInfoList(Env.envDir,null,"dummy","0.0.0.0","0");

	/**
	 * 録画済み一覧
	 */
	public ArrayList<RecordedInfo> getRecorded() { return RECORDED; }
	protected void setRecorded(ArrayList<RecordedInfo> r ) { RECORDED = r; }

	private ArrayList<RecordedInfo> RECORDED = new ArrayList<RecordedInfo>();

	/**
	 * 特定のタイトルを決め打ちで
	 */
	public TitleInfo getTitleInfo(String id) {
		for ( TitleInfo info : TITLES ) {
			if ( info.getId() != null && info.getId().equals(id) ) {
				return info;
			}
		}
		return null;
	}

	/**
	 * 持っているタイトルをすべて…吐き出させるっ…！
	 */
	public ArrayList<TitleInfo> getTitles() { return TITLES; }

	private ArrayList<TitleInfo> TITLES = new ArrayList<TitleInfo>();

	/**
	 * タイトル一覧をセットする
	 * @param t セットするタイトル一覧
	 **/
	public void setTitles(ArrayList<TitleInfo> t) {
		TITLES = t;
	}

	/**
	 * タイトル一覧をキャッシュファイルに保存する
	 *
	 * @param titles 保存対象のタイトル一覧
	 * @param titleFile	保存先のファイル名
	 * @see #TitlesFromFile(String)
	 */
	protected void TitlesToFile(ArrayList<TitleInfo> titles, String titleFile) {
		if ( ! CommonUtils.writeXML(titleFile, titles) ) {
        	System.err.println("タイトルキャッシュの保存に失敗しました: "+titleFile);
		}
	}

	/**
	 * レコーダのタイトル一覧をキャッシュファイルから取得する
	 * @param titleFile 取得元のキャッシュファイル
	 * @return nullは返さない！
	 * @see #TitlesToFile(ArrayList, String)
	 */
	protected ArrayList<TitleInfo> TitlesFromFile(String titleFile) {

		File f = new File(titleFile);
		if ( ! f.exists() ) {
	        System.out.println("+タイトルキャッシュはありません: "+titleFile);
	        return new ArrayList<TitleInfo>();
		}

		@SuppressWarnings("unchecked")
		ArrayList<TitleInfo> tmp = (ArrayList<TitleInfo>) CommonUtils.readXML(titleFile);
		if ( tmp == null ) {
        	System.err.println("タイトルキャッシュの読み込みに失敗しました: "+titleFile);
        	return new ArrayList<TitleInfo>();
		}

        System.out.println("+タイトルキャッシュを読み込みました("+tmp.size()+"件): "+titleFile);
        return tmp;
	}

	/**
	 * デバイス情報
	 */
	private ArrayList<DeviceInfo> DEVICES = new ArrayList<DeviceInfo>();

	public ArrayList<DeviceInfo> getDeviceInfos() { return DEVICES; }
	public void setDeviceInfos(ArrayList<DeviceInfo> di){ DEVICES = di; }

	public DeviceInfo GetRDDeviceInfo(String device_id){
		for ( DeviceInfo info : DEVICES ) {
			if ( info.getId() != null && info.getId().equals(device_id) ) {
				return info;
			}
		}
		return null;
	}

	protected String DEVICE_ID;
	public String getCurrentDeviceID(){ return DEVICE_ID; }
	public void setCurrentDeviceID(String id){ DEVICE_ID = id; }

	/**
	 * デバイス名からデバイス情報を取得します。
	 */
	@Override
	public DeviceInfo GetRDDeviceInfoFromName(String device_name){
		for ( DeviceInfo info : DEVICES ) {
			if ( info.getName() != null && info.getName().equals(device_name) ) {
				return info;
			}
		}
		return null;
	}

	/**
	 * デバイス名を取得します。
	 */
	public String getDeviceName(String device_id){
		return value2text(device, device_id);
	}

	/*
	 * デバイス名からデバイスIDを取得します。
	 */
	public String getDeviceID(String device_name){
		return text2value(device, device_name);
	}

	/**
	 * レコーダのデバイリストをキャッシュファイルに保存する
	 * @param devices	保存対象のデバイスリスト
	 * @param deviceFile	保存先のキャッシュファイル
	 * @see #DeviceInfosFromFile(String)
	 */
	protected void DeviceInfosToFile(ArrayList<DeviceInfo> devices, String deviceFile) {
		if ( ! CommonUtils.writeXML(deviceFile, devices) ) {
        	System.err.println("デバイスキャッシュの保存に失敗しました: "+deviceFile);
		}
	}

	/**
	 * レコーダのデバイスリストをキャッシュファイルから取得する
	 * @param deviceFile	取得元のキャッシュファイル
	 * @return nullは返さない！
	 * @see #DeviceInfosToFile(ArrayList, String)
	 */
	protected ArrayList<DeviceInfo> DeviceInfosFromFile(String deviceFile) {

		File f = new File(deviceFile);
		if ( ! f.exists() ) {
	        System.out.println("+デバイスキャッシュはありません: "+deviceFile);
	        return new ArrayList<DeviceInfo>();
		}

		@SuppressWarnings("unchecked")
		ArrayList<DeviceInfo> tmp = (ArrayList<DeviceInfo>) CommonUtils.readXML(deviceFile);
		if ( tmp == null ) {
        	System.err.println("デバイスキャッシュの読み込みに失敗しました: "+deviceFile);
        	return new ArrayList<DeviceInfo>();
		}

        System.out.println("+デバイスキャッシュを読み込みました("+tmp.size()+"件): "+deviceFile);
        return tmp;
	}

	/***************************************
	 * 利用可能なエンコーダの絞り込み２種
	 **************************************/

	/**
	 *
	 */
	@Override
	public ArrayList<String> getFilteredEncoders(String webChName) {

		ArrayList<String> encs = new ArrayList<String>();

		if ( getEncoderList().size() == 0 ) {
			encs.add(NULL_ENCODER);
			return encs;
		}

		// エンコーダーに地上波・BS/CSの区別のあるとき
		if ( isBroadcastTypeNeeded() ) {

			String code = getChCode().getCH_WEB2CODE(webChName);

			if ( code != null ) {
				for ( TextValueSet enc : getEncoderList() ) {
					if (
							(code.startsWith(BroadcastType.TERRA.getName()+":") && enc.getText().startsWith("地上")) ||
							((code.startsWith(BroadcastType.BS.getName()+":")||code.startsWith(BroadcastType.CS.getName()+":")) && enc.getText().startsWith("BS")) ||
							(code.startsWith(BroadcastType.CAPTURE.getName()+":") && enc.getText().startsWith("キャプチャ")) ) {
						encs.add(enc.getText());
					}
				}
			}
			if ( encs.size() > 0 ) {
				return encs;
			}
		}

		// エンコーダーに地上波・BS/CSの区別のないとき or フィルタ結果が０件のとき
		for ( TextValueSet enc : getEncoderList() ) {
			encs.add(enc.getText());
		}
		return encs;
	}

	@Override
	public String getEmptyEncorder(String webChName, String startDateTime, String endDateTime, ReserveList myrsv, String vardiaVrate) {

		// エンコーダの一覧を作成する
		ArrayList<String> encs = getFilteredEncoders(webChName);

		// 旧RDデジ系かどうか確認する（R1/R2以外のRDかどうか調べる）
		boolean isOldVARDIA = false;
		if ( isRD() ) {
			String vv = null;
			for ( String enc : encs ) {
				if ( ! enc.matches("^R\\d$") ) {
					vv = vardiaVrate;
					isOldVARDIA = true;
					break;
				}
			}
			vardiaVrate = vv;
		}

		// 予約リストをなめて予約済みエンコーダーを探しつつ、裏番組リストも作る
		urabanlist = new ArrayList<ReserveList>();
		String rsvedTuner = null;
		for ( ReserveList r : getReserves() ) {
			if ( r == myrsv ) {
				// 自分自身は排除（予約一覧から開いたときとかに使う）
				continue;
			}
			if ( ! r.getExec() ) {
				// 無効の物はいらない
				continue;
			}

			// 予約時間が重なるものを抽出する
			ArrayList<String> starts = new ArrayList<String>();
			ArrayList<String> ends = new ArrayList<String>();
			CommonUtils.getStartEndList(starts, ends, r);
			for ( int i=0;i<starts.size(); i++ ) {
				// 既に予約済みの場合
				if (
						starts.get(i).equals(startDateTime) &&
						ends.get(i).equals(endDateTime) &&
						webChName.equals(r.getCh_name())
						) {
					rsvedTuner = r.getTuner();
					continue;
				}

				// 時間の重なる番組
				if ( CommonUtils.isOverlap(startDateTime, endDateTime, starts.get(i), ends.get(i), getAdjNotRep()) ) {

					// 裏番組チェック
					if ( ! urabanlist.contains(r) ) {
						urabanlist.add(r);
					}

					// 予約時間が重なるものはエンコーダーの一覧から削除する
					HashMap<String,Boolean> removeitems = new HashMap<String,Boolean>();
					for ( String enc : encs ) {
						if ( enc.equals(r.getTuner()) ) {

							removeitems.put(enc, true);

							// ---- ＲＤデジタルＷ録向け暫定コード ----
							if ( enc.equals("TS1") || enc.equals("DR1") ) {
								// TS1が埋まっていればREは使えない
								removeitems.put("RE", true);
							}
							else if ( enc.equals("RE") ) {
								// REが埋まっていればTS1は使えない
								removeitems.put("TS1", true);
								removeitems.put("DR1", true);
							}
							// ---- ＲＤデジタルＷ録向け暫定コード ----

							break;
						}
					}
					for ( String key : removeitems.keySet() ) {
						encs.remove(key);
					}
				}
			}
		}

		if ( ! isAutoEncSelectEnabled() ) {
			// 空きエンコーダ検索は無効
			return null;
		}

		// 旧RDデジ系 - ここから
		if ( isOldVARDIA ) {
			if (vardiaVrate == null)
				return "";

			return getOldVARDIAEmpEnc(encs, vardiaVrate);
		}
		// 旧RDデジ系 - ここまで

		if ( encs.size() == 0  ) {
			// 空きエンコーダはなかった
			return "";
		}

		// 空きエンコーダがあった

		if ( rsvedTuner != null ) {
			// 予約済みなら同じのでいいよね
			return rsvedTuner;
		}
		if ( encs.size() > 0 ) {
			// エンコーダーが残っていればそれらの先頭を返す（裏番組がない場合は除く）
			return encs.get(0);
		}

		// 空きエンコーダなし
		return "";
	}

	@Override
	public ArrayList<ReserveList> getUrabanList() {
		return urabanlist;
	}

	private ArrayList<ReserveList> urabanlist = null;	// 裏番組の一覧


	/***************************************
	 * R1/R2に統合されていない旧RDデジ系用の部品２種
	 **************************************/

	/**
	 * チューナーにあった画質を拾ってみる
	 */
	public String getPreferredVrate_VARDIA(String tuner) {

		if ( ! isOldVARDIA() ) {
			return null;
		}

		if ( tuner.startsWith("TS") ) {
			// TS1/2では画質に[TS]系列を選ぶ
			return getAppropriateVrate("[TS]",null);
		}
		else if ( tuner.startsWith("DR") ) {
			// DR1/2では画質に[DR]を選ぶ
			return getAppropriateVrate("[DR]",null);
		}
		else if ( tuner.startsWith("RE") ) {
			// REでは画質に[TSE]または[AVC]系列を選ぶ
			return getAppropriateVrate("[TSE] ","[AVC] ");
		}

		return "";
	}

	private String getAppropriateVrate(String vrate1, String vrate2) {
		for ( TextValueSet tv : getVideoRateList() ) {
			String vrate = tv.getText();
			if ( vrate1 != null && vrate.startsWith(vrate1) ) {
				return vrate;
			}
			if ( vrate2 != null && vrate.startsWith(vrate2) ) {
				return vrate;
			}
		}
		return null;
	}

	/**
	 * 画質にあったチューナーだけ拾ってみる
	 */
	public ArrayList<TextValueSet> getPreferredTuners_VARDIA(String vrate) {

		if ( ! isOldVARDIA() ) {
			return null;
		}

		ArrayList<TextValueSet> encs = new ArrayList<TextValueSet>();

		for ( TextValueSet tv : getEncoderList() ) {
			String enc = tv.getText();
			if ( vrate.equals("[TS]") ) {
				if ( enc.startsWith("TS") ) {
					encs.add(tv);
				}
			}
			else if ( vrate.equals("[DR]") ) {
				if ( enc.startsWith("DR") ) {
					encs.add(tv);
				}
			}
			else {
				// TSE or AVC or VR
				if ( enc.equals("RE") || enc.equals("VR") ) {
					encs.add(tv);
				}
			}
		}

		return encs;
	}

	private String getOldVARDIAEmpEnc(ArrayList<String> encs, String vrate) {
		if ( vrate.equals("[TS]") ) {
			for ( String enc : encs ) {
				if ( enc.startsWith("TS") ) {
					return enc;
				}
			}
		}
		else if ( vrate.equals("[DR]") ) {
			for ( String enc : encs ) {
				if ( enc.startsWith("DR") ) {
					return enc;
				}
			}
		}
		else {
			// TSE or AVC or VR
			for ( String enc : encs ) {
				if ( enc.equals("RE") || enc.equals("VR") ) {
					return enc;
				}
			}
		}
		return "";
	}

	/**
	 * R1/R2に統合されていない古いRD(VARDIA・REGZA RD)かどうか調べるかどうか調べる
	 */
	public boolean isOldVARDIA() {
		return ( getRecorderId().startsWith("VARDIA RD-") || getRecorderId().startsWith("REGZA RD-") );
	}

	/**
	 * RDかどうか調べる
	 */
	public boolean isRD() {
		return ( getRecorderId().startsWith("RD-") || getRecorderId().startsWith("VARDIA RD-") || getRecorderId().startsWith("REGZA RD-") || getRecorderId().startsWith("REGZA DBR-Z") );
	}


	/*******************************************************************************
	 * 小物
	 ******************************************************************************/

	// 素直にHashMapつかっておけばよかった
	@Override
	public String text2value(ArrayList<TextValueSet> tvs, String text) {
		for ( TextValueSet t : tvs ) {
			if (t.getText().equals(text)) {
				return(t.getValue());
			}
		}
		return("");
	}
	@Override
	public String value2text(ArrayList<TextValueSet> tvs, String value) {
		for ( TextValueSet t : tvs ) {
			if (t.getValue().equals(value)) {
				return(t.getText());
			}
		}
		return("");
	}

	@Override
	public TextValueSet getDefaultSet(ArrayList<TextValueSet> tvs) {
		for ( TextValueSet t : tvs ) {
			if ( t.getDefval() ) {
				return t;
			}
		}
		return null;
	}

	protected TextValueSet add2tvs(ArrayList<TextValueSet> tvs, String text, String value) {
		TextValueSet t = new TextValueSet();
		t.setText(text);
		t.setValue(value);
		tvs.add(t);
		return t;
	}
	protected TextValueSet add2tvs(int n, ArrayList<TextValueSet> tvs, String text, String value) {
		TextValueSet t = new TextValueSet();
		t.setText(text);
		t.setValue(value);
		tvs.add(n,t);
		return t;
	}

	// 予約日付をId化する（単日以外）
	protected int getRec_pattern_Id(String s) {
		int i = 0;
		for (; i<HDDRecorder.RPTPTN.length;i++) {
			//System.out.println(s + "->" + HDDRecorder.RPTPTN[i]);
			if (s.equals(HDDRecorder.RPTPTN[i])) {
				return(i);
			}
		}
		return(i);
	}

	/* 予約IDが動的に変化するレコーダ向けの処理 */
	private int rsvcnt = 0;
	protected String getUniqId(String rsvId) { return (!rsvId.startsWith("U$"))?(String.format("U$%14s,%05d,%s",CommonUtils.getDateTimeYMD(0),(rsvcnt++)%100000,rsvId)):(rsvId); }
	protected String getRsvId(String uniqId) { return (uniqId.startsWith("U$"))?(uniqId.substring(23)):(uniqId); }


	// 開始日時・終了日時を算出する
	public void getStartEndDateTime(ReserveList r) {
		//
		GregorianCalendar c = CommonUtils.getCalendar(r.getRec_nextdate());
		if ( c != null ) {
			// ★★★　MM/DDをYYYY/MM/DDに戻す？　★★★
			c.set(Calendar.MINUTE, Integer.valueOf(r.getAmm()));
			c.set(Calendar.HOUR_OF_DAY, Integer.valueOf(r.getAhh()));
			r.setStartDateTime(CommonUtils.getDateTime(c));

			c.add(Calendar.MINUTE, Integer.valueOf(r.getRec_min()));
			r.setEndDateTime(CommonUtils.getDateTime(c));
		}
	}

	//
	public String[] _mmdd2yyyymmdd(String mm, String dd)
	{
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(new Date());
		if ( Integer.valueOf(mm) < c.get(Calendar.MONTH)+1 ) {
			c.add(Calendar.YEAR, 1);
		}

		return (new String[] { String.format("%04d",c.get(Calendar.YEAR)), mm, dd });
	}

	//
	public String[] _hhmm2hhmm_min(String ahhmm, String zhhmm)
	{
		String ahh="";
		String amm="";
		String zhh="";
		String zmm="";

		Matcher ma = Pattern.compile("^(\\d+):(\\d+)").matcher(ahhmm);
		if (ma.find()) {
			ahh = String.format("%02d",Integer.valueOf(ma.group(1)));
			amm = String.format("%02d",Integer.valueOf(ma.group(2)));
		}

		ma = Pattern.compile("^(\\d+):(\\d+)").matcher(zhhmm);
		if (ma.find()) {
			zhh = String.format("%02d",Integer.valueOf(ma.group(1)));
			zmm = String.format("%02d",Integer.valueOf(ma.group(2)));
		}

		int min = Integer.valueOf(zhh)*60+Integer.valueOf(zmm) - (Integer.valueOf(ahh)*60+Integer.valueOf(amm));
		if ( min < 0 ) min += 24*60;

		return (new String[] {ahh, amm, zhh, zmm, Integer.toString(min)});
	}

	// レコーダの設定情報をキャッシュする
	public ArrayList<TextValueSet> TVSload(String filename) {
		File f = new File(filename);
		if ( ! f.exists() ) {
	        return new ArrayList<TextValueSet>();
		}

		@SuppressWarnings("unchecked")
		ArrayList<TextValueSet> ar = (ArrayList<TextValueSet>) CommonUtils.readXML(filename);
		if ( ar == null ) {
        	System.err.println("設定ファイルの読み込みに失敗しました: "+filename);
	        return new ArrayList<TextValueSet>();
		}

		return ar;
	}
	public void TVSsave(ArrayList<TextValueSet> ar, String filename) {
		if ( ! CommonUtils.writeXML(filename, ar) ) {
        	System.err.println("設定ファイルの保存に失敗しました: "+filename);
		}
	}

	/**
	 * レコーダの予約リストをキャッシュする
	 * @param rsvedFile
	 * @return nullは返さない！
	 * @see #ReservesToFile(ArrayList, String)
	 */
	protected ArrayList<ReserveList> ReservesFromFile(String rsvedFile) {
		File f = new File(rsvedFile);
		if ( ! f.exists() ) {
	        System.out.println("+予約キャッシュはありません: "+rsvedFile);
	        return new ArrayList<ReserveList>();
		}

		@SuppressWarnings("unchecked")
		ArrayList<ReserveList> tmp = (ArrayList<ReserveList>) CommonUtils.readXML(rsvedFile);
		if ( tmp == null ) {
        	System.err.println("予約キャッシュの読み込みに失敗しました: "+rsvedFile);
        	return new ArrayList<ReserveList>();
		}

		/* もういらんやろ
        // 後方互換
        for (ReserveList r : tmp) {
        	if (r.getId() == null && r.getNo() > 0) {
        		r.setId(String.valueOf(r.getNo()));
        	}
        }
        */

        System.out.println("+予約キャッシュを読み込みました("+tmp.size()+"): "+rsvedFile);
        return tmp;
	}

	/**
	 * @param reserves
	 * @param rsvedFile
	 * @see #ReservesFromFile(String)
	 */
	protected void ReservesToFile(ArrayList<ReserveList> reserves, String rsvedFile) {
		if ( ! CommonUtils.writeXML(rsvedFile, reserves) ) {
        	System.err.println("予約キャッシュの保存に失敗しました: "+rsvedFile);
		}
	}

	/**
	 * レコーダの録画結果リストをキャッシュする<BR>
	 * ※キャッシュから取得したものはIDがnullでクリアされる
	 * @param recedFile
	 * @return nullは返さない！
	 * @see #RecordedToFile(ArrayList, String)
	 */
	protected ArrayList<RecordedInfo> RecordedFromFile(String recedFile) {

		File f = new File(recedFile);
		if ( ! f.exists() ) {
	        System.out.println("+録画結果キャッシュはありません: "+recedFile);
	        return new ArrayList<RecordedInfo>();
		}

		@SuppressWarnings("unchecked")
		ArrayList<RecordedInfo> tmp = (ArrayList<RecordedInfo>) CommonUtils.readXML(recedFile);
		if ( tmp == null ) {
        	System.err.println("録画結果キャッシュの読み込みに失敗しました: "+recedFile);
        	return new ArrayList<RecordedInfo>();
		}

		// 期限切れの情報は捨てる
		String critDate = CommonUtils.getDate(CommonUtils.getCalendar(-86400*getRecordedSaveScope()));
		String specialDate = CommonUtils.getDate(CommonUtils.getCalendar("1970/01/01"));
		for ( int i=tmp.size()-1; i>=0; i-- ) {
			if ( tmp.get(i).getDate().compareTo(critDate) < 0 && tmp.get(i).getDate().compareTo(specialDate) > 0) {
				// 期限切れ
				if (debug) System.out.println("録画結果のキャッシュを削除しました： "+tmp.get(i).getDate()+" "+tmp.get(i).getTitle());
				tmp.remove(i);
			}
			else {
				// オワタ
				break;
			}
		}

		for ( RecordedInfo ri : tmp ) {
			// キャッシュから読みだしたものはIDをクリアする
			ri.setId(null);
			// 後方互換
			if ( ri.getCh_orig() == null ) {
				ri.setCh_orig(ri.getCh_name());
			}
		}

        System.out.println("+録画結果キャッシュを読み込みました("+tmp.size()+"件): "+recedFile);
        return tmp;
	}

	/**
	 * @param recorded
	 * @param recedFile
	 * @see #RecordedToFile(ArrayList, String)
	 */
	protected void RecordedToFile(ArrayList<RecordedInfo> recorded, String recedFile) {
		if ( ! CommonUtils.writeXML(recedFile, recorded) ) {
        	System.err.println("録画結果キャッシュの保存に失敗しました: "+recedFile);
		}
	}

	/*
	 *
	 */
	protected boolean matchReserveV1(ReserveList n, ReserveList o) {
		return (
				n.getTitle().equals(o.getTitle()) &&
				n.getChannel().equals(o.getChannel()) &&
				n.getRec_pattern().equals(o.getRec_pattern()) &&
				n.getAhh().equals(o.getAhh()) && n.getAmm().equals(o.getAmm())
		);
	}
	public void setReservesV1(ArrayList<ReserveList> r) {
		// ライン入力のチャンネル名を保持する
		//System.out.println(RESERVES.size()+","+r.size());
		for (ReserveList o : RESERVES) {
			for (ReserveList n : r) {
				if (matchReserveV1(n, o)) {
					// 外部入力以外は知らん
					if (o.getCh_name() != null && n.getCh_name() != null && n.getCh_name().startsWith("外部入力")) {
						System.out.println("外部入力を次の放送局で置き換えます: "+n.getCh_name()+"->"+o.getCh_name());
						n.setCh_name(o.getCh_name());
					}

					// 鯛ナビの内部フラグ
					n.setAutocomplete(o.getAutocomplete());
					// 予約一覧からは取得できない情報
					n.setDetail(o.getDetail());
					n.setRec_genre(o.getRec_genre());
					n.setRec_device(o.getRec_device());
					n.setRec_folder(o.getRec_folder());
					n.setRec_dvdcompat(o.getRec_dvdcompat());
					n.setRec_xchapter(o.getRec_xchapter());
					n.setRec_mschapter(o.getRec_mschapter());
					n.setRec_mvchapter(o.getRec_mvchapter());
					//
					n.setRec_aspect(o.getRec_aspect());
					n.setRec_bvperf(o.getRec_bvperf());
					n.setRec_lvoice(o.getRec_lvoice());
					n.setRec_autodel(o.getRec_autodel());
				}
			}
		}
		//
		RESERVES = r;
		//
		removePassedReserves();
	}
	public void setReserves(ArrayList<ReserveList> r) {
		// ライン入力のチャンネル名を保持する
		for (ReserveList o : RESERVES) {
			for (ReserveList n : r) {
				// 外部入力以外は知らん
				if (o.getCh_name() != null && n.getCh_name() != null && n.getCh_name().startsWith("外部入力")) {
					if (o.getId() == n.getId() && o.getChannel().equals(n.getChannel())) {
						System.out.println("外部入力を次の放送局で置き換えます: "+n.getCh_name()+"->"+o.getCh_name());
						n.setCh_name(o.getCh_name());
					}
				}
			}
		}
		// 主にDIGA用
		{
			if ( getTunerNum() >= 2 ) {
				// ２チューナー以上は可変

				// ちょっと時刻順に整理しよう
				ArrayList<ReserveList> s = new ArrayList<ReserveList>();
				for ( ReserveList o : r ) {
					if (o.getAhh().isEmpty() || o.getAmm().isEmpty() || o.getZhh().isEmpty() || o.getZmm().isEmpty())
						continue;

					int idx = -1;
					for ( int i=0; i<s.size(); i++ ) {
						if ( o.getStartDateTime().compareTo(s.get(i).getStartDateTime()) < 0 ) {
							idx = i;
							break;
						}
					}
					if ( idx == -1 ) {
						s.add(o);
					}
					else {
						s.add(idx,o);
					}

					o.setTuner("");
				}

				// チューナーを割り振ろう
				for ( int x=0; x<s.size(); x++ ) {
					// 全チューナーをリストアップする
					ArrayList<String> tuns = new ArrayList<String>();
					for ( TextValueSet enc : encoder ) {
						tuns.add(enc.getText());
					}
					// 残っているチューナーをリストアップする
					for ( int y=0; y<s.size() && tuns.size()>0; y++ ) {
						if ( x == y || s.get(y).getTuner().equals("") ) {
							// 自分自身と、チューナー番号が振られていない相手はスルー
							continue;
						}
						// 時間が重なっている予約が既に使用しているチューナーは除外する
						ArrayList<String> starts = new ArrayList<String>();
						ArrayList<String> ends = new ArrayList<String>();
						CommonUtils.getStartEndList(starts, ends, s.get(y));
						for ( int z=0; z<starts.size(); z++ ) {
							// 帯予約を正しく処理するために全予約日時をなめるようにする
							if ( CommonUtils.isOverlap(s.get(x).getStartDateTime(), s.get(x).getEndDateTime(), starts.get(z), ends.get(z), getAdjNotRep()) ) {
								tuns.remove(s.get(y).getTuner());
								break;
							}
						}
					}
					// 残っているチューナーを割り振る
					if ( tuns.size() == 0 ) {
						// 余ってないなら全部０
						s.get(x).setTuner(encoder.get(0).getText());
					}
					else {
						// 余っているならそのうちの最初のものを使用
						s.get(x).setTuner(tuns.get(0));
					}
				}
			}
			else if ( getTunerNum() == 1 ) {
				// １チューナーは固定値
				for ( int x=0; x<r.size(); x++ ) {
					r.get(x).setTuner("■");
				}
			}
		}

		//
		RESERVES = r;

		//
		removePassedReserves();
	}
	public void removePassedReserves() {
		//
		String curDateTime = CommonUtils.getCritDateTime();
		//
		for (int i=RESERVES.size()-1; i>=0; i--) {
			ReserveList r = RESERVES.get(i);
			if (r.getRec_pattern_id() == HDDRecorder.RPTPTN_ID_BYDATE) {
				// 単日予約
				if (r.getEndDateTime().compareTo(curDateTime) < 0) {
					// 当日以前のエントリを削除
					RESERVES.remove(r);
				}
			}
		}
	}




	/***
	 *  RDデジタル系の予約一覧の解読
	 */
	protected ArrayList<ReserveList> decodeReservedList(String response) {

		ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();

		Matcher ma = Pattern.compile("(c1\\[\\d+?\\]=[\\s\\S]+?\";)\\n").matcher(response);
		while ( ma.find() ) {
			// 個々のデータを取り出す
			ReserveList entry = new ReserveList();

			Matcher mb = null;

			String[] d = new String[17];
			mb = Pattern.compile("c\\d+?\\[\\d+?\\]=\"(.*?)\";").matcher(ma.group(1));
			for (int i=0; i<d.length; i++) {
				if ( mb.find()) {
					d[i] = mb.group(1);
				}
				//System.out.println(i+") "+d[i]);
			}

			// 実行ON/OFF
			if (d[1].equals("2")) {
				entry.setExec(false);
			}

			// 番組追跡
			//if (d[12].equals("0") || d[12].equals("4") || d[12].equals("3")) {
			if (d[12].equals("4")) {
				entry.setPursues(true);
			}

			// 記録先デバイス
			entry.setRec_device(d[8]);

			// 予約名のエスケープを解除する
			String title = CommonUtils.unEscape(d[2]).replaceAll("<BR>","");

			//
			entry.setId(d[0]);

			//
			entry.setRec_pattern(d[5]);
			entry.setRec_pattern_id(getRec_pattern_Id(entry.getRec_pattern()));
			mb = Pattern.compile("(\\d\\d):(\\d\\d).*?(\\d\\d):(\\d\\d)").matcher(d[6]+"-"+d[7]);
			if (mb.find()) {
				entry.setAhh(mb.group(1));
				entry.setAmm(mb.group(2));
				entry.setZhh(mb.group(3));
				entry.setZmm(mb.group(4));
			}
			entry.setRec_nextdate(CommonUtils.getNextDate(entry));
			//entry.setRec_nextdate(getNextDate(entry.getRec_pattern(), entry.getZhh()+":"+entry.getZmm()));
			entry.setRec_min(CommonUtils.getRecMin(entry.getAhh(), entry.getAmm(), entry.getZhh(), entry.getZmm()));
			getStartEndDateTime(entry);

			//
			if (d[3].equals("18") || d[3].equals("10")  || d[3].equals("9")) {
				entry.setTuner("TS2");
			}
			else if (d[3].equals("17") || d[3].equals("12")  || d[3].equals("11")) {
				entry.setTuner("TS1");
			}
			else if (d[3].equals("16") || d[3].equals("8") || d[3].equals("7")) {
				entry.setTuner("RE");
			}
			else {
				entry.setTuner("--");
			}

			//
			if (d[10].equals("  ")) {
				if (d[9].equals("A1")) {
					d[9] = "[TSE] AT 4.7GB";
				}
				else if (d[9].equals("A2")) {
					d[9] = "[TSE] AT 9.4GB";
				}
				else if (d[9].equals("DL")) {
					d[9] = "[TSE] AT 8.5GB";
				}
				else {
					Matcher mc = Pattern.compile("^MN").matcher(d[9]);
					if (mc.find()) {
						d[9] = mc.replaceFirst("[TSE] ");
					}
				}
			}
			else {
				if (d[9].equals("A1")) {
					d[9] = "[VR] AT 4.7GB";
				}
				else if (d[9].equals("A2")) {
					d[9] = "[VR] AT 9.4GB";
				}
				else if (d[9].equals("DL")) {
					d[9] = "[VR] AT 8.5GB";
				}
				else {
					Matcher mc = Pattern.compile("^MN").matcher(d[9]);
					if (mc.find()) {
						d[9] = mc.replaceFirst("[VR] ");
					}
					else if ( d[9].startsWith("SP") || d[9].startsWith("LP")) {
						d[9] = "[VR] "+d[9];
					}
				}
			}

			if (d[9].equals("TS")) {
				entry.setRec_mode("[TS]");
			}
			else {
				entry.setRec_mode(d[9]);
			}

			entry.setTitle(title);
			entry.setTitlePop(TraceProgram.replacePop(title));
			//entry.setCh_name(getChCode().getCH_NO2NAME(d[4]));	// 機種固有領域に移動
			entry.setChannel(d[4]);

			entry.setRec_audio(d[10]);
			//entry.rec_folder = data.get();	// 予約一覧からはとれない
			//entry.rec_genre = data.get();		//　予約一覧からはとれない

			// 予約情報を保存
			newReserveList.add(entry.clone());
		}
		return(newReserveList);
	}

	/**
	 * レコーダーから取得できない情報は直接コピー（既存のリストから探して）
	 */
	protected void copyAttributes(ReserveList to, ArrayList<ReserveList> fromlist) {
		ReserveList olde = null;
		for ( ReserveList from : fromlist ) {
			if ( from.getId() != null && from.getId().equals(to.getId()) ) {
				copyAttribute(to, olde = from);
				break;
			}
		}

		// DIGAの終了時間"未定"対応だけど、別にDIGAかどうか確認したりはしない。
		setAttributesDiga(to,olde);
	}

	/**
	 * レコーダーから取得できない情報は直接コピー（既存エントリから直に）
	 */
	protected void copyAttribute(ReserveList to, ReserveList from) {
		// 鯛ナビの内部フラグ
		to.setAutocomplete(from.getAutocomplete());
		// 予約一覧からは取得できない情報
		to.setDetail(from.getDetail());
		to.setRec_genre(from.getRec_genre());
		//n.setRec_device(o.getRec_device());
		to.setRec_folder(from.getRec_folder());
		to.setRec_dvdcompat(from.getRec_dvdcompat());
		to.setRec_xchapter(from.getRec_xchapter());
		to.setRec_mschapter(from.getRec_mschapter());
		to.setRec_mvchapter(from.getRec_mvchapter());
		//
		to.setRec_aspect(from.getRec_aspect());
		to.setRec_bvperf(from.getRec_bvperf());
		to.setRec_lvoice(from.getRec_lvoice());
		to.setRec_autodel(from.getRec_autodel());
		// BZ700以降の取得一覧から取得できない画質の対応
		if (to.getRec_mode().equals("")) {
			to.setRec_mode(from.getRec_mode());
		}
	}

	protected void setAttributesDiga(ReserveList to, ReserveList from) {
		if ( to.getZhh() != null && to.getZmm() != null && to.getRec_min() != null ) {
			// 埋まってる
			return;
		}

		if ( from != null && from.getZhh() != null ) {
			// 引継ぎもとがあれば引き継ぐ
			to.setZhh(from.getZhh());
			to.setZmm(from.getZmm());
			to.setRec_min(from.getRec_min());
			to.setRec_nextdate(CommonUtils.getNextDate(to));
			getStartEndDateTime(to);
			return;
		}

		/*
		// 現在時刻から３０分後か、開始時刻から１時間後の、どちらか短い方に強制設定する
		String curTM = CommonUtils.getTime(30);
		String endTM = String.format("%02d:%s", (Integer.valueOf(to.getAhh())+1)%24,to.getAmm());
		if ( curTM.compareTo(endTM) > 0 ) {
			endTM = curTM;
		}
		to.setZhh(endTM.substring(0,2));
		to.setZmm(endTM.substring(3,5));
		to.setRec_min(CommonUtils.getRecMin(to.getAhh()+":"+to.getAmm(), endTM));
		*/

		// 開始時刻から１時間後に強制設定する
		to.setZhh(String.format("%02d", (Integer.valueOf(to.getAhh())+1)%24));
		to.setZmm(to.getAmm());
		to.setRec_min("60");

		to.setRec_nextdate(CommonUtils.getNextDate(to));
		getStartEndDateTime(to);
	}

	/**
	 * 録画済みフラグを立てる
	 */
	protected void setRecordedFlag() {

		// 過去Ｘ日分までチェック（初期値は１４日）
		final String critDateTime = CommonUtils.getDateTimeW(-86400*getRecordedCheckScope());

		for ( ReserveList reserved : RESERVES ) {
			reserved.setRecorded(false);
		}

		for ( RecordedInfo recorded : RECORDED ) {
			if ( critDateTime.compareTo(recorded.getDate()+" "+recorded.getAhh()+":"+recorded.getAmm()) > 0 ) {
				break;
			}
			String chktitle = recorded.getTitle().replaceFirst(TVProgram.titlePrefixRemoveExpr, "");
			for ( ReserveList reserved : RESERVES ) {
				if ( reserved.getRecorded() ) {
					// 既にフラグが立ってるものはスルー
					continue;
				}
				if ( reserved.getRec_pattern_id() != HDDRecorder.RPTPTN_ID_BYDATE ) {
					// 単日予約のみ
					continue;
				}
				String restitle = reserved.getTitle().replaceFirst(TVProgram.titlePrefixRemoveExpr, "");
				boolean chchk = (recorded.getChannel() != null && recorded.getChannel().length() > 0) ?  (recorded.getChannel().equals(reserved.getChannel())) : (true);
				if ( recorded.getSucceeded() && recorded.getDrop_mpeg() == 0 && chchk && chktitle.equals(restitle) ) {
					// 成功していて、放送局とタイトルが一致
					reserved.setRecorded(true);
				}
			}
		}
	}

	/**
	 * 録画結果一覧は開始日時降順で保存
	 * @param newRecordedList
	 * @param entry
	 */
	protected RecordedInfo addRecorded(ArrayList<RecordedInfo> newRecordedList, RecordedInfo entry) {

		String endt = entry.getDate()+entry.getAhh()+entry.getAmm();

		int n = 0;
		int dn = -1;
		for ( ; n<newRecordedList.size(); n++ ) {
			RecordedInfo ri = newRecordedList.get(n);
			String ridt = ri.getDate()+ri.getAhh()+ri.getAmm();
			int result = ridt.compareTo(endt);
			if ( result == 0 ) {
				if ( dn == -1 ) {
					// 開始時刻が同じ情報を発見したら、最終的にはそれの前に差し込みたいので、nを保存する
					dn = n;
				}

				// 開始時刻が一致して
				if ( ri.getId() == null ) {
					// キャッシュから取得したものだった場合に重複チェックする
					if ( ri.getTitle().equals(entry.getTitle()) && ri.getCh_orig().equals(entry.getCh_orig()) && ri.getLength() == entry.getLength()/* && ri.getDrop() == entry.getDrop()*/ ) {
						// 重複しているようなので捨てる
						System.out.println(MSGID+"録画結果はすでにキャッシュ上に存在していたようです： "+endt+" "+entry.getTitle());
						return null;
					}
					else {
						if (getDebug()) System.out.println(DBGID+"よく似た録画結果です： "+endt+" "+entry.getTitle()+" <-> "+ridt+ri.getTitle());
					}
				}
			}
			else if ( result < 0 ) {
				break;
			}
		}

		entry.setDrop_mpeg(entry.getDrop()-entry.getDrop_mpeg());

		newRecordedList.add((dn!=-1)?(dn):(n),entry);

		if (getDebug()) System.out.println(DBGID+"録画結果を追加しました： "+endt+" "+entry.getTitle());

		return entry;
	}

	/**
	 * <P>自動予約一覧の置き換え
	 * <P>chCode→chName変換もこの中でやる
	 */
	protected void setAutoReserves(AutoReserveInfoList l) {
		for ( AutoReserveInfo r : l ) {
			r.clearChNames();
			for ( String chCode : r.getChCodes() ) {
				String chName = getChCode().getCH_CODE2WEB(chCode);
				r.addChName((chName!=null) ? chName : chCode);
			}
		}
		AUTORESERVES = l;
	}

	/*******************************************************************************
	 * ログと進捗ダイアログ
	 ******************************************************************************/

	private StatusWindow stw = null;
	public void setProgressArea(StatusWindow o) { stw = o; }
	protected void reportProgress(String msg) {
		if (stw != null) {
			stw.append(msg);
		}
		System.out.println(msg);
	}

	// RD系のNewId取得
	protected String getNewId(String response) {
		Matcher ma = null;
		String newid = null;
		ma = Pattern.compile("c1\\[\\d+?\\]=\"(\\d+?)\";").matcher(response);
		while (ma.find()) {

			String idtmp = ma.group(1);

			boolean flag = true;
			for (ReserveList rx : getReserves()) {
				if (rx.getId().equals(idtmp)) {
					flag = false;
					break;
				}
			}
			if (flag == true) {
				newid = idtmp;
				break;
			}
		}
		return(newid);
	}

	/*******************************************************************************
	 * ログ系
	 ******************************************************************************/

	protected void ShowReserves(ArrayList<ReserveList> ress) {

		System.out.println("---Reserved List Start---");
		int i=0;
		for ( ReserveList e : ress ) {
			// 詳細情報の取得
			System.out.println(String.format("[%s] %s\t%s\t%s %s:%s-%s:%s\t%sm\t%s\t%s\t%s(%s)\t%s\t%s\t%s",
					++i, e.getId(), e.getRec_pattern(), e.getRec_nextdate(), e.getAhh(), e.getAmm(), e.getZhh(), e.getZmm(), e.getRec_min(), e.getContentId(), e.getRec_audio(), e.getTitle(), e.getTitlePop(), e.getChannel(), e.getCh_name(), e.getRecorded()));
			if ( i >= 50 ) {
				System.out.println(" *** 以下略 ***");
				break;
			}
		}
		System.out.println("---Reserved List End---");

	}

	protected void ShowRecorded(ArrayList<RecordedInfo> recs) {

		System.out.println("---Recorded List Start---");
		int i=0;
		for ( RecordedInfo e : recs ) {
			// 詳細情報の取得
			System.out.println(String.format("[%s] %s %s\t%s:%s-%s:%s\t%s(%s)\t%s",
					++i, e.getId(), e.getDate(), e.getAhh(), e.getAmm(), e.getZhh(), e.getZmm(), e.getTitle(), e.getCh_name(), e.getResult()));
			if ( i >= 50 ) {
				System.out.println(" *** 以下略 ***");
				break;
			}
		}
		System.out.println("---Recorded List End---");

	}

	/*******************************************************************************
	 * 通信系
	 ******************************************************************************/

	private final DumpHttp dump = new DumpHttp();

	//
	public void wakeup() {
		if ( ! getMacAddr().equals("") && ! getBroadcast().equals("")) {
			//
			byte[] magic = new byte[102];
			int i = 0;
			for (; i<6; i++) {
				magic[i] = (byte) 0xff;
			}
			for (int j=0; j<16; j++) {
				for (int k=0; k<6; k++) {
					short sv = Short.decode("0x"+getMacAddr().substring(k*2,k*2+2));
					magic[i++] = (byte)sv;
				}
			}

			//
			try {
				InetSocketAddress remote = new InetSocketAddress(getBroadcast(), 1234);
				DatagramPacket packet = new DatagramPacket(magic, magic.length, remote);
				new DatagramSocket().send(packet);
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			//
			System.out.println("send magic packet to "+getBroadcast()+","+getMacAddr());
		}
	}

	//
	public void shutdown() {
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		//
		reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/remote.htm?key=12", null);
		//
		System.out.println("send shutdown request to "+getBroadcast()+","+getMacAddr());
	}

	// おまじない
	public class MyAuthenticator extends Authenticator {
		private String username;
		private String password;

		public MyAuthenticator(String username, String password) {
			this.username = username;
			this.password = password;
		}
		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(username, password.toCharArray());
		}
	}

	// GET
	public String[] reqGET(String uri, Hashtable<String, String>property) {
		return reqGET(uri, property, "MS932");
	}
	public String[] reqGET(String uri, Hashtable<String, String>property, String encoding)
	{
        //CookieManager manager = new CookieManager();
        //CookieHandler.setDefault(manager);

		String header = "";
		String response = "";
		boolean getSuccess = false;

		HttpURLConnection conn = null;
		BufferedReader reader = null;
		InputStreamReader sr = null;
		try {
			if (debug) {
				System.out.println("# GET: "+uri);
				dump.request("# GET: "+uri);
			}

			// コネクションの確立
			URL url = new URL(uri);
			conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5*1000);
			conn.setReadTimeout(15*1000);
			conn.addRequestProperty("User-Agent", userAgent);
			if (property != null) {
				for (String key : property.keySet()) {
					conn.setRequestProperty(key, property.get(key));
				}
			}

			//conn.connect();

			Map<String, List<String>> h = conn.getHeaderFields();
			for ( String key : h.keySet() ) {
				// ヘッダ情報
				if (key == null) {
					header += h.get(key).get(0)+"\n";
					Matcher ma = Pattern.compile(" 200 ").matcher(h.get(key).get(0).toString());
					if (ma.find()) {
						getSuccess = true;
					}
				}
				else {
					header += key+": "+h.get(key).get(0)+"\n";
				}
			}
			if (debug) {
				System.out.println("# Header");
				System.out.println(header);
				dump.res_header("# Header\n"+header);
			}
			if (getSuccess == false) {
				// コネクション切断はfinallyで
				return(new String[] {header,null});
			}

			// データ部
			sr = new InputStreamReader(conn.getInputStream(),encoding);
			reader = new BufferedReader(sr);

			String s = null;
			StringBuilder sb = new StringBuilder();
			while ((s = reader.readLine()) != null) {
				sb.append(s);
				sb.append("\n");
			}

			response = sb.toString();

			// コネクション切断はfinallyで

		    if (debug) {
				//System.out.printf("# RESPONSE\n%s\n", response);
		    	System.out.println("# DUMP TO FILE: "+dump.res_body("<!-- # RESPONSE -->\n"+response));
			}

		     return(new String[] {header,response});
		}
		catch (UnsupportedEncodingException e) {
			System.err.println("[ERROR] レコーダへのアクセスで問題が発生しました(GET)： "+e.toString());
			if (getSuccess == true) {
				return(new String[] {header,null});
			}
		}
		catch (IOException e) {
			System.err.println("[ERROR] レコーダへのアクセスで問題が発生しました(GET)： "+e.toString());
			if (getSuccess == true) {
				return(new String[] {header,null});
			}
		}
		finally {
			CommonUtils.closing(reader);
			CommonUtils.closing(sr);
			CommonUtils.closing(conn);
		}

		return(new String[] {null,null});
	}

	// POST
	public String[] reqPOST(String uri, String pstr, Hashtable<String, String>property) {
		return reqPOST(uri, pstr, property, "MS932");
	}
	public String[] reqPOST(String uri, String pstr, Hashtable<String, String>property, String encoding)
	{
        //CookieManager manager = new CookieManager();
        //CookieHandler.setDefault(manager);

		boolean postSuccess = false;
		String header = "";
		String response = "";

		HttpURLConnection conn = null;
		OutputStreamWriter writer = null;
		BufferedReader reader = null;
		InputStreamReader sr = null;
		try {
			if (debug) {
				System.out.println("# POST: "+uri+"?"+pstr);
				dump.request("# POST: "+uri+"?"+pstr);
			}

			URL url = new URL(uri);
			conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			conn.setConnectTimeout(5*1000);
			conn.setReadTimeout(15*1000);
			conn.setDoOutput(true);
			conn.addRequestProperty("User-Agent", userAgent);
			if (property != null) {
				for (String key : property.keySet()) {
					conn.setRequestProperty(key, property.get(key));
				}
			}

			//conn.connect();

			writer = new OutputStreamWriter(conn.getOutputStream(),encoding);
			writer.write(pstr);
			writer.close();
			writer = null;

			Map<String, List<String>> h = conn.getHeaderFields();
			for ( String key : h.keySet() ) {
				// ヘッダ情報
				if (key == null) {
					header += h.get(key).get(0)+"\n";
					Matcher ma = Pattern.compile(" 200 ").matcher(h.get(key).get(0).toString());
					if (ma.find()) {
						postSuccess = true;
					}
				}
				else {
					header += key+": "+h.get(key).get(0)+"\n";
				}
			}
			if (debug) {
				System.out.println("# Header");
				System.out.println(header);
				dump.res_header("# Header\n"+header);
			}
			if (postSuccess == false) {
				// コネクション切断はfinallyで
				return(new String[] {header,null});
			}

			sr = new InputStreamReader(conn.getInputStream(),encoding);
			reader = new BufferedReader(sr);

			String s = null;
			StringBuilder sb = new StringBuilder();
			while ((s = reader.readLine()) != null) {
				sb.append(s);
				sb.append("\n");
			}

			response = sb.toString();

			// コネクション切断はfinallyで

		    if (debug) {
				//System.out.printf("# RESPONSE\n%s\n", response);
		    	System.out.println("# DUMP TO FILE: "+dump.res_body("<!-- # RESPONSE -->\n"+response));
			}

			return(new String[] {header,response});
		}
		catch (UnsupportedEncodingException e) {
			System.err.println("[ERROR] レコーダへのアクセスで問題が発生しました(POST)： "+e.toString());
			if (postSuccess == true) {
				return(new String[] {header,null});
			}
		}
		catch (IOException e) {
			System.err.println("[ERROR] レコーダへのアクセスで問題が発生しました(POST)： "+e.toString());
			if (postSuccess == true) {
				return(new String[] {header,null});
			}
		}
		finally {
			CommonUtils.closing(writer);
			CommonUtils.closing(reader);
			CommonUtils.closing(sr);
			CommonUtils.closing(conn);
		}

		return(new String[] {null,null});
	}

	/*******************************************************************************
	 * ここから下は該当機能が無効なプラグイン用のダミー
	 ******************************************************************************/

	@Override
	public RecType getType() {
		return null;
	}
	@Override
	public ChannelCode getChCode() {
		return null;
	}
	@Override
	public boolean ChangeChannel(String Channel) {
		return false;
	}
	@Override
	public boolean GetRdSettings(boolean force) {
		return true;
	}
	@Override
	public boolean GetRdReserve(boolean force) {
		return true;
	}
	@Override
	public boolean GetRdAutoReserve(boolean force) {
		return true;
	}
	@Deprecated
	@Override
	public boolean GetRdReserveDetails() {
		return false;
	}
	@Override
	public boolean GetRdRecorded(boolean force) {
		return true;
	}
	@Override
	public boolean PostRdEntry(ReserveList r) {
		return false;
	}
	@Override
	public boolean UpdateRdEntry(ReserveList o, ReserveList r) {
		return false;
	}
	@Override
	public ReserveList RemoveRdEntry(String delno) {
		return null;
	}
	@Override
	public boolean CreateRdFolder(String device_id, String folder_name) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean UpdateRdFolderName(String device_id, String folder_id, String folder_name) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean RemoveRdFolder(String device_id, String folder_id) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean GetRdTitles(String device_id, boolean force, boolean detail, boolean mountedOnly) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean GetRdTitleDetails(String device_id, boolean force){
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean GetRdTitleDetail(TitleInfo t) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean UpdateRdTitleInfo(String device_id, TitleInfo o, TitleInfo t) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public TitleInfo RemoveRdTitle(String device_id, String title_id) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public boolean StartStopPlayRdTitle(String device_id, String title_id, boolean start) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public String getErrmsg() {
		return null;
	}
}
