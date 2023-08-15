package tainavi;

import java.util.ArrayList;

/**
 * レコーダプラグインに実装されるべきインタフェースです。
 * @see HDDRecorderUtils
 */
public interface HDDRecorder {

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	/**
	 * 繰り返し予約を表す文字列定数。（毎日曜日～毎日）
	 */
	public static final String[] RPTPTN = {"毎日曜日","毎月曜日","毎火曜日","毎水曜日","毎木曜日","毎金曜日","毎土曜日","毎月～木","毎月～金","毎月～土","毎日"};

	// enum使えよ！！
	public static final int RPTPTN_ID_SAT = 6;
	public static final int RPTPTN_ID_MON2THU = 7;
	public static final int RPTPTN_ID_MON2FRI = 8;
	public static final int RPTPTN_ID_MON2SAT = 9;
	public static final int RPTPTN_ID_EVERYDAY = 10;
	public static final int RPTPTN_ID_BYDATE = 11;

	/**
	 * レコーダの種別を表す定数
	 */
	public static enum RecType { RECORDER, EPG, MAIL, CALENDAR, TUNER, NULL };

	/**
	 * 録画結果と予約一覧の引き当てを行う範囲の最大
	 */
	public static final int SCOPEMAX = 366;

	// 特殊なレコーダID
	public static final String SELECTED_ALL = "すべて";
	public static final String SELECTED_PICKUP = "ピックアップのみ";

	// 特殊な選択肢
	public static final String ITEM_REC_TYPE_EPG		= "EPG";
	public static final String ITEM_REC_TYPE_PROG		= "ﾌﾟﾛｸﾞﾗﾑ";

	public static final String VALUE_REC_TYPE_EPG		= "EPG";
	public static final String VALUE_REC_TYPE_PROG		= "PROGRAM";

	public static final String DEVICE_ALL = "ALL";
	public static final String DEVICE_NAME_ALL = "すべて";

	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/

	public String getRecorderId();

	public RecType getType();

	public ArrayList<TextValueSet> getVideoRateList();
	public ArrayList<TextValueSet> getAudioRateList();
	public ArrayList<TextValueSet> getFolderList();
	public ArrayList<TextValueSet> getEncoderList();
	public ArrayList<TextValueSet> getDVDCompatList();
	public ArrayList<TextValueSet> getDeviceList();
	public ArrayList<TextValueSet> getXChapter();
	public ArrayList<TextValueSet> getMsChapter();
	public ArrayList<TextValueSet> getMvChapter();
	public ArrayList<TextValueSet> getChValue();
	public ArrayList<TextValueSet> getChType();
	public ArrayList<TextValueSet> getAspect();
	public ArrayList<TextValueSet> getBVperf();
	public ArrayList<TextValueSet> getLVoice();
	public ArrayList<TextValueSet> getAutodel();
	public ArrayList<TextValueSet> getPortable();

	public String getLabel_Videorate();
	public String getLabel_Audiorate();
	public String getLabel_Folder();
	public String getLabel_Device();
	public String getLabel_DVDCompat();
	public String getLabel_XChapter();
	public String getLabel_MsChapter();
	public String getLabel_MvChapter();
	public String getLabel_Aspect();
	public String getLabel_BVperf();
	public String getLabel_LVoice();
	public String getLabel_Autodel();
	public String getLabel_Portable();

	public String text2value(ArrayList<TextValueSet> tvs, String text);
	public String value2text(ArrayList<TextValueSet> tvs, String value);
	public TextValueSet getDefaultSet(ArrayList<TextValueSet> tvs);

	// 個体の特性
	public String getIPAddr();
	public void setIPAddr(String s);
	public String getPortNo();
	public void setPortNo(String s);
	public String getUser();
	public void setUser(String s);
	public String getPasswd();
	public void setPasswd(String s);
	public String getColor(String tuner);
	public void setColor(String s);
	public String getMacAddr();
	public void setMacAddr(String s);
	public String getBroadcast();
	public void setBroadcast(String s);
	public String getName();
	public void setName(String s);
	public String getDispName();

	/**
	 * デバッグ用
	 */
	public ArrayList<String> getColors();

	/**
	 * 仮想エンコーダ数（チューナ数）を返します。DIGAなどレコーダからエンコーダ数を取得できない場合に使用します。
	 */
	public int getTunerNum();

	/**
	 * 仮想エンコーダ数（チューナ数）を設定します。DIGAなどレコーダからエンコーダ数を取得できない場合に使用します。
	 */
	public void setTunerNum(int n);

	/**
	 * 放送波の種類にあったエンコーダのリストを返します。「外部」で始まるエンコーダは強制的に追加されます
	 */
	public ArrayList<String> getFilteredEncoders(String webChName);

	/**
	 * 使用されていないエンコーダのリストアップと、裏番組リストを作成します
	 * <P>
	 * @param myrsv 予約一覧から開いた場合など、選択中の予約情報はスルーしてほしい場合に指定します。それ以外はnullで
	 * @param selectedVrate 特定の画質に対しては特定のエンコーダを指定してやらなければならない場合に指定します(RDのみ)
	 * @return null:自動検索無効、"":空きエンコーダなし、その他:空きエンコーダ
	 */
	public String getEmptyEncorder(String webChName, String startDateTime, String endDateTime, ReserveList myrsv, String selectedVrate);

	/**
	 * 裏番組のリストを返します
	 * @see #getEmptyEncorder(ProgDetailList, String)
	 */
	public ArrayList<ReserveList> getUrabanList();

	/**
	 * 画質にあったチューナーだけ拾ってみる
	 */
	public String getPreferredVrate_VARDIA(String tuner);

	/**
	 * チューナーにあった画質だけ拾ってみる
	 */
	public ArrayList<TextValueSet> getPreferredTuners_VARDIA(String vrate);

	/*
	 * 予約一覧系
	 */

	/**
	 * 特定の予約を返します。
	 */
	public ReserveList getReserveList(String rsvId);

	/**
	 * 予約一覧を返します。
	 */
	public ArrayList<ReserveList> getReserves();

	/**
	 * 自動予約一覧を返します。
	 */
	public AutoReserveInfoList getAutoReserves();

	/**
	 * 録画結果一覧を返します。
	 */
	public ArrayList<RecordedInfo> getRecorded();

	/**
	 * 過去日の情報を削除したり、繰り返し予約の次回実行予定日を設定したりします。
	 * @see HDDRecorderUtils#removePassedReserves()
	 */
	public void removePassedReserves();



	/*
	 * オプション系
	 */

	/**
	 * 予約一覧をサポートしているかどうか、ってサポートしてないのはカレンダーだけっすよ
	 */
	public boolean isReserveListSupported();

	/**
	 * <P>予約一覧へのアクセスだけでは予約の詳細がわからないレコーダなので詳細情報の個別取得が必要か？どうかを返します。
	 * <P>ただし、これをtrueにするのは個別取得に時間がかかる（旧RD系など）だけで、高速なもの（TvRockやEDCBなど）ではfalseでかまいません。
	 * @see #GetRdReserveDetails()
	 */
	public boolean isThereAdditionalDetails();

	/**
	 * 自動予約の編集をサポートしているかどうかを返します。
	 */
	public boolean isEditAutoReserveSupported();

	/**
	 * 自動予約の編集をサポートしているかどうかを返します。
	 */
	public boolean isRecordedListSupported();

	/**
	 * 録画タイトルをサポートしているかどうか
	 */
	public boolean isTitleListSupported();

	/**
	 * 自動エンコーダ選択を使用するかどうかを返します。
	 */
	public boolean isAutoEncSelectEnabled();

	/**
	 * 繰り返し予約をサポートしているかどうかを返します。
	 */
	public boolean isRepeatReserveSupported();

	/**
	 * 番組追従の変更をサポートしているかどうかを返します。
	 */
	public boolean isPursuesEditable();

	/**
	 * タイトル自動補完をサポートしているかどうかを返します。
	 */
	public boolean isAutocompleteSupported();

	/**
	 * チャンネル操作をサポートしているかどうかを返します。
	 */
	public boolean isChangeChannelSupported();

	/**
	 * Googleカレンダープラグインなど、通常操作用には選択できないプラグインです。
	 */
	public boolean isBackgroundOnly();

	/**
	 * 「レコーダの放送局名」にchvalueの値を利用できる
	 */
	public boolean isChValueAvailable();

	/**
	 * 「レコーダの放送局名」と「放送局コード」に異なる値を設定する必要がある（isChValueAvailable()==trueなら通常使われない）
	 */
	public boolean isChCodeNeeded();

	/**
	 * レコーダの放送局名すら必要ない(NULL)
	 */
	public boolean isRecChNameNeeded();

	/**
	 * 放送波の種別設定を必要とする(TvRock)
	 */
	public boolean isBroadcastTypeNeeded();

	/**
	 * 持ち出しをサポートしているかどうかを返す
	 */
	public boolean isPortableSupported();

	/**
	 * フォルダ作成をサポートしているかどうかを返す
	 */
	public boolean isFolderCreationSupported();

	/**
	 *  フリーワードオプションの処理（設定）
	 */
	public boolean setOptString(String s);

	/**
	 *  フリーワードオプションの処理（記録）
	 */
	public String getOptString();

	/*
	 * タイトル一覧系
	 */

	/**
	 * 特定のタイトルを返します。
	 */
	public TitleInfo getTitleInfo(String id);

	/**
	 * タイトル一覧を返します。
	 */
	public ArrayList<TitleInfo> getTitles();

	/*
	 * オプション系
	 */

	/**
	 * <P>タイトル一覧へのアクセスだけではタイトルの詳細がわからないレコーダなので詳細情報の個別取得が必要か？どうかを返します。
	 * <P>ただし、これをtrueにするのは個別取得に時間がかかる（旧RD系など）だけで、高速なもの（TvRockやEDCBなど）ではfalseでかまいません。
	 * @see #GetRdTitleDetails()
	 */
	public boolean isThereTitleDetails();

	/*
	 * 識別系
	 */

	/**
	 * 実際のレコーダを示すユニークID返却します。
	 * @return IP:PORT:RECORDER_ID (基本形。違う形式もあるかもしれない。)
	 */
	public String Myself();

	/**
	 * このプラグインが実際のレコーダとマッチするかどうか判定します。
	 * @param id : MySelf()で得られる値
	 */
	public boolean isMyself(String id);


	/**
	 *
	 */
	public String getChDatHelp();


	/*
	 * 動作設定系
	 */

	/**
	 * デバッグログ出力（主にレコーダとのHTTPのやりとり）をＯＮ／ＯＦＦします。
	 */
	public void setDebug(boolean b);

	/**
	 * 終了時刻と開始時刻が重なる番組を重複として処理するかどうかを指定します。
	 * @param b : trueの場合、重複とみましません。
	 */
	public void setAdjNotRep(boolean b);

	/**
	 * カレンダー連携を個別にＯＮ／ＯＦＦします。
	 */
	public void setUseCalendar(boolean b);
	public boolean getUseCalendar();

	/**
	 * カレンダー連携を個別にＯＮ／ＯＦＦします。
	 */
	public void setUseChChange(boolean b);
	public boolean getUseChChange();

	/**
	 * 成功した記録をチェックする範囲
	 */
	public void setRecordedCheckScope(int n);
	//public int getRecordedCheckScope();

	/**
	 * 録画結果を残す範囲
	 */
	public void setRecordedSaveScope(int n);

	/**
	 * <P>HTTPアクセス時のUser-Agentの値を設定します。
	 * <P>基本的に設定する意味はないでしょう。
	 */
	public void setUserAgent(String s);

	/**
	 * 動作状況を出力するステータスウィンドウを設定します。
	 */
	public void setProgressArea(StatusWindow o);


	/*
	 *  主要な操作系メソッド
	 */

	public ChannelCode getChCode();

	/**
	 * <P>レコーダを起動させます。MACアドレスとブロードキャストアドレスの設定が必要です。
	 * <P>基本的にはWOLを実行しますが、DIGAは電源が落ちていてもHTTPサーバが生きているのでHTTPリクエストによる起動が実行されます。
	 */
	public void wakeup();

	/**
	 * レコーダを停止させます。MACアドレスとブロードキャストアドレスの設定が必要です。
	 */
	public void shutdown();

	/**
	 * チャンネルを切り替えます。
	 * @param Channel : Web番組表の放送局名を指定します。
	 */
	public boolean ChangeChannel(String Channel);

	/**
	 * <P>レコーダから各種設定の取得を行います。（全部のレコーダには実装していない）
	 * @param force : trueの場合レコーダへのアクセスを強制します。falseの場合キャッシュファイルがあればそちらを利用します。
	 * @see #GetRdReserve(boolean)
	 * @see #GetRdReserveDetails()
	 * @see #GetRdRecorded(boolean)
	 */
	public boolean GetRdSettings(boolean force);

	/**
	 * <P>レコーダから予約一覧（と各種設定）の取得を行います。
	 * <P>将来的には、各種設定の取得は別メソッドにわけたいところ。
	 * @param force : trueの場合レコーダへのアクセスを強制します。falseの場合キャッシュファイルがあればそちらを利用します。
	 * @see #GetRdSettings(boolean)
	 */
	public boolean GetRdReserve(boolean force);

	/**
	 * <P>レコーダから自動予約一覧の取得を行います。
	 * @param force : trueの場合レコーダへのアクセスを強制します。falseの場合キャッシュファイルがあればそちらを利用します。
	 * @see #isEditAutoReserveSupported()
	 */
	public boolean GetRdAutoReserve(boolean force);

	/**
	 * <P>レコーダから録画結果一覧の取得を行います。
	 * @see #GetRdSettings(boolean)
	 */
	public boolean GetRdRecorded(boolean force);

	/**
	 * 詳細情報の個別取得を行います。
	 * @see #GetRdReserve(boolean)
	 * @see #isThereAdditionalDetails()
	 */
	public boolean GetRdReserveDetails();

	/**
	 * 予約の新規登録を行います。
	 */
	public boolean PostRdEntry(ReserveList r);

	/**
	 * 予約の更新を行います。
	 * @param o : 旧情報
	 * @param r : 新情報
	 */
	public boolean UpdateRdEntry(ReserveList o, ReserveList r);

	/**
	 * 予約の削除を行います。
	 * @param delno : 削除する予約の予約IDを指定します。
	 * @see ReserveList#id
	 */
	public ReserveList RemoveRdEntry(String delno);


	/**
	 * フォルダーを作成します。
	 */
	public boolean CreateRdFolder(String device_id, String folder_name);

	/**
	 * フォルダーの名前を更新します。
	 */
	public boolean UpdateRdFolderName(String device_id, String folder_id, String folder_name);

	/**
	 * フォルダーを削除します。
	 */
	public boolean RemoveRdFolder(String device_id, String folder_id);

	/**
	 * <P>レコーダからタイトル一覧（と各種設定の取得を行います。
	 * @param device_id : デバイスID
	 * @param force : trueの場合レコーダへのアクセスを強制します。falseの場合キャッシュファイルがあればそちらを利用します。
	 * @param detail: trueの場合詳細情報も取得します。
	 * @param mountedOnly : trueの場合マウントされている場合のみ取得します。
	 */
	public boolean GetRdTitles(String device_id, boolean force, boolean detail, boolean mountedOnly);

	/**
	 * <P>レコーダからタイトル詳細の取得を行います。
	 */
	public boolean GetRdTitleDetail(TitleInfo t);

	/**
	 * <P>レコーダから未取得のタイトルの詳細情報の取得を行います。
	 * @param device_id : デバイスID
	 * @param force : trueの場合詳細取得済のタイトルも取得します。
	 */
	public boolean GetRdTitleDetails(String device_id, boolean force);

	/**
	 * タイトル情報を更新します。
	 */
	public boolean UpdateRdTitleInfo(String device_id, TitleInfo o, TitleInfo t);

	/**
	 * タイトルを削除します。
	 */
	public TitleInfo RemoveRdTitle(String device_id, String title_id);

	/**
	 * タイトルの再生を開始・終了します。
	 */
	public boolean StartStopPlayRdTitle(String device_id, String title_id, boolean start);

	/**
	 * デバイス情報を取得します。
	 */
	public DeviceInfo GetRDDeviceInfo(String device_id);

	/**
	 * デバイス名からデバイス情報を取得します。
	 */
	public DeviceInfo GetRDDeviceInfoFromName(String device_name);

	/**
	 * デバイス名を取得します。
	 */
	public String getDeviceName(String device_id);

	/**
	 * デバイスIDを取得します。
	 */
	public String getDeviceID(String device_name);

	/**
	 * 処理の結果に応じて追加のメッセージが取得できます。
	 * @return "" : まったくの正常に終わった場合。
	 */
	public String getErrmsg();


	/**
	 * クローンとコンピュータの融合体は新たな生命と呼べるのか
	 */
	public HDDRecorder clone();
}
