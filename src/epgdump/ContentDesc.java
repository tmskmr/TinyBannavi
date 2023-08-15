package epgdump;

public class ContentDesc {

	int descriptor_tag;
	int descriptor_length;
	byte[] content = new byte[Util.MAXSECLEN];

}
