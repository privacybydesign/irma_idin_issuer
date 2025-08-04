package org.irmacard.idin.web;

import net.bankid.merchant.library.internal.DirectoryResponseBase;

import java.util.List;
import java.util.Map;

public class IdinIssuers {
	private final long updated;
	private final Map<String,List<DirectoryResponseBase.Issuer>> issuers;

	public IdinIssuers(final Map<String,List<DirectoryResponseBase.Issuer>> issuers) {
		this.updated = System.currentTimeMillis()/1000;
		this.issuers = issuers;
	}

	public Map<String,List<DirectoryResponseBase.Issuer>> getIssuers() {
		return issuers;
	}

	public boolean shouldUpdate() {
		return System.currentTimeMillis()/1000 - updated > 60*60*24*7;
	}

	public boolean containsBankCode (final String bankCode){
		for (final List<DirectoryResponseBase.Issuer> country: issuers.values()){
			for (final DirectoryResponseBase.Issuer bank: country){
				if (bank.getIssuerID().equals(bankCode))
					return true;
			}
		}
		return false;
	}
}
