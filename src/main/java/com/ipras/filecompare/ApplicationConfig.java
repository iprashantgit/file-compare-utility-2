package com.ipras.filecompare;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.ipras.filecompare.preprocess.PreProcessFile;
import com.ipras.filecompare.render.ExcelRender;
import com.ipras.filecompare.service.FileCompare;
import com.ipras.filecompare.service.TextComparator;
import com.ipras.filecompare.utils.GenerateTempFileName;

@Configuration
@PropertySource("application.properties")
public class ApplicationConfig {

	@Bean
	public TextComparator textComparator() {
		return new TextComparator();
	}
	
	@Bean
	public PreProcessFile preProcessFile() {
		return new PreProcessFile();
	}
	
	@Bean
	public FileCompare fileCompare() {
		return new FileCompare();
	}
	
	@Bean
	public ExcelRender excelRender() {
		return new ExcelRender();
	}
	
	@Bean
	public GenerateTempFileName generateTempFileName() {
		return new GenerateTempFileName();
	}

}
