package epgdump;

public class SECcache {
	
	int			pid;
	byte[]		buf = new byte[Util.MAXSECLEN+2];	// [鯛ナビ] 元ソースには+2しないといけないバグがあるっぽい
	int			seclen;
	int			setlen;
	TSpacket	cur = new TSpacket();
	int			curlen;
	int			cont;

}
