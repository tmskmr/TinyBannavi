package tainavi.pluginrec;

import java.util.HashMap;

import tainavi.HDDRecorder;
import tainavi.HDDRecorder.RecType;


/*
 * 
 */

public class PlugIn_RecDIGA_DMR_BZT720 extends PlugIn_RecDIGA_DMR_BWT2100 implements HDDRecorder,Cloneable {

	@Override
	public PlugIn_RecDIGA_DMR_BZT720 clone() {
		return (PlugIn_RecDIGA_DMR_BZT720) super.clone();
	}

	/* 必須コード  - ここから */
	
	// 種族の特性
	@Override
	public String getRecorderId() { return "DIGA DMR-BZT720"; }
	@Override
	public RecType getType() { return RecType.RECORDER; }
	
	@Override
	protected int get_com_try_count() { return 5; }
	
	public PlugIn_RecDIGA_DMR_BZT720() {
		super();
		this.setTunerNum(3);
	}
	
	/*
	 * 公開メソッド
	 */
	
	/*
	 * 非公開メソッド
	 */
	
	@Override
	protected HashMap<PostMode, String[]> getPostKeys() {
		
		String[] pkeys1a = { "cRVMD","cRVHM1","cRVHM2","cCHSRC","cCHNUM","cRSPD1" };
		String[] pkeys1b = { "cTHEX","cTIMER" };
		String[] pkeys1 = joinArrays(pkeys1a, DIGA_WDPTNSTR, pkeys1b, null);
		String[] pkeys2 = { "cRVID","cRVORG","cRVORGEX","cRVORGEX2","cRVORGEX3" };
		String[] pkeys3 = {	"cRPG","cRHEX","cTSTR","cRHEXEX" };
		String[] pkeys4 = {	"RSV_FIX.x","RSV_FIX.y" };
		String[] pkeys5 = {	"RSV_EXEC.x","RSV_EXEC.y" };
		String[] pkeys6 = { "RSV_DEL.x","RSV_DEL.y" };
		String[] pkeys7 = { "RSV_EDIT.x","RSV_EDIT.y" };
		String[] pkeys8 = {	"cRPG","cERR","TTL_DRIVE","cRVID","cRHEX","cTSTR","cRHEXEX","Image_BtnRyoukai.x","Image_BtnRyoukai.y" };
		String[] pkeys9 = { "cRECMODE" };
		
		HashMap<PostMode, String[]> keys = new HashMap<PostMode, String[]>();
		
		keys.put(PostMode.ADD_CMD,  joinArrays( pkeys1, pkeys3, pkeys4, null ));
		keys.put(PostMode.ADD_EXEC, joinArrays( pkeys9, pkeys3, pkeys5, null ));
		
		keys.put(PostMode.UPD_CMD,  joinArrays( pkeys1, pkeys2, pkeys3, pkeys7 ));
		keys.put(PostMode.UPD_EXEC, joinArrays( pkeys9, pkeys2, pkeys3, pkeys5 ));
		
		keys.put(PostMode.DEL_CMD,  joinArrays( pkeys1, pkeys2, pkeys3, pkeys6 ));
		keys.put(PostMode.DEL_EXEC, joinArrays( pkeys9, pkeys2, pkeys3, pkeys5 ));
		
		keys.put(PostMode.ERR_OK,   joinArrays( pkeys8, null, null, null ));
		
		return keys;
	}
}
