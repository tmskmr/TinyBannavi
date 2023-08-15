package tainavi;

/**
 * 別にContentIdEDCBを継承する必要はないような…
 */
public class ContentIdDIMORA extends ContentIdEDCB {

	public static final String CIDMARK = "DIMORA$";
	private static final int chid_len = 12;
	private static final int evid_len = 4;

	//protected static int original_network_id ;	// これは放送種別がわかるだけで正しい値ではない
	//protected static int transport_stream_id ;	// これは常に０
	//protected static int servive_id ;				// これは正しい値のはず
	//protected static int event_id ;				// これは使えない

	//private static String station_id;				// これは使える

	protected static String dimora_program_id;				// DIMORA独自。用途が謎だがDimora経由での予約に必要になるかもしれないので保存

	public static boolean isValid(String cId) {
		return (cId!=null)?(cId.startsWith(CIDMARK) && cId.length()>=CIDMARK.length()+chid_len+evid_len):(false);
	}
	
	public static String stripMark(String contentid) { return contentid.substring(CIDMARK.length()); }
	
	public static String getDimId() { return dimora_program_id; }
	
	/**
	 * DimoraのIDを変換する
	 */
	public static String getChId(String dimoraid) {
		try {
			int id = Integer.decode("0x"+dimoraid);
			int onid = 0;
			int sid = 0;
			if ( (id & 0x8000) != 0 ) {
				sid = ((id & 0x7F00) << 1) | (id & 0x00FF);	// これは一体どういう…？
				onid = 0x7FFF;
			}
			else if ( id >= 0x3C00 ) {
				onid = 0x0004;
				sid = id - 0x3C00;
			}
			else if ( id >= 0x3800 ) {
				onid = 0x0007;
				sid = id - 0x3800;
			}
			else {
				onid = 0x0006;
				sid = id - 0x3400;
			}
			return String.format("%04X%04X%04X", onid, 0, sid);
		}
		catch (NumberFormatException e) {
		}
		return null;
	}
	
	public static String getChId() {
		return String.format("%04X%04X%04X", original_network_id, transport_stream_id, servive_id);
	}

	// decodeChIdは共通
	
	public static String getContentId(int evid) {
		event_id = evid;
		return getContentId(original_network_id, transport_stream_id, servive_id, event_id, dimora_program_id);
	}
	
	public static String getContentId(String dimoid) {
		dimora_program_id = dimoid;
		return getContentId(original_network_id, transport_stream_id, servive_id, event_id, dimora_program_id);
	}

	public static String getContentId(int evid, String dimoid) {
		event_id = evid;
		dimora_program_id = dimoid;
		return getContentId(original_network_id, transport_stream_id, servive_id, event_id, dimora_program_id);
	}

	private static String getContentId(int onid, int tsid, int sid, int evid, String dimid) {
		return String.format("%s%04X%04X%04X%04X%s", CIDMARK, onid, tsid, sid, evid, (dimid!=null)?(dimid):(""));
	}
	
	public static boolean decodeContentId(String cId) {
		if ( isValid(cId) ) {
			try {
				String xId = stripMark(cId);
				if ( ! decodeChId(xId) ) {
					return false;
				}
				transport_stream_id = 0;
				event_id = Integer.decode("0x"+xId.substring(chid_len,chid_len+evid_len));
				dimora_program_id = xId.substring(chid_len+evid_len);
				return true;
			}
			catch ( Exception e ) {
				e.printStackTrace();
			}
		}
		return false;
	}

}
