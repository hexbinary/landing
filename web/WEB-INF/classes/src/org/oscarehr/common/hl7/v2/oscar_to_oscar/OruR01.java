package org.oscarehr.common.hl7.v2.oscar_to_oscar;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.oscarehr.common.model.Demographic;
import org.oscarehr.common.model.Provider;
import org.oscarehr.util.MiscUtils;

import oscar.util.BuildInfo;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.datatype.FT;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.ROL;

public final class OruR01 {

	private static final Logger logger = MiscUtils.getLogger();

	private static final String TEXT_DATA_FILENAME_PLACEHOLDER="textData.txt";
	
	public static class ObservationData {
		/**
		 * dataName i.e. "wcb_form"
		 */
		public String dataName;
		
		/**
		 * textData can be any text
		 */
		public String textData;
		
		/**
		 * dataFileName is meant to be the file name for the binary data. This field is important and required if binary data is provided because the extention of the file name determines the type of binary data, i.e. foo.jpg lets us know it's a jpg.
		 */
		public String binaryDataFileName;
		
		/**
		 * binaryData is any bytes usually representing bytes from a file / image / pdf / jpg what ever...
		 */
		public byte[] binaryData;
	}

	/**
	 * This method is essentially used to make an ORU_R01 containing pretty much any random data.
	 * @throws UnsupportedEncodingException 
	 */
	public static ORU_R01 makeOruR01(String facilityName, Demographic demographic, ObservationData observationData, Provider sendingProvider, Provider receivingProvider) throws HL7Exception, UnsupportedEncodingException {
		ORU_R01 observationMsg = new ORU_R01();

		DataTypeUtils.fillMsh(observationMsg.getMSH(), new Date(), facilityName, "ORU", "R01", "ORU_R01", DataTypeUtils.HL7_VERSION_ID);
		DataTypeUtils.fillSft(observationMsg.getSFT(), BuildInfo.getBuildTag(), BuildInfo.getBuildDate());

		ORU_R01_PATIENT_RESULT patientResult = observationMsg.getPATIENT_RESULT(0);
		DataTypeUtils.fillPid(patientResult.getPATIENT().getPID(), 1, demographic);

		ORU_R01_ORDER_OBSERVATION orderObservation = patientResult.getORDER_OBSERVATION(0);
		fillBlankOBR(orderObservation.getOBR());
		fillNtesWithObservationData(orderObservation, observationData);

		// use ROL for the sending and receiving provider
		DataTypeUtils.fillRol(orderObservation.getROL(0), sendingProvider, DataTypeUtils.ACTION_ROLE_SENDER);
		DataTypeUtils.fillRol(orderObservation.getROL(1), receivingProvider, DataTypeUtils.ACTION_ROLE_RECEIVER);

		return (observationMsg);
	}

	private static void fillNtesWithObservationData(ORU_R01_ORDER_OBSERVATION orderObservation, ObservationData observationData) throws HL7Exception, UnsupportedEncodingException {
		// Use NTE's to send random data, each nte represents one piece of data. i.e. text data and binary data
		// Each comment text is only 64k so we'll break large data into comment repetitions.
		// Example : nte.commenttype.Text="WCB Form", nte.commenttype.NameOfCodingSystem="foo.pdf", nte.comment=<base64 encoded contents of the pdf file>

		int nteCounter = 0;
		
		if (observationData.textData!=null)
		{
			NTE nte = orderObservation.getNTE(nteCounter);
			fillOneNte(nte, observationData.dataName, TEXT_DATA_FILENAME_PLACEHOLDER, OscarToOscarUtils.encodeToBase64String(observationData.textData.getBytes()));
			nteCounter++;
		}
		
		if (observationData.binaryData!=null)
		{
			NTE nte = orderObservation.getNTE(nteCounter);
			fillOneNte(nte, observationData.dataName, observationData.binaryDataFileName, OscarToOscarUtils.encodeToBase64String(observationData.binaryData));
		}
	}

	private static void fillOneNte(NTE nte, String dataName, String fileName, String stringData) throws HL7Exception {

		nte.getCommentType().getText().setValue(dataName);
		nte.getCommentType().getNameOfCodingSystem().setValue(fileName);

		int dataLength = stringData.length();
		int chunks = dataLength / DataTypeUtils.NTE_COMMENT_MAX_SIZE;
		if (dataLength % DataTypeUtils.NTE_COMMENT_MAX_SIZE != 0) chunks++;
		logger.debug("Breaking Observation Data (" + dataLength + ") into chunks:" + chunks);

		for (int i = 0; i < chunks; i++) {
			FT commentPortion = nte.getComment(i);

			int startIndex = i * DataTypeUtils.NTE_COMMENT_MAX_SIZE;
			int endIndex = Math.min(dataLength, startIndex + DataTypeUtils.NTE_COMMENT_MAX_SIZE);

			commentPortion.setValue(stringData.substring(startIndex, endIndex));
		}
	}

	private static String getNteCommentsAsSingleString(NTE nte) {
		FT[] fts = nte.getComment();

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fts.length; i++)
			sb.append(fts[i].getValue());

		return (sb.toString());
	}

	/**
	 * An OBR segment is required even though none of the fields are relevant. This will create essentially a blank / useless OBR. It will fill in required fields with valid but essentially useless data.
	 * 
	 * @throws DataTypeException
	 */
	public static void fillBlankOBR(OBR obr) throws DataTypeException {
		obr.getUniversalServiceIdentifier().getIdentifier().setValue(String.valueOf(System.nanoTime()));
	}

	public static Provider getProviderByActionRole(ORU_R01 oruR01, String actionRole) throws HL7Exception {
		ORU_R01_ORDER_OBSERVATION orderObservation = oruR01.getPATIENT_RESULT(0).getORDER_OBSERVATION(0);

		for (int i = 0; i < orderObservation.getROLReps(); i++) {
			ROL rol = orderObservation.getROL(i);
			if (actionRole.equals(rol.getRoleROL().getIdentifier().getValue())) {
				return (DataTypeUtils.parseRolAsProvider(rol));
			}
		}

		return (null);
	}

	public static ObservationData getObservationData(ORU_R01 oruR01) throws HL7Exception, UnsupportedEncodingException {
		ORU_R01_ORDER_OBSERVATION orderObservation = oruR01.getPATIENT_RESULT(0).getORDER_OBSERVATION(0);

		ObservationData result = new ObservationData();

		for (int i = 0; i < orderObservation.getNTEReps(); i++) {
			fillObservationDataFromNte(result, orderObservation.getNTE(i));
		}

		return (result);
	}

	private static void fillObservationDataFromNte(ObservationData observationData, NTE nte) throws UnsupportedEncodingException {
		
		String temp=nte.getCommentType().getText().getValue();
		if (temp!=null) observationData.dataName = temp;
		
		temp=nte.getCommentType().getNameOfCodingSystem().getValue();
		if (TEXT_DATA_FILENAME_PLACEHOLDER.equals(temp))
		{
			observationData.textData=new String(OscarToOscarUtils.decodeBase64(getNteCommentsAsSingleString(nte)), OscarToOscarUtils.ENCODING);
		}
		else
		{
			observationData.binaryDataFileName=temp;
			observationData.binaryData=OscarToOscarUtils.decodeBase64(getNteCommentsAsSingleString(nte));
		}
	}

	public static void main(String... argv) throws Exception {
		// this is here just to test some of the above functions since 
		// we are not using junit tests...
		
		Demographic demographic = new Demographic();
		demographic.setLastName("test LN");
		demographic.setLastName("test FN");
		demographic.setBirthDay(new GregorianCalendar(1960, 2, 3));

		ObservationData observationData = new ObservationData();
		observationData.dataName="txt test";
		observationData.textData="once upon a time";
		observationData.binaryDataFileName="/tmp/oscar.properties";
		byte[] b=FileUtils.readFileToByteArray(new File(observationData.binaryDataFileName));;
		observationData.binaryData = b;
		

		Provider sender = new Provider();
		sender.setProviderNo("111");
		sender.setLastName("sender ln");
		sender.setFirstName("sender fn");

		Provider receiver = new Provider();
		receiver.setProviderNo("222");
		receiver.setLastName("receiver ln");
		receiver.setFirstName("receiver fn");

		ORU_R01 observationMsg = makeOruR01("facility name", demographic, observationData, sender, receiver);

		String messageString = OscarToOscarUtils.pipeParser.encode(observationMsg);
		logger.info(messageString);

		ORU_R01 newObservationMsg = (ORU_R01) OscarToOscarUtils.pipeParser.parse(messageString);
		byte[] decoded = OscarToOscarUtils.decodeBase64(getNteCommentsAsSingleString(newObservationMsg.getPATIENT_RESULT(0).getORDER_OBSERVATION(0).getNTE(1)));
		logger.info("equal binary data:" + Arrays.equals(b, decoded));
		logger.info("text data:" + getNteCommentsAsSingleString(newObservationMsg.getPATIENT_RESULT(0).getORDER_OBSERVATION(0).getNTE(0)));
		FileUtils.writeByteArrayToFile(new File("/tmp/out.test"), decoded);
	}
}