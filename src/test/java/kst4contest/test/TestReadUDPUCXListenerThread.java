package kst4contest.test;

import kst4contest.controller.ReadUDPbyUCXMessageThread;
import org.junit.jupiter.api.Test;

public class TestReadUDPUCXListenerThread {

	@Test
	public static void main(String[] args) {
		
		ReadUDPbyUCXMessageThread ucxUDPReader = new ReadUDPbyUCXMessageThread(12060);
		ucxUDPReader.start();
		
		
		String testThis;
		
		testThis = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
				+ "<contactinfo>\r\n"
				+ "  <app>UcxLog</app>\r\n"
				+ "  <contestname></contestname>\r\n"
				+ "  <contestnr></contestnr>\r\n"
				+ "  <timestamp>2022-10-30 11:51:00</timestamp>\r\n"
				+ "  <mycall>DO5AMF</mycall>\r\n"
				+ "  <band>144</band>\r\n"
				+ "  <rxfreq>14400000</rxfreq>\r\n"
				+ "  <txfreq>14400000</txfreq>\r\n"
				+ "  <operator></operator>\r\n"
				+ "  <mode>USB</mode>\r\n"
				+ "  <call>DM1AO</call>\r\n"
				+ "  <countryprefix></countryprefix>\r\n"
				+ "  <wpxprefix></wpxprefix>\r\n"
				+ "  <stationprefix></stationprefix>\r\n"
				+ "  <continent>EU</continent>\r\n"
				+ "  <snt>59</snt>\r\n"
				+ "  <sntnr>021 JN49FK</sntnr>\r\n"
				+ "  <rcv>59</rcv>\r\n"
				+ "  <rcvnr>001 JO50LT</rcvnr>\r\n"
				+ "  <gridsquare></gridsquare>\r\n"
				+ "  <exchange1></exchange1>\r\n"
				+ "  <section></section>\r\n"
				+ "  <comment></comment>\r\n"
				+ "  <qth></qth>\r\n"
				+ "  <name></name>\r\n"
				+ "  <power></power>\r\n"
				+ "  <misctext></misctext>\r\n"
				+ "  <zone>0</zone>\r\n"
				+ "  <prec></prec>\r\n"
				+ "  <ck>0</ck>\r\n"
				+ "  <ismultiplierl></ismultiplierl>\r\n"
				+ "  <ismultiplier2></ismultiplier2>\r\n"
				+ "  <ismultiplier3></ismultiplier3>\r\n"
				+ "  <points></points>\r\n"
				+ "  <radionr>1</radionr>\r\n"
				+ "  <run1run2>1</run1run2>\r\n"
				+ "  <RoverLocation></RoverLocation>\r\n"
				+ "  <RadioInterfaced>0</RadioInterfaced>\r\n"
				+ "  <NetworkedCompNr>0</NetworkedCompNr>\r\n"
				+ "  <IsOriginal>True</IsOriginal>\r\n"
				+ "  <NetBiosName></NetBiosName>\r\n"
				+ "  <IsRunQSO>0</IsRunQSO>\r\n"
				+ "  <StationName>LAPTOP-05GRNFHI</StationName>\r\n"
				+ "  <ID>0C73B5B3B82C4836</ID>\r\n"
				+ "  <IsClaimedQso>True</IsClaimedQso>\r\n"
				+ "</contactinfo>  ";
//		ucxUDPReader.processUCXUDPMessage(testThis);
		
		
		testThis ="<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
				+ "<RadioInfo>\r\n"
				+ "  <app>UcxLog</app>\r\n"
				+ "  <RadioNr>1</RadioNr>\r\n"
				+ "  <Freq>2101100</Freq>\r\n"
				+ "  <TXFreq>2101100</TXFreq>\r\n"
				+ "  <Mode>CW</Mode>\r\n"
				+ "  <IsSplit>False</IsSplit>\r\n"
				+ "</RadioInfo>";
		
//		ucxUDPReader =new ReadUDPbyUCXMessageThread(12060);
//		ucxUDPReader.processUCXUDPMessage(testThis);
	}

}
