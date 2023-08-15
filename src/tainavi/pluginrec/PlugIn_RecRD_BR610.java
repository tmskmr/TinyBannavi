package tainavi.pluginrec;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.HDDRecorder;
import tainavi.ReserveList;
import tainavi.TextValueSet;

public class PlugIn_RecRD_BR610 extends PlugIn_RecRD_BZ700 implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_BR610 clone() {
		return (PlugIn_RecRD_BR610) super.clone();
	}

	// 種族の特性
	@Override
	public String getRecorderId() { return "REGZA RD-BR610"; }
	
	@Override
	protected void setSettingEncoder(ArrayList<TextValueSet> encoder, String res)
	{
		System.out.println("BR610's setSettingEncoder().");
		
		encoder.clear();
		
		Matcher mb = Pattern.compile("var double_encode_flg = (\\d+?);").matcher(res);
		while (mb.find()) {
			Matcher mc = Pattern.compile("\\n\\s*?switch \\( double_encode_flg \\) \\{([\\s\\S]+?default:)").matcher(res);
			if (mc.find()) {
				Matcher md = Pattern.compile("(case "+mb.group(1)+":[\\s\\S]+?break;)").matcher(mc.group(1));
				if (md.find()) {
					Matcher me = Pattern.compile("name=enc_type value=.\"(\\d+?).\"[\\s\\S]+?act_(.+?)\\.gif").matcher(md.group(1));
					while (me.find()) {
						if ( ! me.group(2).equals("RE")) {
							TextValueSet t = new TextValueSet();
							t.setText(me.group(2).replaceFirst("DR","--"));	
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
		String tuner = entry.getTuner();
		if (tuner.equals("RE")) {
			entry.setTuner("--");
		}
	}
	
	//
	@Override
	protected String text2valueEncoderSP(ArrayList<TextValueSet> tvs, String text, String vrate) {
		if ( ! vrate.equals("[DR]")) {
			return "1"; // RE
		}
		return text2value(tvs, text);
	}
}