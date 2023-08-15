package epgdump;

public class EEVTDitem {

	int  item_description_length;
	byte[] item_description = new byte[Util.MAXSECLEN];
	int  item_length;
	byte[] item = new byte[Util.MAXSECLEN];
	
	/* 退避用 */
	//int  descriptor_number;

}
