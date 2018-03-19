package org.irmacard.ideal.web;

import com.ing.ideal.connector.Country;
import com.ing.ideal.connector.Issuer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdealIssuers {
	private long updated;
	private Map<String,List<Issuer>> issuers;

	public IdealIssuers(List<Country> countryList){
		this.updated = System.currentTimeMillis()/1000;
		issuers=new HashMap<>();
		for (Country country : countryList){
			issuers.put(country.getCountryNames(),country.getIssuers());
		}
	}

	public Map<String,List<Issuer>> getIssuers() {
		return issuers;
	}

	public boolean shouldUpdate() {
		return System.currentTimeMillis()/1000 - updated > 60*60*24*7;
	}

	public boolean containsBankCode (String bankCode){
		for (List<Issuer> country: issuers.values()){
			for (Issuer bank: country){
				if (bank.getIssuerID().equals(bankCode))
					return true;
			}
		}
		return false;
	}
}
