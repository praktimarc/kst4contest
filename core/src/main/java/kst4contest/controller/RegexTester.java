package kst4contest.controller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kst4contest.model.ChatMember;

public class RegexTester {

	public static void main(String[] args) {

		
		String temp;
		
		temp = "DL1KDA JO30EP Alex\r\n"
				+ "DL3LST JO61FI Ray(Rainer)\r\n"
				+ "DL4ZAQ JN49MP Peter 2m&70cm&6m\r\n"
				+ "DL9LBH JN59ID Hans 2/70/23\r\n"
				+ "DO5AMF JN49FK Marc\r\n"
				+ "F5ICN JN03BF Alex QRV 2/70/23\r\n"
				+ "F6GRA JN04DB Carol 2m\r\n"
				+ "F6HTJ JN12KQ Michel\r\n"
				+ "G0BIX JO01GI Terry\r\n"
				+ "G0HOF IO92UL Ken 11el + 400 W\r\n"
				+ "G0LBK JO03BD David\r\n"
				+ "G0MBL JO01QH Andrew 2m\r\n"
				+ "G1SDX IO80FL Grant\r\n"
				+ "G3MXH JO02LF Terry\r\n"
				+ "G3OVH IO92JP Tony\r\n"
				+ "G3VCA IO93MG bob\r\n"
				+ "G4AEP IO91MB bill\r\n"
				+ "G4DHF IO92UU David\r\n"
				+ "G4FUF JO01GN Keith\r\n"
				+ "G4TRA IO81WN Steve\r\n"
				+ "G6HKS IO92OB Richard\r\n"
				+ "G8SEI IO92FO Jeff 6/4/2/70\r\n"
				+ "GM0EWX IO67UL Calum\r\n"
				+ "GW8IZR IO73TI Paul\r\n"
				+ "IV3GTH JN65RU Gigi\r\n"
				+ "IZ2XZM JN45KH Nick\r\n"
				+ "LY1BWB KO24PR VU Club 2/70\r\n"
				+ "LY2EN KO24PQ QRT\r\n"
				+ "OK1FPR JO80CE Milos\r\n"
				+ "OV3T JO46CM Thomas\r\n"
				+ "OY4TN IP62NB Trygvi 11EL/350W\r\n"
				+ "OZ2M JO65FR Bo PNGonVHF\r\n"
				+ "OZ3Z JO45UN Anders @432.228\r\n"
				+ "OZ7UV JO65DH Svend\r\n"
				+ "PA3DOL JO22MT Sjoerd\r\n"
				+ "PA3PCV JO20VV Marcel 2m\r\n"
				+ "PA9R JO22JK Rob\r\n"
				+ "S52FO JN76EF Janez\r\n"
				+ "S53RM JN76HD Sine\r\n"
				+ "SM2CEW KP15CR Peter\r\n"
				+ "SP6VGJ JO81HU Jacek";

		
		Pattern pattern = Pattern.compile("([a-zA-Z0-9]{2}/{1})?([a-zA-Z0-9]{1,3}[0-9][a-zA-Z0-9]{0,3}[a-zA-Z]{0,3})(/p)? [a-zA-Z]{2}[0-9]{2}[a-zA-Z]{2} [ -~]{1,20}");
		Matcher matcher = pattern.matcher(temp);
		/**
		 * "([a-zA-Z0-9]{1,2}\/)?[a-zA-Z0-9]{1,3}[0-9][a-zA-Z0-9]{0,3}[a-zA-Z](\/(p|m))?( )[a-zA-Z]{2}[0-9]{2}[a-zA-Z]{2}[ -~]{0,30}"gm
		 * Thats a line of the show users list
		 */
		while (matcher.find()) {
//			System.out.println("Chatmember detected: "+ matcher.group() + " " + matcher.start());
			
			ChatMember member = new ChatMember();
			String matchedString = matcher.group();
			
			String[] splittedUserString;
			splittedUserString = matchedString.split(" ");
			
			member.setCallSign(splittedUserString[0]);
			member.setQra(splittedUserString[1]);
			
			String stringAggregation = ""; 
			for (int i = 2; i < splittedUserString.length; i++) {
				stringAggregation += splittedUserString[i] + " ";
			}
			member.setName(stringAggregation);
			
			System.out.println("Call: " + member.getCallSign() + ", QRA: " + member.getQra() + ", Name: " + member.getName());
		}
	}

}
