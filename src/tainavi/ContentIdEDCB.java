package tainavi;

/**
 * EpgDataCap_BonプラグインとEDCBプラグインで利用するコンテンツIDを操作するstaticメソッドの集合
 */
public class ContentIdEDCB {

	public static final String CIDMARK = "EDCB$";
	private static final int chid_len = 12;
	private static final int evid_len = 4;
	
	protected static int original_network_id ;
	protected static int transport_stream_id ;
	protected static int servive_id ;
	protected static int event_id ;
	
	private static String station_id;
	
	public static boolean isValid(String cId) {
		return (cId!=null)?(cId.startsWith(CIDMARK) && cId.length()>=CIDMARK.length()+chid_len+evid_len):(false);
	} 
	
	public static String stripMark(String edcbid) { return edcbid.substring(CIDMARK.length()); }
	
	public static int getOnId() { return original_network_id; }
	public static int getTSId() { return transport_stream_id; }
	public static int getSId() { return servive_id; }
	public static int getEvId() { return event_id; }
	public static String getStId() { return station_id; }
	
	public static String getChId() {
		return getChId(original_network_id,transport_stream_id,servive_id);
	}
	
	/**
	 * 
	 * @param onid
	 * @param tsid
	 * @param sid
	 * @return
	 */
	public static String getChId(int onid, int tsid, int sid) {
		return String.format("%04X%04X%04X",onid,tsid,sid);
	}
	
	/**
	 * １０進表記のChIDを１６進表記に変更
	 */
	public static String getChId(String dec) {
		try {
			long n = Long.valueOf(dec);
			if ( (n&0xFFFF00000000L) == 0 || (n&0x0000FFFF0000L) == 0 || (n&0x00000000FFFFL) == 0 ) { 
				return null;
			}
			return String.format("%012X", n);	// Javaでは%lxではなく%xでいいらしい
		}
		catch ( NumberFormatException e ) {
		}
		return null;
	}
	
	public static String getContentId(int onid, int tsid, int sid, int evid) {
		return getContentId(getChId(onid,tsid,sid),evid);
	}
	
	public static String getContentId(String chid, int evid) {
		return String.format("%s%s%04X",CIDMARK,chid,evid);
	}
	
	public static String getContentId(int evid) {
		return getContentId(getChId(),evid);
	}

	public static boolean decodeChId(String chid) {
		if ( chid != null && chid.length()>=chid_len ) {
			try {
				original_network_id = Integer.decode("0x"+chid.substring(0,4));
				transport_stream_id = Integer.decode("0x"+chid.substring(4,8));
				servive_id = Integer.decode("0x"+chid.substring(8,12));
				// iEPG2
				if ( original_network_id == 4 ) {
					station_id = String.format("%s%3d", BroadcastType.BS.getHeader(),servive_id);
				}
				else if ( original_network_id == 6 || original_network_id == 7 ) {
					station_id = String.format("%s%d", BroadcastType.CS.getHeader(),servive_id);
				}
				//else if ( 0x7880 <= original_network_id && original_network_id <= 0x7FE8 ) {
				else {
					station_id = String.format("%s%05X", BroadcastType.TERRA.getHeader(),servive_id);
				}
				return true;
			}
			catch ( Exception e ) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public static boolean decodeContentId(String cId) {
		if ( isValid(cId) ) {
			try {
				String xId = stripMark(cId);
				if ( ! decodeChId(xId) ) {
					return false;
				}
				event_id = Integer.decode("0x"+xId.substring(chid_len,chid_len+evid_len));
				return true;
			}
			catch ( Exception e ) {
				e.printStackTrace();
			}
		}
		return false;
	}
}
