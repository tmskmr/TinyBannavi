package tainavi.pluginrec;

import java.io.File;
import java.net.Authenticator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.ChannelCode;
import tainavi.CommonUtils;
import tainavi.HDDRecorder;
import tainavi.HDDRecorderUtils;
import tainavi.ReserveList;
import tainavi.TextValueSet;
import tainavi.TraceProgram;
import tainavi.HDDRecorder.RecType;
import tainavi.HDDRecorderUtils.MyAuthenticator;


/**
 * REGZAテレビ系
 */
public class PlugIn_RecRD_M190 extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_M190 clone() {
		return (PlugIn_RecRD_M190) super.clone();
	}
	
	//private static final String thisEncoding = "MS932";

	/* 必須コード  - ここから */
	
	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/
	
	public String getRecorderId() { return "REGZA DBR-M190"; }
	public RecType getType() { return RecType.RECORDER; }
	
	// 繰り返し予約をサポートしていない
	@Override
	public boolean isRepeatReserveSupported() { return false; }
	// 番組追従が可能
	@Override
	public boolean isPursuesEditable() { return true; }
	// chvalueをつかってもいーよ
	@Override
	public boolean isChValueAvailable() { return true; }
	// CHコードは入力しなくていい
	@Override
	public boolean isChCodeNeeded() { return false; }
	
	/*******************************************************************************
	 * 予約ダイアログなどのテキストのオーバーライド
	 ******************************************************************************/
	
	@Override
	public String getLabel_Videorate() { return "予約方式"; }
	@Override
	public String getLabel_MsChapter() { return "放送時間連動"; }
	@Override
	public String getLabel_MvChapter() { return "ﾏｼﾞｯｸﾁｬﾌﾟﾀ"; }
	
	@Override
	public String getChDatHelp() { return
			"「レコーダの放送局名」は、予約一覧取得が正常に完了していれば設定候補がコンボボックスで選択できるようになります。"+
			"";
	}
	
	/*******************************************************************************
	 * 定数
	 ******************************************************************************/
	
	protected String getDefFile() { return "env/rzparam_m190.def"; }
	
	private static final String RSVINFOEXPR = "([0-9a-f]+?) (\\d\\d\\d\\d-\\d\\d-\\d\\dT(\\d\\d):(\\d\\d):\\d\\d) (\\d\\d\\d\\d-\\d\\d-\\d\\dT(\\d\\d):(\\d\\d):\\d\\d) (.+?) (.+?) (.+?) (.+?)[\\r\\n]";

	private static final String ITEM_REQUEST_TYPE_DT  = "日時指定予約";
	private static final String ITEM_REQUEST_TYPE_RZ  = "RZｽｹｼﾞｭｰﾗ予約";
	private static final String ITEM_REQUEST_TYPE_DTX = "本体予約(日時)\0#ff0000";
	private static final String ITEM_REQUEST_TYPE_HON = "本体予約\0#ff0000";
	private static final String ITEM_REQUEST_TYPE_REN = "連ﾄﾞﾗ予約\0#ff0000";

	private static final String ID_REQUEST_TYPE_DT   = "00000000";
	private static final String ID_REQUEST_TYPE_RZ   = "00000004";
	private static final String ID_REQUEST_TYPE_DTX  = "00000008";
	private static final String ID_REQUEST_TYPE_HON  = "0000000c";
	private static final String ID_REQUEST_TYPE_REN  = "0000000d";

	//private static final String ITEM_FOLLW_OFF  = "切";
	private static final String ITEM_FOLLW_ON   = "入";
	
	// ログ関連
	
	private final String MSGID = "["+getRecorderId()+"] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final String DBGID = "[DEBUG]"+MSGID;
	
	/*******************************************************************************
	 * CHコード設定、エラーメッセージ
	 ******************************************************************************/

	public ChannelCode getChCode() {
		return cc;
	}
	private ChannelCode cc = new ChannelCode(getRecorderId());
	
	public String getErrmsg() {
		return(errmsg.replaceAll("\\\\r\\\\n", ""));
	}
	protected void setErrmsg(String s) { errmsg = s; }
	private String errmsg = "";

	/*******************************************************************************
	 * フリーオプション関係
	 ******************************************************************************/
	
	// ないよ
	
	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	private String rsvedFile = "";
	
	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
	
	// ないよ
	
	/*******************************************************************************
	 * チャンネルリモコン機能
	 ******************************************************************************/

	public boolean ChangeChannel(String Channel) {
		return false;
	}

	@Override
	public void shutdown() {
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		//
		reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/remote.htm?key=40BF12", null);
		//
		System.out.println("send shutdown request to "+getBroadcast()+","+getMacAddr());
	}

	/*******************************************************************************
	 * レコーダーから予約一覧を取得する
	 ******************************************************************************/
	
	public boolean GetRdReserve(boolean force)
	{
		System.out.println("レコーダから予約一覧を取得します("+force+")： "+getRecorderId()+"("+getIPAddr()+":"+getPortNo()+")");

		errmsg = "";
		
		String defFile = getDefFile();
		
		// 選択肢はREGZAからとれないので定義ファイルに固定で記述
		String res = CommonUtils.read4file(defFile, false);
		if ( res == null ) {
			errmsg = ERRID+"設定ファイルが取得できない： "+defFile;
			return false;
		}
		vrate = new ArrayList<TextValueSet>();
		device = new ArrayList<TextValueSet>();
		mvchapter = new ArrayList<TextValueSet>();
		mschapter = new ArrayList<TextValueSet>();
		for ( String s : res.split("[\r\n]+") ) {
			String[] b = s.split(",");
			if ( b.length >= 3 ) {
				TextValueSet t = new TextValueSet() ;
				t.setText(b[1]) ;
				t.setValue(b[2]) ;
				
				if ( b[0].equals("10") ) {
					vrate.add(t) ;
				}
				else if ( b[0].equals("12") ) {
					device.add(t) ;
				}
				else if ( b[0].equals("19") ) {
					mvchapter.add(t) ;
				}
				else if ( b[0].equals("102") ) {
					mschapter.add(t) ;
				}
			}
		}
		
		// 予約一覧をキャッシュから
		rsvedFile = "env/reserved."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		
		String chValueTFile = "env/chvalue."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		
		File f = new File(rsvedFile);
		File g = new File(chValueTFile);
		if ( force == false && f.exists() && g.exists() ) {
			// キャッシュから読み出し（予約一覧）
			ArrayList<ReserveList> newrl = ReservesFromFile(rsvedFile);
			ArrayList<TextValueSet> newcv = TVSload(chValueTFile);
			if ( newrl != null && (newcv != null && newcv.size() > 0) ) {
				setReserves(newrl);
				chvalue = newcv;
				return true;
			}
		}

		// 放送局名一覧の取得
		if ( ! getRecChNameList() ) {
			errmsg = ERRID+"放送局一覧が取得できませんでした。対応していないレコーダ／テレビなのかもしれません。";
			return false;
		}
		
		TVSsave(chvalue, chValueTFile);
		// 予約一覧の取得・キャッシュへの保存
		reportProgress("予約一覧を取得します(1/1).");
		ArrayList<ReserveList> newReserveList = getRegzaReserveList();
		if ( newReserveList == null ) {
			return(false);
		}

		// リストの置き換え
		setReserves(newReserveList);
		
		// 詳細情報の取得
		System.out.println("========");
		for (int i=0; i<getReserves().size(); i++) {
			ReserveList e = getReserves().get(i);
			
			reportProgress(String.format("[%s] %s\t%s\t%s\t%s:%s\t%s:%s\t%sm\t%s\t%s\t%s\t%s",
					(i+1), e.getId(), e.getRec_pattern(), e.getRec_nextdate(), e.getAhh(), e.getAmm(), e.getZhh(),	e.getZmm(),	e.getRec_min(), e.getRec_mode(), e.getTitle(), e.getChannel(), e.getCh_name()));
		}
		System.out.println("========");

		// キャッシュファイルに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		return(true);
	}
	
	/*******************************************************************************
	 * 新規予約
	 ******************************************************************************/
	
	public boolean PostRdEntry(ReserveList r) {
		
		System.out.println("Run: PostRdEntry("+r.getTitle()+")");

		errmsg = "";
		
		//
		if (cc.getCH_WEB2CODE(r.getCh_name()) == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}
		
		GregorianCalendar c = CommonUtils.getCalendar(r.getRec_pattern());
		if (c == null) {
			errmsg = "【警告】日付指定しか利用出来ません。" ;
			return(false) ;
		}
		
		//
		int stepc = 1;
		int steps = (r.getRec_mode().equals(ITEM_REQUEST_TYPE_DT))?(3):(4);
		
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		String header;
		String response;
		
		// 現在の予約IDのリストを作る
		ArrayList<ReserveList> oldrl = null;
		{
			reportProgress(String.format("%s(%d/%d).","予約登録前の予約一覧を取得します",stepc++,steps));
			
			oldrl = getRegzaReserveList();
			if ( oldrl == null ) {
				return(false);
			}
		}
		
		// 予約パターンID
		// 次回予定日
		// 録画長
		// 開始日時・終了日時
		r.setRec_pattern_id(getRec_pattern_Id(r.getRec_pattern()));
		r.setRec_nextdate(CommonUtils.getNextDate(r));
		r.setRec_min(CommonUtils.getRecMin(r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm()));
		getStartEndDateTime(r);
		
		if ( r.getRec_mode().equals(ITEM_REQUEST_TYPE_RZ) ) {
			if ( r.getRec_mschapter().equals(ITEM_FOLLW_ON) ) {
				r.setPursues(true);
			}
		}
		
		// RDへ情報送信
		if ( r.getRec_mode().equals(ITEM_REQUEST_TYPE_DT) ) {
			// POSTデータを変換する
			HashMap<String, String> pdat = modPostdata(r);

			// RDへの情報作成
			String pstr = joinPoststrRsvDate(pdat);

			reportProgress(String.format("%s(%d/%d).","予約を登録します",stepc++,steps));
			
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/rsvdate.htm?"+pstr, null);
			header = d[0];
			response = d[1];
			
			if (response == null) {
				errmsg = "レコーダーが反応しません";
				return(false);
			}
			
			if ( ! response.substring(0,1).equals("0") ) {
				errmsg = "予約の登録に失敗しました。";
				return(false);
			}
		}
		else {
			
			{
				// POSTデータを変換する
				HashMap<String, String> pdat = modPostdata(r);
				
				// RDへの情報作成
				String pstr = joinPoststrRsvCnv(pdat);
				
				reportProgress(String.format("%s(%d/%d).","番組IDを取得します",stepc++,steps));
				
				String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/rsvcnv.htm?"+pstr, null);
				header = d[0];
				response = d[1];
				
				if (response == null) {
					errmsg = "レコーダーが反応しません";
					return(false);
				}
				
				Matcher ma = Pattern.compile("content_id=(.+?)[\\n\\r]+?start_time=(.+?)[\\n\\r]+?end_time=(.+?)[\\n\\r]+?title_name=(.*?)[\\n\\r]").matcher(response);
				if ( ! ma.find() ) {
					errmsg = "番組IDの取得に失敗しました。";
					return(false);
				}
				
				r.setContentId(ma.group(1));
				
				GregorianCalendar ca = CommonUtils.getCalendar(ma.group(2));
				GregorianCalendar cz = CommonUtils.getCalendar(ma.group(3));
				r.setRec_pattern(CommonUtils.getDate(ca));
				System.err.println(CommonUtils.getDateTime(ca));
				r.setAhh(String.format("%02d",ca.get(Calendar.HOUR_OF_DAY)));
				r.setAmm(String.format("%02d",ca.get(Calendar.MINUTE)));
				r.setZhh(String.format("%02d",cz.get(Calendar.HOUR_OF_DAY)));
				r.setZmm(String.format("%02d",cz.get(Calendar.MINUTE)));
				r.setRec_pattern_id(getRec_pattern_Id(r.getRec_pattern()));
				r.setRec_nextdate(CommonUtils.getNextDate(r));
				r.setRec_min(CommonUtils.getRecMin(r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm()));
				getStartEndDateTime(r);
				
				r.setTitle(ma.group(4));
				r.setTitlePop(TraceProgram.replacePop(r.getTitle()));
			}
			
			{
				// POSTデータを変換する
				HashMap<String, String> pdat = modPostdata(r);
				
				// RDへの情報作成
				String pstr = joinPoststrRsvId(pdat);
				
				reportProgress(String.format("%s(%d/%d).","予約を登録します",stepc++,steps));
				
				String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/rsvid.htm?"+pstr, null);
				header = d[0];
				response = d[1];
				
				if ( ! response.substring(0,1).equals("0") ) {
					errmsg = "予約の登録に失敗しました。";
					return(false);
				}
			}
			
			errmsg = "REGZA本体からの情報で内容が変更されました: "+r.getStartDateTime()+"～"+r.getZhh()+":"+r.getZmm()+" "+r.getTitle();
		}

		// 予約IDのリフレッシュ
		ArrayList<ReserveList> newrl = null;
		{
			reportProgress(String.format("%s(%d/%d).","予約IDを取得します",stepc++,steps));
			
			newrl = getRegzaReserveList();
			if ( newrl == null ) {
				return(false);
			}
			
			ReserveList id = null;
			for ( ReserveList newid : newrl ) {
				id = newid;
				for ( ReserveList oldid : oldrl ) {
					if ( oldid.getId().equals(newid.getId()) ) {
						id = null;
						break;
					}
				}
				if ( id != null ) {
					r.setId(id.getId());
					newrl.remove(id);
					newrl.add(r);
					break;
				}
			}
			if ( id == null ) {
				errmsg = "予約IDの取得に失敗しました。";
				return(false);
			}
		}
		
		// 予約リストを更新
		setReserves(newrl);
		
		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に登録できました。");
		return(true);
	}

	/*******************************************************************************
	 * 予約更新
	 ******************************************************************************/
	
	public boolean UpdateRdEntry(ReserveList o, ReserveList r) {
		
		System.out.println("Run: UpdateRdEntry()");
		
		errmsg = "";

		//
		if ( ! r.getModEnabled() ) {
			errmsg = "【警告】鯛ナビから予約したエントリ以外は更新できません。" ;
			return(false);
		}
		
		GregorianCalendar c = CommonUtils.getCalendar(r.getRec_pattern());
		if (c == null) {
			errmsg = "【警告】日付指定しか利用出来ません。" ;
			return(false) ;
		}
		
		reportProgress("既存の予約を削除します<1/2>.");
		if ( RemoveRdEntry(o.getId()) == null ) {
			return(false);
		}

		reportProgress("新規の予約を登録します<2/2>.");
		if ( PostRdEntry(r) == false ) {
			return(false);
		}

		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に更新できました。");
		return(true);
	}

	/*******************************************************************************
	 * 予約削除
	 ******************************************************************************/
	
	public ReserveList RemoveRdEntry(String delid) {
		
		System.out.println("Run: RemoveRdEntry()");
		
		errmsg = "";

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

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		String header;
		String response;

		// RDに削除要請
		{		
			reportProgress("予約を削除します(1/1).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/rsvdel.htm?reserve_id="+rx.getId(), null);
			header = d[0];
			response = d[1];
			
			if (response == null) {
				errmsg = "レコーダーが反応しません";
				return(null);
			}
			
			if ( ! response.substring(0,1).equals("0") ) {
				errmsg = "予約の削除に失敗しました。";
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
	

	
	/* 個別コード－ここから最後まで */

	/*******************************************************************************
	 * 非公開メソッド
	 ******************************************************************************/
	
	/*
	 *	予約一覧を取得する 
	 */
	private ArrayList<ReserveList> getRegzaReserveList() {
		return _getRegzaReserveList("/remote/rsvlist.htm");
	}
	private ArrayList<ReserveList> _getRegzaReserveList(String html) {
		
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		String header="";
		String response="";
		
		//　RDから予約一覧を取り出す
		{
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+html,null);
			header = d[0];
			response = d[1];
			
			if (response == null) {
				errmsg = "レコーダーが反応しません";
				return(null);
			}
			
			if ( ! response.substring(0,1).equals("0") ) {
				errmsg = "予約一覧の取得に失敗しました。";
				return(null);
			}
		}
		
		// 旧リストは全部削除
		ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();
		
		// 予約詳細を作る
		ReserveList entry = new ReserveList();
		Matcher ma = Pattern.compile(RSVINFOEXPR).matcher(response);
		while (ma.find()) {
			
			// 予約番号
			entry.setId(ma.group(1));
			
			// 開始終了日時と録画時間・パターン
			GregorianCalendar ca = CommonUtils.getCalendar(ma.group(2));
			//GregorianCalendar cz = CommonUtils.getCalendar(ma.group(5));
			entry.setRec_pattern(CommonUtils.getDate(ca));
			entry.setAhh(ma.group(3));
			entry.setAmm(ma.group(4));
			entry.setZhh(ma.group(6));
			entry.setZmm(ma.group(7));
			entry.setRec_pattern_id(getRec_pattern_Id(entry.getRec_pattern()));
			entry.setRec_nextdate(CommonUtils.getNextDate(entry));
			entry.setRec_min(String.valueOf(CommonUtils.getDiffDateTime(ma.group(2),ma.group(5))/60000L));
			getStartEndDateTime(entry);
			
			// チャンネル
			entry.setChannel(ma.group(8));
			entry.setCh_name(cc.getCH_CODE2WEB(entry.getChannel()));
			
			// 録画先
			entry.setRec_device(ma.group(9));
			
			// 本体予約
			if ( ma.group(10).equals(ID_REQUEST_TYPE_DT) ) {
				entry.setModEnabled(true);
				entry.setRec_mode(ITEM_REQUEST_TYPE_DT);
			}
			else if ( ma.group(10).equals(ID_REQUEST_TYPE_RZ) ) {
				entry.setModEnabled(true);
				entry.setRec_mode(ITEM_REQUEST_TYPE_RZ);
			}
			else if ( ma.group(10).equals(ID_REQUEST_TYPE_DTX) ) {
				entry.setModEnabled(false);
				entry.setRec_mode(ITEM_REQUEST_TYPE_DTX);
			}
			else if ( ma.group(10).equals(ID_REQUEST_TYPE_HON) ) {
				entry.setModEnabled(false);
				entry.setRec_mode(ITEM_REQUEST_TYPE_HON);
			}
			else if ( ma.group(10).equals(ID_REQUEST_TYPE_REN) ) {
				entry.setModEnabled(false);
				entry.setRec_mode(ITEM_REQUEST_TYPE_REN);
			}
			else {
				entry.setModEnabled(false);
				entry.setRec_mode(String.format("★未定義(%s)", ma.group(10)));
			}

			// タイトル
			String title = CommonUtils.unEscape(ma.group(11)).replaceAll("<BR>","");
			entry.setTitle(title);
			entry.setTitlePop(TraceProgram.replacePop(title));

			// タイトル自動補完フラグなど本体からは取得できない情報を引き継ぐ
			copyAttributes(entry, getReserves());
			
			// 予約情報を保存
			newReserveList.add(entry.clone());
		}
		
		return(newReserveList);
	}

	/**
	 * レコーダの放送局名＆CHコードをログに出力する
	 */
	private boolean getRecChNameList() {
		
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		String url = "http://"+getIPAddr()+":"+getPortNo()+"/remote/channel2.htm";
		String[] d = reqGET(url, null);
		if (d[1] == null) {
			errmsg = "レコーダーが反応しません： "+url;
			return false;
		}
		
		ArrayList<TextValueSet> newcv = new ArrayList<TextValueSet>();
		for ( String s : d[1].split("[\\r\\n]+") ) {
			String[] e = s.split("\\s+",4);
			if ( e.length == 4 ) {
				TextValueSet t = new TextValueSet();
				t.setText(e[3]);
				t.setValue(e[0]);
				newcv.add(t);
			}
		}
		if ( newcv.size() == 0 ) {
			errmsg = "放送局リストが取得できませんでした。非対応機種なのかもしれません： "+url;
			return false;
		}
		chvalue = newcv;
		
		if (getDebug()) {
			System.err.println("=== CHコード一覧 for "+getRecorderId()+" ===");
			for ( TextValueSet t : chvalue ) {
				System.err.println(String.format("レコーダの放送局名＆CHコード＝\"%s\" 放送局＝%s",t.getValue(),t.getText()));
			}
			System.err.println("=============================");
		}
		
		return true;
	}
	
	// レコーダーから取得できない情報は直接コピー
	@Override
	protected void copyAttributes(ReserveList entry, ArrayList<ReserveList> reserves) {
		for ( ReserveList e : reserves ) {
			if ( e.getId().equals(entry.getId()) ) {
				// 鯛ナビの内部フラグ
				entry.setAutocomplete(e.getAutocomplete());
				// 予約一覧からは取得できない情報
				if ( ! e.getModEnabled() && ! e.getTitle().equals("") ) {
					entry.setTitle(e.getTitle());
					entry.setTitlePop(e.getTitlePop());
				}
				entry.setDetail(e.getDetail());
				entry.setRec_genre(e.getRec_genre());
				entry.setRec_mschapter(e.getRec_mschapter());
				entry.setRec_mvchapter(e.getRec_mvchapter());
				return;
			}
		}
	}
	private String joinPoststrRsvDate(HashMap<String, String> pdat) {
		
		String[] pkeys = {
			"start_time",
			"end_time",
			"ch_code",
			"media",
			"indexing"
		};
		
		return _joinPoststr(pdat,pkeys);
	}
	private String joinPoststrRsvCnv(HashMap<String, String> pdat) {
		
		String[] pkeys = {
			"start_time",
			"ch_code"
		};
		
		return _joinPoststr(pdat,pkeys);
	}
	private String joinPoststrRsvId(HashMap<String, String> pdat) {
		
		String[] pkeys = {
			"content_id",
			"follow",
			"media",
			"indexing"
		};
		
		return _joinPoststr(pdat,pkeys);
	}
	private String _joinPoststr(HashMap<String, String> pdat, String[] pkeys) {
		
		String pstr = "";
		for ( String key : pkeys ) {
			if (pdat.containsKey(key)) {
				pstr += key+"="+pdat.get(key)+"+";
			}
		}
		pstr = pstr.substring(0, pstr.length()-1);
		
		System.err.println("poststr: "+pstr);
		
		return(pstr);
	}
	
	private HashMap<String, String> modPostdata(ReserveList r) {

		HashMap<String, String> newdat = new HashMap<String, String>();
		try {
			
			// 録画チャンネル
			newdat.put("ch_code", cc.getCH_WEB2CODE(r.getCh_name()));
			
			// 開始終了日時
			newdat.put("start_time", CommonUtils.getIsoDateTime(CommonUtils.getCalendar(r.getStartDateTime())));
			newdat.put("end_time", CommonUtils.getIsoDateTime(CommonUtils.getCalendar(r.getEndDateTime())));
			
			// 番組ID
			newdat.put("content_id", r.getContentId());
			
			// 番組追跡
			newdat.put("follow", text2value(mschapter, r.getRec_mschapter()));
			
			// 自動チャプター関連
			newdat.put("indexing", text2value(mvchapter, r.getRec_mvchapter()));
			
			// 記録先
			newdat.put("media", text2value(device, r.getRec_device()));
			
			// 保護
			//newdat.put("protect", text2value(autodel, r.getRec_autodel()));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return(newdat);
	}
}
