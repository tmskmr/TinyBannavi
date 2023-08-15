package tainavi;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.InsetsUIResource;

public class VWLookAndFeel {

	ArrayList<String> lafnames = null;
	//Component comp = null;
	
	//
	public ArrayList<String> getNames() {
		return lafnames;
	}
	
	public String update(String lafname) {
		
		String name = "";
		if ( ! lafname.equals("") ) {
			name = lafname;
		}
		else if ( CommonUtils.isWindows() ) {
			name = "Windows";
		}
		else if ( CommonUtils.isLinux() ) {
			name = "GTK";
		}
		
		if (/*comp != null &&*/ ! name.equals("")) {
			for ( String className : lafnames ) {
				if ( className.endsWith("."+name+"LookAndFeel") ) {
					try {
						UIManager.setLookAndFeel(className);
						//SwingUtilities.updateComponentTreeUI(comp);
						
						// テーブルの色だけ固定
						UIDefaults defaultTable = UIManager.getLookAndFeelDefaults();
						for(Object o: defaultTable.keySet()) {
							if (o.toString().equals("Table.selectionBackground")) {
								UIManager.put(o, new Color(182,207,229));
							}
							else if (o.toString().equals("Table.selectionForeground")) {
								UIManager.put(o, new Color(0,0,0));
							}
							else if (o.toString().equals("Button.margin")) {
								InsetsUIResource ins = (InsetsUIResource) UIManager.get(o);
								ins.left = ins.right = 4;
								UIManager.put(o,ins);
							}
						}
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (UnsupportedLookAndFeelException e) {
						e.printStackTrace();
					}
					break;
				}
			}
		}
		
		return name;
	}
	
	
	//
	private void findLookAndFeel() {
		
		lafnames = new ArrayList<String>();

		// 組み込みLAF
		for ( LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels() ) {
			if ( laf.getClassName().endsWith("LookAndFeel") ) {
				lafnames.add(laf.getClassName());
			}
		}
		
		// 追加LAF
		File skinFolder = new File("skin");
		if ( ! skinFolder.exists()) {
			skinFolder.mkdir();
		}
		for ( File file : skinFolder.listFiles() ) {
			try {
				JarFile jarfile = new JarFile(file) ;
				for ( Enumeration<JarEntry> en = jarfile.entries(); en.hasMoreElements(); ) {
					JarEntry entry = (JarEntry)en.nextElement() ;
					if ( entry.getName().endsWith(".class") ) {
						String className = entry.getName().replaceAll("/", "\\.").replaceAll("\\.class$", "");
						lafnames.add(className);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	// コンストラクタ
	public VWLookAndFeel(/*Component c*/) {
		//comp = c;
		findLookAndFeel();
	}
}
