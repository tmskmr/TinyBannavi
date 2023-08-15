package tainavi;

import java.io.File;

/**
 * @see SearchProgram
 */
public class ExtProgram extends SearchProgram {
	// コンストラクタ
	public ExtProgram() {
		setSearchKeyFile("env"+File.separator+"extkey.xml");
		setSearchKeyLabel("延長警告管理");
	}
}
