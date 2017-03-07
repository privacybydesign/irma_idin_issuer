package org.irmacard.idin.web;

import net.bankid.merchant.library.internal.DirectoryResponseBase;

import java.util.List;

public class IdinIssuers {
	private long updated;
	private List<DirectoryResponseBase.Issuer> issuers;

	public IdinIssuers(List<DirectoryResponseBase.Issuer> issuers) {
		this.updated = System.currentTimeMillis()/1000;
		this.issuers = issuers;
	}

	public List<DirectoryResponseBase.Issuer> getIssuers() {
		return issuers;
	}

	public boolean shouldUpdate() {
		return System.currentTimeMillis()/1000 - updated > 60*60*24*7;
	}
}
