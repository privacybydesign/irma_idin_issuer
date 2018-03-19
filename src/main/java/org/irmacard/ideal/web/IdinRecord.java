package org.irmacard.ideal.web;

import org.javalite.activejdbc.Model;

public class IdinRecord extends Model {
	public static final String BIN_FIELD = "bin";
	public static final String EMAIL_FIELD = "email";
	public static final String TIME_FIELD = "time";

	public static void New(String bin) {
		IdealApplication.openDatabase();
		new IdinRecord(bin);
		IdealApplication.closeDatabase();
	}

	public IdinRecord(String bin) {
		setString(BIN_FIELD, bin);
		setLong(TIME_FIELD, System.currentTimeMillis()/1000);
		saveIt();
	}
}
