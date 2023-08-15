package tainavi;

import java.io.File;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 番組IDってなんですか？私、気になります！
 */
public class GetEventId extends TVProgramUtils implements TVProgram,Cloneable {

	private static final String thisEncoding = "MS932";

	private static boolean debug = false;

	private static final String MSGID = "[番組ID取得] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;
	private static final String pFILE = "env"+File.separator+"eventidmap.xml";

	private static final int REMAIN_CACHE = 24*3600;	// これだけキャッシュは残しておく

	private HashMap<String,Integer> pMap = new HashMap<String, Integer>();

	private String errmsg = null;

	/*
	 * コンストラクタ
	 */
	public GetEventId() {
		this.load();
		setRetryCount(0);
	}

	// 本文
	public Integer getEvId(final String chId, final String startDateTime, boolean force) {

		errmsg = "";

		if (startDateTime == null) {
			return -1; // 使いたくないのでしょう
		}

		final GregorianCalendar ca = CommonUtils.getCalendar(startDateTime);
		if ( ca == null ) {
			System.err.println(errmsg = ERRID+"開始日時の書式が不正です： "+startDateTime);
			return -1;
		}
		final String ymd = CommonUtils.getDateTimeYMD(ca).replaceFirst("\\d\\d$", "");	// YYYYMMDDhhmm

		int chType;
		int chNum;
		try {
			chType = Integer.decode("0x"+chId.substring(0,4));
			chNum = Integer.decode("0x"+chId.substring(8,12));
		}
		catch (StringIndexOutOfBoundsException e) {
			System.err.println(errmsg = ERRID+"放送局IDが不正です： "+chId+" "+e.toString());
			return -1;
		}
		catch (NumberFormatException e) {
			System.err.println(errmsg = ERRID+"放送局IDが不正です： "+chId+" "+e.toString());
			return -1;
		}

		Integer evId = pMap.get(chId+ymd);
		if ( evId != null ) {
			if (debug) System.out.println(MSGID+"キャッシュにヒットしました。");
			return evId;
		}
		if ( ! force ) {
			return evId;
		}

		String chStr = null;
		switch ( chType ) {
		case 0x0004:
			chStr = String.format("200%03d",chNum%1000);
			break;
		case 0x0006:
		case 0x0007:
			chStr = String.format("500%03d",chNum%1000);
			break;
		default:
			chStr = String.format("%06d",(chNum+100000)%1000000);
			break;
		}

		final String uri = "https://www.tvkingdom.jp/iepg.tvpid?id="+chStr+ymd;
		final String res = webToBuffer(uri, thisEncoding, false);
		if ( res == null ) {
			System.err.println(errmsg = ERRID+"サイトへのアクセスが失敗しました： "+uri);
			return null;
		}

		if (debug) System.out.println(DBGID+"レスポンス： \n"+res);

		Matcher ma = Pattern.compile("program-id: (\\d+)").matcher(res);
		if ( ! ma.find() ) {
			System.err.println(errmsg = ERRID+"program-idがみつかりません： "+uri);
			return -1;
		}

		evId = Integer.valueOf(ma.group(1));

		this.put(chId+ymd, evId);

		this.save();

		return evId;
	}

	//
	private Integer put(String key, Integer value) {
		// マップのリフレッシュ
		Set<String> keyset = pMap.keySet();
		String[] kd = new String[keyset.size()];
		int i=0;
		for ( String k : keyset ) {
			kd[i++] = k;
		}
		String curdt = CommonUtils.getDateTimeYMD(-REMAIN_CACHE).substring(0,12);
		for ( String k : kd ) {
			if ( k.substring(12).compareTo(curdt) >= 0) {
				if (debug) System.out.println(DBGID+"有効なキャッシュです： キャッシュ削除期限="+curdt+" キャッシュ上の番組の開始日時="+k.substring(12));
				continue;
			}

			if (debug) System.out.println(DBGID+"期限切れのキャッシュを削除しました： キャッシュ削除期限="+curdt+" キャッシュ上の番組の開始日時="+k.substring(12));
			pMap.remove(k);
		}

		// 追加
		return pMap.put(key, value);
	}

	//
	private boolean load() {
		if ( ! new File(pFILE).exists() ) {
			return true;
		}

		@SuppressWarnings("unchecked")
		HashMap<String,Integer> tmp = (HashMap<String, Integer>) CommonUtils.readXML(pFILE);
		if ( tmp == null ) {
			return false;
		}

		pMap = tmp;

		return true;
	}

	//
	private boolean save() {
		if ( ! CommonUtils.writeXML(pFILE, pMap) ) {
			return false;
		}
		return false;
	}

	/*
	 *  以下は全部ダミー(non-Javadoc)
	 * @see tainavi.TVProgramUtils#clone()
	 */

	@Override
	public GetEventId clone() {
		return null;
	}

	@Override
	public String getTVProgramId() {
		return null;
	}

	@Override
	public ArrayList<ProgList> getCenters() {
		return null;
	}

	@Override
	public ArrayList<Center> getCRlist() {
		return null;
	}

	@Override
	public ArrayList<Center> getSortedCRlist() {
		return null;
	}

	@Override
	public void setSortedCRlist() {
	}

	@Override
	public void setExtension(String spoexSearchStart, String spoexSearchEnd,
			boolean spoexLimitation, ArrayList<SearchKey> extKeys) {
	}

	@Override
	public void abon(ArrayList<String> ngword) {
	}

	@Override
	public String chkComplete() {
		return null;
	}

	@Override
	public ProgType getType() {
		return null;
	}

	@Override
	public ProgSubtype getSubtype() {
		return null;
	}

	@Override
	public boolean loadProgram(String areaCode, boolean force) {
		return true;
	}

	@Override
	public int getTimeBarStart() {
		return 0;
	}

	@Override
	public void setExpandTo8(boolean b) {
	}

	@Override
	public void setUseDetailCache(boolean b) {
	}

	@Override
	public ArrayList<AreaCode> getAClist() {
		return null;
	}

	@Override
	public void loadAreaCode() {
	}

	@Override
	public void saveAreaCode() {
	}

	@Override
	public String getDefaultArea() {
		return null;
	}

	@Override
	public String getArea(String code) {
		return null;
	}

	@Override
	public String getCode(String Area) {
		return null;
	}

	@Override
	public String setSelectedAreaByName(String area) {
		return null;
	}

	@Override
	public String getSelectedArea() {
		return null;
	}

	@Override
	public String getSelectedCode() {
		return null;
	}

	@Override
	public void loadCenter(String area, boolean force) {
	}

	@Override
	public boolean saveCenter() {
		return false;
	}

	@Override
	public void setProgDir(String s) {
	}

	@Override
	public void setCacheExpired(int h) {
	}

	/*
	@Override
	public void setProgressArea(StatusWindow o) {
	}

	@Override
	public void setChConv(ChannelConvert chconv) {
	}
	*/

	@Override
	public void setContinueTomorrow(boolean b) {
	}

	@Override
	public void setSplitEpno(boolean b) {
	}

	@Override
	public boolean setOptString(String s) {
		return false;
	}

	@Override
	public String getOptString() {
		return null;
	}

}
