package tainavi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class unzip {

	public static void doUnzip(String dir, String zip) throws IOException,Exception {
		ZipInputStream zis = new ZipInputStream(new FileInputStream(new File(zip)));
		ZipEntry entry = null;
		try {
			while ((entry = zis.getNextEntry()) != null) {
				try {
					unzipEntry(entry, dir, zis);
				}
				finally {
					zis.closeEntry();
				}
			}
		}
		finally {
			if (zis != null) {
				try {
					zis.close();
				}
				catch (IOException e) {}
			}
		}
	}
	
	private static final void unzipEntry( ZipEntry entry, String dir, ZipInputStream zis ) throws IOException {
		File f  = null;
		if (entry.isDirectory()) {
			// フォルダ
			f = new File(dir, entry.getName());
			if ( ! f.exists() && ! f.mkdirs()) {
				throw new IOException( "create directory faild. directory="+f.getAbsolutePath() );
			}
		}
		else {
			// ファイル
			f = new File(dir, entry.getName());
			File parent = new File(f.getParent());
			if ( ! parent.exists() && ! new File(f.getParent()).mkdirs()) {
				throw new IOException( "create directory faild. directory="+parent.getAbsolutePath() );
			}
			FileOutputStream out = new FileOutputStream(f);
			byte[] buf = new byte[65536];
			int size = 0;
			try {
				while ((size = zis.read(buf,0,buf.length)) != -1) {
					out.write(buf, 0, size);
				}
				out.close();
				long l = entry.getTime();
				f.setLastModified(l);
			}
			finally {
				if (out != null) {
					try {
						out.close();
					}
					catch (IOException e) { }
				}
			}
		}
	}
}
