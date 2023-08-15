package tainavi;

import java.io.File;

/**
 * log.txtにダラーっと吐くとあとで分離するのにすごい苦労するのでリクエストごとに分離するようにしてみた。大したものではない。
 */
public class DumpHttp {

	public void setCounterMax(int n) { COUNTERMAX = n; }
	private int COUNTERMAX = 800;
	
	private static int counter = -1;	// クラス間共通である。多重アクセスはありえないので排他はしない
	
	private String DUMPDIR = "debug"; 
	private String DUMPDIRALT = DUMPDIR+"alt";
	private final String cname = "counter.txt";
	
	private String request = null;
	private String header = null;
	private String body = null;

	public DumpHttp() {
		
	}
	
	public DumpHttp(String s) {
		DUMPDIR = s;
		DUMPDIRALT = s+"alt";
	}

	/**
	 * 
	 * @param s
	 */
	public void request(String s) {
		request = s;
	}
	
	public void res_header(String s) {
		header = s;
	}
	
	public String res_body(String s) {
		body = s;
		
		return close();
	}
	
	private String close() {
		
		String dir = DUMPDIR;
		if ( ! new File(dir).exists() ) {
			new File(dir).mkdir();
		}
		else {
			if ( ! new File(dir).isDirectory() ) {
				// りとらいするお
				dir = DUMPDIRALT;
				if ( ! new File(dir).exists() ) {
					new File(dir).mkdir();
				}
				else if ( ! new File(dir).isDirectory() ) {
					return null;	// こりゃだめだ
				}
			}
		}

		if ( counter == -1 ) {
			counter = CommonUtils.loadCnt(dir+File.separator+cname);
		}
		
		counter = (counter+1) % COUNTERMAX;
		
		CommonUtils.saveCnt(counter, dir+File.separator+cname);
		
		String fname = String.format("%04d.htm", counter);
		String fpath = dir+File.separator+fname;
		
		StringBuilder sb = new StringBuilder();
		sb.append("<!-- "+CommonUtils.getDateTime(0)+" -->\n");
		sb.append("<!--\n"+request+"\n-->\n");
		sb.append("<!--\n"+header+"\n-->\n");
		sb.append(body);
		String s = sb.toString();
		
		request = header = body = null;
		
		if ( ! CommonUtils.write2file(fpath, s) ) {
			return null;
		}
		
		return fname;
	}
	
}
