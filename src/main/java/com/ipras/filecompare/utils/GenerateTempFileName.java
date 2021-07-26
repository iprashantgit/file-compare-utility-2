package com.ipras.filecompare.utils;

public class GenerateTempFileName {

	public String getTempFileName(String filePath, String type) {

		// String tempFileTimeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());

		String sourceFileNameWithExtention = filePath.substring(filePath.lastIndexOf("/") + 1);
		String sourceFileName = sourceFileNameWithExtention.substring(0, sourceFileNameWithExtention.indexOf(".")) 
				// + "_"+ tempFileTimeStamp
				;

		String tempFileName = "";

		if (type.equals("Mapper")) {
			tempFileName = sourceFileName + "_mapped" + ".txt";
		} else if (type.equals("Sort")) {
			tempFileName = sourceFileName + "_sorted" + ".txt";
		} else {
			tempFileName = sourceFileName + "_filtered" + ".txt";
		}

		return tempFileName;

	}

}
