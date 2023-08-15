package epgdump;


public class Util {

	public static boolean debug = false;
	
	public static final String thisEncoding = "UTF-8";

	public static final int MAXSECLEN = 4096;

	//
	public static int getBit(byte[] buf, int ptr, int[] pbit, int gbit) {
		int pbyte = pbit[0] / 8;
		//unsigned char *fbyte = byte + pbyte;

		int cutbit = pbit[0] - (pbyte * 8);
		int lcutbit = 32 - (cutbit + gbit);

		byte[] tbuf = new byte[4]; /* intの最大32bit */
		int tnum;

		// [鯛ナビ] rが4固定だとbufをoverrunするバグがある(TSpacket.payload) ↓の修正でいいのかどうかは、不明
		int r = buf.length - (ptr+pbyte);
		r =  (r>=4)?(4):(r);
		if ( Util.debug && r<4 ) System.err.println("# "+ptr+" "+r+" "+cutbit+" "+lcutbit);
		System.arraycopy(buf, ptr+pbyte, tbuf, 0, r);
		//memcpy(tbuf, fbyte, sizeof(unsigned char) * 4);

		/* 先頭バイトから不要bitをカット */
		tbuf[0] = (byte) (tbuf[0] & (0x00FF >>> cutbit));

		/* intにしてしまう */
		
		tnum = (tbuf[0]&0xFF) << 24 | (tbuf[1]&0xFF) << 16 | (tbuf[2]&0xFF) << 8 | (tbuf[3]&0xFF);

		/* 後ろの不要バイトをカット */
		tnum = tnum >>> lcutbit;

		pbit[0] += gbit;

		return tnum;
	}
	
	//
	public static int getStr(byte[] tostr, byte[] buf, int ptr, int[] pbit, int len) {
		byte[] str = new byte[len];
		int pbyte = pbit[0] / 8;
		//unsigned char *fbuf = buf + pbyte;

		//Arrays.fill(str, (byte) 0x00);
		System.arraycopy(buf, ptr+pbyte, str, 0, len);
		//memset(str, 0, sizeof(char) * MAXSECLEN);
		//memcpy(str, fbuf, len);

		pbit[0] += (len * 8);
	  
		return Aribstr.AribToString(tostr, str, len);
	}
	
	//
	public static int parseOTHERdesc(byte[] data, int ptr) {
		int[] boff = {0};
		//int descriptor_tag;
		int descriptor_length;

		/*descriptor_tag =*/ Util.getBit(data, ptr, boff, 8);
		descriptor_length = Util.getBit(data, ptr, boff, 8);

		/* printf("other desc_tag:0x%x\n", descriptor_tag); */

		return descriptor_length + 2;
	}

	// Cのstrlenもどき
	public static int strlen(byte[] b) {
		for ( int i=0; i<b.length; i++ ) {
			if ( b[i] == (byte) 0x00 ) {
				return i;
			}
		}
		return b.length;
	}
	
	// Cのmemcmp()もどき
	public static int memcmp(byte[] src, byte[] dst, int len) {
		int x=0,y=0;
		for ( ; x<src.length && y<dst.length && x<len; x++,y++ ) {
			int d =  src[x]&0xFF - dst[y]&0xFF;
			if ( d < 0 ) {
				return -1;
			}
			else if ( d > 0 ) {
				return 1;
			}
		}
		if ( x >= len && src.length < dst.length ) {
			return -1;
		}
		else if ( x >= len && src.length > dst.length ) {
			return 1;
		}
		return 0;
	}
	
	// byteを16進文字列に変換する
	public static Integer hex2dec(byte b) throws NumberFormatException {
		return Integer.decode(String.format("%x",b));
	}
	
	// 文字列バイナリをStringに変換する
	public static String getText(byte[] b, int len, String enc) {
		try {
			byte[] buf = new byte[len];
			System.arraycopy(b, 0, buf, 0, len);
			return new String(buf,Util.thisEncoding);
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		return null;
	}

}
