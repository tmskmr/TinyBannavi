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

import tainavi.BroadcastType;
import tainavi.ChannelCode;
import tainavi.CommonUtils;
import tainavi.ContentIdDIMORA;
import tainavi.ContentIdEDCB;
import tainavi.ContentIdREGZA;
import tainavi.HDDRecorder;
import tainavi.HDDRecorderUtils;
import tainavi.RecordedInfo;
import tainavi.ReserveList;
import tainavi.TextValueSet;
import tainavi.TraceProgram;
import tainavi.HDDRecorder.RecType;
import tainavi.HDDRecorderUtils.MyAuthenticator;


/**
 * 
 */
public class PlugIn_RecRD_TvRock extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	@Override
	public PlugIn_RecRD_TvRock clone() {
		return (PlugIn_RecRD_TvRock) super.clone();
	}
	
	private static final String thisEncoding = "MS932";

	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/

	@Override
	public String getRecorderId() { return "TvRock"; }
	@Override
	public RecType getType() { return RecType.RECORDER; }
	
	// 録画結果一覧を取得できる
	@Override
	public boolean isRecordedListSupported() { return true; }
	// 番組追従が可能
	@Override
	public boolean isPursuesEditable() { return true; }
	// タイトル自動補完はできない
	@Override
	public boolean isAutocompleteSupported() { return false; }
	// chvalueをつかってもいーよ
	@Override
	public boolean isChValueAvailable() { return true; }
	// CHコードは入力しなくていい
	@Override
	public boolean isChCodeNeeded() { return false; }
	// 放送は種別は選択してほしい
	@Override
	public boolean isBroadcastTypeNeeded() { return true; }

	/*******************************************************************************
	 * 予約ダイアログなどのテキストのオーバーライド
	 ******************************************************************************/

	@Override
	public String getLabel_Audiorate() { return "予約方法"; }
	@Override
	public String getLabel_Folder() { return "録画ﾓｰﾄﾞ"; }
	@Override
	public String getLabel_XChapter() { return "待機時間(秒前)"; }
	@Override
	public String getLabel_MsChapter() { return "録画開始(秒前)"; }
	@Override
	public String getLabel_MvChapter() { return "録画終了(秒後)"; }
	@Override
	public String getLabel_DVDCompat() { return "録画終了後"; }
	@Override
	public String getLabel_Aspect() { return "終了後ｺﾏﾝﾄﾞ"; }
	@Override
	public String getLabel_LVoice() { return "ｺﾝﾋﾟｭｰﾀ名"; }
	
	@Override
	public String getChDatHelp() { return
			"「レコーダの放送局名」は、予約一覧取得が正常に完了していれば設定候補がコンボボックスで選択できるようになります。"+
			"「放送波の種別」は、チューナーの自動選択に必要なので極力設定してください（詳細はWiki参照）";
	}

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/
	
	private static final String ITEM_CH_EPGGET		= "[番組情報取得スケジュール]";
	
	private static final String VALUE_CH_EPGGET		= "";

	private static final String VALUE_YES  = "1";
	private static final String VALUE_NO   = "0";
	
	private static final String ITEM_REC_MODE_RECORD	= "録画のみ";
	private static final String ITEM_REC_MODE_WATCH		= "視聴のみ";
	private static final String ITEM_REC_MODE_RANDW		= "録画＋視聴";
	private static final String ITEM_REC_MODE_FANDO		= "録画＋ワンセグ";
	
	private static final String VALUE_REC_MODE_RECORD	= "100";
	private static final String VALUE_REC_MODE_WATCH	= "010";
	private static final String VALUE_REC_MODE_RANDW	= "110";
	private static final String VALUE_REC_MODE_FANDO	= "101";
	
	private static final String[] KEYS_REC_MODE = {
		"reconly",
		"watchonly",
		"oneseg"
	};
	
	// ログ関連
	
	private final String MSGID = "["+getRecorderId()+"] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final String DBGID = "[DEBUG]"+MSGID;

	// 録画結果一覧の特殊日付
	private static final String RECORDED_SPDATE = CommonUtils.getDate(CommonUtils.getCalendar("1970/01/01"));
	
	// 定数ではない
	private int retryMax = 3;

	/*******************************************************************************
	 * CHコード設定、エラーメッセージ
	 ******************************************************************************/
	
	@Override
	public ChannelCode getChCode() {
		return cc;
	}

	private final ChannelCode cc = new ChannelCode(getRecorderId());

	@Override
	public String getErrmsg() {
		return errmsg;
	}

	private String errmsg = "";
	
	/*******************************************************************************
	 * フリーオプション関係
	 ******************************************************************************/
	/*
	private static final String OPTKEY_DIR = "libdir";
	private static final String OPTVAL_DIR_DEFAULT = "D:/PT2/TVRockLib/";
	
	private OptMap opts = null;
	
	private class OptMap extends MapCtrl {
		@Override
		protected boolean chkOptString() {
			// ディレクトリ設定があるかしら
			{
				String datRoot = get(OPTKEY_DIR);
				if ( datRoot == null ) {
					// なければデフォルト
					put(OPTKEY_DIR, OPTVAL_DIR_DEFAULT);
					datRoot = get(OPTKEY_DIR);
					System.out.println("[TvRock未使用の方は無視してください] TVRockLibフォルダは設定値になります： "+datRoot);
				}
				else {
					System.out.println("[TvRock未使用の方は無視してください] TVRockLibフォルダはデフォルト値になります： "+datRoot);
				}
			}
			return true;
		}
	}
	
	@Override
	public boolean setOptString(String s) {
		return opts.setOptString(s);
	}
	
	@Override
	public String getOptString() {
		return opts.toString();
	}
	*/
	
	/*******************************************************************************
	 * 部品
	 ******************************************************************************/
	
	private String rsvedFile = "";
	
	private ArrayList<TextValueSet> recDefaults = new ArrayList<TextValueSet>();
	private final String[] recDefaulltKeys = {"idle","ready","tale","watchonly","reconly","cname","extmd"};
	
	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
	
	public PlugIn_RecRD_TvRock() {
		setIPAddr("127.0.0.1(サンプル)");
		setPortNo("8969(サンプル)");
		setUser("nobody(サンプル)");
		setPasswd("********");
	}
	
	/*******************************************************************************
	 * チャンネルリモコン機能
	 ******************************************************************************/
	
	@Override
	public boolean ChangeChannel(String webChName) {
		
		if (webChName == null) {
			return true;	// 有効だよと返す
		}
		
		errmsg = "";
		
		String chCode = cc.getCH_WEB2CODE(webChName);
		if ( chCode == null || chCode.length() == 0 ) {
			errmsg = ERRID+"Web番組表の放送局名CHコードに変換できない： "+webChName;
			return false;
		}
		
		String recChName = cc.getCH_CODE2REC(chCode);
		if ( recChName == null ) {
			errmsg = ERRID+"CHコードをレコーダの放送局名に変換できない： "+webChName+"("+chCode+")";
			return false;
		}

		String hdr = null;
		if ( chCode.startsWith(BroadcastType.TERRA.getName()+":") ) {
			hdr = "地上";
		}
		else if ( chCode.startsWith(BroadcastType.BS.getName()+":") || chCode.startsWith(BroadcastType.CS.getName()+":") ) {
			hdr = "BS";
		}
		if ( hdr == null ) {
			errmsg = ERRID+"放送波の種別の指定がないか、不正： "+chCode;
			return false;
		}

		ArrayList<TextValueSet> tuners = new ArrayList<TextValueSet>();
		for ( TextValueSet tun : encoder ) {
			if ( tun.getText().startsWith(hdr) ) {
				tuners.add(tun);	// 特定放送波のチューナーだけ
			}
		}
		if ( tuners.size() == 0 ) {
			tuners = encoder;	// 全部なめてみる
		}

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		String selectedtuner = null;
		String chid = null;
		boolean selected = false;
		boolean sleeping = false;
		for ( TextValueSet tuner : tuners ) {
			
			// チューナーが操作対象か、起動中かどうか調べる
			String uri = "http://"+getIPAddr()+":"+getPortNo()+"/"+getUser()+"/cnt?d="+tuner.getValue();
			String[] d = null;
			for ( int l=2; l>0; l-- ) {
				if (getDebug()) System.out.println(DBGID+"tuner select "+uri);
				d = reqGET(uri, null);
				if ( d[0] == null || d[1] == null ) {
					if (getDebug()) System.out.println(DBGID+"Retrying...");
				}
			}
			if ( d == null || d[0] == null || d[1] == null ) {
				errmsg = ERRID+"TvRockへのアクセスが失敗： "+uri;
				return false;
			}
			
			sleeping = ! d[1].matches("^[\\s\\S]*<td align=center bgcolor=#fed8c6><small><font color=#......>"+tuner.getText()+"</font></small></td>[\\s\\S]*$");
			boolean recording = d[1].matches("^[\\s\\S]*bgcolor=#c88084 width=\\d+%><small><font color=#......><b>録画</b>[\\s\\S]*$");
			
			// 放送局が選択可能が調べる
			Matcher ma = Pattern.compile("<tr><td align=center width=3% bgcolor=#......>(.*?)</tr>",Pattern.DOTALL).matcher(d[1]);
			while ( ma.find() ) {
				Matcher mb = Pattern.compile("&q=(.+?)\"",Pattern.DOTALL).matcher(ma.group(1));
				if ( mb.find() ) {
					String chname = CommonUtils.unEscape(mb.group(1));
					if ( recChName.equals(chname) ) {
						mb = Pattern.compile("cnt\\?z=(\\d+)",Pattern.DOTALL).matcher(ma.group(1));
						if ( mb.find() ) {
							chid = mb.group(1);
						}
						else {
							selected = true;
						}
						break;
					}
				}
			}
			if ( ! selected && chid == null ) {
				errmsg = ERRID+"放送局が見つからない： "+webChName+"("+recChName+","+chCode+")";
				continue;
			}
			if ( recording ) {
				errmsg = MSGID+"録画中のためチャンネル変更できません： "+tuner.getText()+" "+webChName+"("+recChName+","+chCode+") @ "+tuner.getText();
				return false;
			}
			if ( selected ) {
				errmsg = MSGID+"すでに選択済みです： "+webChName+"("+recChName+","+chCode+") @ "+tuner.getText();
				break;
			}
			if ( chid != null ) {
				errmsg = "";
				selectedtuner = tuner.getText();
				break;	// みつかった
			}
		}
		if ( ! selected && chid == null ) {
			//errmsg = ERRID+"放送局が見つからない： "+webChName;
			return false;
		}
		
		// 寝てたら起こす
		if ( sleeping ) {
			String uri = "http://"+getIPAddr()+":"+getPortNo()+"/"+getUser()+"/cnt?bt=1";
			if (getDebug()) System.out.println(DBGID+"tuner wakeup "+uri);
			String[] d = reqGET(uri, null);
			if ( d[0] == null || d[1] == null ) {
				errmsg = ERRID+"TvRockへのアクセスが失敗： "+uri;
				return false;
			}
			CommonUtils.milSleep(3000);
		}
		
		// 放送局を選択する
		if ( ! selected ) {
			String uri = "http://"+getIPAddr()+":"+getPortNo()+"/"+getUser()+"/cnt?z="+chid;
			if (getDebug()) System.out.println(DBGID+"center select "+uri);
			String[] d = reqGET(uri, null);
			if ( d[0] == null || d[1] == null ) {
				errmsg = ERRID+"TvRockへのアクセスが失敗： "+uri;
				return false;
			}
			
			errmsg = MSGID+"視聴します： "+webChName+" @ "+selectedtuner;
		}

		return true;
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
		
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		System.out.println("レコーダから各種設定を取得します("+force+")： "+getRecorderId()+"("+getIPAddr()+":"+getPortNo()+")");
		
		errmsg = "";
		
		/*
		 *  CHコード設定
		 */
		
		cc.load(force);
		replaceChNames(cc);		// これは予約一覧取得からの場合は無駄な処理になるが、GetRdSettings単体呼び出しなら意味がある
		
		/*
		 *  選択肢集団
		 */
		
		String xChapterTFile = "env/xchapter."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String msChapterTFile = "env/mschapter."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String mvChapterTFile = "env/mvchapter."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String encoderTFile = "env/encoders."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String dvdcompatTFile = "env/dvdcompat."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String aspectTFile = "env/aspect."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String lvoiceTFile = "env/lvoice."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String channelTFile = "env/channel."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		
		String recDefaultsTFile = "env/recdefaults."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		
		// ハードコーディングな選択肢の面々
		setSettingRecMode(folder);
		setSettingRecType(arate);
		setSettingBvperf(bvperf);
		
		if ( ! force ) {
			/*
			 *  キャッシュから読み出し
			 */
			encoder = TVSload(encoderTFile);
			chvalue = TVSload(channelTFile);
			
			xchapter = TVSload(xChapterTFile);
			mschapter = TVSload(msChapterTFile);
			mvchapter = TVSload(mvChapterTFile);
			
			dvdcompat = TVSload(dvdcompatTFile);
			aspect = TVSload(aspectTFile);
			lvoice = TVSload(lvoiceTFile);
			
			recDefaults = TVSload(recDefaultsTFile);
		}
		else {
			/*
			 *  レコーダから読み出し
			 */
			reportProgress("録画設定を取得します(1/1).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/"+getUser()+"/reg",null);
			String res = d[1];
			
			if ( res == null ) {
				errmsg = "レコーダーが反応しません";
				return false;
			}
			
			// (1-X)録画デフォルト値
			setSettigRecDefaults(recDefaults,recDefaulltKeys,res);
			TVSsave(recDefaults, recDefaultsTFile);
			
			// (1-4)エンコーダ
			setSettingEtc(encoder,"devno",0,res);
			TVSsave(encoder, encoderTFile);
			
			// (1-5)チャンネル
			setSettingEtc(chvalue,"station",0,res);
			TVSsave(chvalue, channelTFile);
			
			// (1-6)録画終了後
			setSettingEtc(dvdcompat,"extmd",0,res);
			TVSsave(dvdcompat, dvdcompatTFile);
			
			// (1-7)待機時間
			setSettingEtc(xchapter,"idle",0,res);
			TVSsave(xchapter, xChapterTFile);
			
			// (1-8)録画開始
			setSettingEtc(mschapter,"ready",0,res);
			TVSsave(mschapter, msChapterTFile);
			
			// (1-9)録画終了
			setSettingEtc(mvchapter,"tale",0,res);
			TVSsave(mvchapter, mvChapterTFile);
			
			// (1-10)コンピュータ名
			setSettingEtc(lvoice,"cname",0,res);
			TVSsave(lvoice, lvoiceTFile);
			
			// (1-11)終了後ｺﾏﾝﾄﾞ
			setSettingEtc(aspect,"cuscom",0,res);
			TVSsave(aspect, aspectTFile);
		}
		
		/*
		 * フリーオプション関連（future use.）
		 */
		//opts.setFilename("env/options_"+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml");
		
		if ( getDebug() ) {
			System.err.println("=== CHコード一覧 for TvRock ===");
			System.err.println("有効な \"レコーダの放送局名\" 一覧");
			System.err.println("=============================");
			
			for ( TextValueSet tv : chvalue ) {
				System.err.println(String.format("\"%s\"",tv.getText()));
			}
			System.err.println("=============================");
		}
		
		// ちゃんと設定を取得できているよね？
		return (encoder.size()>0 && chvalue.size()>0);
	}
	
	@Override
	public boolean GetRdReserve(boolean force)
	{
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		System.out.println("レコーダから予約一覧を取得します("+force+")： "+getRecorderId()+"("+getIPAddr()+":"+getPortNo()+")");
		
		errmsg = "";

		//
		rsvedFile = "env/reserved."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		
		File f = new File(rsvedFile);
		if ( ! force && f.exists() ) {
			// キャッシュから読み出し（予約一覧）
			setReserves(ReservesFromFile(rsvedFile));
			replaceChNames(cc);
			if (getDebug()) ShowReserves(getReserves());

			return true;
		}
		
		// レコーダから読み出し（予約一覧）
		ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();
		if ( ! GetRdReservedList(newReserveList) ) {
			return false;
		}
		if ( ! GetRdReservedDetails(newReserveList) ) {
			return false;
		}
		
		setReserves(newReserveList);
		ReservesToFile(getReserves(), rsvedFile);	// キャッシュに保存

		// 録画済みフラグを立てる（録画結果一覧→予約一覧）
		setRecordedFlag();

		if (getDebug()) ShowReserves(getReserves());
		
		return(true);
	}
	
	/**
	 * CHコードを置き換えよう（TvRockの場合はREC2WEB）
	 */
	private void replaceChNames(ChannelCode cc) {
		for ( ReserveList r : getReserves() ) {
			if ( VALUE_CH_EPGGET.equals(r.getChannel()) ) {
				r.setCh_name(ITEM_CH_EPGGET);
			}
			else {
				r.setCh_name(cc.getCH_REC2WEB(r.getChannel()));
			}
		}
	}
	
	/**
	 * @see #GetRdSettings(boolean)
	 */
	@Override
	public boolean GetRdRecorded(boolean force) {
		
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		System.out.println("レコーダから録画結果一覧を取得します("+force+")： "+getRecorderId()+"("+getIPAddr()+":"+getPortNo()+")");
		
		String recedFile = "env/recorded."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		
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
		
		if ( ! GetRdRecordedList(newRecordedList) ) {
			return false;
		}
		setRecorded(newRecordedList);				// 置き換え
		RecordedToFile(getRecorded(), recedFile);	// キャッシュに保存
		
		// 録画済みフラグを立てる（録画結果一覧→予約一覧）
		setRecordedFlag();
		
		if (getDebug()) ShowRecorded(getRecorded());
		
		return true;
	}

	
	/**
	 * 予約一覧を取得する
	 */
	private boolean GetRdReservedList(ArrayList<ReserveList> newReserveList) {
		
		//　RDから予約一覧を取り出す
		String response="";
		{
			reportProgress("予約一覧を取得します(1/1).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/"+getUser()+"/list?md=0&iv=0&st=0&ft=0",null);
			response = d[1];
			
			if (response == null) {
				errmsg = "レコーダーが反応しません";
				return(false);
			}
		}
		
		Matcher mc = Pattern.compile("予約件数([\\s\\S]+?)</table>").matcher(response);
		if ( mc.find() ) {
			Matcher mx = Pattern.compile("<tr>([\\s\\S]+?)</tr>").matcher(mc.group(1));
			while ( mx.find() ) {
				
				boolean exec = false;
				boolean pursues = false;
				boolean autoreserved = false;
				String title = "";
				String id = "";
				String center = "";
				boolean tunershort = false;
				
				int cnt = 1;
				Matcher mz = null;
				Matcher my = Pattern.compile("<td([\\s\\S]+?)</td>").matcher(mx.group(1));
				while ( my.find() ) {
					switch (cnt) {
					case 2: // 有効
						if (my.group(1).contains(">○<")) {
							exec = true;
						}
						break;
					case 5: // リピート
						if (my.group(1).contains(">追<")) {
							pursues = true;
						}
						break;
					case 8: // 検索予約
						if (my.group(1).contains(">★<")) {
							autoreserved = true;
						}
						break;
					case 10: // チャンネル
						mz = Pattern.compile("<font.+?>(.+?)</font>").matcher(my.group(1));
						if (mz.find()) {
							center = mz.group(1);
						}
						break;
					case 11: // タイトル
						mz = Pattern.compile("<a href=\"reg\\?i=(\\d+?)\"><small><font.+?><b>(.*?)</b>").matcher(my.group(1));
						if (mz.find()) {
							id = mz.group(1);
							title = CommonUtils.unEscape(mz.group(2)).replaceAll("<BR>", "");
						}
						break;
					case 1: // 順
					case 3: // コンピュータ
					case 4: // デバイス
					case 6: // アプリ終了
					case 7: // 予定
					case 9: // 予定日時
						mz = Pattern.compile("<font color=#f86878><b>").matcher(my.group(1));
						tunershort = mz.find();	// チューナー重複警告
						break;
					}
					cnt++;
				}
				if ( cnt != 12 || id.equals("") ) {
					continue;
				}
				
				// 登録
				ReserveList entry = new ReserveList();
				
				// 実行ON/OFF
				entry.setExec(exec);

				// 追跡ON/OFF
				entry.setPursues(pursues);
				
				// 検索予約
				entry.setAutoreserved(autoreserved);
				
				// 放送局
				{
					if ( center == null || center.length() == 0 ) {
						entry.setCh_name(ITEM_CH_EPGGET);
						entry.setChannel(VALUE_CH_EPGGET);
					}
					else {
						entry.setCh_name(cc.getCH_REC2WEB(center));
						entry.setChannel(center);
					}
				}

				// Id
				entry.setId(id);

				// 予約名
				entry.setTitle(title);
				entry.setTitlePop(TraceProgram.replacePop(title));
				
				// チューナー重複警告
				entry.setTunershort(tunershort);
				
				// 予約情報を保存
				newReserveList.add(entry);
			}
		}
		
		return true;
	}
	

	// 予約詳細を追加取得する
	private boolean GetRdReservedDetails(ArrayList<ReserveList> newReserveList) {
		int cnt = 0;
		for ( ReserveList r : newReserveList ) {
			// 進捗状況を報告する
			++cnt;
			reportProgress("+番組詳細を取得します("+cnt+"/"+newReserveList.size()+")");
			if ( ! getReserveDetail(r) ) {
				return false;
			}
			
			// 情報の引き継ぎ
			for ( ReserveList ro : getReserves() ) {
				if ( ro.getId().equals(r.getId()) ) {
					r.setRec_genre(ro.getRec_genre());
					r.setRec_subgenre(ro.getRec_subgenre());
					if (
							ro.getDetail() != null &&
							(r.getDetail() == null || r.getDetail().length() < ro.getDetail().length()) ) {
						r.setDetail(ro.getDetail());
					}
					if ( ! ContentIdEDCB.decodeContentId(r.getContentId()) || ContentIdEDCB.getEvId() == 0xFFFF ) {
						r.setContentId(ro.getContentId());
					}
					break;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * 予約詳細を取得する
	 */
	private boolean getReserveDetail(ReserveList r) {
		
		String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/"+getUser()+"/reg?i="+r.getId(),null);
		String res = d[1];
		
		if (res != null) {
			
			// 録画優先度
			Matcher mb = Pattern.compile("<input type=\"text\" name=\"pri\".+?value=\"(\\d+?)\">").matcher(res);
			if (mb.find()) {
				r.setRec_bvperf(mb.group(1));
			}

			// (1-4)エンコーダ
			r.setTuner(getSelectedSetting("devno",res));
			
			// (1-6)録画終了
			r.setRec_dvdcompat(getSelectedSetting("extmd",res));
			
			// (1-7)待機時間
			r.setRec_xchapter(getSelectedSetting("idle",res));
			
			// (1-8)録画開始
			r.setRec_mschapter(getSelectedSetting("ready",res));
			
			// (1-9)録画終了
			r.setRec_mvchapter(getSelectedSetting("tale",res));
			
			// (1-10)コンピュータ名
			r.setRec_lvoice(getSelectedSetting("cname",res));
			
			// (1-11)終了後コマンド
			r.setRec_aspect(getSelectedSetting("cuscom",res));
			
			// 予約実行＋録画モード
			mb = Pattern.compile("<input type=\"checkbox\" name=\"valid\" value=\"true\"( checked)?>",Pattern.DOTALL).matcher(res);
			r.setExec(mb.find() && mb.group(1) != null);
			
			// 録画モード
			{
				String rec_mode_value = "";
				for ( int i=0; i < KEYS_REC_MODE.length; i++ ) {
					String rmname = KEYS_REC_MODE[i];
					Matcher mc = Pattern.compile("<input type=\"checkbox\" name=\""+rmname+"\" value=\"true\"( checked)?>",Pattern.DOTALL).matcher(res);
					rec_mode_value += (mc.find() && mc.group(1) != null) ? VALUE_YES : VALUE_NO;
				}
				
				String rec_mode_text = value2text(folder, rec_mode_value);
				r.setRec_folder(rec_mode_text != null ? rec_mode_text : ITEM_REC_MODE_RECORD);
			}
			
			// 時刻を調べる
			r.setAhh(getSelectedSetting("shour",res));
			r.setAmm(getSelectedSetting("smin",res));
			r.setZhh(getSelectedSetting("ehour",res));
			r.setZmm(getSelectedSetting("emin",res));

			// 日付を調べる
			mb = Pattern.compile("<input type=\"checkbox\" name=\"epgflw\" value=\"true\"(\\s*checked)?>").matcher(res);
			if ( mb.find() ) {
				// EPG予約
				if ( mb.group(1) != null ) {
					// 追跡ON
					getPatternPursues(r,res);
					r.setPursues(true);
				}
				else {
					// 追跡OFF
					getPattern(r,res);
					r.setPursues(false);
				}
				if ( r.getContentId() == null || r.getContentId().length() == 0 ) {
					r.setContentId(ContentIdEDCB.getContentId(0xFFFF,0xFFFF,0xFFFF,0xFFFF));
				}
				r.setRec_audio(ITEM_REC_TYPE_EPG);
			}
			else {
				// プログラム予約
				getPattern(r,res);
				r.setContentId("");
				r.setRec_audio(ITEM_REC_TYPE_PROG);
				r.setPursues(false);
			}
			
			// 番組詳細
			mb = Pattern.compile("<font color=#e8e8e8><small>([\\s\\S]*?)</small>(.*?<small>([\\s\\S]*?)</small>)?").matcher(res);
			if ( mb.find() ) {
				if ( mb.group(3) == null ) {
					r.setDetail(CommonUtils.unEscape(mb.group(1)));
				}
				else {
					r.setDetail(CommonUtils.unEscape(mb.group(1)+"\n\n"+mb.group(3)));
				}
			}
			
			// 調整する
			r.setRec_nextdate(CommonUtils.getNextDate(r));
			r.setRec_min(CommonUtils.getRecMin(r.getAhh(), r.getAmm(), r.getZhh(), r.getZmm()));
			getStartEndDateTime(r);
			
			return true;
		}
		return false;
	}
	
	// 時間追従あり
	private void getPatternPursues(ReserveList r, String res) {
		
		Matcher mb = null;
		
		/*
		 *  時刻を調べる
		 */
		
		String mm = null;
		String dd = null;
		
		mb = Pattern.compile("</form>.*?>(\\d+)/(\\d+) (\\d+):(\\d+)～(\\d+):(\\d+)<br>").matcher(res);
		if ( mb.find() ) {
			// 番組詳細から取得
			mm = mb.group(1);
			dd = mb.group(2);
			r.setAhh(String.format("%02d", Integer.valueOf(mb.group(3))));
			r.setAmm(String.format("%02d", Integer.valueOf(mb.group(4))));
			r.setZhh(String.format("%02d", Integer.valueOf(mb.group(5))));
			r.setZmm(String.format("%02d", Integer.valueOf(mb.group(6))));
		}
		else {
			// 「予約日時」から取得
			mb = Pattern.compile("<input type=\"hidden\" name=\"shour\" value=\"\\d+?\">(\\d+?)<").matcher(res);
			if (mb.find()) {
				r.setAhh(String.format("%02d", Integer.valueOf(mb.group(1))));
			}
			mb = Pattern.compile("<input type=\"hidden\" name=\"smin\" value=\"\\d+?\">(\\d+?)<").matcher(res);
			if (mb.find()) {
				r.setAmm(String.format("%02d", Integer.valueOf(mb.group(1))));
			}
			mb = Pattern.compile("<input type=\"hidden\" name=\"ehour\" value=\"\\d+?\">(\\d+?)<").matcher(res);
			if (mb.find()) {
				r.setZhh(String.format("%02d", Integer.valueOf(mb.group(1))));
			}
			mb = Pattern.compile("<input type=\"hidden\" name=\"emin\" value=\"\\d+?\">(\\d+?)<").matcher(res);
			if (mb.find()) {
				r.setZmm(String.format("%02d", Integer.valueOf(mb.group(1))));
			}
		}
		
		/*
		 * 日付を調べる
		 */
		
		int year = 1970;
		int month = 1;
		int day = 1;
		//int wday = 1;
		mb = Pattern.compile("<input type=\"hidden\" name=\"year\" value=\"\\d+?\">(\\d+?)<").matcher(res);
		if (mb.find()) {
			year = Integer.valueOf(mb.group(1));
		}
		if ( mm != null ) {
			// 番組詳細から取得
			month = Integer.valueOf(mm);
			day = Integer.valueOf(dd);
		}
		else {
			// 「予約日時」から取得
			mb = Pattern.compile("<input type=\"hidden\" name=\"mon\" value=\"\\d+?\">(\\d+?)<").matcher(res);
			if (mb.find()) {
				month = Integer.valueOf(mb.group(1));
			}
			mb = Pattern.compile("<input type=\"hidden\" name=\"day\" value=\"\\d+?\">(\\d+?)<").matcher(res);
			if (mb.find()) {
				day = Integer.valueOf(mb.group(1));
			}
		}
		GregorianCalendar c = new GregorianCalendar();
		c.set(Calendar.DATE, day);
		c.set(Calendar.MONTH, month-1);
		c.set(Calendar.YEAR, year);
		c.set(Calendar.MINUTE, Integer.valueOf(r.getAhh()));
		c.set(Calendar.SECOND, Integer.valueOf(r.getAmm()));
		//wday = c.get(Calendar.DAY_OF_WEEK);
		
		// 年をまたいで追跡されると追いかけられないのである
		
		r.setRec_pattern(CommonUtils.getDate(c));
		r.setRec_pattern_id(HDDRecorder.RPTPTN_ID_BYDATE);
	}
	
	// 時間追従なし
	private void getPattern(ReserveList r, String res) {
		int year = 1970;
		int month = 1;
		int day = 1;
		int wday = 1;
		Matcher mb = Pattern.compile("<select name=\"year\">.*?<option value=\"[^\"]+?\"\\s*selected>(.+?)</option>.*?</select>").matcher(res);
		if (mb.find()) {
			year = Integer.valueOf(mb.group(1));
		}
		mb = Pattern.compile("<select name=\"mon\">.*?<option value=\"[^\"]+?\"\\s*selected>(.+?)</option>.*?</select>").matcher(res);
		if (mb.find()) {
			month = Integer.valueOf(mb.group(1));
		}
		mb = Pattern.compile("<select name=\"day\">.*?<option value=\"[^\"]+?\"\\s*selected>(.+?)</option>.*?</select>").matcher(res);
		if (mb.find()) {
			day = Integer.valueOf(mb.group(1));
		}
		GregorianCalendar c = new GregorianCalendar();
		c.set(Calendar.DATE, day);
		c.set(Calendar.MONTH, month-1);
		c.set(Calendar.YEAR, year);
		wday = c.get(Calendar.DAY_OF_WEEK);
		
		mb = Pattern.compile("<input type=\"radio\" name=\"repeat\" value=\"([^\"]+?)\"\\s*checked>").matcher(res);
		if (mb.find()) {
			if (mb.group(1).equals("0")) {
				// 当日限り
				r.setRec_pattern(CommonUtils.getDate(c));
				r.setRec_pattern_id(HDDRecorder.RPTPTN_ID_BYDATE);
			}
			else if (mb.group(1).equals("1")) {
				// 毎日
				r.setRec_pattern(RPTPTN[HDDRecorder.RPTPTN_ID_EVERYDAY]);
				r.setRec_pattern_id(HDDRecorder.RPTPTN_ID_EVERYDAY);
			}
			else if (mb.group(1).equals("2")) {
				// 毎週
				for ( int i=0; i<7; i++ ) {
					if (RPTPTN[i].equals("毎"+CommonUtils.WDTPTN[wday-1]+"曜日")) {
						r.setRec_pattern(RPTPTN[i]);
						r.setRec_pattern_id(i);
						break;
					}
				}
			}
			else if (mb.group(1).equals("4")) {
				// 帯予約
				int fw = 0;
				int i = 0;
				Matcher mc = Pattern.compile("<input type=\"checkbox\" name=\"repw.\" value=\"true\"\\s*(checked)?>").matcher(res);
				while (mc.find()) {
					if (mc.group(1) != null) {
						fw |= (1<<i);
					}
					i++;
				}
				
				// 24:00～28:59開始は深夜帯
				if (CommonUtils.isLateNight(r.getAhh()) && (fw == 125 || fw == 124 || fw == 60)) {
					fw = -fw;
				}
							
				switch (fw) {
				case 126:	//   2+4+8+16+32+64
				case -125:	// 1+  4+8+16+32+64
					r.setRec_pattern(RPTPTN[HDDRecorder.RPTPTN_ID_MON2SAT]);
					r.setRec_pattern_id(HDDRecorder.RPTPTN_ID_MON2SAT);
					break;
				case 30:	// 2+4+8+16
				case -60:	//   4+8+16+32
					r.setRec_pattern(RPTPTN[HDDRecorder.RPTPTN_ID_MON2THU]);
					r.setRec_pattern_id(HDDRecorder.RPTPTN_ID_MON2THU);
					break;
				case 62:	// 2+4+8+16+32
				case -124:	//   4+8+16+32+64
				default:
					r.setRec_pattern(RPTPTN[HDDRecorder.RPTPTN_ID_MON2FRI]);
					r.setRec_pattern_id(HDDRecorder.RPTPTN_ID_MON2FRI);
					break;
				}
			}
		}
	}
	
	// 各番組の、指定されている値を取得する
	private String getSelectedSetting(String key, String res) {
		Matcher mb = Pattern.compile("<select name=\""+key+"\">.*?<option value=\"([^\"]*?)\"\\s*selected>(.+?)</option>.*?</select>").matcher(res);
		if (mb.find()) {
			return mb.group(2);
		}
		return null;
	}
	
	
	/**
	 *	録画結果一覧を取得する
	 */
	
	private boolean GetRdRecordedList(ArrayList<RecordedInfo> newRecordedList) {
		
		String cutDate = CommonUtils.getDate(CommonUtils.getCalendar(-86400*getRecordedSaveScope()));
		String critDate = null;
		if ( newRecordedList.size() > 0 ) {
			// 最新の情報の前日分までチェックする
			GregorianCalendar cal = CommonUtils.getCalendar(newRecordedList.get(0).getDate());
			cal.add(Calendar.DATE, -1);
			critDate = CommonUtils.getDate(cal);
			
			// 期限切れの情報のカット
			ArrayList<RecordedInfo> removeList = new ArrayList<RecordedInfo>();
			for ( RecordedInfo entry : newRecordedList ) {
				if ( ! entry.getDate().equals(RECORDED_SPDATE) && entry.getDate().compareTo(cutDate) < 0 ) {
					removeList.add(entry);
				}
			}
			for ( RecordedInfo entry : removeList ) {
				newRecordedList.remove(entry);
			}
		}
		else {
			// 既存情報が無ければ上限まで
			critDate = cutDate;
		}
		
		//　RDから予約一覧を取り出す
		String response="";
		{
			reportProgress("録画結果一覧を取得します.");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/nobody/page20",null,"utf8");
			response = d[1];
			
			if (response == null) {
				errmsg = "レコーダーが反応しません";
				return false;
			}
		}
		
		String[] list = response.split("\n");
		
		HashMap<String,RecordedInfo> results = new HashMap<String,RecordedInfo>();
		ArrayList<RecordedInfo> other_messages_buf = new ArrayList<RecordedInfo>();
		
		for ( int n=list.length-1; n>=0; n--) {
			if ( list[n].matches(".*(TvRockの(起動|終了)|システム休止|(休止|スタンバイ)[のを]キャンセル|番組情報の取得).*") ) {
				continue;
			}
			
			Matcher ma = Pattern.compile("\\[(\\d\\d/\\d\\d/\\d\\d) (\\d\\d):(\\d\\d):.+?\\]:\\[(.+?)\\](.+)$",Pattern.DOTALL).matcher(list[n]);
			if ( ma.find() ) {
				String date = "20"+ma.group(1);
				String hh = ma.group(2);
				String mm = ma.group(3);
				String tuner = ma.group(4);
				String message = ma.group(5);
				
				RecordedInfo entry = null;
				
				if ( message.contains("録画開始") ) {
					if ( results.containsKey(tuner) ) {
						// 既になんかあるね！吐き出せー
						if (results.get(tuner).getDate().compareTo(critDate) >= 0) {
							entry = results.get(tuner);
							if ( entry.getZhh() == null || entry.getZhh().equals("") ) {
								entry.setResult("※録画が中断された可能性があります。");
								entry.setZhh(hh);
								entry.setZmm(mm);
								int len =  (int) (CommonUtils.getCompareDateTime(date+" "+hh+":"+mm, entry.getDate()+" "+entry.getAhh()+":"+entry.getAmm()) / 60000L);
								entry.setLength(len);
								entry.setSucceeded(false);
							}
							addRecorded(newRecordedList,entry);
						}
					}
					
					entry = new RecordedInfo();
					results.put(tuner, entry);
					
					entry.setId(String.valueOf(n));
					
					entry.setDate(CommonUtils.getDate(CommonUtils.getCalendar(date)));
					entry.setAhh(hh);
					entry.setAmm(mm);
					entry.setLength(0);	// 仮置き
					
					Matcher mb = Pattern.compile("Sig=(\\d+\\.\\d+),.*, Drop=(\\d+),.*, DiskFree=(\\d+\\.\\d+)\\%",Pattern.DOTALL).matcher(message);
					if ( mb.find() ) {
						entry.setSig_a(Float.valueOf(mb.group(1)));
						//entry.setDrop(Integer.valueOf(mb.group(2)));	// リストに追加するときに調整してください
						//entry.setResult("ディスクの空き領域："+mb.group(3)+"%");
						//entry.setSucceeded(Float.valueOf(mb.group(3))>0.1F);
					}
					
					mb = Pattern.compile("番組「(.*?)」 録画開始",Pattern.DOTALL).matcher(message);
					if ( mb.find() ) {
						entry.setTitle(mb.group(1));
						//entry.setCh_name(mb.group(2));
					}
					
					entry.setDetail(list[n]+"\n#録画終了#\n");
					
					entry.setCh_name("TvRockでは取得できません");
					entry.setCh_orig(entry.getCh_name());
					
					entry.setSucceeded(true);
				}
				else if ( message.contains("録画終了") ) {
					if ( ! results.containsKey(tuner) ) {
						// 「録画開始」以外から始まっていたら無視しよう
						continue;
					}
					
					entry = results.get(tuner);
					if ( entry == null ) {
						continue;
					}
					
					entry.setZhh(hh);
					entry.setZmm(mm);
					
					int len =  (int) (CommonUtils.getCompareDateTime(date+" "+hh+":"+mm, entry.getDate()+" "+entry.getAhh()+":"+entry.getAmm()) / 60000L);
					entry.setLength(len);
					
					Matcher mb = Pattern.compile("Sig=(\\d+\\.\\d+),.*, Drop=(\\d+),.*, DiskFree=(\\d+\\.\\d+)\\%",Pattern.DOTALL).matcher(message);
					if ( mb.find() ) {
						entry.setSig_z(Float.valueOf(mb.group(1)));
						entry.setDrop(Integer.valueOf(mb.group(2)));			// リストに追加するときに調整してください
						entry.setResult(String.format("DiskFree=%s%% Sig=%.2f-%.2fdb",mb.group(3),entry.getSig_a(),entry.getSig_z()));
						entry.setSucceeded(Float.valueOf(mb.group(3))>0.1F);	// 録画終了時にディスクの空き容量が0.1%(3000GBで3GB)を切っていたら録画失敗じゃないかと
					}
					
					entry.setDetail(entry.getDetail().replaceFirst("#録画終了#", list[n]));
				}
				else if ( message.contains("視聴開始") ) {
					if ( results.containsKey(tuner) ) {
						// 既になんかあるね！吐き出せー
						if (results.get(tuner).getDate().compareTo(critDate) >= 0) addRecorded(newRecordedList,results.get(tuner));
					}
					
					results.remove(tuner);
				}
				else {
					if ( ! results.containsKey(tuner) ) {
						// 「録画開始」以外から始まっていたら無視しよう
						continue;
					}
					
					entry = results.get(tuner);
					if ( entry == null ) {
						continue;
					}
					
					// MPEG2のDropだけ拾うには？
					Matcher mb = Pattern.compile("エラー詳細:PID (0x.+?), Total=(\\d+), Drop=(\\d+), Scrambling=(\\d+)",Pattern.DOTALL).matcher(message);
					if ( mb.find() ) {
						int pid = Integer.decode(mb.group(1));
						if ( pid < 0x100 || pid == 0x500 ) {
							int drop = Integer.valueOf(mb.group(3));
							entry.setDrop_mpeg(entry.getDrop_mpeg()+drop);
						}
					}
					
					entry.setDetail(entry.getDetail()+list[n]+"\n");
				}
				
				continue;
			}
			
			ma = Pattern.compile("\\[(\\d\\d/\\d\\d/\\d\\d) (\\d\\d):(\\d\\d):.+?\\]:(.*)$",Pattern.DOTALL).matcher(list[n]);
			if ( ma.find() ) {
				
				// 日付時刻
				String date = "20"+ma.group(1);
				String hh = ma.group(2);
				String mm = ma.group(3);
				
				String title = "";
				String message = "";
				Matcher mb; 
				if ( (mb = Pattern.compile("ターゲットアプリケーションの異常終了コードを検出しました",Pattern.DOTALL).matcher(ma.group(4))) != null && mb.find() ) {
					title = "＜＜＜エラーメッセージ＞＞＞";
					message = ma.group(4);
				}
				else if ( (mb = Pattern.compile("番組「(.*?)」の(予約は実行されませんでした)",Pattern.DOTALL).matcher(ma.group(4))) != null && mb.find() ) {
					title = mb.group(1);
					message = mb.group(2);
				}
				else {
					RecordedInfo e = new RecordedInfo();
					e.setDate(date);
					e.setAhh(hh);
					e.setAmm(mm);
					if ( (mb = Pattern.compile("番組「(.*?)」の(?:開始|終了)時間を(?:\\d+?)分(?:\\d+?)秒調整しました",Pattern.DOTALL).matcher(ma.group(4))) != null && mb.find() ) {
						e.setSucceeded(true);
						e.setTitle(mb.group(1));	// 変更された番組のタイトル
						e.setDetail(null);
						e.setResult(list[n]);
						other_messages_buf.add(e);
						continue;
					}
					else if ( (mb = Pattern.compile("番組「(.*?)」のタイトルを「(.*?)」へ調整しました",Pattern.DOTALL).matcher(ma.group(4))) != null && mb.find() ) {
						e.setSucceeded(true);
						e.setTitle(mb.group(1));	// 変更前のタイトル
						e.setDetail(mb.group(2));	// 変更後のタイトル
						e.setResult(list[n]);
						other_messages_buf.add(e);
						continue;
					}
					else {
						e.setSucceeded(false);
						e.setTitle(null);
						e.setDetail(null);
						e.setResult(list[n]);
						other_messages_buf.add(e);
						continue;
					}
				}
				
				// 新規のエントリを追加する
				
				RecordedInfo entry = new RecordedInfo();
					
				entry.setId(String.valueOf(n));
					
				entry.setDate(CommonUtils.getDate(CommonUtils.getCalendar(date)));
				entry.setAhh(hh);
				entry.setAmm(mm);
				entry.setZhh(hh);
				entry.setZmm(mm);
				entry.setLength(0);	// 仮置き
					
				entry.setTitle(title);
				
				entry.setDetail(list[n]+"\n");
				
				entry.setChannel(null);
				entry.setCh_name("TvRockでは取得できません");
				entry.setCh_orig(entry.getCh_name());
				
				entry.setResult(message);
				
				entry.setSucceeded(false);
				
				if (entry.getDate().compareTo(critDate) >= 0) addRecorded(newRecordedList, entry);
				continue;
			}
		}
		
		for ( RecordedInfo entry : results.values() ) {
			if ( entry.getLength() > 0 ) {
				if (entry.getDate().compareTo(critDate) >= 0) addRecorded(newRecordedList,entry);
			}
		}
		
		// その他のメッセージの整理
		String other_messages = "";
		for ( RecordedInfo e : other_messages_buf ) {
			if ( ! e.getSucceeded() ) {
				other_messages += e.getResult()+"\n";
				continue;
			}
			
			GregorianCalendar cea = CommonUtils.getCalendar(e.getDate()+" "+e.getAhh()+":"+e.getAmm());
			for ( RecordedInfo f : newRecordedList ) {
				GregorianCalendar cfz = CommonUtils.getCalendar(f.getDate()+" "+f.getAhh()+":"+f.getAmm());
				cfz.add(Calendar.MINUTE, f.getLength());
				Long dz = CommonUtils.getCompareDateTime(cea, cfz);
				if (
						(dz<=0L && dz>-86400000L*7L) &&
						(f.getTitle().equals(e.getTitle()) || (e.getDetail()!=null && f.getTitle().equals(e.getDetail()))) ) {
					// 一週間以内に、タイトルが同じor変更前のタイトルと一致したものに追加しよう
					for ( String msg : f.getDetail().split("\n") ) {
						if ( msg.equals(e.getResult()) ) {
							// 重複メッセージは要らね
							cea = null;
						}
					}
					if ( cea != null ) {
						f.setDetail(f.getDetail()+e.getResult()+"\n");
						cea = null;
					}
					break;
				}
			}
			if ( cea != null ) {
				other_messages += e.getResult()+"\n";
			}
		}

		// その他のログ
		if ( other_messages.length() > 0 ) {
			// 古いその他ログの削除
			for ( int index=newRecordedList.size()-1; index>=0; index-- ) {
				if ( ! RECORDED_SPDATE.equals(newRecordedList.get(index).getDate()) ) {
					break;
				}
				
				newRecordedList.remove(index);
			}
			/* - 新旧ログの重複チェックをしなくてはいけないので保留
			String old_other_messages = "";
			String messages_tmp = null;
			for ( int index=newRecordedList.size()-1; index>=0; index-- ) {
				if ( ! RECORDED_SPDATE.equals(newRecordedList.get(index).getDate()) ) {
					break;
				}
				
				messages_tmp = newRecordedList.remove(index).getDetail();
			}
			if ( messages_tmp != null ) {
				String dt = critDate.substring(2,9);
				String[] a = messages_tmp.split("\n");
				
				for ( String m : a ) {
					String mdt = m.substring(1,8);
					if ( mdt.compareTo(dt) >= 0 ) {
						old_other_messages += m+"\n";
					}
				}
			}
			*/
			
			RecordedInfo entry = new RecordedInfo();
			
			entry.setDate(RECORDED_SPDATE);
			entry.setAhh("00");
			entry.setAmm("00");
			entry.setZhh("00");
			entry.setZmm("00");
			entry.setLength(0);	// 仮置き
				
			entry.setTitle("＜＜＜その他のメッセージ＞＞＞");
			
			entry.setDetail(other_messages);
			
			entry.setChannel(null);
			entry.setCh_name("TvRockでは取得できません");
			entry.setCh_orig(entry.getCh_name());
			
			entry.setResult("その他のメッセージ");
			
			entry.setSucceeded(false);

			newRecordedList.add(entry);
		}

		return true;
	}
	
	/*******************************************************************************
	 * 新規予約
	 ******************************************************************************/
	
	@Override
	public boolean PostRdEntry(ReserveList r) {
		
		errmsg = "";

		String chCode = ((ITEM_CH_EPGGET.equals(r.getCh_name())) ? VALUE_CH_EPGGET : cc.getCH_WEB2REC(r.getCh_name()));
		if ( chCode == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}
		
		// EPG予約かプログラム予約か
		//int onid = -1;
		//int tsid = -1;
		int sid = -1;
		int evid = -1;
		if ( r.getRec_audio() != null && r.getRec_audio().equals(ITEM_REC_TYPE_EPG) ) {
			if ( ContentIdEDCB.isValid(r.getContentId()) ) {
				ContentIdEDCB.decodeContentId(r.getContentId());
				//onid = ContentIdEDCB.getOnId();
				//tsid = ContentIdEDCB.getTSId();
				sid = ContentIdEDCB.getSId();
				evid = ContentIdEDCB.getEvId();
			}
			else if ( ContentIdDIMORA.isValid(r.getContentId()) ) {
				ContentIdDIMORA.decodeContentId(r.getContentId());
				sid = ContentIdDIMORA.getSId();
				evid = ContentIdDIMORA.getEvId();
			}
			else if ( ContentIdREGZA.isValid(r.getContentId()) ) {
				ContentIdREGZA.decodeContentId(r.getContentId());
				sid = ContentIdREGZA.getSId();
				evid = ContentIdREGZA.getEvId();
			}
			else {
				errmsg = "番組表に予約IDがないためEPG予約は利用できません。EDCB番組表を利用するかプログラム予約を行ってください。";
				return false;
			}
			
			return PostRdEntryEPG(r,sid,evid);
		}
		
		System.out.println("Run: PostRdEntry("+r.getTitle()+")");
		
		if ( r.getPursues() ) {
			errmsg = "プログラム予約では次の指定はできないので規定値に変更しました：番組追従＝なし";
			r.setPursues(false);
		}

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		// 予約パターンID
		// 次回予定日
		// 録画長
		// 開始日時・終了日時
		r.setRec_pattern_id(getRec_pattern_Id(r.getRec_pattern()));
		r.setRec_nextdate(CommonUtils.getNextDate(r));
		r.setRec_min(CommonUtils.getRecMin(r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm()));
		getStartEndDateTime(r);
		
		// タイトルは200文字まで
		r.setTitle(CommonUtils.substringrb(r.getTitle(), 200));
		
		// 番組詳細は200文字まで(3.7.10β)
		r.setDetail(CommonUtils.substringrb(r.getDetail(), 200));
		
		// プログラム予約なので
		r.setContentId("");
		
		// CHコード入れ忘れてたよ！
		r.setChannel(chCode);
		
		//　TVRockへ登録する
		String header;
		String response;
		
		// 情報作成
		String pstr = genPoststr(r, "0", "　予約を追加　");
		
		System.out.println(pstr);

		// 情報送信
		{		
			reportProgress("send request(1/1).");
			header = response = null;
			String reqStr = "http://"+getIPAddr()+":"+getPortNo()+"/"+getUser()+"/regsch?"+pstr;
			for (int i=0; i<retryMax; i++) {
				String[] d = reqGET(reqStr,null);
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
		
		// 予約ID番号を取得（キャッシュに存在しない番号が新番号）
		r.setId(getNewId(response));
		if (r.getId() == null) {
			errmsg = "予約に失敗しました。";
			return(false);
		}
		reportProgress("予約IDは"+r.getId()+"です。");
		
		// 予約リストを更新
		getReserves().add(r);
		
		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に登録できました。");
		return(true);
	}

	private boolean PostRdEntryEPG(ReserveList r, int sid, int evid) {
		
		System.out.println("Run: PostRdEntryEPG("+r.getTitle()+")");
		
		String enc = text2value(encoder, r.getTuner());
		if ( enc == null || enc.length() == 0 ) {
			errmsg = ERRID+"エンコーダが認識できない： "+r.getTuner();
			return false;
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
			errmsg = ERRID+"EPG予約に繰り返し指定はできない： "+r.getRec_pattern();
			return false;
		}
		
		// タイトルは200文字まで
		r.setTitle(CommonUtils.substringrb(r.getTitle(), 200));
		
		// 番組詳細は200文字まで(3.7.10β)
		r.setDetail(CommonUtils.substringrb(r.getDetail(), 200));

		// EPG予約なので
		//r.setContentId(ほげほげ);

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		ReserveList newRsv = r;	// 予約内容が強制変更されるかもしれん
		
		// 登録
		{
			String reqStr = "http://"+getIPAddr()+":"+getPortNo()+"/"+getUser()+"/kws?r=1&c="+sid+"&e="+evid+"&d="+enc;
			
			reportProgress(DBGID+"EPG予約を登録します(1/2)： "+reqStr);
			
			String header = null;
			String response = null;
			
			for (int i=0; i<retryMax; i++) {
				String[] d = reqGET(reqStr,null);
				header = d[0];
				response = d[1];
				if ( header != null && response == null ) {
					reportProgress(ERRID+"コネクションがリセットされました。リトライします。");
					CommonUtils.milSleep(1000);
				}
				else {
					break;
				}
			}
			if ( header == null || response == null ) {
				errmsg = ERRID+"レコーダーが反応しません。";
				return(false);
			}
			
			String id = null;
			int cnt = 0;
			Matcher ma = Pattern.compile("<tr><td align=center width=\\d+% bgcolor=#(......)>(.*?)</td></tr>",Pattern.DOTALL).matcher(response);
			while ( ma.find() ) {
				
				++cnt;
				System.err.println(ma.group(1));
				
				if ( ma.group(1).equals("c0f0d4") ) {
					
					Matcher mb = Pattern.compile("<a href=\"reg\\?i=(\\d+)\">").matcher(ma.group(2));
					if ( mb.find() ) {
							
						r.setId(id = mb.group(1));
						reportProgress("予約IDは"+r.getId()+"です。");
						
						ReserveList n = r.clone();
						
						if ( ! getReserveDetail(n) ) {
							errmsg = ERRID+"登録は行われましたが、処理が途中で中断しました。確認してください。";
							
							getReserves().add(r);
							ReservesToFile(getReserves(), rsvedFile);
							return false;
						}
	
						if ( isModified(r,n) ) {
							newRsv = n;
						}
						
						break;
					}
				}
			}
			if ( id == null ) {
				if ( cnt == 0 ) {
					errmsg = ERRID+"登録できませんでした";
				}
				else {
					errmsg = ERRID+"TvRock番組表に該当の番組IDが存在しないか、すでに登録済みです";
				}
				return false;
			}
	
		}

		// 更新
		{
			// 情報作成
			String pstr = genPoststr(r, newRsv.getId(), "　予約を変更　");
			
			String reqStr = "http://"+getIPAddr()+":"+getPortNo()+"/"+getUser()+"/regsch?"+pstr;

			reportProgress(DBGID+"EPG予約を更新します(2/2)： "+reqStr);
			
			String header = null;
			String response = null;
			
			for (int i=0; i<retryMax; i++) {
				String[] d = reqGET(reqStr,null);
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
		
		if ( newRsv != r ) {
			errmsg = "TvRock番組表からの情報で内容が変更されました: "+newRsv.getStartDateTime()+"～"+newRsv.getZhh()+":"+newRsv.getZmm()+" "+newRsv.getTitle();
		}
		
		getReserves().add(newRsv);
		ReservesToFile(getReserves(), rsvedFile);

		return true;
	}
	
	
	/*******************************************************************************
	 * 予約更新
	 ******************************************************************************/

	@Override
	public boolean UpdateRdEntry(ReserveList o, ReserveList r) {
		
		errmsg = "";

		String chCode = ((ITEM_CH_EPGGET.equals(r.getCh_name())) ? VALUE_CH_EPGGET : cc.getCH_WEB2REC(r.getCh_name()));
		if ( chCode == null) {
			errmsg = ERRID+"Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}
		
		System.out.println("Run: UpdateRdEntry()");
		
		if ( ! r.getRec_audio().equals(o.getRec_audio()) ) {
			errmsg = MSGID+"予約方式の変更はできません。旧予約方式のまま更新しました： 旧＝"+o.getRec_audio()+" 新＝"+r.getRec_audio();
			r.setRec_audio(o.getRec_audio());
		}
		if ( (r.getContentId() == null || r.getContentId().length() == 0) && r.getPursues() ) {
			System.out.println(MSGID+"プログラム予約では次の指定はできないので規定値に変更しました：番組追従＝なし");
			r.setPursues(false);
		}

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		// 予約パターンID
		// 次回予定日
		// 録画長
		// 開始日時・終了日時
		r.setRec_pattern_id(getRec_pattern_Id(r.getRec_pattern()));
		r.setRec_nextdate(CommonUtils.getNextDate(r));
		r.setRec_min(CommonUtils.getRecMin(r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm()));
		getStartEndDateTime(r);
		
		// CHコード入れ忘れてたよ！
		r.setChannel(chCode);
		
		if ( r.getRec_audio().equals(ITEM_REC_TYPE_EPG) && r.getRec_pattern_id() != 11 ) {
			errmsg = ERRID+"EPG予約に繰り返し指定はできない： "+r.getRec_pattern();
			return false;
		}
		
		// タイトルは200文字まで
		r.setTitle(CommonUtils.substringrb(r.getTitle(), 200));
		
		// 番組詳細は200文字まで(3.7.10β)
		r.setDetail(CommonUtils.substringrb(r.getDetail(), 200));
		
		//　TVRockへ登録する
		String header;
		String response;
		
		// 情報作成
		String pstr = genPoststr(r, r.getId(), "　予約を変更　");

		// 情報送信
		{
			reportProgress("send request(1/1).");
			header = response = null;
			String reqStr = "http://"+getIPAddr()+":"+getPortNo()+"/"+getUser()+"/regsch?"+pstr;
			for (int i=0; i<retryMax; i++) {
				String[] d = reqGET(reqStr,null);
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
		
		// 情報置き換え
		getReserves().remove(o);
		getReserves().add(r);

		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);

		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に更新できました。");
		return(true);
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

		// 削除対象を探す
		ReserveList rx = null;
		for (  ReserveList reserve : getReserves() )  {
			if (reserve.getId().equals(delid)) {
				rx = reserve;
				break;
			}
		}
		if (rx == null) {
			return(null);
		}
		
		//　TVRockから削除する
		String header;
		String response;

		/*
		// 情報作成
		StringBuilder sb = new StringBuilder();
		try {
			sb.append("i="+delid+"&");
			sb.append("submit="+URLEncoder.encode("　予約を削除　",thisEncoding));	// EOL
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String pstr = sb.toString();

		// 情報送信
		{		
			reportProgress("send request(1/2).");
			String reqStr = "http://"+getIPAddr()+":"+getPortNo()+"/"+getUser()+"/regsch?"+pstr;
			String[] d = reqGET(reqStr,null);
			header = d[0];
			response = d[1];
			if ( header != null && response == null ) {
				reportProgress("コネクションがリセットされました。リトライします。");
				String[] d2 = reqGET(reqStr,null);
				header = d2[0];
				response = d2[1];
			}
			if ( response == null ) {
				errmsg = "レコーダーが反応しません。";
				return(null);
			}
		}
		 */

		// はい。
		StringBuilder sb = new StringBuilder();
		try {
			sb.append("i="+delid+"&");
			sb.append("submit="+URLEncoder.encode("はい",thisEncoding)+"&");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String pstr = sb.toString();
		pstr = pstr.substring(0,pstr.length()-1);
		{		
			reportProgress("send request(1/1).");
			header = response = null;
			String reqStr = "http://"+getIPAddr()+":"+getPortNo()+"/"+getUser()+"/regsch?"+pstr;
			for (int i=0; i<retryMax; i++) {
				String[] d = reqGET(reqStr,null);
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
		}
		
		// 予約リストを更新
		getReserves().remove(rx);
		
		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に削除できました。");
		return(rx);
	}
	
	/* ここまで */
	
	
	

	/*******************************************************************************
	 * 非公開メソッド
	 ******************************************************************************/
	
	/* 個別コード－ここから最後まで */

	@Override
	protected String getNewId(String response) {
		String newid = null;
		Matcher ma = Pattern.compile("<a href=\"reg\\?i=([^\"]+?)\">").matcher(response);
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

	private String genPoststr(ReserveList r, String rsvId, String cmd) {
		StringBuilder sb = new StringBuilder();
		try {
			sb.append("title="+URLEncoder.encode(r.getTitle().replaceAll("[\\r\\n]", ""),thisEncoding)+"&");
			sb.append("subtitle="+URLEncoder.encode(r.getDetail().replaceAll("[\\r\\n]", ""),thisEncoding)+"&");
			String chCode = (ITEM_CH_EPGGET.equals(r.getCh_name()) ? VALUE_CH_EPGGET : cc.getCH_WEB2CODE(r.getCh_name()));
			if (chCode.indexOf(":") >= 0) {
				sb.append("station="+URLEncoder.encode(chCode.substring(chCode.indexOf(":")+1),thisEncoding)+"&");
			}
			else {
				sb.append("station="+URLEncoder.encode(chCode,thisEncoding)+"&");
			}
			Matcher ma = Pattern.compile("^(\\d\\d\\d\\d)/(\\d\\d)/(\\d\\d)").matcher(r.getRec_nextdate());
			if (ma.find()) {
				sb.append("year="+Integer.valueOf(ma.group(1))+"&");
				sb.append("mon="+Integer.valueOf(ma.group(2))+"&");
				sb.append("day="+Integer.valueOf(ma.group(3))+"&");
			}
			sb.append("shour="+Integer.valueOf(r.getAhh())+"&");
			sb.append("smin="+Integer.valueOf(r.getAmm())+"&");
			sb.append("ehour="+Integer.valueOf(r.getZhh())+"&");
			sb.append("emin="+Integer.valueOf(r.getZmm())+"&");
			if ( ! r.getPursues() ) {
				// 時間追従なし
				if (r.getRec_pattern_id() < 7) {
					// 毎＊曜日
					sb.append("repeat=2&");
				}
				else if (r.getRec_pattern_id() < 10) {
					// 帯予約（月～木・金・土）
					sb.append("repeat=4&");
					int d = ((CommonUtils.isLateNight(r.getAhh()))?(1):(0));
					for (int c=1; c <= r.getRec_pattern_id()-3; c++) {
						sb.append(String.format("repw%d=true&", (c+d)%7+1));
					}
				}
				else if (r.getRec_pattern_id() == 10) {
					// 毎日
					sb.append("repeat=1&");
				}
				else {
					// 単日
					sb.append("repeat=0&");
				}
			}
			else {
				// 時間追従あり
				sb.append("epgflw=true&");
			}
			if (r.getExec()) {
				sb.append("valid=true&");
			}
			if ( r.getRec_folder() != null ) {
				String rmvalue = text2value(folder, r.getRec_folder());
				for ( int i=0; i<rmvalue.length(); i++ ) {
					String val = rmvalue.substring(i,i+1).equals(VALUE_YES) ? "true" : "";
					sb.append(KEYS_REC_MODE[i]+"="+val+"&");
				}
			}
			if (aspect.size() > 0) {
				sb.append("extmd="+text2value(dvdcompat,r.getRec_dvdcompat())+"&");
				sb.append("idle="+text2value(xchapter,r.getRec_xchapter())+"&");
				sb.append("ready="+text2value(mschapter,r.getRec_mschapter())+"&");
				sb.append("tale="+text2value(mvchapter,r.getRec_mvchapter())+"&");
				sb.append("cname="+text2value(lvoice,r.getRec_lvoice())+"&");
				sb.append("cuscom="+text2value(aspect,r.getRec_aspect())+"&");
				sb.append("pri="+text2value(bvperf,r.getRec_bvperf())+"&");
			}
			else {
				// 後方互換
				for (TextValueSet rd : recDefaults) {
					sb.append(rd.getText()+"="+rd.getValue()+"&");
				}
			}
			sb.append("appexit=true&");
			sb.append("coop=true&");
			//sb.append("cname="+getIPAddr()+"&");
			for ( TextValueSet t : encoder ) {
				if (t.getText().equals(r.getTuner())) {
					sb.append("devno="+t.getValue()+"&");
				}
			}
			sb.append("i="+rsvId+"&");
			if (rsvId.equals("0")) {
				sb.append("lei=-1&");
			}
			sb.append("submit="+URLEncoder.encode(cmd,thisEncoding));	// EOL
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}

	//
	private boolean isModified(ReserveList o, ReserveList r) {
		if ( ! r.getRec_pattern().equals(o.getRec_pattern()) ||
				! r.getAhh().equals(o.getAhh()) ||
				! r.getAmm().equals(o.getAmm()) ||
				! r.getZhh().equals(o.getZhh()) ||
				! r.getZmm().equals(o.getZmm()) ||
				! r.getTitle().equals(o.getTitle())
				) {
			return true;
		}
		return false;
	}
	
	// 録画モード
	private void setSettingRecMode(ArrayList<TextValueSet> tvs) {
		tvs.clear();
		TextValueSet t = add2tvs(tvs,ITEM_REC_MODE_RECORD,VALUE_REC_MODE_RECORD);
		t.setDefval(true);
		add2tvs(tvs,ITEM_REC_MODE_WATCH,VALUE_REC_MODE_WATCH);
		add2tvs(tvs,ITEM_REC_MODE_RANDW,VALUE_REC_MODE_RANDW);
		add2tvs(tvs,ITEM_REC_MODE_FANDO,VALUE_REC_MODE_FANDO);
	}
	
	// 既存ユーザが混乱するのでデフォルトはプログラム予約
	private void setSettingRecType(ArrayList<TextValueSet> tvs) {
		tvs.clear();
		TextValueSet t = add2tvs(tvs,ITEM_REC_TYPE_PROG,VALUE_REC_TYPE_PROG);
		t.setDefval(true);
		add2tvs(tvs,ITEM_REC_TYPE_EPG,VALUE_REC_TYPE_EPG);
	}
	
	//
	private void setSettingBvperf(ArrayList<TextValueSet> tvs) {
		tvs.clear();
		for (int i=-1; i<100; i++) {
			TextValueSet t = new TextValueSet();
			t.setText(String.valueOf(i));
			t.setValue(String.valueOf(i));
			if ( i == 0 ) {
				t.setDefval(true);
			}
			tvs.add(t);
		}
	}
	private void setSettigRecDefaults(ArrayList<TextValueSet> tvs, String[] keys, String res) {
		tvs.clear();
		for (String key : keys) {
			if (key.equals("watchonly") || key.equals("reconly")) {
				Matcher mb = Pattern.compile("<input type=\"checkbox\" name=\""+key+"\"\\s*value=\"([^\"]+?)\"\\s*checked>").matcher(res);
				if (mb.find()) {
					TextValueSet t = new TextValueSet();
					t.setText(key);
					t.setValue(mb.group(1));
					tvs.add(t);
				}
			}
			else {
				Matcher mb = Pattern.compile("<select name=\""+key+"\">(.+?)</select>").matcher(res);
				if (mb.find()) {
					Matcher mc = Pattern.compile("<option value=\"([^\"]+?)\"\\s*selected>").matcher(mb.group(1));
					if (mc.find()) {
						TextValueSet t = new TextValueSet();
						t.setText(key);
						t.setValue(mc.group(1));
						tvs.add(t);
					}
				}
			}
		}
	}
	//
	protected void setSettingEtc(ArrayList<TextValueSet> tvs, String key, int typ, String res) {
		tvs.clear();
		Matcher mb = Pattern.compile("<select name=\""+key+"\">(.+?)</select>").matcher(res);
		if (mb.find()) {
			Matcher mc = Pattern.compile("<option value=\"([^\"]*?)\"(\\s*selected\\s*)?>(.*?)</option>").matcher(mb.group(1));
			while (mc.find()) {
				//if ( mc.group(1).length() == 0 || mc.group(3).length() == 0 ) {
				if ( mc.group(3).length() == 0 ) {
					continue;
				}
				TextValueSet t = new TextValueSet();
				t.setText(mc.group(3));
				t.setValue(mc.group(1));
				if (mc.group(2) != null) {
					t.setDefval(true);
				}
				else {
					t.setDefval(false);
				}
				tvs.add(t);
			}
		}
	}
}
