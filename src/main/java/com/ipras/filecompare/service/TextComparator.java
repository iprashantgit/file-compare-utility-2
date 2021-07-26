package com.ipras.filecompare.service;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.beans.factory.annotation.Value;

public class TextComparator {

	@Value("${input.file1.path}")
	private String sourcePath1;

	@Value("${input.file2.path}")
	private String sourcePath2;

	@Value("${input.file.delimiter}")
	private String delimiter;

	@Value("${temp.file.path}")
	private String tempPath;

	@Value("${header.row.count}")
	private String headerCount;

	@Value("${footer.row.count}")
	private String footerCount;

	@Value("${sort.column.number}")
	private String sortColumns;

	@Value("${sort.preprocess.trim}")
	private String sortTrimFlag;

	@Value("${file.copy.encoding}")
	private String fileEncoding;

	@Value("${color.highlight.whitespace}")
	private String whitespaceHighlight;

	@Value("${color.highlight.alternate}")
	private String alternateHighlight;

	@Value("${color.highlight.header}")
	private String headerHighlight;

	@Value("${output.file.path}")
	private String outputPath;

	@Value("${output.file.name}")
	private String outputFileName;

	@Value("${output.file.date}")
	private String outputFileDate;

	int detailGridRowCount = 0;
	int summaryGridRowCount = 0;

	List<Integer> columnBreakCounts;
	List<Integer> whiteSpaceBreakCount;

	String[] headers;

	SXSSFWorkbook wb;

	SXSSFSheet detailSheet;
	SXSSFSheet summarySheet;

	XSSFCellStyle whitespaceMismatchStyle;
	XSSFCellStyle alternateRowStyle;
	XSSFCellStyle headerStyle;

	boolean trimSortingFlag;
	
	int sortRowCount;

	public void compareText()
			throws EncryptedDocumentException, InvalidFormatException, IOException, URISyntaxException {

		int[] fileNotFound = { 0, 0 };

		if (sortTrimFlag.equals("Y")) {
			trimSortingFlag = true;
		} else {
			trimSortingFlag = false;
		}

		// begin excel comparison

		// load source file
		FileInputStream file1 = null;
		try {
			file1 = new FileInputStream(new File(sourcePath1));
		} catch (FileNotFoundException e) {
			fileNotFound[0] = 1;
		}
		FileInputStream file2 = null;
		try {
			file2 = new FileInputStream(new File(sourcePath2));
		} catch (FileNotFoundException e) {
			fileNotFound[1] = 1;
		}

		if (fileNotFound[0] == 1 || fileNotFound[1] == 1) {
			System.out.println("file not found");
			return;
		}

		String tempFileTimeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());

		String sourceFile1NameWithExtention = sourcePath1.substring(sourcePath1.lastIndexOf("/") + 1);
		String sourceFile1Name = sourceFile1NameWithExtention.substring(0, sourceFile1NameWithExtention.indexOf("."))
				+ "_" + tempFileTimeStamp;

		String sourceFile2NameWithExtention = sourcePath2.substring(sourcePath2.lastIndexOf("/") + 1);
		String sourceFile2Name = sourceFile2NameWithExtention.substring(0, sourceFile2NameWithExtention.indexOf("."))
				+ "_" + tempFileTimeStamp;

		if (sortColumns == null || sortColumns.equals("")) {
			System.out.println("INFO: No Sorting is applied on the files.");

			prepareFileForCompare(sourcePath1, tempPath + "/" + sourceFile1Name + "_filtered.csv",
					Integer.parseInt(headerCount), Integer.parseInt(footerCount));

			prepareFileForCompare(sourcePath2, tempPath + "/" + sourceFile2Name + "_filtered.csv",
					Integer.parseInt(headerCount), Integer.parseInt(footerCount));
		} else {
			prepareFileForCompare(sourcePath1, tempPath + "/" + sourceFile1Name + "_sorted.csv",
					Integer.parseInt(headerCount), Integer.parseInt(footerCount));

			prepareFileForCompare(sourcePath2, tempPath + "/" + sourceFile2Name + "_sorted.csv",
					Integer.parseInt(headerCount), Integer.parseInt(footerCount));
		}

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

		if (Integer.parseInt(headerCount) == 1) {
			addFileHeaders(sourcePath1);
		}

		// compare();
		LineIterator leftFile;
		LineIterator rightFile;

		if (sortColumns == null || sortColumns.equals("")) {
			leftFile = FileUtils.lineIterator(new File(tempPath + "/" + sourceFile1Name + "_filtered.csv"),
					fileEncoding);
			rightFile = FileUtils.lineIterator(new File(tempPath + "/" + sourceFile2Name + "_filtered.csv"),
					fileEncoding);
		} else {
			leftFile = FileUtils.lineIterator(new File(tempPath + "/" + sourceFile1Name + "_sorted.csv"), fileEncoding);
			rightFile = FileUtils.lineIterator(new File(tempPath + "/" + sourceFile2Name + "_sorted.csv"),
					fileEncoding);
		}


		boolean rowCountMismatch = false;

		whiteSpaceBreakCount = new ArrayList<>(Arrays.asList(new Integer[headers.length]));
		Collections.fill(whiteSpaceBreakCount, 0);

		int sortedRowNum = 0;
		
		while (leftFile.hasNext() || rightFile.hasNext()) {

			if (leftFile.hasNext() != rightFile.hasNext()) {
				System.out.println("Warning: Row Number does not match between the files.");
				rowCountMismatch = true;
				break;
			}
			
			sortedRowNum++;

			String left = leftFile.nextLine();
			String right = rightFile.nextLine();

			compare(sortedRowNum, Arrays.asList(left.split(Pattern.quote(delimiter), -1)),
					Arrays.asList(right.split(Pattern.quote(delimiter), -1)));
		}

		if (!rowCountMismatch) {
			createSummaryGrid();
		}

		FileOutputStream out;

		if (outputFileDate == null || outputFileDate.equals("")) {
			out = new FileOutputStream(outputPath + "/" + outputFileName + ".xlsx");
		} else {
			String outputFileTimeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
			out = new FileOutputStream(outputPath + "/" + outputFileName + "_" + outputFileTimeStamp + ".xlsx");
		}

		wb.write(out);
		out.close();

		System.out.println("");

	}

	private void createSummaryGrid() {

		System.out.println("creating summary grid.");

		summarySheet.setRandomAccessWindowSize(100);

		Row row = summarySheet.createRow(0);

		Cell cell = row.createCell(0);
		cell.setCellValue("Column Name");
		cell.setCellStyle(headerStyle);

		cell = row.createCell(1);
		cell.setCellValue("Total Break Count");
		cell.setCellStyle(headerStyle);

		cell = row.createCell(2);
		cell.setCellValue("Whitespace Break Count");
		cell.setCellStyle(headerStyle);

		cell = row.createCell(3);
		cell.setCellValue("Value Break Count");
		cell.setCellStyle(headerStyle);

		for (int i = 0; i < headers.length; i++) {

			row = summarySheet.createRow(i + 1);

			cell = row.createCell(0);
			cell.setCellValue(headers[i]);

			cell = row.createCell(1);
			cell.setCellValue(columnBreakCounts.get(i));

			cell = row.createCell(2);
			cell.setCellValue(whiteSpaceBreakCount.get(i));

			cell = row.createCell(3);
			cell.setCellValue(columnBreakCounts.get(i) - whiteSpaceBreakCount.get(i));

		}

	}

	private void compare(int sortedRowNum, List<String> left, List<String> right) throws IOException {

		String leftRowNumStr = left.get(left.size()-1);
		int leftRowNum = Integer.parseInt(leftRowNumStr.substring(leftRowNumStr.indexOf("--")+2 ));
		
		left.set(left.size()-1, left.get(left.size()-1).substring(0,left.get(left.size()-1).indexOf("--")));
		
		String rightRowNumStr = right.get(right.size()-1);
		int rightRowNum = Integer.parseInt(rightRowNumStr.substring(rightRowNumStr.indexOf("--")+2 ));
		
		right.set(right.size()-1, right.get(right.size()-1).substring(0,right.get(right.size()-1).indexOf("--")));
		
		
		System.out.println("left rowNum: " + leftRowNum + ", left: " + left + "right rowNum: " + rightRowNum + ", right: " + right);

		if (left.size() != right.size()) {

			System.out.println("Warning: Column Count does not match between the files");
			return;
		}

		List<String> leftMismatch = IntStream.range(0, left.size()).filter(i -> !left.get(i).equals(right.get(i)))
				.mapToObj(i -> left.get(i)).collect(Collectors.toList());

		List<String> rightMismatch = IntStream.range(0, right.size()).filter(i -> !right.get(i).equals(left.get(i)))
				.mapToObj(i -> right.get(i)).collect(Collectors.toList());

		List<Integer> mismatchColumnIndex = IntStream.range(0, left.size())
				.mapToObj(i -> left.get(i) + delimiter + (i + 1) + delimiter + right.get(i))
				.filter(e -> !e.split(Pattern.quote(delimiter), -1)[0].equals(e.split(Pattern.quote(delimiter), -1)[2]))
				.mapToInt(e -> Integer.valueOf(e.split(Pattern.quote(delimiter), -1)[1])).mapToObj(e -> e)
				.collect(Collectors.toList());

		if (mismatchColumnIndex.size() == 0) {
			return;
		}

		// System.out.println(leftMismatch);
		// System.out.println(rightMismatch);
		// System.out.println(mismatchColumnIndex);

		addLineMismatch(sortedRowNum, leftRowNum, rightRowNum, leftMismatch, rightMismatch, mismatchColumnIndex);

	}

	private void addLineMismatch(int sortedRowNum, int leftRowNum, int rightRowNum, List<String> leftMismatch, List<String> rightMismatch,
			List<Integer> mismatchColumnIndex) {

		detailGridRowCount++;

		Row row = detailSheet.createRow(detailGridRowCount - 1);

		Cell cell = row.createCell(0);
		cell.setCellValue(sourcePath1.substring(sourcePath1.lastIndexOf("/") + 1));

		cell = row.createCell(1);
		cell.setCellValue(leftRowNum);
		
		cell = row.createCell(2);
		cell.setCellValue(sortedRowNum);
		

		for (int i = 0; i < mismatchColumnIndex.size(); i++) {

			Cell mismatchCell = row.createCell(mismatchColumnIndex.get(i) + 2);
			mismatchCell.setCellValue(leftMismatch.get(i));

			if (leftMismatch.get(i).trim().equals(rightMismatch.get(i).trim())) {

				whiteSpaceBreakCount.set(mismatchColumnIndex.get(i) - 1,
						whiteSpaceBreakCount.get(mismatchColumnIndex.get(i) - 1) + 1);

				mismatchCell.setCellStyle(whitespaceMismatchStyle);
			}

		}

		detailGridRowCount++;

		row = detailSheet.createRow(detailGridRowCount - 1);

		row.setRowStyle(alternateRowStyle);

		cell = row.createCell(0);
		cell.setCellValue(sourcePath2.substring(sourcePath2.lastIndexOf("/") + 1));
		cell.setCellStyle(alternateRowStyle);

		cell = row.createCell(1);
		cell.setCellValue(rightRowNum);
		cell.setCellStyle(alternateRowStyle);
		
		cell = row.createCell(1);
		cell.setCellValue(sortedRowNum);
		cell.setCellStyle(alternateRowStyle);
		

		for (int i = 0; i < mismatchColumnIndex.size(); i++) {

			Cell mismatchCell = row.createCell(mismatchColumnIndex.get(i) + 1);
			mismatchCell.setCellValue(rightMismatch.get(i));

			if (leftMismatch.get(i).trim().equals(rightMismatch.get(i).trim())) {

				mismatchCell.setCellStyle(whitespaceMismatchStyle);

			}

		}

		for (int i = 0; i < mismatchColumnIndex.size(); i++) {
			columnBreakCounts.set(mismatchColumnIndex.get(i) - 1,
					columnBreakCounts.get(mismatchColumnIndex.get(i) - 1) + 1);
		}

	}

	private void addFileHeaders(String sourcePath) throws IOException {

		detailGridRowCount++;

		Path path = Paths.get(sourcePath);

		Charset cs = Charset.forName(fileEncoding);

		Stream<String> lines = Files.lines(path, cs);

		headers = lines.limit(1).collect(Collectors.joining()).split(Pattern.quote(delimiter), -1);

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

		// initializing counter array to get per column breaks
		columnBreakCounts = new ArrayList<>(Arrays.asList(new Integer[headers.length]));
		Collections.fill(columnBreakCounts, 0);

		lines.close();
	}

	private void prepareFileForCompare(String filePath, String fileCopyPath, int headerCount, int footerCount)
			throws URISyntaxException, IOException {

		Path path = Paths.get(filePath);

		Path copyPath = Paths.get(fileCopyPath);

		Charset cs = Charset.forName(fileEncoding);

		Stream<String> lines = Files.lines(path, cs);

		int rowCount = (int) Files.lines(path, cs).count();

		Stream<String> filteredLines = lines.limit(rowCount - footerCount).skip(headerCount);

		if (sortColumns == null || sortColumns.equals("")) {

			Files.write(copyPath, filteredLines.collect(Collectors.toList()));
			lines.close();
			return;
		}

		sortRowCount = 1;
		
		filteredLines = filteredLines.map(l -> l.concat("--" + counter() ));
		
		// System.out.println(sortColumnsArray[0] + "||" + sortColumnsArray[1]);

		List<String> sortedLines = filteredLines.sorted(new Comparator<String>() {
			@Override
			public int compare(String l1, String l2) {

				if (sortColumns.toLowerCase().equals("all")) {
					return l1.compareTo(l2);
				}

				String[] sortColumnsArray = sortColumns.split(Pattern.quote(","));

				String substringL1 = "";

				String[] l1Array = l1.split(Pattern.quote(delimiter));
				String[] l2Array = l2.split(Pattern.quote(delimiter));

				for (int i = 0; i < sortColumnsArray.length; i++) {

					if (trimSortingFlag)
						substringL1 += l1Array[Integer.parseInt(sortColumnsArray[i]) - 1].trim();
					else
						substringL1 += l1Array[Integer.parseInt(sortColumnsArray[i]) - 1];

				}

				String substringL2 = "";

				for (int i = 0; i < sortColumnsArray.length; i++) {

					if (trimSortingFlag)
						substringL2 += l2Array[Integer.parseInt(sortColumnsArray[i]) - 1].trim();
					else
						substringL2 += l2Array[Integer.parseInt(sortColumnsArray[i]) - 1];

				}

				return substringL1.compareTo(substringL2);
			}
		}).collect(Collectors.toList());

		Files.write(copyPath, sortedLines);

		// Files.delete(copyPath);

		lines.close();

		// System.out.println(sortedLines);

	}

	private int counter() {
		// TODO Auto-generated method stub
		return sortRowCount++;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public String getSourcePath1() {
		return sourcePath1;
	}

	public void setSourcePath1(String sourcePath1) {
		this.sourcePath1 = sourcePath1;
	}

	public String getSourcePath2() {
		return sourcePath2;
	}

	public void setSourcePath2(String sourcePath2) {
		this.sourcePath2 = sourcePath2;
	}

	/**
	 * 
	 * @param colorStr e.g. "#FFFFFF"
	 * @return
	 */
	public static Color hex2Rgb(String colorStr) {
		return new Color(Integer.valueOf(colorStr.substring(1, 3), 16), Integer.valueOf(colorStr.substring(3, 5), 16),
				Integer.valueOf(colorStr.substring(5, 7), 16));
	}

}
