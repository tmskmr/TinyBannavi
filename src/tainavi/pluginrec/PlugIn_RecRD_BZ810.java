package tainavi.pluginrec;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.HDDRecorder;
import tainavi.ReserveList;
import tainavi.TextValueSet;

public class PlugIn_RecRD_BZ810 extends PlugIn_RecRD_BZ700 implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_BZ810 clone() {
		return (PlugIn_RecRD_BZ810) super.clone();
	}

	// 種族の特性
	@Override
	public String getRecorderId() { return "REGZA RD-BZ810"; }
	
	@Override
	protected void setSettingEncoder(ArrayList<TextValueSet> encoder, String res)
	{
		System.out.println("BZ810's setSettingEncoder().");
		
		encoder.clear();
		
		Matcher mb = Pattern.compile("var double_encode_flg = (\\d+?);").matcher(res);
		while (mb.find()) {
			Matcher mc = Pattern.compile("\\n\\s*?switch \\( double_encode_flg \\) \\{([\\s\\S]+?default:)").matcher(res);
			if (mc.find()) {
				Matcher md = Pattern.compile("(case "+mb.group(1)+":[\\s\\S]+?break;)").matcher(mc.group(1));
				if (md.find()) {
					Matcher me = Pattern.compile("name=enc_type value=.\"(\\d+?).\"[\\s\\S]+?toru_(.+?)\\.gif").matcher(md.group(1));
					while (me.find()) {
						if ( ! me.group(2).equals("RE")) {
							TextValueSet t = new TextValueSet();
							t.setText(me.group(2).replaceFirst("^TS","R"));
							t.setValue(me.group(1));
							encoder.add(t);
						}
					}
				}
			}
		}
	}
	
	//
	@Override
	protected void translateAttributeTuner(ReserveList entry) {
		if (entry.getTuner().startsWith("TS")) {
			entry.setTuner(entry.getTuner().replaceFirst("^TS", "R"));
		}
		else if (entry.getTuner().startsWith("RE")) {
			entry.setTuner(ITEM_ENCODER_R1);
		}
	}
	
	//
	@Override
	protected String text2valueEncoderSP(ArrayList<TextValueSet> tvs, String text, String vrate) {
		if (ITEM_ENCODER_R2.equals(text) && ITEM_VIDEO_TYPE_VR.equals(vrate)) {
			//return VALUE_ENCODER_RE_2;	// RE? 指定できない
			setErrmsg("【警告】BZ810プラグインでは、R2選択時は画質に[VR]を指定できません。");
		}
		return text2value(tvs, text);
	}
}
