package tainavi;

/**
 * <P>録画予約の開始・終了日時と重複度を管理します
 */
public class ResTimeItem {
	String recorder;
	String start;
	String end;
	int count;
	String tooltip;

	public ResTimeItem(String r, String s, String e, int c, String t){
		this.recorder = r;
		this.start = s;
		this.end = e;
		this.count = c;
		this.tooltip = t;
	}

	public ResTimeItem(String r, String s, String e, String t){
		this.recorder = r;
		this.start = s;
		this.end = e;
		this.count = 1;
		this.tooltip = t;
	}

	public void setStart(String s){ start = s; }
	public void setEnd(String e){ end = e; }
	public void addCount(String t){
		count++;
		tooltip = tooltip + t;
	}
	public void setTooltip(String t){ tooltip = t; }

	public String getRecorder(){ return recorder; }
	public String getStart(){ return start; }
	public String getEnd(){ return end; }
	public int getCount(){ return count; }
	public String getTooltip(){ return tooltip; }
}
