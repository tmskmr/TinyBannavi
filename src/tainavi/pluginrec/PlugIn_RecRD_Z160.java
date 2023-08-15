package tainavi.pluginrec;

import java.util.ArrayList;

import tainavi.HDDRecorder;
import tainavi.TextValueSet;

public class PlugIn_RecRD_Z160 extends PlugIn_RecRD_Z260 implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_Z160 clone() {
		return (PlugIn_RecRD_Z160) super.clone();
	}

	// 種族の特性
	@Override
	public String getRecorderId() { return "REGZA DBR-Z160"; }

	/*
	 * 録画設定の解読
	 */
	@Override
	protected void setSettingVrate(ArrayList<TextValueSet> vrate)
	{
		vrate.clear();
		TextValueSet t = null;
		
		t = new TextValueSet();
		t.setText("[DR]");
		t.setValue("128:6");
		vrate.add(t);
		
		t = new TextValueSet();
		t.setText("[AVC] AF 12.0");
		t.setValue("2:8");
		vrate.add(t);
		t = new TextValueSet();
		t.setText("[AVC] AN 8.0");
		t.setValue("2:9");
		vrate.add(t);
		t = new TextValueSet();
		t.setText("[AVC] AE 2.4");
		t.setValue("2:10");
		vrate.add(t);
		
		t = new TextValueSet();
		t.setText("[AVC] AT 4.7GB");
		t.setValue("2:4");
		vrate.add(t);
		t = new TextValueSet();
		t.setText("[AVC] AT 8.5GB");
		t.setValue("2:7");
		vrate.add(t);
		t = new TextValueSet();
		t.setText("[AVC] AT 9.4GB");
		t.setValue("2:5");
		vrate.add(t);
		t = new TextValueSet();
		t.setText("[AVC] AT 25GB");
		t.setValue("2:11");
		vrate.add(t);
		t = new TextValueSet();
		t.setText("[AVC] AT 50GB");
		t.setValue("2:12");
		vrate.add(t);
		
		for (int br=1400; br<=17000; ) {
			t = new TextValueSet();
			t.setText(String.format("[AVC] %d.%d", (br-br%1000)/1000, (br%1000)/100));
			t.setValue("2:"+String.valueOf(br));
			vrate.add(t);
			if (br < 10000) {
				br += 200;
			}
			else {
				br += 500;
			}
		}
		
		t = new TextValueSet();
		t.setText("[VR] SP4.4/4.6");
		t.setValue("1:1");
		vrate.add(t);
		t = new TextValueSet();
		t.setText("[VR] LP2.0/2.2");
		t.setValue("1:2");
		vrate.add(t);
		
		t = new TextValueSet();
		t.setText("[VR] AT 4.7GB");
		t.setValue("1:4");
		vrate.add(t);
		t = new TextValueSet();
		t.setText("[VR] AT 8.5GB");
		t.setValue("1:7");
		vrate.add(t);
		t = new TextValueSet();
		t.setText("[VR] AT 9.4GB");
		t.setValue("1:5");
		vrate.add(t);
		
		t = new TextValueSet();
		t.setText("[VR] 1.0");
		t.setValue("1:1000");
		vrate.add(t);
		t = new TextValueSet();
		t.setText("[VR] 1.4");
		t.setValue("1:1400");
		vrate.add(t);
		for (int br=2000; br<=9200; br+=200) {
			t = new TextValueSet();
			t.setText(String.format("[VR] %d.%d", (br-br%1000)/1000, (br%1000)/100));
			t.setValue("1:"+String.valueOf(br));
			vrate.add(t);
		}
	}
}
