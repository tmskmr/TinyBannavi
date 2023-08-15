package tainavi.plugintv;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import epgdump.EIT_CONTROL;
import epgdump.Epgdump;
import epgdump.SVT_CONTROL;
import tainavi.AreaCode;
import tainavi.Center;
import tainavi.CommonUtils;
import tainavi.ContentIdEDCB;
import tainavi.MapCtrl;
import tainavi.MapCtrl.KeyType;
import tainavi.ProgDateList;
import tainavi.ProgDetailList;
import tainavi.ProgList;
import tainavi.TVProgram;
import tainavi.TVProgramUtils;
import tainavi.TatCount;


public class PlugIn_TVPEDCB extends TVProgramUtils implements TVProgram,Cloneable {

	public PlugIn_TVPEDCB clone() {
		return (PlugIn_TVPEDCB) super.clone();
	}

	private static final String thisEncoding = "UTF-8";


	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/

	@Override
	public String getTVProgramId() { return "EDCB"; }

	@Override
	public ProgType getType() { return ProgType.PROG; }
	@Override
	public ProgSubtype getSubtype() { return ProgSubtype.TERRA; }


	/*******************************************************************************
	 * 個体の特性
	 ******************************************************************************/

	@Override
	public int getTimeBarStart() {return 5;}

	private int getDogDays() { return ((getExpandTo8())?(8):(7)); }


	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	private static final String OPTKEY_DIR = "datdir";
	private static final String OPTVAL_DIR_DEFAULT = "D:/PT2/EpgDataCap_Bon/Setting/EpgData/";
	private static final String fnext = "_epg.dat";

	private static final String OPTKEY_URL = "url";
	private static final String OPTVAL_URL_DEFAULT = "http://127.0.0.1:5510/(dat参照をやめる場合はこのコメントを削除)";

	private static final String TEXT_NANSHICHO_HEADER = "臨)";

	private final String MSGID = "["+getTVProgramId()+"] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final String DBGID = "[DEBUG]"+MSGID;

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// 新しい入れ物の臨時格納場所
	private ArrayList<ProgList> newplist;
	private ArrayList<SVT_CONTROL> svtlist;

	// 未定義のフラグの回収場所
	private HashMap<String,String> nf = null;

	private String getProgCacheFile() { return getProgDir()+File.separator+"EDCB.xml"; }

	/*
	 * フリーオプション用のクラス
	 */

	private OptMap opts = null;

	private class OptMap extends MapCtrl {
		@Override
		protected boolean chkOptString() {
			// ディレクトリ設定があるかしら
			{
				String optval = get(OPTKEY_DIR);
				if ( optval == null ) {
					// なければデフォルト
					put(OPTKEY_DIR, OPTVAL_DIR_DEFAULT);
					optval = get(OPTKEY_DIR);
					System.out.println(MSGID+"[EDCB番組表未使用の方は無視してください] EPG.datフォルダはデフォルト値になります： "+optval);
				}
				else {
					System.out.println(MSGID+"[EDCB番組表未使用の方は無視してください] EPG.datフォルダは設定値になります： "+optval);
				}
			}
			// URL設定があるかしら
			{
				String optval = get(OPTKEY_URL);
				if ( optval == null || ! optval.matches("^https?://[^/]+?:\\d+/$") ) {
					/// なければデフォルト
					put(OPTKEY_URL, OPTVAL_URL_DEFAULT);
					optval = get(OPTKEY_URL);
					System.out.println(MSGID+"[EDCB番組表未使用の方は無視してください] EDCBのURLはデフォルト値になります： "+optval);
				}
				else {
					System.out.println(MSGID+"[EDCB番組表未使用の方は無視してください] EDCBのURLは設定値になります： "+optval);
				}
			}

			return true;
		}
	}

	// datファイルをリストアップする
	private ArrayList<String> getDatFiles(String path, String ext) {
		ArrayList<String> dfiles = new ArrayList<String>();
		File d = new File(path);
		if ( d.isDirectory() ) {
			for ( String fn : d.list() ) {
				File fdat = new File(path+fn);
				if ( fdat.isFile() && fn.endsWith(ext) ) {
					dfiles.add(path+fn);
				}
			}
			Collections.sort(dfiles);
			return dfiles;
		}
		else {
			return new ArrayList<String>();
		}
	}


	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public PlugIn_TVPEDCB() {

		super();

		opts = new OptMap();

		// 保存先の設定
		opts.setFilename("env"+File.separator+"options_"+getTVProgramId()+".xml");

		// オプションキーリストの設定
		opts.putdef(OPTKEY_DIR, KeyType.DIR);
		opts.putdef(OPTKEY_URL, KeyType.URL);

		//
		opts.initOptString();

	}

	@Override
	public boolean setOptString(String s) {
		if ( s == null ) {
			if (getDebug()) System.out.println(DBGID+"フリーオプションがリセットされました.");
			return opts.initOptString();
		}
		return opts.setOptString(s);
	}

	@Override
	public String getOptString() {
		// ちょっとちょーだい
		return opts.toString();
	}


	/*******************************************************************************
	 * 番組情報を取得する
	 ******************************************************************************/

	@Override
	public boolean loadProgram(String areaCode, boolean force) {

		// 新しい入れ物（トップ）を用意する
		newplist = new ArrayList<ProgList>();
		svtlist = new ArrayList<SVT_CONTROL>();
		nf = new HashMap<String, String>();

		// トップの下に局ごとのリストを生やす
		for ( Center cr : crlist ) {

			if ( cr.getOrder() <= 0 ) {
				// 設定上無効な局はいらない
				continue;
			}

			// ProgList
			ProgList pl = new ProgList();
			pl.Area = cr.getAreaCode();
			pl.SubArea = cr.getType();
			pl.Center = cr.getCenter();
			pl.BgColor = cr.getBgColor();
			pl.CenterId = cr.getLink();
			pl.enabled = true;
			newplist.add(pl);

			// SVT
			SVT_CONTROL svt = new SVT_CONTROL();
			svt.setServicename(pl.Center);
			setCenterLink(svt,cr.getLink());
			svt.setEnabled(true);
			svtlist.add(svt);

			if (getDebug()) System.err.println(DBGID+"Center added: "+pl.Center+"("+pl.CenterId+")");
		}

		// 局の下に日付ごとのリストを生やす（当日前日からの日跨りを考慮して、前日から７＋１日分）
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(new Date());
		cal.add(Calendar.DATE, -1);
		if ( CommonUtils.isLateNight(cal) ) {
			// ４時までは当日扱いにする
			cal.add(Calendar.DATE, -1);
		}
		GregorianCalendar cale = (GregorianCalendar) cal.clone();
		for (int i=0; i<getDogDays()+1; i++) {
			String date = CommonUtils.getDate(cale);
			for ( ProgList pl : newplist ) {
				ProgDateList cl = new ProgDateList();
				cl.Date = date;
				pl.pdate.add(cl);
			}
			cale.add(Calendar.DATE, 1);
		}

		// 日付の下に番組情報ごとのリストを生やす
		if ( opts.get(OPTKEY_URL) != null && opts.get(OPTKEY_URL).matches("^https?://[^/]+?:\\d+/$") ) {
			//
			int counterMax = getSortedCRlist().size();
			int counter=1;
			for ( Center c : getSortedCRlist() ) {
				_loadProgramFromAPI(c, force, counter++, counterMax);
			}
		}
		else {
			_loadProgramFromDAT(force);
		}

		// 隙間を埋める
		for ( ProgList pl : newplist ) {
			refreshList(pl.pdate);
		}

		// 余分に作成した前日分はカット
		for ( ProgList pl : newplist ) {
			pl.pdate.remove(0);
		}

		// 開始・終了日時を正しい値に計算しなおす
		for ( ProgList pl : newplist ) {
			setAccurateDate(pl.pdate);
		}

		// 古いデータから補完できないかな？
		CompensatesPrograms(newplist);

		// 解析用
		{
			for ( String f : nf.keySet() ) {
				System.err.println(String.format(DBGID+"未定義のフラグです: [%s]",f));
			}
		}

		// 古い番組データを置き換える
		pcenter = newplist;
		svtlist = null;
		return true;
	}

	/* ここまで */



	/*
	 * 非公開メソッド
	 */

	private void _loadProgramFromAPI(Center cr, boolean force, int counter, int counterMax) {
		//
		try {

			// いれるところ
			SVT_CONTROL svt = new SVT_CONTROL();
			ContentIdEDCB.decodeChId(cr.getLink());
			svt.setOriginal_network_id(ContentIdEDCB.getOnId());
			svt.setTransport_stream_id(ContentIdEDCB.getTSId());
			svt.setServive_id(ContentIdEDCB.getSId());
			svt.setServicename(cr.getCenterOrig());
			svt.setEnabled(true);

			svtlist = new ArrayList<SVT_CONTROL>();
			svtlist.add(svt);

			final String progCacheFile = String.format(getProgDir()+File.separator+"TVEDCB_%s.xml.zip", cr.getLink());	// getLink()はCHコード
			String response = null;

			File f = new File(progCacheFile);
			if (force == true ||
					(f.exists() == true && isCacheOld(progCacheFile) == true) ||
					(f.exists() == false && isCacheOld(null) == true)) {

				String url = opts.get(OPTKEY_URL)+"api/EnumEventInfo?ONID="+svt.getOriginal_network_id()+"&TSID="+svt.getTransport_stream_id()+"&SID="+svt.getServive_id()+"&basic=0&count=2000";

				// Web番組表の読み出し
				response = webToBuffer(url, thisEncoding, true);

				// キャッシュファイルの保存
				if ( ! CommonUtils.write2file(progCacheFile, response) ) {
					reportProgress(ERRID+"番組表(キャッシュ)の保存に失敗しました: ("+counter+"/"+counterMax+") "+progCacheFile);
				}

				reportProgress(MSGID+"(オンライン)を取得しました: ("+counter+"/"+counterMax+") "+cr.getCenter()+"    "+url);
			}
			else if (f.exists()) {
				// キャッシュファイルの読み込み
				response = CommonUtils.read4file(progCacheFile, true);
				if ( response == null ) {
					reportProgress(ERRID+"番組表(キャッシュ)の取得に失敗しました: ("+counter+"/"+counterMax+") "+cr.getCenter()+"    "+progCacheFile);
				}
				reportProgress(MSGID+"番組表(キャッシュ)を取得しました: ("+counter+"/"+counterMax+") "+cr.getCenter()+"    "+progCacheFile);
			}
			else {
				reportProgress(ERRID+"番組表(キャッシュ)がみつかりません: ("+counter+"/"+counterMax+") "+cr.getCenter()+"    "+progCacheFile);
				return;
			}

			//

			Matcher ma = Pattern.compile("<eventinfo>(.+?)</eventinfo>", Pattern.DOTALL).matcher(response);
			while ( ma.find() ) {

				Integer evid = null;
				GregorianCalendar cal = null;
				Integer hh = null;
				Integer hm = null;
				Integer length = null;
				String title = null;
				String subtitle = null;
				String detail = null;
				ArrayList<String> content_type = new ArrayList<String>();

				Matcher mb = Pattern.compile("<(.+?)>(.+?)</\\1>", Pattern.DOTALL).matcher(ma.group(1));
				while ( mb.find() ) {
					if ( mb.group(1).equals("eventID") ) {
						evid = Integer.valueOf(mb.group(2));
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
							hm = Integer.valueOf(mc.group(2));
						}
					}
					else if ( mb.group(1).equals("duration") ) {
						length = Integer.valueOf(mb.group(2))/60;
					}
					else if ( mb.group(1).equals("event_name") ) {
						title = CommonUtils.unEscape(mb.group(2));
					}
					else if ( mb.group(1).equals("event_text") ) {
						subtitle = CommonUtils.unEscape(mb.group(2));
					}
					else if ( mb.group(1).equals("event_ext_text") ) {
						detail = CommonUtils.unEscape(mb.group(2));
					}
					else if ( mb.group(1).equals("contentInfo") ) {
						Integer g = null;
						Integer subg = null;
						Matcher mc = Pattern.compile("<(.+?)>(\\d+?)</\\1>", Pattern.DOTALL).matcher(mb.group(2));
						while ( mc.find() ) {
							if ( mc.group(1).equals("nibble1") ) {
								g = Integer.valueOf(mc.group(2));
							}
							else if ( mc.group(1).equals("nibble2") ) {
								subg = Integer.valueOf(mc.group(2));
							}
						}
						if ( g != null && subg != null ) {
							content_type.add(String.format("%X%X", g, subg));
							/* - 結局API経由でも拡張ジャンルコードは取得できなかった -
							if ( ProgGenre.getByIEPG(String.format("%X", g)) == null ) {
								System.out.println(DBGID+"未定義のジャンルコード "+g+", "+subg);
							}
							*/
						}
					}
				}

				//

				EIT_CONTROL eit = new EIT_CONTROL();

				eit.setEvent_id(evid);

				eit.setYy(cal.get(Calendar.YEAR));
				eit.setMm(cal.get(Calendar.MONTH)+1);
				eit.setDd(cal.get(Calendar.DAY_OF_MONTH));

				cal.set(Calendar.HOUR, hh);
				cal.set(Calendar.MINUTE, hm);
				eit.setHh(hh);
				eit.setHm(hm);
				eit.setSs(0);

				int dhm = length % 60;
				int dhh = (length-dhm) / 60;

				eit.setDhh(dhh);
				eit.setDhm(dhm);

				/* - 使わないので要らない -
				cal.add(Calendar.MINUTE, length);
				eit.setEhh(cal.get(Calendar.HOUR));
				eit.setEhm(cal.get(Calendar.DAY_OF_MONTH));
				eit.setEss(0);
				*/

				eit.setTitle(title);
				eit.setSubtitle(subtitle);
				eit.setDetail(detail);
				eit.setContent_type(content_type);

				/* - 使わないので要らない -
				eit.setPerformer(null)
				*/

				int n = 0;
				for ( EIT_CONTROL etmp : svt.getEittop() ) {
					String a = String.format("%04d%02d%02d%02d%02d", etmp.getYy(), etmp.getMm(), etmp.getDd(), etmp.getHh(), etmp.getHm());
					String b = String.format("%04d%02d%02d%02d%02d", eit.getYy(), eit.getMm(), eit.getDd(), eit.getHh(), eit.getHm());
					if ( a.compareTo(b) > 0 ) {
						break;
					}
					n++;
				}

				svt.getEittop().add(n,eit);
			}

			// データ形式の変換
			convEpg2Programs();
		}
		catch (Exception e) {
			reportProgress(ERRID+"番組表の取得で例外が発生しました： "+e.toString());
			e.printStackTrace();
		}
	}

	private void _loadProgramFromDAT(boolean force) {
		//
		final String progCacheFile = getProgCacheFile();
		//
		try {
			//
			File f = new File(progCacheFile);
			if (force == true ||
					(f.exists() == true && isCacheOld(progCacheFile) == true) ||
					(f.exists() == false && isCacheOld(null) == true)) {

				// 参照するファイルの一覧
				ArrayList<String> dfiles = getDatFiles(opts.get(OPTKEY_DIR),fnext);

				// 参照する総ファイル数
				int counterMax = dfiles.size();

				// 入力クラス
				Epgdump epg = new Epgdump();

				// EPGデータの取得
				TatCount tcall = new TatCount();
				TatCount tc = new TatCount();
				for ( int i=0; i<counterMax; i++ ) {
					/*
					if ( ! dfiles.get(i).matches(".*000440D0_epg.dat*") ) {
						continue;
					}
					*/
					tc.restart();
					if ( epg.getEitControl(dfiles.get(i), svtlist) == null ) {
						reportProgress("+データが取得できませんでした.");
						return;
					}
					reportProgress(String.format("%s(EPGデータ)を取得しました[%5.1fMB,%.2f秒]: (%d/%d) %s",getTVProgramId(),new File(dfiles.get(i)).length()/1000000.0D,tc.end(),i+1,counterMax,dfiles.get(i)));
				}

				reportProgress(String.format("%s(EPGデータ)をすべて取得しました[%.2f秒]",getTVProgramId(),tcall.end()));

				epg = null;

				tc.restart();
				reportProgress(String.format("%s(キャッシュ)を保存します： %s",getTVProgramId(),progCacheFile));
				if ( ! CommonUtils.writeXML(progCacheFile, svtlist) ) {
					reportProgress(getTVProgramId()+"(キャッシュ)の保存に失敗しました: "+progCacheFile);
				}
				else {
					reportProgress(String.format("%s(キャッシュ)を保存しました[%5.1fMB,%.2f秒]: %s",getTVProgramId(),new File(progCacheFile).length()/1000000.0D,tcall.end(),progCacheFile));
				}
			}
			else if (f.exists()) {
				TatCount tc = new TatCount();
				reportProgress(String.format("%s(キャッシュ)を取得します（選択した局が多いと時間がかかります）: %s (%.2fMB)",getTVProgramId(),progCacheFile,f.length()/1000000.0D));
				@SuppressWarnings("unchecked")
				ArrayList<SVT_CONTROL> ns = (ArrayList<SVT_CONTROL>) CommonUtils.readXML(progCacheFile);
				if ( ns != null ) {
					svtlist = ns;
					reportProgress(String.format("%s(キャッシュ)を取得しました (%.2f秒): %s",getTVProgramId(),tc.end(),progCacheFile));
				}
				else {
					reportProgress(getTVProgramId()+"(キャッシュ)が取得できません: "+progCacheFile);
					return;
				}
			}
			else {
				reportProgress(getTVProgramId()+"(キャッシュ)がみつかりません: "+progCacheFile);
				return;
			}

			if (getDebug()) {
				for ( SVT_CONTROL svt : svtlist ) {
					System.err.println("[DEBUG] programs loaded: "+svt.getServicename()+" ("+svt.getEittop().size()+")");
				}
			}

			// データ形式の変換
			convEpg2Programs();

		}
		catch (Exception e) {
			// 例外
			System.out.println("Exception: _loadProgram()");
			e.printStackTrace();
		}
	}

	//
	private void convEpg2Programs() {

		for ( SVT_CONTROL svt : svtlist ) {

			// 不要になる領域は早々に処分する
			ArrayList<EIT_CONTROL> eittop = svt.getEittop();
			svt.setEittop(null);

			if ( eittop.size() == 0 ) {
				continue;
			}

			// 登録先の放送局をみつける
			ProgList pl = null;
			for ( int i=0; i<newplist.size(); i++ ) {
				if ( newplist.get(i).CenterId.equals(getCenterLink(svt)) ) {
					pl = newplist.get(i);
					break;
				}
			}
			if ( pl == null ) {
				continue;
			}

			for ( EIT_CONTROL eit : eittop ) {

				ProgDetailList pdl = new ProgDetailList();

				// 開始日時
				GregorianCalendar ca = new GregorianCalendar();
				ca.set(Calendar.YEAR, eit.getYy());
				ca.set(Calendar.MONTH, eit.getMm()-1);
				ca.set(Calendar.DATE, eit.getDd());
				ca.set(Calendar.HOUR_OF_DAY, eit.getHh());
				ca.set(Calendar.MINUTE, eit.getHm());
				ca.set(Calendar.SECOND, 0);
				pdl.start = CommonUtils.getTime(ca);
				pdl.startDateTime = CommonUtils.getDateTime(ca);

				// 登録先の日付をみつける
				GregorianCalendar ct = (GregorianCalendar) ca.clone();
				if ( CommonUtils.isLateNight(ca) ) {
					ct.add(Calendar.DATE,-1);
				}
				String tDate = CommonUtils.getDate(ct);
				ProgDateList pcl = null;
				for ( int i=0; i<pl.pdate.size(); i++ ) {
					if ( pl.pdate.get(i).Date.equals(tDate) ) {
						pcl = pl.pdate.get(i);
						break;
					}
				}
				if ( pcl == null ) {
					continue;
				}

				// 長さ
				pdl.length = eit.getDhh()*60 + eit.getDhm();

				// 終了日時
				GregorianCalendar cz = (GregorianCalendar) ca.clone();
				cz.add(Calendar.MINUTE, pdl.length);
				pdl.end = CommonUtils.getTime(cz);
				pdl.endDateTime = CommonUtils.getDateTime(cz);

				// 番組ID
				pdl.progid = ContentIdEDCB.getContentId(pl.CenterId,eit.getEvent_id());

				// タイトル＆番組詳細
				pdl.title = (eit.getTitle() == null) ? "" : eit.getTitle();
				pdl.detail =
						((eit.getSubtitle() != null)   ? (eit.getSubtitle()+DETAIL_SEP):(""))
						+((eit.getDetail() != null)    ? (eit.getDetail()+DETAIL_SEP):(""))
						+((eit.getPerformer() != null) ? (eit.getPerformer()+DETAIL_SEP):(""));
				pdl.detail = pdl.detail.replaceFirst("[\r\n]+$", "");

				// タイトルから各種フラグを分離する
				doSplitFlags(pdl, nf);

				// ジャンル
				setMultiGenre(pdl,eit.getContent_type());

				// サブタイトル分離
				doSplitSubtitle(pdl);

				// 検索対象外領域にこっそりジャンル文字列を入れる
				pdl.setGenreStr();

				// 検索対象外領域にこっそりID文字列を入れる（progidもここで入る）
				pdl.setContentIdStr();

				// その他フラグ
				pdl.extension = false;
				//pdl.flag = ProgFlags.NOFLAG;
				pdl.nosyobo = false;

				//
				pcl.pdetail.add(pdl);

				//
				pcl.row += pdl.length;
			}
		}
	}

	//
	private void refreshList(ArrayList<ProgDateList> pcenter) {
		// 日付（２９時）をまたいでいるものをコピーする
		for ( int i=0; i<pcenter.size()-1; i++ ) {
			ProgDateList pl = pcenter.get(i);
			if ( pl.pdetail.size() <= 0 ) {
				continue;
			}
			// 基準日時
			GregorianCalendar tbca = CommonUtils.getCalendar(pl.Date+" "+CommonUtils.getTime(getTimeBarStart(),0));
			tbca.add(Calendar.DATE, 1);
			// 最終番組の終了日時
			ProgDetailList pdl = pl.pdetail.get(pl.pdetail.size()-1);
			GregorianCalendar cz = CommonUtils.getCalendar(pdl.endDateTime);
			// ２９時をまたいでいるか
			if ( cz.compareTo(tbca) > 0 ) {
				// またいでいたので翌日の先頭にコピーする
				pdl = pdl.clone();
				pdl.length = (int) ((cz.getTimeInMillis() - tbca.getTimeInMillis())/60000L);
				pcenter.get(i+1).pdetail.add(0,pdl);

				if (getDebug()) System.err.println("[DEBUG] copy "+pdl.startDateTime+" - "+pdl.endDateTime+" to "+pcenter.get(i+1).Date);
			}
		}
		// 隙間を埋める
		for ( ProgDateList pl : pcenter ) {
			ArrayList<ProgDetailList> cur = new ArrayList<ProgDetailList>();
			String preend = pl.Date.substring(0,10)+" "+CommonUtils.getTime(getTimeBarStart(),0);	// 最初の"前番組のおしり"は05:00
			for ( int i=0; i<pl.pdetail.size(); i++ ) {
				ProgDetailList pdl = pl.pdetail.get(i);
				if ( preend.compareTo(pdl.startDateTime) < 0 ) {
					// 前の番組との間に隙間があれば埋める
					ProgDetailList npdl = new ProgDetailList();
					npdl.title = "番組情報がありません";
					npdl.detail = "";
					npdl.length = (int)(CommonUtils.getDiffDateTime(preend, pdl.startDateTime)/60000L);
					cur.add(npdl);
				}
				cur.add(pdl);
				preend = pdl.endDateTime;
			}
			pl.pdetail = cur;
		}

		// 総時間数等を整理する
		for ( ProgDateList pl : pcenter ) {
			// １日の合計分数を足し合わせる
			pl.row = 0;
			for ( ProgDetailList pdl : pl.pdetail ) {
				pl.row += pdl.length;
			}
			// おしりがとどかない場合（デメリット：これをやると、サイト側のエラーで欠けてるのか、そもそも休止なのかの区別がつかなくなる）
			if ( pl.row < 24*60 ) {
				ProgDetailList npdl = new ProgDetailList();
				npdl.title = "番組情報がありません";
				npdl.detail = "";
				npdl.length = 24*60 - pl.row;
				pl.pdetail.add(npdl);
				pl.row += npdl.length;
			}
		}
	}


	/*******************************************************************************
	 * 地域情報を取得する
	 ******************************************************************************/

	// EDCBは地元というか実際に視聴できる局しか情報がとれないね
	@Override
	public String getDefaultArea() { return "すべて"; }

	//
	@Override
	public void loadAreaCode() {
		// 地域一覧の作成
		aclist = new ArrayList<AreaCode>();
		{
			AreaCode ac = new AreaCode();
			ac.setArea("すべて");
			ac.setCode(allCode);
			aclist.add(ac);
		}
		{
			AreaCode ac = new AreaCode();
			ac.setArea("地上");
			ac.setCode(trCode);
			aclist.add(ac);
		}
		{
			AreaCode ac = new AreaCode();
			ac.setArea("ＢＳ");
			ac.setCode(bsCode);
			aclist.add(ac);
		}
		{
			AreaCode ac = new AreaCode();
			ac.setArea("ＣＳ");
			ac.setCode(csCode);
			aclist.add(ac);
		}
	}

	@Override
	public void saveAreaCode() {
	}


	/*******************************************************************************
	 * 放送局情報を取得する
	 ******************************************************************************/

	// 設定ファイルがなければWebから取得
	@SuppressWarnings("unchecked")
	@Override
	public void loadCenter(String code, boolean force) {

		String centerListFile = getCenterListFile(getTVProgramId(), code);

		if (force) {
			File f = new File(centerListFile);
			f.delete();
		}

		File f = new File(centerListFile);
		if (f.exists() == true) {
			ArrayList<Center> tmp = (ArrayList<Center>) CommonUtils.readXML(centerListFile);
			if ( tmp != null ) {

				crlist = tmp;

				// 放送局名変換
				attachChFilters();

				System.out.println(MSGID+"放送局リストを読み込みました: "+centerListFile);
				return;
			}
			else {
				System.out.println(ERRID+"放送局リストの読み込みに失敗しました: "+centerListFile);
			}
		}

		// Web上から放送局の一覧を取得する
		ArrayList<Center> tmpCrList = new ArrayList<Center>();
		ArrayList<Center> trCrList = new ArrayList<Center>();
		ArrayList<Center> bsCrList = new ArrayList<Center>();
		ArrayList<Center> csCrList = new ArrayList<Center>();
		ArrayList<Center> otCrList = new ArrayList<Center>();

		if ( opts.get(OPTKEY_URL) != null && opts.get(OPTKEY_URL).matches("^https?://[^/]+?:\\d+/$") ) {
			// APIを使う
			String url = opts.get(OPTKEY_URL)+"api/EnumService";
			String response = webToBuffer(url,thisEncoding,true);
			if ( response == null ) {
				System.err.println(ERRID+"放送局情報の取得に失敗しました: "+url);
				return;
			}

			reportProgress(MSGID+"放送局情報を取得しました： "+url);

			Matcher ma = Pattern.compile("<serviceinfo>(.+?)</serviceinfo>", Pattern.DOTALL).matcher(response);
			while ( ma.find() ) {
				Integer onid = null;
				Integer tsid = null;
				Integer sid = null;
				String ch_name = null;
				Matcher mb = Pattern.compile("<(.+?)>(.+?)</\\1>", Pattern.DOTALL).matcher(ma.group(1));
				while ( mb.find() ) {
					/*
					 * 解析
					 */

					if ( mb.group(1).equals("service_name") ) {
						ch_name = mb.group(2);
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
				}

				String chid = ContentIdEDCB.getChId(onid, tsid, sid);

				Center cr = new Center();
				cr.setLink(chid);
				cr.setAreaCode(getCenterCode(cr.getLink()));

				if ( tsid == 0x4310 || tsid == 0x4311 ) {
					cr.setCenterOrig(TEXT_NANSHICHO_HEADER+ch_name);
				}
				else {
					cr.setCenterOrig(ch_name);
				}

				cr.setCenter(null);
				cr.setType("");
				cr.setEnabled(true);

				tmpCrList.add(cr);
			}
		}
		else {
			// datファイルを使う
			if ( ! new File(opts.get(OPTKEY_DIR)).isDirectory() ) {
				System.out.println(ERRID+".datファイルの場所が正しくないため放送局リストの読み込みはキャンセルされました: "+opts.get(OPTKEY_DIR));
				return;
			}

			Epgdump epg = new Epgdump();

			ArrayList<String> dfiles = getDatFiles(opts.get(OPTKEY_DIR),fnext);
			if ( dfiles != null ) {
				// SVT
				ArrayList<SVT_CONTROL> svta = new ArrayList<SVT_CONTROL>();
				for ( int cnt=0; cnt<dfiles.size(); cnt++ ) {
					String fpath = dfiles.get(cnt);
					ArrayList<SVT_CONTROL> svttop = epg.getSvtControl(fpath);
					for ( SVT_CONTROL svt : svttop ) {
						epg.enqueueSVT(svta, svt);
					}
					reportProgress(MSGID+"放送局情報を取得しました: ("+(cnt+1)+"/"+dfiles.size()+") "+fpath);
				}
				//
				for ( SVT_CONTROL svt : svta ) {
					Center cr = new Center();
					cr.setLink(getCenterLink(svt));
					cr.setAreaCode(getCenterCode(cr.getLink()));

					if ( svt.getTransport_stream_id() == 0x4310 || svt.getTransport_stream_id() == 0x4311 ) {
						cr.setCenterOrig(TEXT_NANSHICHO_HEADER+svt.getServicename());
					}
					else {
						cr.setCenterOrig(svt.getServicename());
					}

					cr.setCenter(null);
					cr.setType("");
					cr.setEnabled(true);

					int n = 0;
					for ( Center ctmp : tmpCrList ) {
						if ( ctmp.getLink().compareTo(cr.getLink()) < 0 ) {
							break;
						}
						n++;
					}
					tmpCrList.add(n, cr);
				}
			}
		}
		for ( Center cr : tmpCrList ) {
			// 登録順序を整理する
			if ( cr.getAreaCode().equals(trCode) ) {
				addCenter(trCrList,cr);
				//trCrList.add(cr);
			}
			else if ( cr.getAreaCode().equals(bsCode) ) {
				addCenter(bsCrList,cr);
				//bsCrList.add(cr);
			}
			else if ( cr.getAreaCode().equals(csCode) ) {
				addCenter(csCrList,cr);
				//csCrList.add(cr);
			}
			else {
				addCenter(otCrList,cr);
				//otCrList.add(cr);
			}
		}

		ArrayList<Center> newcrlist = new ArrayList<Center>();

		for ( Center cr : trCrList ) {
			newcrlist.add(cr);
		}
		for ( Center cr : bsCrList ) {
			newcrlist.add(cr);
		}
		for ( Center cr : csCrList ) {
			newcrlist.add(cr);
		}
		for ( Center cr : otCrList ) {
			newcrlist.add(cr);
		}

		if ( newcrlist.size() == 0 ) {
			System.err.println(ERRID+"放送局情報の取得結果が０件だったため情報を更新しません");
			return;
		}

		crlist = newcrlist;
		chkCrNameDuped(crlist);	// 放送局名の重複排除
		attachChFilters();		// 放送局名変換
		saveCenter();
	}

	private void addCenter(ArrayList<Center> newcrlist, Center cr) {
		int n=0;
		for ( ; n<newcrlist.size(); n++ ) {
			if ( newcrlist.get(n).getLink().compareTo(cr.getLink()) > 0 ) {
				break;
			}
		}
		newcrlist.add(n,cr);
	}

	//
	private String getCenterLink(SVT_CONTROL svt) {
		return ContentIdEDCB.getChId(svt.getOriginal_network_id(),svt.getTransport_stream_id(),svt.getServive_id());
	}

	//
	private void setCenterLink(SVT_CONTROL svt, String link) {
		if ( ContentIdEDCB.decodeChId(link) ) {
			svt.setOriginal_network_id(ContentIdEDCB.getOnId());
			svt.setTransport_stream_id(ContentIdEDCB.getTSId());
			svt.setServive_id(ContentIdEDCB.getSId());
		}
	}

	//
	private void chkCrNameDuped(ArrayList<Center> crList) {
		HashMap<String,String> crNameMap = new HashMap<String, String>();
		for ( Center cr : crList ){
			if ( crNameMap.get(cr.getCenterOrig()) == null ) {
				crNameMap.put(cr.getCenterOrig(),"BINGO!");
			}
			else {
				for ( int n=2; n<10; n++ ) {
					String nName = cr.getCenterOrig()+"・"+n;
					if ( crNameMap.get(nName) == null ) {
						crNameMap.put(nName,"BINGO!");
						cr.setCenterOrig(nName);
						break;
					}
				}
			}
		}
	}

	//
	private String getCenterCode(String id) {
		Matcher ma = Pattern.compile("^(..)(..)").matcher(id);
		if ( ma.find() ) {
			if ( ma.group(1).equals("00") ) {
				if ( ma.group(2).equals("04") ) {
					return bsCode;
				}
				else {
					return csCode;
				}
			}
			else {
				return trCode;
			}
		}
		return allCode;
	}
}
