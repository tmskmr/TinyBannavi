package epgdump;

public class SEVTdesc {

	int  descriptor_tag;
	int  descriptor_length;
	byte[] ISO_639_language_code = new byte[3];
	int  event_name_length;
	byte[] event_name = new byte[Util.MAXSECLEN];
	int  text_length;
	byte[] text = new byte[Util.MAXSECLEN];

}
