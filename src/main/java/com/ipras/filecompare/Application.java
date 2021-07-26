package com.ipras.filecompare;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.ipras.filecompare.service.FileCompare;

public class Application {

	public static void main(String[] args)
			throws EncryptedDocumentException, InvalidFormatException, IOException, URISyntaxException {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ApplicationConfig.class);


		FileCompare compare = context.getBean(FileCompare.class);

		compare.fileCompare();
		
		context.close();

	}

}