package tainavi.plugintv;

import java.io.File;
import java.util.ArrayList;

import tainavi.AreaCode;
import tainavi.Center;
import tainavi.CommonUtils;
import tainavi.ProgList;
import tainavi.TVProgram;



public class PlugIn_CSPtheTelevisionA extends PlugIn_TVPtheTelevision implements TVProgram,Cloneable {

	final String thisEncoding = "UTF-8";

	/* 必須コード  - ここから */

	// 種族の特性
	@Override
	public String getTVProgramId() { return "webザテレビジョン(CSアナ)"; }

	@Override
	public boolean isAreaSelectSupported() { return false; }

	@Override
	public ProgType getType() { return ProgType.PROG; }
	@Override
	public ProgSubtype getSubtype() { return ProgSubtype.CS; }

	//
	public PlugIn_CSPtheTelevisionA clone() {
		return (PlugIn_CSPtheTelevisionA) super.clone();
	}

	private final String MSGID = "["+getTVProgramId()+"] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final String DBGID = "[DEBUG]"+MSGID;

	//
	public boolean loadProgram(String areaCode, boolean force) {

		// 新しい入れ物（トップ）を用意する
		newplist = new ArrayList<ProgList>();

		//
		int counterMax = getSortedCRlist().size();
		int counter=1;
		for ( Center c : getSortedCRlist() ) {
			_loadProgram(c, force, counter++, counterMax);
		}

		// 古い番組データを置き換える
		pcenter = newplist;
		return true;
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
	    		attachChFilters();	// 放送局名変換

				System.out.println("放送局リストを読み込みました: "+centerListFile);
				return;
			}
            else {
				System.err.println("放送局リストの読み込みに失敗しました: "+centerListFile);
	        }
		}

		// Web上から放送局の一覧を取得する
		ArrayList<Center> newcrlist = new ArrayList<Center>();

		String url = "http://www.television.co.jp/programlist/guide.php?type=cs&page=1";
		if ( _loadCenter(newcrlist, code,"csa",url) ) {
			reportProgress("放送局情報を取得しました: (1/1) "+url);
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

	/*
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 * ★★★★★　放送局を選択する（TVCenterから降格）－ここまで　★★★★★
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 */
}
