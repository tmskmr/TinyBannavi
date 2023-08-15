package tainavi;

public class LikeReserveItem {

	private HDDRecorder rec = null;
	private ReserveList rsv = null;
	private long dist = 0;
	
	public LikeReserveItem(HDDRecorder rec, ReserveList rsv, long dist) {
		this.rec = rec;
		this.rsv = rsv;
		this.dist = dist;
	}
	
	public HDDRecorder getRec() { return rec; }
	
	public ReserveList getRsv() { return rsv; }
	
	/**
	 * 比較元の情報の開始時間との差をミリ秒で返す
	 */
	public long getDist() { return dist; }
	
	/**
	 * 比較元の情報の開始時間との差がないか、または１分前であるを返す
	 */
	public boolean isCandidate(boolean overlapup) {
		return (getDist() == 0 || (overlapup ? getDist() == -60000 : false));
	}
	
	@Override
	public String toString() {
		return rsv.getTitle()+", "+rsv.getRec_pattern()+", "+rsv.getAhh()+":"+rsv.getAmm()+", "+rec.Myself()+", "+rsv.getTuner();
	}
	
}
