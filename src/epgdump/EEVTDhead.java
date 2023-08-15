package epgdump;

public class EEVTDhead {

	int  descriptor_tag;
	int  descriptor_length;
	int  descriptor_number;
	int  last_descriptor_number;
	byte[] ISO_639_language_code = new byte[3];
	int  length_of_items;

}
