package tainavi.plugintv;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.AreaCode;
import tainavi.Center;
import tainavi.CommonUtils;
import tainavi.ProgDateList;
import tainavi.ProgList;
import tainavi.TVProgram;



public class PlugIn_CSPTVKingdom110 extends PlugIn_TVPTVKingdom implements TVProgram,Cloneable {

	public PlugIn_CSPTVKingdom110 clone() {
		return (PlugIn_CSPTVKingdom110) super.clone();
	}

	private static final String thisEncoding = "UTF-8";


	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/

	@Override
	public String getTVProgramId() { return "Gガイド.テレビ王国(スカパー!e2)"; }

	@Override
	public ProgType getType() { return ProgType.PROG; }

	@Override
	public ProgSubtype getSubtype() { return ProgSubtype.CS2; }


	/*******************************************************************************
	 * 個体の特性
	 ******************************************************************************/

	private int getDogDays() { return ((getExpandTo8())?(8):(7)); }

	protected String getCSType() { return "cs110"; }

	@Override
	public boolean isAreaSelectSupported() { return false; }


	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	private final String MSGID = "["+getTVProgramId()+"] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final String DBGID = "[DEBUG]"+MSGID;

	private final String PROGCACHE_PREFIX = "TVK_";


	/*******************************************************************************
	 * 部品
	 ******************************************************************************/


	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/


	/*******************************************************************************
	 * 番組情報を取得する
	 ******************************************************************************/
	@Override
	public boolean loadProgram(String areaCode, boolean force) {
		setCacheFileOnly(true);
		String prefix = PROGCACHE_PREFIX + getCSType() + "_" + areaCode;

		if (!force && loadFromProgCache(prefix))
			return true;

		// 入れ物を空にする
		newplist = new ArrayList<ProgList>();
		nf.clear();

		//
		int counterMax = getSortedCRlist().size();
		int counter=1;
		for ( Center c : getSortedCRlist() ) {
			if (!_loadProgram(c, force, counter++, counterMax)){
				newplist = null;
				return false;
			}

			if (isCancelRequested()){
				reportProgress(ERRID+"中止要求があったので番組表の取得にを中止します。");
				newplist = null;
				return false;
			}
		}

//		debugNF();

		// 古い番組データを置き換える
		if (pcenter != null)
			pcenter.clear();
		pcenter = newplist;
		newplist = null;

		saveToProgCache(prefix);

		return true;
	}

	private boolean _loadProgram(Center cr, boolean force, int counter, int counterMax) {
		//
		try {
			// 局リストの追加
			ProgList pl = new ProgList();
			pl.Area = cr.getAreaCode();
			pl.Center = cr.getCenter();
			pl.CenterId = cr.getLink();
			pl.Type = cr.getType();
			pl.BgColor = cr.getBgColor();
			pl.enabled = true;
			newplist.add(pl);

			String response = null;

			//
			final String progCacheFile = String.format(getProgDir()+File.separator+"tvk_%s_%s_%s.html", getCSType(), pl.Area, pl.CenterId);
			//
			File f = new File(progCacheFile);
			if (force == true || f.exists() == false || isCacheOld(progCacheFile)){
				GregorianCalendar c = new GregorianCalendar();
				c.setTime(new Date());
				if (CommonUtils.isLateNight(c.get(Calendar.HOUR_OF_DAY))) {
					c.add(Calendar.DATE, -1);
				}
				//
				String url = String.format("https://www.tvkingdom.jp/chart/%s/%s.action?parentId=9999&childId=%s&id=%s&head=%04d%02d%02d0500&span=24",
						getCSType(), pl.CenterId,pl.CenterId,pl.CenterId, c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));

				response = webToBuffer(url, thisEncoding, true);
				if (response != null){
					CommonUtils.write2file(progCacheFile, response);
					reportProgress(String.format("%s (オンライン)を取得しました: (%d/%d) %s %s",getTVProgramId(),counter,counterMax,pl.Center,url));

					// 連続アクセス規制よけ
					CommonUtils.milSleep(accessWait);
				}
				else if ( f.exists() == false ) {
					return false;
				}

				setCacheFileOnly(false);
			}

			if (response == null){
				if (f.exists()) {
					response = CommonUtils.read4file(progCacheFile, true);
					if ( response == null ) {
						reportProgress(String.format("%s (キャッシュ)が不正です: (%d/%d) %s %s",getTVProgramId(),counter,counterMax,pl.Center,progCacheFile));
						return false;
					}

					reportProgress(String.format("%s (キャッシュ)を取得しました: (%d/%d) %s %s",getTVProgramId(),counter,counterMax,pl.Center,progCacheFile));
				}
				else {
					reportProgress(String.format("%s (キャッシュ)がみつかりません: (%d/%d) %s %s",getTVProgramId(),counter,counterMax,pl.Center,progCacheFile));
					return false;
				}
			}

			// 日付リストの追加
			getDateList(pl);

			// 番組リストの追加
			getPrograms(pl, response);

			// 日付の調整
			setAccurateDate(pl.pdate);
		}
		catch (Exception e) {
			// 例外
			System.out.println("Exception: _loadProgram()");
			e.printStackTrace();
			return false;
		}

		return true;
	}

	//
	private void getDateList(ProgList pl) {
		//
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(new Date());
		if (CommonUtils.isLateNight(c.get(Calendar.HOUR_OF_DAY))) {
			c.add(Calendar.DATE, -1);
		}
		for (int i=0; i<getDogDays(); i++) {
		    ProgDateList cl = new ProgDateList();
			cl.Date = String.format("%04d/%02d/%02d(%s)", c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH), CommonUtils.WDTPTN[c.get(Calendar.DAY_OF_WEEK)-1]);
			cl.row = 0;
			pl.pdate.add(cl);
			c.add(Calendar.DAY_OF_MONTH,1);
		}
	}

	//
	private void getPrograms(ProgList pl, String src) {
		//
		Matcher ma = Pattern.compile("<div class=\"cell-date cell-top cell-station\"(.+?)<div class=\"cell-date cell-station\"").matcher(src);
		for (int i=0; ma.find() && i<getDogDays(); i++) {
			Matcher mb = Pattern.compile("title=\"(\\d+)月\\s*(\\d+)日").matcher(ma.group(1));
			if (mb.find()) {
				String date = String.format("%02d/%02d",Integer.valueOf(mb.group(1)),Integer.valueOf(mb.group(2)));
				for ( int j=0; j<pl.pdate.size(); j++ ) {
					if ( pl.pdate.get(j).Date.substring(5,10).equals(date) ) {
						// 一日分取り込む
						getDetails(pl.pdate.get(j), ma.group(1));
						break;
					}
				}
			}
		}
	}

	/*
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 * ★★★★★　放送地域を取得する（TVAreaから降格）－ここから　★★★★★
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 */

	/*
	 * 公開メソッド
	 */

	public String getDefaultArea() {return "全国";}
	public String getDefaultCode() {return "tokyo";}

	public void loadAreaCode() {
		aclist = new ArrayList<AreaCode>();
		AreaCode ac = new AreaCode();
		ac.setArea(getDefaultArea());
		ac.setCode(getDefaultCode());
		ac.setSelected(true);
		aclist.add(ac);
	}
	public void saveAreaCode() {}

	public String getArea(String code) { return(getDefaultArea()); }
	public String getCode(String area) { return(getDefaultCode()); }
	public String setSelectedAreaByName(String area) { return(getDefaultCode()); }
	public String getSelectedArea() { return(getDefaultArea()); }
	public String getSelectedCode() { return(getDefaultCode()); }

	/*
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 * ★★★★★　放送地域を取得する（TVAreaから降格）－ここまで　★★★★★
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 */



	/*
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 * ★★★★★　放送局を選択する（TVCenterから降格）－ここから　★★★★★
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 */

	/*
	 * 公開メソッド
	 */

	// 設定ファイルがなければWebから取得
	public void loadCenter(String code, boolean force) {

		if ( code == null ) {
			System.out.println(ERRID+"地域コードがnullです.");
			return;
		}

		String centerListFile = getCenterListFile(getTVProgramId(), code);

		if (force) {
			File f = new File(centerListFile);
			f.delete();
		}

		File f = new File(centerListFile);
		if (f.exists() == true) {
			@SuppressWarnings("unchecked")
			ArrayList<Center> tmp = (ArrayList<Center>) CommonUtils.readXML(centerListFile);
			if ( tmp != null ) {

				crlist = tmp;

	    		// 放送局名変換
	    		attachChFilters();

				System.out.println("放送局リストを読み込みました: "+centerListFile);
				return;
			}

			System.out.println("放送局リストの読み込みに失敗しました: "+centerListFile);
		}

		// Web上から放送局の一覧を取得する
		ArrayList<Center> newcrlist = new ArrayList<Center>();

		if (getCSType().equals(csCode)) {
			_loadCenter(newcrlist,code,getCSType(),"https://www.tvkingdom.jp/chart/cs/skylist.action");
		}
		else {
			_loadCenter(newcrlist,code,getCSType(),"https://www.tvkingdom.jp/chart/cs110/e2list.action");
		}

		if ( newcrlist.size() == 0 ) {
			System.err.println(ERRID+"放送局情報の取得結果が０件だったため情報を更新しません");
			return;
		}

		// ソートする
		ArrayList<Center> tmpcrlist = new ArrayList<Center>();
		for (Center cr : newcrlist) {
			int idx = 0;
			for ( Center ncr : tmpcrlist ) {
				if (ncr.getCenter().compareTo(cr.getCenter()) > 0) {
					break;
				}
				idx++;
			}
			tmpcrlist.add(idx, cr);
		}

		crlist = tmpcrlist;
		attachChFilters();	// 放送局名変換
		saveCenter();
	}

	protected boolean _loadCenter(ArrayList<Center> newcrlist, String code, String type, String uri) {

		String response = webToBuffer(uri,thisEncoding,true);
		if ( response == null ) {
			System.out.println("放送局情報の取得に失敗しました: "+uri);
			return false;
		}

		reportProgress("放送局情報を取得しました: (1/1) "+uri);

		// 局名リストに追加する

		Matcher ma = Pattern.compile("<select name=\"id\" id=\"id\" class=\"inputTxt\">(.+?)</select>").matcher(response);
		if (ma.find()) {
			Matcher mb = Pattern.compile("<option value=\"(.+?)\"( selected)?>(.+?)</option>").matcher(ma.group(1));
			while (mb.find()) {
				String centerName = mb.group(3);
				Center cr = new Center();
				cr.setAreaCode(code);
				cr.setCenterOrig(centerName);
				cr.setLink(mb.group(1));
				cr.setType(type);
				cr.setEnabled(true);
				newcrlist.add(cr);
			}
		}

		return true;
	}

	/*
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 * ★★★★★　放送局を選択する（TVCenterから降格）－ここまで　★★★★★
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 */
}
