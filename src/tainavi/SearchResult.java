package tainavi;

import java.util.ArrayList;


public class SearchResult extends TVProgramUtils implements TVProgram,Cloneable {

	public void setDebug(boolean b) { debug = b; }
	private static boolean debug = false;

	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/

	@Override
	public String getTVProgramId() { return tvProgId; }
	private static final String tvProgId = "SearchResult";

	@Override
	public boolean isAreaSelectSupported() { return false; }

	@Override
	public ProgType getType() { return ProgType.SEARCHED; }

	@Override
	public ProgSubtype getSubtype() { return ProgSubtype.NONE; }

	public PassedProgram clone() {
		return (PassedProgram) super.clone();
	}


	/*******************************************************************************
	 * 個体の特性
	 ******************************************************************************/

	@Override
	public int getTimeBarStart() { return 5; }

	// 検索結果を何件保存するか
	public void setResultBufferMax(int n) { resultBufferMax = n; }
	private int resultBufferMax = 5;


	/*******************************************************************************
	 * 定数
	 ******************************************************************************/


	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public SearchResult() {

		super();

		// ガワ
		crlist = new ArrayList<Center>();		// 番組情報を全部破棄
		sortedcrlist = new ArrayList<Center>();	// 番組情報を全部破棄
		pcenter = new ArrayList<ProgList>();	// 番組情報を全部破棄

	}


	/*******************************************************************************
	 * 検索結果固有
	 ******************************************************************************/

	public ArrayList<ProgDetailList> getResultBuffer(final String label) {

		// 履歴保存先を指定サイズまで縮小
		for ( int i=pcenter.size(); i>=resultBufferMax && i>1; i-- ) {
			crlist.remove(i-1);
			pcenter.remove(i-1);
		}

		// 追加
		{
			Center srchcr = new Center();
			srchcr.setCenter(label);

			ProgList srchpl = new ProgList();
			srchpl.enabled = true;
			srchpl.Center = srchcr.getCenter();
			srchpl.pdate = new ArrayList<ProgDateList>();

			ProgDateList srchpdt = new ProgDateList();
			srchpdt.Date = CommonUtils.getDateTime(CommonUtils.getCalendar("2999/12/31 23:59"));
			srchpl.pdate.add(srchpdt);

			crlist.add(0,srchcr);
			//sortedcrlist.add(srchcr);
			pcenter.add(0,srchpl);
		}

		return pcenter.get(0).pdate.get(0).pdetail;
	}

	public int getResultBufferSize() { return pcenter.size(); }

	public String getLabel(int index) { return pcenter.get(index).Center; }

	public ArrayList<ProgDetailList> getResult(int index) { return pcenter.get(index).pdate.get(0).pdetail; }

	/*******************************************************************************
	 * 本体
	 ******************************************************************************/

	@Override
	public boolean loadProgram(String areaCode, boolean force) {
		return true;
	}

	@Override
	public void loadAreaCode() {
	}

	@Override
	public void loadCenter(String code, boolean force) {
	}

}
