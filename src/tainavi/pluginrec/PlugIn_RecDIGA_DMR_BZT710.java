package tainavi.pluginrec;

import tainavi.HDDRecorder;
import tainavi.HDDRecorder.RecType;


/*
 * 
 */

public class PlugIn_RecDIGA_DMR_BZT710 extends PlugIn_RecDIGA_DMR_BWT2100 implements HDDRecorder,Cloneable {

	@Override
	public PlugIn_RecDIGA_DMR_BZT710 clone() {
		return (PlugIn_RecDIGA_DMR_BZT710) super.clone();
	}

	/* 必須コード  - ここから */
	
	// 種族の特性
	@Override
	public String getRecorderId() { return "DIGA DMR-BZT710"; }
	@Override
	public RecType getType() { return RecType.RECORDER; }
	
	@Override
	protected int get_com_try_count() { return 5; }
	
	public PlugIn_RecDIGA_DMR_BZT710() {
		super();
		this.setTunerNum(3);
	}
}
