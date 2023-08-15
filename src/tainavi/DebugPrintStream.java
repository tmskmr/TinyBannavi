package tainavi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class DebugPrintStream extends PrintStream {

	private PrintStream cw = null;
	private String logname = null;
	
	private final long switchSize = 5*1024*1024;
	
	public DebugPrintStream(OutputStream out, String logfile, boolean fileOut) {
		super(out);
		/*
		try {
			if ( CommonUtils.isWindows() ) {
				cw = new PrintStream(out,true,"MS932");
			}
			else {
				cw = new PrintStream(out);
			}
			if (fileOut) {
				logname = logfile;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		*/
		cw = new PrintStream(out);
		if (fileOut) {
			logname = logfile;
		}
	}
	
	@Override
	public PrintStream printf(String format, Object ... args) {
		try {
			PrintStream fw = logOpen();
			if (fw != null) {
				fw.printf(format, args);
				fw.close();
			}
		} catch (FileNotFoundException e) {
			cw.println(e.toString());
		}
		
		return cw.printf(format, args);
	}
	
	@Override
	public void println(String x) {
		try {
			PrintStream fw = logOpen();
			if (fw != null) {
				fw.println(x);
				fw.close();
			}
		} catch (FileNotFoundException e) {
			cw.println(e.toString());
		}
		
		cw.println(x);
	}
	
	@Override
	public void write(byte[] buf, int off, int len) {
		try {
			PrintStream fw = logOpen();
			if (fw != null) {
				fw.write(buf,off,len);
				fw.close();
			}
		} catch (FileNotFoundException e) {
			cw.println(e.toString());
		}

		cw.write(buf,off,len);
	}
	
	private PrintStream logOpen() throws FileNotFoundException {
		if (logname == null)
			return null;
		
		File f = new File(logname);
		if ( f.exists() && f.length() >= switchSize ) {
			// 削除
			File o = new File(logname+".bak");
			o.delete();
			
			// 退避
			f.renameTo(o);
		}
		
		FileOutputStream fos = new FileOutputStream(f,true);
		PrintStream ps = new PrintStream(fos); 
		
		// 追記or新規
		return ps;
	}
}
