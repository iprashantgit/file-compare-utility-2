package com.ipras.filecompare.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.ipras.filecompare.preprocess.PreProcessFile;
import com.ipras.filecompare.render.ExcelRender;
import com.ipras.filecompare.utils.GenerateTempFileName;

public class FileCompare {

	@Value("${input.file1.path}")
	private String sourcePath1;

	@Value("${input.file2.path}")
	private String sourcePath2;

	@Value("${input.file.delimiter}")
	private String delimiter;

	@Value("${compare.preprocess.sort}")
	private boolean sortFlag;

	@Value("${app.hide.matched.rows}")
	private boolean hideMatchedRowsFlag;

	@Value("${temp.file.path}")
	private String tempFilePath;

	@Autowired
	PreProcessFile processFile;

	@Autowired
	ExcelRender excelRender;

	@Autowired
	private GenerateTempFileName tempFileName;

	String file1, file2;

	List<Integer> columnBreakCounts;

	public void fileCompare() throws IOException {

		try {
			processFile.preProcessFile(sourcePath1, sourcePath2);

		} catch (IOException e) {
			System.out.println("File does not exists: " + e.getMessage());
			return;
		}

		// adding file headers in output
		String[] fileHeaders = processFile.getFileHeaders(new File(sourcePath1).toPath());

		excelRender.addFileHeaders(fileHeaders);

		// initializing counter array to get per column breaks
		columnBreakCounts = new ArrayList<>(Arrays.asList(new Integer[fileHeaders.length]));
		Collections.fill(columnBreakCounts, 0);

		// now start the comparison

		// System.out.println(processFile.isUnevenRowsProcess());

		// get pre processed files to compare
		if (processFile.isUnevenRowsProcess()) {
			file1 = tempFilePath + tempFileName.getTempFileName(sourcePath1, "Mapper");
			file2 = tempFilePath + tempFileName.getTempFileName(sourcePath2, "Mapper");

		} else if (sortFlag) {
			file1 = tempFilePath + tempFileName.getTempFileName(sourcePath1, "Sort");
			file2 = tempFilePath + tempFileName.getTempFileName(sourcePath2, "Sort");
		} else {
			file1 = tempFilePath + tempFileName.getTempFileName(sourcePath1, "Filter");
			file2 = tempFilePath + tempFileName.getTempFileName(sourcePath2, "Filter");
		}

		int sortedRowNum = 0;

		// System.out.println(file1);

		try {

			LineIterator file1Iterator = FileUtils.lineIterator(new File(file1));

			LineIterator file2Iterator = FileUtils.lineIterator(new File(file2));

			while (file1Iterator.hasNext() && file2Iterator.hasNext()) {

				String left = file1Iterator.nextLine();
				String right = file2Iterator.nextLine();

				sortedRowNum++;

				String[][] compareResult = compareLine(left, right, sortedRowNum);

				for (int i = 0; i < compareResult.length; i++) {
					List<String> list = Arrays.asList(compareResult[i]);
					System.out.println(list);
				}

				boolean lineMismatch = false;

				for (int i = 1; i < compareResult[2].length - 1; i++) {
					if (compareResult[2][i].equals("F")) {
						lineMismatch = true;
						columnBreakCounts.set(i - 1, columnBreakCounts.get(i - 1) + 1);
					}
				}

				// System.out.println(lineMismatch);

				if (hideMatchedRowsFlag) {

					if (lineMismatch)
						excelRender.addNewLine(compareResult, sortedRowNum);

				} else {
					excelRender.addNewLine(compareResult, sortedRowNum);
				}

			}

		} catch (IOException e) {
			System.out.println(e);
		}

		excelRender.createSummaryGrid(fileHeaders, columnBreakCounts);

		excelRender.generateOutput();

	}

	private String[][] compareLine(String left, String right, int sortedLineNum) {

		// 1, 2, 3, 4
		// 1, 3, 4, 4

		String[] leftArr = left.split(Pattern.quote(delimiter), -1);

		String[] rightArr = right.split(Pattern.quote(delimiter), -1);

		int compArrLength = leftArr.length >= rightArr.length ? leftArr.length : rightArr.length;

		String[][] compArr = new String[3][compArrLength + 1];

		compArr[0][0] = String.valueOf(sortedLineNum);
		compArr[1][0] = String.valueOf(sortedLineNum);

		// get and remove row number from left and right array
		if (sortFlag) {

			compArr[0][0] = String.valueOf(leftArr[leftArr.length - 1]);
			compArr[1][0] = String.valueOf(rightArr[rightArr.length - 1]);

		}

		Iterator<String> leftIterator = Arrays.stream(leftArr).iterator();
		Iterator<String> rightIterator = Arrays.stream(rightArr).iterator();

		// System.out.println("left rowNum: " + leftRowNum + ", left: " + left + "right
		// rowNum: " + rightRowNum
		// + ", right: " + right);

		int compArrIndex = 1;

		while (leftIterator.hasNext() && rightIterator.hasNext()) {

			// both left and right index should be equal in this loop

			String leftElement = leftIterator.next();
			String rightElement = rightIterator.next();

			compArr[0][compArrIndex] = leftElement;
			compArr[1][compArrIndex] = rightElement;

			if (leftElement.equals(rightElement))
				compArr[2][compArrIndex] = "T";
			else
				compArr[2][compArrIndex] = "F";

			compArrIndex++;

		}

		while (leftIterator.hasNext()) {

			String leftElement = leftIterator.next();

			compArr[0][compArrIndex] = leftElement;
			compArr[1][compArrIndex] = "<NA>";
			compArr[2][compArrIndex] = "F";

			compArrIndex++;

		}

		while (rightIterator.hasNext()) {

			String rightElement = rightIterator.next();

			compArr[0][compArrIndex] = "<NA>";
			compArr[1][compArrIndex] = rightElement;
			compArr[2][compArrIndex] = "F";

			compArrIndex++;

		}

		// 1 - prashant - 70 <-- 0 index
		// 2 - prabhat - 80 <-- 1 index
		// String [] []

		return compArr;

	}

}
