package tainavi.pluginrec;

import tainavi.HDDRecorder;


public class PlugIn_RecRD_Z160_TSync extends PlugIn_RecRD_BZ810_TSync implements HDDRecorder,Cloneable {
	
	public PlugIn_RecRD_Z160_TSync clone() {
		return (PlugIn_RecRD_Z160_TSync) super.clone();
	}
	
	// 種族の特性
	@Override
	public String getRecorderId() { return "REGZA DBR-Z160(TaiSync)"; }
	
	// 個体の特性
	@Override
	protected String getTSyncVersion() { return "z160"; }
}
