/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.core;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

@ConfigParam(name="EXTRACT_NUMBERS", desc="Convert string to numbers when gathering markers", value="true|false")
public class DataNormalizer {
	
	private final static String IN_NUMBER = "0123456789+-.,eE";	
	
	private final static ValueHolder<Boolean> EXTRACT_NUMBERS = 
		PEProc.getVolatileBooleanProperty("EXTRACT_NUMBERS", "true");
	
	private static boolean isLikelyInNumber(char c) {
		return (Character.isDigit(c) || Character.isWhitespace(c) || 
			IN_NUMBER.indexOf(c)>-1);			
	}

	public static String normalizeValue(String val) {
		if (val!=null && val.trim().length()>0) {
			int dotCnt = 0;
			int commaCnt = 0;
			for (int i=0; i < val.length(); i++) {
				char c = val.charAt(i);
				if (!isLikelyInNumber(c))
					return val;
				if (c==',')
					commaCnt++;
				else if (c=='.')
					dotCnt++;
			}
			/* if we are here we have got likely a number */
			String v = val.trim();
			if (commaCnt==1)
				v = v.replace(',', '.');
			if (dotCnt==1) {
				int idx = v.lastIndexOf('.');
				if (idx > -1) {
					boolean canCut = true;
					for (int k=idx+1; k < v.length(); k++) {
						canCut &= v.charAt(k) == '0';
						if (!canCut)
							break;
					}
					if (canCut)
						v= v.substring(0, idx);
				}				
			}
			
			/* try decimal */
			try {
				return new BigDecimal(v).toPlainString();
			}
			catch (Exception ex) {}
			
			/* try long */
			try {
				return Long.valueOf(v).toString();
			}
			catch (Exception ex) {}
			
			/* try double */
			try {
				return Double.valueOf(v).toString();
			}
			catch (Exception ex) {}
		}
		return val;
	}	
	
	private static String getNumberOrNull(String testForNumber) {
		int nonDigitCnt = 0;
		int n = testForNumber.length();
		for (int i=0; i < n; i++) {
			char c = testForNumber.charAt(i);
			if (!(Character.isDigit(c) || c=='.' || c==',' || c=='e' || c=='E'))
				nonDigitCnt++;
		}
		if (n >= 5 && nonDigitCnt==1) {
			String val = null;
			char first = testForNumber.charAt(0);
			if (!Character.isDigit(first) && first!='+' && first!='-')
				val = normalizeValue(testForNumber.substring(1));
			char last = testForNumber.charAt(n-1);
			if (!Character.isDigit(last) && last!='+' && last!='-')
				val = normalizeValue(testForNumber.substring(0, n-1));
			if (val!=null) {
				return val;
			}				
		}
		return null;
	}
	
	public static void normalizeSet(Set<String> hs) {
		LinkedList<String> input = new LinkedList<String>(hs);
		boolean extractNumbers = EXTRACT_NUMBERS.get();
		hs.clear();
		for (String s : input) {
			String sv = normalizeValue(s);
			if (extractNumbers) {
				String num = getNumberOrNull(sv);
				if (num!=null)
					sv = num;
			}
			hs.add(sv);
		}
		hs.remove(null);
		hs.remove("");
	}
	
	public final static void main(String[] args) {
		LinkedHashSet<String> set = new LinkedHashSet<String>();
		set.add("Q49504550312");
		set.add("Q49504550312");
		set.add("");
		set.remove(null);
		DataNormalizer.normalizeSet(set);
		
		Iterator<String> it = set.iterator();
		StringBuffer sb = new StringBuffer();
		do {
			String next = it.next();
			sb.append(next);
			if (it.hasNext())
				sb.append("|");
		}
		while (it.hasNext());
		System.out.println(sb);
	}
}
