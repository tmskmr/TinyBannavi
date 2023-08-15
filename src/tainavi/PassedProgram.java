package tainavi;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.ProgDetailList.WrHeader;
import tainavi.TVProgramIterator.IterationType;

/**
 * 過去ログを処理するクラス
 */
public class PassedProgram extends TVProgramUtils implements TVProgram,Cloneable {

	//private static final String thisEncoding = "UTF-8";

	public void setDebug(boolean b) { debug = b; }
	private static boolean debug = false;

	/* 必須コード  - ここから */

	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/

	@Override
	public String getTVProgramId() { return tvProgId; }
	private static final String tvProgId = "PassedProgram";

	@Override
	public boolean isAreaSelectSupported() { return false; }

	@Override
	public ProgType getType() { return ProgType.PASSED; }
	@Override
	public ProgSubtype getSubtype() { return ProgSubtype.NONE; }

	public PassedProgram clone() {
		return (PassedProgram) super.clone();
	}

	// リフレッシュされないようにする
	@Override
	public void refresh() {}

	/*******************************************************************************
	 * 個体の特性
	 ******************************************************************************/

	@Override
	public int getTimeBarStart() {return 5;}

	//private int getDogDays() { return 7; }

	public void setUseXML(boolean b) { useXML = b; }
	private static boolean useXML = false;


	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	public static final String SEARCH_RESULT_CENTER = "$SRCRESCNT$";

	private static final String MSGID = "[過去ログ] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// コンポーネント以外

	/**
	 * 保存先のディレクトリを設定する
	 * @param s
	 */
	public void setPassedDir(String s) {
		dname = s;
	}
	private static String dname = "passed";

	public int getProgCount() { return progcount; }
	private int progcount = 0;


	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public PassedProgram() {
		super();
		cleanup();
	}

	/**
	 * 情報を全部破棄
	 */
	private void cleanup() {

		crlist = new ArrayList<Center>();		// 番組情報を全部破棄
		sortedcrlist = new ArrayList<Center>();	// 番組情報を全部破棄
		pcenter = new ArrayList<ProgList>();	// 番組情報を全部破棄

	}


	/*******************************************************************************
	 * 本体
	 ******************************************************************************/

	public boolean loadProgram(String areaCode, boolean force) { return true; }	// 使用しない。ダミー。

	/**
	 * 指定の日付のすべての過去ログを取得する
	 * @param date
	 * @return
	 */
	public boolean loadAllCenters(String date) {
		return loadByCenter(date,null);
	}

	final String expr_a = "^(\\d)_(\\d+)_(.+)\\.";
	final String expr_z = "(xml|txt)$";

	/*
	 * 指定した放送局の期間分の番組情報を読み込む
	 */
	public ProgList loadByCenterDates(String startDate, int max, int interval, String center){
		ProgList pl = new ProgList();
		pl.Center = center;
		pl.enabled = true;

		String today = CommonUtils.getDate529(0, true);
		GregorianCalendar c = CommonUtils.getCalendar(startDate);

		// 日数分ループする
		for (int n=0; n<max; n++){
			String date = CommonUtils.getDate(c);
			// 当日以降になったらループを抜ける
			if (date.compareTo(today) >= 0)
				break;

			// １日分をロードする
			if (loadByCenter(date, center) && pcenter.size() > 0 && pcenter.get(0).pdate.size() > 0){
				// 成功したら追加する
				pl.pdate.add(pcenter.get(0).pdate.get(0));
			}
			else{
				// 失敗したら空の番組情報を追加する
				ProgDateList pcl = new ProgDateList();
				pcl.Date = date;
				pl.pdate.add(pcl);
			}

			// 日付を１日ずらす
			c.add(GregorianCalendar.DAY_OF_MONTH, interval);
		}

		return pl;
	}

	/**
	 * 指定の日付の指定の放送局の過去ログを取得する
	 * @param date
	 * @param center
	 * @return
	 */
	public boolean loadByCenter(String date, String center) {

		progcount = 0;

		final String ymname = dname+File.separator+date.substring(0,7).replace("/", "_");
		final String ddname = ymname+File.separator+date.replace("/", "_");

		File f = new File(ymname);
		if ( ! f.exists() && ! f.isDirectory()) {
			System.out.println(MSGID+"年ディレクトがみつかりません: "+f.getAbsolutePath());
			return false;
		}

		File g = new File(ddname);
		if ( ! g.exists() && ! g.isDirectory()) {
			System.out.println(MSGID+"月日ディレクトがみつかりません: "+g.getAbsolutePath());
			return false;
		}

		// 取得開始

		TatCount tc = new TatCount();

		// 番組情報を全部破棄
		this.cleanup();

		String[] flist = g.list();
		Arrays.sort(flist);
		ArrayList<String> fxlist = new ArrayList<String>();
		for (int i=0; i<flist.length; i++)
		{
			if ( ! flist[i].matches(expr_a+expr_z) )
			{
				if (debug) System.out.println(DBGID+"Invalid file name： "+flist[i]);
				continue;				// ファイル名の書式が合わないものは無視
			}

			String base = flist[i].replaceFirst(expr_z, "");
			if ( ! fxlist.contains(base) )
			{
				fxlist.add(base);
			}
		}

		for ( String fx : fxlist ) {

			Matcher ma = Pattern.compile(expr_a,Pattern.DOTALL).matcher(fx);
			ma.find();

			String fcenter = CommonUtils.unEscape(ma.group(3));

			if ( center != null && ! center.equals(fcenter) )
			{
				if (debug) System.out.println(DBGID+"not selected： "+fx);
				continue;				// 放送局指定がされている場合はその局のみ
			}

			int order = Integer.valueOf(ma.group(2));
			if ( order == 0 )
			{
				if (debug) System.out.println(DBGID+"ignored center： "+fx);
				continue;				// 0 だったらそれは無効放送局の情報
			}

			final String txtname = ddname+File.separator+fx+"txt";
			final String xmlname = ddname+File.separator+fx+"xml";

			// 局リストの追加
			Center cr = new Center();
			cr.setCenter(fcenter);
			cr.setEnabled(true);
			cr.setOrder(order);
			crlist.add(cr);
			sortedcrlist.add(cr);

			// 番組表局リストの追加
			ProgList pl = new ProgList();
			pl.Center = fcenter;
			pl.enabled = true;
			pcenter.add(pl);

			// 日付リストの追加
			ProgDateList pcl = new ProgDateList();
			pcl.Date = date;
			pl.pdate.add(pcl);

			TatCount tx = new TatCount();
			if ( new File(txtname).exists() ) {
				progcount += readTXT(pl.Center,pcl,txtname);
				if (debug) System.err.println(String.format(DBGID+"テキスト形式での読み込みにかかった時間(%.4f秒)： %s",tx.end(),txtname));
			}
			else if ( new File(xmlname).exists() ) {
				progcount += readXML(pl.Center,pcl,date,xmlname);
				if (debug) System.err.println(String.format(DBGID+"XML形式での読み込みにかかった時間(%.4f秒)： %s",tx.end(),xmlname));
				tx.restart();
				writeTXT(pcl,txtname);	//かきこ
				if (debug) System.err.println(String.format(DBGID+"テキスト形式での書き込みにかかった時間(%.4f秒)： %s",tx.end(),txtname));
			}
			else {
				System.err.println(String.format(ERRID+"ファイルがない：　%s.(xml|txt)",ddname+File.separator+fx));
			}
		}

		if (debug)
		{
			for ( ProgList pl : pcenter )
			{
				int cnt = 0;
				for ( ProgDateList pcl : pl.pdate )
				{
					for ( ProgDetailList pdl : pcl.pdetail )
					{
						cnt++;
					}
				}
				System.err.println(DBGID+"取得した過去ログ： "+pl.Center+" "+cnt);
				progcount += cnt;
			}
		}
		System.out.println(String.format(MSGID+"取得しました(%.2f秒)： %s %s %d件",tc.end(),date,(center!=null)?(center):("全放送局"),progcount));

		return true;
	}

	private int readTXT(String center, ProgDateList pcl, String txtname) {

		String txt = CommonUtils.read4file(txtname, false);
		if ( txt == null ) {
			return -1;
		}

		int index = 0;
		while ( index < txt.length() )
		{
			// 番組ヘッダを探す
			int newtop = txt.indexOf(WrHeader.STARTMARK.toString(),index);
			if ( newtop == -1 ) {
				break;
			}
			newtop += WrHeader.STARTMARK.toString().length()+1;

			// 番組フッタを探す
			int newtail = txt.indexOf(WrHeader.ENDMARK.toString(),newtop);
			if ( newtail == -1 ) {
				break;
			}
			index = newtail+WrHeader.ENDMARK.toString().length()+1;

			// 解析する
			ProgDetailList pdl = new ProgDetailList(txt.substring(newtop,newtail));

			// サブタイトル分離
			doSplitSubtitle(pdl);

			pcl.pdetail.add(pdl);

			// 情報を追加
			pdl.center = center;
			pdl.type = this.getType();
		}

		return pcl.pdetail.size();
	}

	private int readXML(String center, ProgDateList pcl, String date, String xmlname) {
		try {

			@SuppressWarnings("unchecked")
			ArrayList<PassedProgramList> da = (ArrayList<PassedProgramList>) CommonUtils.readXML(xmlname);
			if ( da == null )
			{
				System.err.println(ERRID+"読み込みに失敗しました： "+xmlname);
				return -1;
			}

			GregorianCalendar cal = CommonUtils.getCalendar(date);
			for ( int j=0; j<da.size(); j++ )
			{
				PassedProgramList data = da.get(j);

				// 5:00以前から続いている場合の長さ補正
				int prelength = 0;

				// クソバグ対応（GENRE.KIDSを廃止したら過去ログが読めなくなったという）
				if (data.genre == null)
				{
					data.genre = ProgGenre.NOGENRE;
					data.subgenre = ProgSubgenre.NOGENRE_ETC;
				}
				if ( data.genrelist != null )
				{
					for ( int n=0; n<data.genrelist.size(); n++ )
					{
						if (data.genrelist.get(n) == null)
						{
							data.genrelist.set(n, ProgGenre.NOGENRE);
							data.subgenrelist.set(n, ProgSubgenre.NOGENRE_ETC);
						}
					}
				}

				// クソバグ対応（番組詳細のない情報があった）
				if (data.detail == null)
				{
					data.detail="";
				}

				// 後方互換（EDCB番組表で追加された追加番組詳細情報への対応）
				if (data.addedDetail == null)
				{
					data.addedDetail = "";
				}

				//
				data.splitted_title = data.title;
				data.splitted_detail = data.detail;

				// 検索用インデックスを生成
				data.titlePop = TraceProgram.replacePop(data.title);
				data.detailPop = TraceProgram.replacePop(data.detail);

				// 終了時刻・実日付を生成
				String[] Ahm = data.start.split(":",2);
				if ( Ahm.length == 2 )
				{
					// 詳細情報をもつもの
					GregorianCalendar cale = (GregorianCalendar) cal.clone();
					int hh = Integer.valueOf(Ahm[0]);
					int mm = Integer.valueOf(Ahm[1]);

					if ( j == 0 )
					{
						if ( CommonUtils.isLateNight(hh) )
						{
							// １個目で00-05時はうめうめ
							prelength = (5*60 - (hh*60+mm));
						}
						else if ( hh >= 18 && hh < 24 )
						{
							// １個目で18-24時は前日だ
							cale.add(Calendar.DATE, -1);
							prelength = (29*60 - (hh*60+mm));
						}
					}
					else if ( CommonUtils.isLateNight(hh) )
					{
						// ２個目以降で00-05時は翌日だ
						cale.add(Calendar.DATE, 1);
					}

					data.accurateDate = CommonUtils.getDate(cale);

					cale.set(Calendar.HOUR_OF_DAY, hh);
					cale.set(Calendar.MINUTE, mm);
					data.startDateTime = CommonUtils.getDateTime(cale);
					data.start = CommonUtils.getTime(cale);

					cale.add(Calendar.MINUTE, prelength+data.length);
					data.end = CommonUtils.getTime(cale);
					data.endDateTime = CommonUtils.getDateTime(cale);

					data.recmin = CommonUtils.getRecMinVal(data.start,data.end);
				}

				pcl.pdetail.add(data);

				// 情報を追加
				data.center = center;
				data.type = this.getType();
			}

			return pcl.pdetail.size();
		}
		catch ( Exception e )
		{
			System.err.println(ERRID+"過去ログの読み込みに失敗しました： "+xmlname);
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * プログラムリストをファイルに保存する
	 * @param tviterator
	 * @param clst
	 * @param prepCount 何日先のログまで保存するか
	 * @return
	 */
	public boolean save(TVProgramIterator tviterator, ArrayList<Center> clst, int prepCount) {

		TVProgramIterator pli = tviterator.build(clst, IterationType.ALL);

		String curDate = CommonUtils.getDate529(0,true);

		// 古いログは削除
		{
			ArrayList<String> dirs = new ArrayList<String>();
			// リストアップ
			for ( ProgList pl : pli )
			{
				for (ProgDateList cl : pl.pdate)
				{
					if (cl.Date.compareTo(curDate) < 0)
					{
						continue;
					}
					String ymname = dname+File.separator+cl.Date.substring(0,7).replace("/", "_");
					String ddname = ymname+File.separator+cl.Date.replace("/", "_");
					boolean b  = true;
					for (String dir : dirs)
					{
						if (dir.equals(ddname))
						{
							b = false;
							break;
						}
					}
					if (b)
					{
						dirs.add(ddname);
					}
				}
			}

			// 削除
			for (String dir : dirs)
			{
				File d = new File(dir);
				if (d.exists())
				{
					for (String file : d.list())
					{
						if (file.matches(expr_a+expr_z))
						{
							File f = new File(dir+File.separator+file);
							f.delete();
						}
					}
				}
			}
		}

		// 放送局名をキーにプログラムリストをなめる
		TatCount tc = new TatCount();

		pli.rewind();	// まきもどせ

		int centerid = 0;
		for ( ProgList pl : pli )
		{
			++centerid;

			tc.restart();
			String firstD = "";
			String lastD = "";

			int count = 1;
			for ( ProgDateList cl : pl.pdate )
			{
				if (cl.Date.compareTo(curDate) < 0)
				{
					continue;	// 過去日のログは不要
				}

				if ( count++ > prepCount )
				{
					break;		// 指定日数以上は保存しない
				}

				if ( firstD.length() == 0 ) firstD = cl.Date;
				lastD = cl.Date;

				// 出力先の決定
				String ymname = dname+File.separator+cl.Date.substring(0,7).replace("/", "_");
				String ddname = ymname+File.separator+cl.Date.replace("/", "_");
				String xmlname = ddname+File.separator+String.format("%d_%04d_%s.xml", 1, centerid, CommonUtils.escapeFilename(pl.Center));
				String txtname = ddname+File.separator+String.format("%d_%04d_%s.txt", 1, centerid, CommonUtils.escapeFilename(pl.Center));

				for ( String xname : new String[] { dname,ymname,ddname } )
				{
					File f = new File(xname);
					if ( ! f.exists() && ! f.mkdir() )
					{
			        	System.err.println(ERRID+"ディレクトリの作成に失敗しました： "+f.getAbsolutePath());
			        	continue;
					}
				}

				TatCount tx = new TatCount();
				if ( ! useXML ) {
					writeTXT(cl,txtname);
					if (debug) System.err.println(String.format(DBGID+"テキスト形式での保存にかかった時間(%.4f秒)： %s->%s %s",tx.end(),firstD,lastD,pl.Center));
				}
				else {
					writeXML(cl,xmlname);
					if (debug) System.err.println(String.format(DBGID+"XML形式での保存にかかった時間(%.4f秒)： %s->%s %s",tx.end(),firstD,lastD,pl.Center));
				}
			}

			reportProgress(String.format(MSGID+"保存しました(%.2f秒)： %s->%s %s",tc.end(),firstD,lastD,pl.Center));
		}
		return true;

	}

	private boolean writeTXT(ProgDateList pcl, String txtname) {
		StringBuilder sb = new StringBuilder();
		for ( ProgDetailList d : pcl.pdetail ) {
			sb.append(d.toString());
		}
		if ( ! CommonUtils.write2file(txtname, sb.toString()) ) {
			return false;
		}
		return true;
	}

	private boolean writeXML(ProgDateList pcl, String xmlname) {
		// 詳細をコピーする
		ArrayList<PassedProgramList> da = new ArrayList<PassedProgramList>();
		for ( ProgDetailList d : pcl.pdetail )
		{
			PassedProgramList data = new PassedProgramList();
			data.setTitle(d.title);
			data.setDetail(d.detail);
			data.setAddedDetail(d.addedDetail);
			data.setStart(d.start);
			data.setLength(d.length);
			data.setExtension(d.extension);
			data.setNoscrumble(d.noscrumble);
			data.setGenre(d.genre);
			data.setSubgenre(d.subgenre);
			data.setFlag(d.flag);
			data.setOption(d.option);
			da.add(data);
		}

		// 出力
		if ( ! CommonUtils.writeXML(xmlname, da) )
		{
        	return false;
		}

		return true;
	}

	// @see #getDateList()
	private static String pregetdate = "1999/12/31";
	private static String[] dd = new String[0];
	private static final int maxCntDef = 365*5;	// 放送局別過去日表示のための最大カウント

	/**
	 *  ディレクトリ一覧を取得する（日に一回しか検索しない）
	 */
	public String[] getDateList(int maxCntArg) {
		int maxCnt = maxCntDef > maxCntArg ? maxCntDef : maxCntArg;

		String getdate = CommonUtils.getDate529(0,true);
		if ( getdate.compareTo(pregetdate) <= 0 )
		{
			return dd;
		}
		pregetdate = getdate;

		System.out.println(MSGID+"フォルダを検索して過去ログをリストアップします. "+getdate);

		// 最初のエントリーはダミー
		ArrayList<String> da = new ArrayList<String>();
		da.add("過去ログ");

		// 過去日のディレクトリのみが対象
		GregorianCalendar cal = CommonUtils.getCalendar(0);
		if ( CommonUtils.isLateNight(cal) )
		{
			cal.add(Calendar.DAY_OF_MONTH, -1);
		}
		String date = String.format("%04d_%02d_%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH));

		// トップディレクトリは存在するかな？
		File d = new File(dname);
		if ( ! d.exists() && ! d.isDirectory())
		{
			System.out.println(MSGID+"ディレクトリがみつかりません: "+d.getAbsolutePath());
			return da.toArray(new String[0]);
		}

		// 日付降順に処理する
		int cnt = 0;
		String[] dlist = d.list();
		Arrays.sort(dlist);
		for (int j=dlist.length-1; j>=0 && cnt < maxCnt; j--)
		{
			// YYYY_MM形式のもののみ
			Matcher ma = Pattern.compile("^\\d\\d\\d\\d_\\d\\d$").matcher(dlist[j]);
			if ( ! ma.find())
			{
				continue;
			}

			// ディレクトリだよね？
			final String ddname = d.getName()+File.separator+dlist[j];
			File f = new File(ddname);
			if ( ! f.exists() && ! f.isDirectory())
			{
				System.out.println(MSGID+"ディレクトリがみつかりません: "+f.getAbsolutePath());
				return null;
			}

			// 日付降順に処理する
			String[] flist = f.list();
			Arrays.sort(flist);
			for (int i=flist.length-1; i>=0 && cnt<maxCnt; i--)
			{
				// YYYY_MM_DD(曜日)形式で過去日のもののみ
				ma = Pattern.compile("^(\\d\\d\\d\\d_\\d\\d_\\d\\d)").matcher(flist[i]);
				if ( ! ma.find())
				{
					continue;
				}
				if (ma.group(1).compareTo(date) >= 0)
				{
					continue;
				}
				da.add(flist[i].replace("_", "/"));
				cnt++;
			}
		}
		dd = da.toArray(new String[0]);
		return dd;
	}

	/*
	 * 次の日付を取得する
	 */
	public String getNextDate(String dateArg, int count){
		int num = dd.length;

		for (int n=0; n<num-1; n++){
			String date = dd[num-1-n];

			if (date.compareTo(dateArg) <= 0)
				continue;

			count--;
			if (count <= 0)
				return date;
		}

		return null;
	}

	/*
	 * 前の日付を取得する
	 */
	public String getPrevDate(String dateArg, int count){
		int num = dd.length;
		String dateLast = null;

		for (int n=0; n<num; n++){
			String date = dd[n];

			if (date.compareTo(dateArg) >= 0)
				continue;

			count--;
			if (count <= 0)
				return date;

			dateLast = date;
		}

		// 指定したカウント前の日付がなければ最古の日付を返す
		return dateLast;
	}
	/* ここまで */



	/*
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 * ★★★★★　放送地域を取得する（TVAreaから降格）－ここから　★★★★★
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 */

	/*
	 * 公開メソッド
	 */

	//
	@Override
	public String getDefaultArea() {return "東京";}

	//
	public void loadAreaCode() {}

	//
	public void saveAreaCode() {}

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
	public void loadCenter(String code, boolean force) {}

	// 設定ファイルへ書き出し
	public boolean saveCenter() { return false; }

	/*
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 * ★★★★★　放送局を選択する（TVCenterから降格）－ここまで　★★★★★
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 */
}

