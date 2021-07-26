package com.ipras.filecompare.render;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.beans.factory.annotation.Value;

public class ExcelRender {

	@Value("${input.file1.path}")
	private String sourcePath1;

	@Value("${input.file2.path}")
	private String sourcePath2;

	@Value("${color.highlight.whitespace}")
	private String whitespaceHighlight;

	@Value("${color.highlight.alternate}")
	private String alternateHighlight;

	@Value("${color.highlight.header}")
	private String headerHighlight;

	@Value("${header.row.count}")
	private int headerCount;

	@Value("${output.file.path}")
	private String outputPath;

	@Value("${output.file.name}")
	private String outputFileName;

	@Value("${output.file.date}")
	private String outputFileDate;

	SXSSFWorkbook wb;

	SXSSFSheet detailSheet;
	SXSSFSheet summarySheet;

	XSSFCellStyle whitespaceMismatchStyle;
	XSSFCellStyle alternateRowStyle;
	XSSFCellStyle headerStyle;

	int detailGridRowCount = 0;

	public void addFileHeaders(String[] headers) throws IOException {

		wb = new SXSSFWorkbook();
		wb.setCompressTempFiles(true);

		detailSheet = wb.createSheet("Detail View");
		summarySheet = wb.createSheet("Summary View");

		whitespaceMismatchStyle = (XSSFCellStyle) wb.createCellStyle();
		alternateRowStyle = (XSSFCellStyle) wb.createCellStyle();
		headerStyle = (XSSFCellStyle) wb.createCellStyle();

		whitespaceMismatchStyle
				.setFillForegroundColor(new XSSFColor(hex2Rgb(whitespaceHighlight), new DefaultIndexedColorMap()));
		whitespaceMismatchStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		alternateRowStyle
				.setFillForegroundColor(new XSSFColor(hex2Rgb(alternateHighlight), new DefaultIndexedColorMap()));
		alternateRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		headerStyle.setFillForegroundColor(new XSSFColor(hex2Rgb(headerHighlight), new DefaultIndexedColorMap()));
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		detailGridRowCount++;

		detailSheet.setRandomAccessWindowSize(100);

		Row row = detailSheet.createRow(detailGridRowCount - 1);

		Cell cell = row.createCell(0);
		cell.setCellValue("File Name");
		cell.setCellStyle(headerStyle);

		cell = row.createCell(1);
		cell.setCellValue("Source Row Number");
		cell.setCellStyle(headerStyle);

		cell = row.createCell(2);
		cell.setCellValue("Sorted Row Number");
		cell.setCellStyle(headerStyle);

		for (int i = 0; i < headers.length; i++) {
			cell = row.createCell(i + 3);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}

	}

	public static Color hex2Rgb(String colorStr) {
		return new Color(Integer.valueOf(colorStr.substring(1, 3), 16), Integer.valueOf(colorStr.substring(3, 5), 16),
				Integer.valueOf(colorStr.substring(5, 7), 16));
	}

	public void addNewLine(String[][] compareResult, int sortRowNum) {

		detailGridRowCount++;

		Row row = detailSheet.createRow(detailGridRowCount - 1);

		Cell cell = row.createCell(0);
		cell.setCellValue(sourcePath1.substring(sourcePath1.lastIndexOf("/") + 1));

		cell = row.createCell(1);
		cell.setCellValue(compareResult[0][0]);

		cell = row.createCell(2);
		cell.setCellValue(sortRowNum);

		for (int i = 1; i < compareResult[0].length - 1; i++) {

			Cell mismatchCell = row.createCell(i + 2);
			mismatchCell.setCellValue(compareResult[0][i]);

		}

		detailGridRowCount++;

		row = detailSheet.createRow(detailGridRowCount - 1);

		cell = row.createCell(0);
		cell.setCellValue(sourcePath2.substring(sourcePath2.lastIndexOf("/") + 1));
		cell.setCellStyle(alternateRowStyle);

		cell = row.createCell(1);
		cell.setCellValue(compareResult[1][0]);
		cell.setCellStyle(alternateRowStyle);

		cell = row.createCell(2);
		cell.setCellValue(sortRowNum);
		cell.setCellStyle(alternateRowStyle);

		for (int i = 1; i < compareResult[1].length - 1; i++) {

			Cell mismatchCell = row.createCell(i + 2);
			mismatchCell.setCellValue(compareResult[1][i]);
			mismatchCell.setCellStyle(alternateRowStyle);

		}

	}

	public void generateOutput() throws IOException {

		FileOutputStream out;

		if (outputFileDate == null || outputFileDate.equals("")) {
			out = new FileOutputStream(outputPath + "/" + outputFileName + ".xlsx");
		} else {
			String outputFileTimeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
			out = new FileOutputStream(outputPath + "/" + outputFileName + "_" + outputFileTimeStamp + ".xlsx");
		}

		wb.write(out);
		out.close();

	}

	public void createSummaryGrid(String[] fileHeaders, List<Integer> columnBreakCounts) {

		System.out.println("creating summary grid.");

		summarySheet.setRandomAccessWindowSize(100);

		Row row = summarySheet.createRow(0);

		Cell cell = row.createCell(0);
		cell.setCellValue("Column Name");
		cell.setCellStyle(headerStyle);

		cell = row.createCell(1);
		cell.setCellValue("Break Count");
		cell.setCellStyle(headerStyle);

		for (int i = 0; i < columnBreakCounts.size(); i++) {

			row = summarySheet.createRow(i + 1);

			cell = row.createCell(0);
			cell.setCellValue(fileHeaders[i]);

			cell = row.createCell(1);
			cell.setCellValue(columnBreakCounts.get(i));


		}

	}

}
