package tainavi;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * ソース中にPickupとPickedと表記揺れが発生してしまったのが非常に心残り
 */
public class PickedProgram extends TVProgramUtils implements TVProgram,Cloneable {

	//private static final String thisEncoding = "UTF-8";

	public void setDebug(boolean b) { debug = b; }
	private static boolean debug = false;

	/* 必須コード  - ここから */

	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/

	@Override
	public String getTVProgramId() { return tvProgId; }
	private static final String tvProgId = "PickedProgram";

	@Override
	public boolean isAreaSelectSupported() { return false; }

	@Override
	public ProgType getType() { return ProgType.PICKED; }
	@Override
	public ProgSubtype getSubtype() { return ProgSubtype.NONE; }

	@Override
	public PickedProgram clone() {
		return (PickedProgram) super.clone();
	}

	/*******************************************************************************
	 * 個体の特性
	 ******************************************************************************/
	//
	@Override
	public int getTimeBarStart() {return 5;}

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	public static enum WrHeader {

		// 順番をかえなければ、どこに追加してもいい

		CENTER		( "$CH$", true ),
		DATE		( "$DT$", true ),

		// ここから下は別の領域なので追加はNG

		BEND		( "$PI#YY$", false ),	// 項目ごとのフッタ
		STARTMARK	( "$PI#AA$", false ),	// ヘッダ
		ENDMARK		( "$PI#ZZ$", false ),	// フッタ

		;

		private String hdr;
		private boolean marker;

		private WrHeader(String hdr, boolean marker) {
			this.hdr = hdr;
			this.marker = marker;
		}

		// ここはtoString()をOverrideしてよい
		@Override
		public String toString() { return hdr; }
	}

	// 区切り文字
	private static final String S_CR = "\n";

	private final static String txtname = "env"+File.separator+"picked.txt";

	private static final String MSGID = "[ピックアップ] ";
	private static final String DBGID = "[DEBUG]"+MSGID;
	private static final String ERRID = "[ERROR]"+MSGID;

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	/*******************************************************************************
	 * 本体
	 ******************************************************************************/

	@Override
	public boolean loadProgram(String areaCode, boolean force) {

		//
		if ( ! new File(txtname).exists() ) {
			System.out.println(MSGID+"ピックアップリストのファイルがありませんでした: "+txtname);
			return true;
		}

		// 番組リストの追加
		int cnt = load();
		System.out.println(MSGID+"ピックアップリストを取得しました: "+cnt);
		return true;
	}




	// 内部的な

	/*
	 * プログラムリストをファイルに保存する
	 */
	public boolean save() {

		StringBuilder sb = new StringBuilder();

		for (ProgList pl : pcenter) {
			for (ProgDateList cl : pl.pdate) {
				if ( cl.pdetail.size() == 0 ) {
					continue;
				}

				sb.append(WrHeader.STARTMARK);
				sb.append(S_CR);
				sb.append(WrHeader.CENTER);
				sb.append(pl.Center);
				sb.append(WrHeader.BEND);
				sb.append(WrHeader.DATE);
				sb.append(cl.Date);
				sb.append(WrHeader.BEND);

				// 詳細をコピーする
				for (ProgDetailList d : cl.pdetail) {
					sb.append(d.toString());
				}

				sb.append(WrHeader.ENDMARK);
				sb.append(S_CR);
			}
		}

		// 出力
		if ( ! CommonUtils.write2file(txtname, sb.toString()) ) {
        	System.err.println(ERRID+"保存に失敗しました: "+txtname);
        	return false;
		}

		return true;
	}

	private int load() {

		String txt = CommonUtils.read4file(txtname, false);
		if ( txt == null ) {
			return -1;
		}

		ArrayList<ProgList> newplist = new ArrayList<ProgList>();

		int cnt = 0;

		while ( txt.length() > 0 )
		{
			if ( ! txt.startsWith(WrHeader.STARTMARK.toString()+S_CR) ) {
				break;
			}
			int newtail = txt.indexOf(WrHeader.ENDMARK.toString()+S_CR);
			if ( newtail == -1 ) {
				break;
			}

			// 今回の情報
			String data = txt.substring(WrHeader.STARTMARK.toString().length()+1, newtail+1);

			// 次回に続く
			txt = txt.substring(newtail+WrHeader.STARTMARK.toString().length()+1);

			// 放送局名
			String center = null;

			{
				int btail = 0;

				if ( ! data.startsWith(WrHeader.CENTER.toString()) ) {
					break;
				}
				data = data.substring(WrHeader.CENTER.toString().length());

				btail = data.indexOf(WrHeader.BEND.toString());
				if ( btail == -1 ) {
					break;
				}
				center = data.substring(0,btail);
				data = data.substring(btail+WrHeader.BEND.toString().length());
			}

			// 日付
			String date =null;

			{
				int btail = 0;

				if ( ! data.startsWith(WrHeader.DATE.toString()) ) {
					break;
				}
				data = data.substring(WrHeader.DATE.toString().length());

				btail = data.indexOf(WrHeader.BEND.toString());
				if ( btail == -1 ) {
					break;
				}
				date = data.substring(0,btail);
				data = data.substring(btail+WrHeader.BEND.toString().length());
			}

			// 番組詳細へ

			// 解析する
			ProgList pcenter = null;
			for ( ProgList p : newplist ) {
				if ( p.Center.equals(center) ) {
					pcenter = p;
					break;
				}
			}
			if ( pcenter == null ) {
				pcenter = new ProgList();
				pcenter.Center = center;
				pcenter.pdate = new ArrayList<ProgDateList>();
				newplist.add(pcenter);
			}

			ProgDateList pdate = null;
			for ( ProgDateList p : pcenter.pdate ) {
				if ( p.Date.equals(date) ) {
					pdate = p;
					break;
				}
			}
			if ( pdate == null ) {
				pdate = new ProgDateList();
				pdate.Date = date;
				pdate.pdetail = new ArrayList<ProgDetailList>();
				pcenter.pdate.add(pdate);
			}

			cnt += loadDetail(pdate.pdetail,data);
		}

		pcenter = newplist;

		return cnt;
	}

	private int loadDetail(ArrayList<ProgDetailList> pdetail, String txt) {
		int index = 0;
		while ( index < txt.length() )
		{
			// 番組ヘッダを探す
			int newtop = txt.indexOf(ProgDetailList.WrHeader.STARTMARK.toString(),index);
			if ( newtop == -1 ) {
				break;
			}
			newtop += ProgDetailList.WrHeader.STARTMARK.toString().length()+1;

			// 番組フッタを探す
			int newtail = txt.indexOf(ProgDetailList.WrHeader.ENDMARK.toString(),newtop);
			if ( newtail == -1 ) {
				break;
			}
			index = newtail+ProgDetailList.WrHeader.ENDMARK.toString().length()+1;

			// 解析する
			ProgDetailList pdl = new ProgDetailList(txt.substring(newtop,newtail));
			pdetail.add(pdl);

		}

		return pdetail.size();
	}

	/*
	 * 日付でリフレッシュ
	 */
	public void refresh() {

		GregorianCalendar cs = CommonUtils.getCalendar(0);
		if (CommonUtils.isLateNight(cs.get(Calendar.HOUR_OF_DAY))) {
			cs.add(Calendar.DATE, -1);
		}

		ArrayList<ProgList> tPlist = new ArrayList<ProgList>();
		for (ProgList tPl : pcenter) {
			GregorianCalendar c = (GregorianCalendar)cs.clone();
			ProgList pl = new ProgList();
			pl.Area = tPl.Area;
			pl.Center = tPl.Center;
			pl.enabled = tPl.enabled;
			for (int i=0; i<8; i++) {
				ProgDateList tPcl = new ProgDateList();
				tPcl.Date = CommonUtils.getDate(c);
				pl.pdate.add(tPcl);
				c.add(Calendar.DATE, 1);
			}
			for (ProgDateList tPcl : tPl.pdate) {
				for (ProgDateList pcl : pl.pdate) {
					if (pcl.Date.equals(tPcl.Date)) {
						for (ProgDetailList tPdl : tPcl.pdetail) {
							pcl.pdetail.add(tPdl);
						}
						tPcl.pdetail.clear();	// ごみ掃除
					}
				}
			}
			tPlist.add(pl);
		}

		pcenter = tPlist;
	}

	/*
	 * 追加しよう
	 */
	public boolean add(ProgDetailList tvd) {

		ProgList pl = null;
		for (ProgList tPl : pcenter) {
			if (tPl.Center.equals(tvd.center)) {
				pl = tPl;
				break;
			}
		}
		if (pl == null) {
			// 放送局リスト
			pl = new ProgList();
			pl.Area = getDefaultArea();
			pl.Center = tvd.center;
			pl.enabled = true;
			// 日付リスト
			{
				GregorianCalendar c = new GregorianCalendar();
				c.setTime(new Date());
				if (CommonUtils.isLateNight(c.get(Calendar.HOUR_OF_DAY))) {
					c.add(Calendar.DATE, -1);
				}
				for (int i=0; i<8; i++) {
					ProgDateList tPcl = new ProgDateList();
					tPcl.Date = CommonUtils.getDate(c);
					pl.pdate.add(tPcl);
					c.add(Calendar.DATE, 1);
				}
			}
			pcenter.add(pl);
		}

		ProgDateList pcl = null;
		for (ProgDateList tPcl : pl.pdate) {
			if (tPcl.Date.equals(tvd.accurateDate)) {
				pcl = tPcl;
				break;
			}
		}
		if (pcl == null) {
			System.err.println(ERRID+"過去の番組は登録できません:"+tvd.title+"("+tvd.startDateTime+")");
			return false;
		}
		for (ProgDetailList tPdl : pcl.pdetail) {
			if (tPdl.title.equals(tvd.title) && tPdl.start.equals(tvd.start) && tPdl.length == tvd.length) {
				System.err.println(ERRID+"二重登録はできません:"+tvd.title+"("+tvd.startDateTime+")");
				return false;
			}
		}

		// clone()する必要はあるのか？
		pcl.pdetail.add(tvd);

		return true;
	}


	/*
	 * 削除しよう
	 */
	public boolean remove(ProgDetailList data, String center, String date, boolean force) {

		ProgList pl = null;
		for (ProgList tPl : pcenter) {
			if (tPl.Center.equals(center)) {
				pl = tPl;
				break;
			}
		}
		if (pl == null) {
			// なんかおかしくねー
			return false;
		}

		ProgDateList pcl = null;
		for (ProgDateList tPcl : pl.pdate) {
			if (tPcl.Date.equals(date)) {
				pcl = tPcl;
				break;
			}
		}
		if (pcl == null) {
			// 過去ログとかね…
			return false;
		}

		for (ProgDetailList tPdl : pcl.pdetail) {
//			if (tPdl.title.equals(data.title) && tPdl.start.equals(data.start) && tPdl.end.equals(data.end)) {
			if (tPdl.start.equals(data.start) && tPdl.end.equals(data.end)) {
				if (force) {
					pcl.pdetail.remove(tPdl);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * 指定した番組情報に対応するピックアップ情報を返す
	 */
	public ProgDetailList find(ProgDetailList srctvd) {

		for ( ProgList tvpl : pcenter ) {
			if ( ! tvpl.Center.equals(srctvd.center) ) {
				continue;
			}

			for ( ProgDateList tvc : tvpl.pdate ) {
				if ( ! tvc.Date.equals(srctvd.accurateDate) ) {
					continue;
				}

				for ( ProgDetailList tvd : tvc.pdetail ) {
					if ( tvd.startDateTime.equals(srctvd.startDateTime) && tvd.endDateTime.equals(srctvd.endDateTime) ) {
						return tvd;
					}
				}
			}
		}
		return null;
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

