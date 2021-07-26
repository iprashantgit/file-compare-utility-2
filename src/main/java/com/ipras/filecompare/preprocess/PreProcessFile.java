package com.ipras.filecompare.preprocess;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.poi.util.SystemOutLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.ipras.filecompare.utils.GenerateTempFileName;

public class PreProcessFile {

	@Value("${input.file1.path}")
	private String sourcePath1;

	@Value("${input.file2.path}")
	private String sourcePath2;

	@Value("${input.file.delimiter}")
	private String delimiter;

	@Value("${header.row.count}")
	private int headerCount;

	@Value("${footer.row.count}")
	private int footerCount;

	@Value("${compare.preprocess.sort}")
	private boolean sortFlag;

	@Value("${sort.column.index}")
	private String sortColumns;

	@Value("${compare.preprocess.trim}")
	private Boolean trimSortingFlag;

	@Value("${temp.file.path}")
	private String tempPath;

	@Value("${file.copy.encoding}")
	private String fileEncoding;

	@Value("${temp.file.clean}")
	private Boolean tempCleanFlag;

	@Autowired
	private GenerateTempFileName tempFileName;

	int sourceRowCount;

	private boolean unevenRowsProcess = false;

	public void preProcessFile(String filePath1, String filePath2) throws IOException {

		File f1 = new File(filePath1);
		File f2 = new File(filePath2);

		// Check if the specified file exists or not
		if (!f1.exists())
			throw new IOException(filePath1);
		if (!f2.exists())
			throw new IOException(filePath2);

		String preProcessFile1 = filterAndSortFile(filePath1);
		String preProcessFile2 = filterAndSortFile(filePath2);

		// if rows are uneven, we need to map the rows
		unevenRowsProcess = getRowCount(new File(preProcessFile1).toPath()) != getRowCount(
				new File(preProcessFile2).toPath());

		if (isUnevenRowsProcess())
			mapSortedFiles(preProcessFile1, preProcessFile2);

	}

	private void mapSortedFiles(String file1, String file2) {

		// create copy of the files
		String file1Copy = tempPath + tempFileName.getTempFileName(sourcePath1, "Mapper");
		String file2Copy = tempPath + tempFileName.getTempFileName(sourcePath2, "Mapper");

		createFileCopy(new File(file1).toPath(), file1Copy);
		createFileCopy(new File(file2).toPath(), file2Copy);

		try {
			LineIterator file1Iterator = FileUtils.lineIterator(new File(file1));

			LineIterator file2Iterator = FileUtils.lineIterator(new File(file2));

			int leftIndex = 0;
			int rightIndex = 0;

			while (file1Iterator.hasNext()) {

				String left = file1Iterator.nextLine();
				String right = file2Iterator.nextLine();

				String leftSortKey = getSortKey(left);
				String rightSortKey = getSortKey(right);
				
				leftIndex++;
				rightIndex++;
				

				while (leftSortKey.compareTo(rightSortKey) > 0) {
					
					//System.out.println(leftSortKey +", " + rightSortKey);

					List<String> lines = Files.readAllLines(Paths.get(file1Copy));
					lines.add(leftIndex-1, getEmptyLine());
					Files.write(Paths.get(file1Copy), lines);

					rightIndex++;
					right = file2Iterator.nextLine();
					rightSortKey = getSortKey(right);

				}

				while (leftSortKey.compareTo(rightSortKey) < 0) {
					
					//System.out.println(leftSortKey +", " + rightSortKey);

					List<String> lines = Files.readAllLines(Paths.get(file2Copy));
					lines.add(rightIndex-1, getEmptyLine());
					Files.write(Paths.get(file2Copy), lines);

					leftIndex++;
					left = file1Iterator.nextLine();
					leftSortKey = getSortKey(left);

				}

			}

		} catch (IOException e) {
			System.out.println("Temp file was not found: " + e.getMessage());
		}

	}

	private String getEmptyLine() {

		String emptyLine = "";

		try (Stream<String> lines = Files.lines(Paths.get(sourcePath1))) {

			String str = lines.findFirst().get();

			String[] strArr = str.split(Pattern.quote(delimiter));

			for (int i = 0; i < strArr.length; i++) {

				emptyLine += delimiter;

			}

		} catch (IOException e) {

			System.out.println("Error: Cant not read the file. " + e.getMessage());
		}

		return emptyLine;
	}

	private void createFileCopy(Path path, String copyPath) {

		try (Stream<String> lines = Files.lines(path)) {

			List<String> lineList = lines.collect(Collectors.toList());

			Files.write(new File(copyPath).toPath(), lineList);

		} catch (IOException e) {

			System.out.println("Error: Cant not read the file. " + e.getMessage());
		}

	}

	private String filterAndSortFile(String filePath) {

		String copyPathString = "";

		// get file path for the temp file

		if (sortFlag)
			copyPathString = tempPath + tempFileName.getTempFileName(filePath, "Sort");
		else
			copyPathString = tempPath + tempFileName.getTempFileName(filePath, "Filter");

		Path copyPath = Paths.get(copyPathString);

		// pre process the file
		Charset cs = Charset.forName(fileEncoding);

		Path path = Paths.get(filePath);

		try (Stream<String> lines = Files.lines(path, cs)) {

			// filter header and footers
			List<String> filteredRows = lines.limit(getRowCount(path) - footerCount).skip(headerCount)
					.collect(Collectors.toList());

			Files.write(copyPath, filteredRows);

		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!sortFlag)
			return copyPathString;

		try (Stream<String> lines = Files.lines(path, cs)) {

			sourceRowCount = 1;

			// filter header and footers
			List<String> filteredSortedLines = lines.limit(getRowCount(path) - footerCount).skip(headerCount)
					.map(l -> l.concat(delimiter + counter())).sorted(new Comparator<String>() {
						@Override
						public int compare(String l1, String l2) {

							if (sortColumns.toLowerCase().equals("all")) {
								return l1.compareTo(l2);
							}

							String substringL1 = getSortKey(l1);

							String substringL2 = getSortKey(l2);

							return substringL1.compareTo(substringL2);

						}

					}).collect(Collectors.toList());

			Files.write(copyPath, filteredSortedLines);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return copyPathString;

	}

	public String getSortKey(String line) {

		String[] sortColumnsArray = sortColumns.split(Pattern.quote(","));

		String lineKey = "";

		String[] lineArray = line.split(Pattern.quote(delimiter));

		for (int i = 0; i < sortColumnsArray.length; i++) {

			if (trimSortingFlag)
				lineKey += lineArray[Integer.parseInt(sortColumnsArray[i]) - 1].trim();
			else
				lineKey += lineArray[Integer.parseInt(sortColumnsArray[i]) - 1];

		}

		return lineKey;

	}

	public String[] getFileHeaders(Path path) {

		String[] headers = null;

		try (Stream<String> lines = Files.lines(path)) {

			headers = lines.findFirst().get().split(Pattern.quote(delimiter));

		} catch (IOException e) {

			System.out.println("Error: Cant not read the file. " + e.getMessage());
		}

		return headers;

	}

	private int getRowCount(Path path) {

		int rowCount = 0;

		try (Stream<String> lines = Files.lines(path)) {

			rowCount = (int) lines.count();

		} catch (IOException e) {

			System.out.println("Error: Cant not read the file. " + e.getMessage());
		}

		return rowCount;

	}

	private int counter() {
		return sourceRowCount++;
	}

	public boolean isUnevenRowsProcess() {
		return unevenRowsProcess;
	}

}
