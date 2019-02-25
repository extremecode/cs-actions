package io.cloudslang.content.excel.services;

import io.cloudslang.content.excel.entities.*;
import io.cloudslang.content.utils.OutputUtilities;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static io.cloudslang.content.excel.utils.Constants.BAD_CREATE_EXCEL_FILE_MSG;
import static io.cloudslang.content.excel.utils.Constants.BAD_EXCEL_FILE_MSG;
import static io.cloudslang.content.excel.utils.Constants.EXCEPTION_WORKSHEET_NAME_EMPTY;
import static io.cloudslang.content.excel.utils.Constants.FORMAT_XLS;
import static io.cloudslang.content.excel.utils.Constants.FORMAT_XLSM;
import static io.cloudslang.content.excel.utils.Constants.FORMAT_XLSX;
import static io.cloudslang.content.excel.utils.Constants.ROW_DATA_REQD_MSG;
import static io.cloudslang.content.excel.utils.Constants.YES;
import static io.cloudslang.content.excel.utils.Outputs.GetCellOutputs.COLUMNS_COUNT;
import static io.cloudslang.content.excel.utils.Outputs.GetCellOutputs.HEADER;
import static io.cloudslang.content.excel.utils.Outputs.GetRowIndexByCondition.ROWS_COUNT;
import static io.cloudslang.content.utils.OutputUtilities.getFailureResultsMap;
import static io.cloudslang.content.utils.OutputUtilities.getSuccessResultsMap;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

/**
 * Created by danielmanciu on 18.02.2019.
 */
public class ExcelServiceImpl {
    private static boolean incompleted;
    private static String inputFormat = null;

    public static Map<String, String> addExcelData(AddExcelDataInputs addExcelDataInputs) {
        boolean hasHeaderData = false;
        try {
            final String excelFileName = addExcelDataInputs.getCommonInputs().getExcelFileName();
            final String format = getFileFormat(excelFileName);
            final Workbook excelDoc;

            if (isValidExcelFormat(format)) {
                excelDoc = getWorkbook(excelFileName);
            } else {
                return getFailureResultsMap(BAD_EXCEL_FILE_MSG);
            }
            int rowsAdded;
            if (excelDoc == null) {
                return getFailureResultsMap("Could not open " + excelFileName);
            }
            final String sheetName = addExcelDataInputs.getCommonInputs().getWorksheetName();
            final Sheet worksheet = excelDoc.getSheet(sheetName);
            if (worksheet == null) {
                return getFailureResultsMap("Worksheet " + sheetName + " does not exist.");
            }
            String columnDelimiter = addExcelDataInputs.getColumnDelimiter();
            String rowDelimiter = addExcelDataInputs.getRowDelimiter();
            final String[] specialChar = {"\\", "?", "|", "*", "$", ".", "+", "(", ")", "{", "}", "[", "]"};
            for (int i = 0; i < specialChar.length; i++) {
                rowDelimiter = rowDelimiter.replace(specialChar[i], "\\" + specialChar[i]);
                columnDelimiter = columnDelimiter.replace(specialChar[i], "\\" + specialChar[i]);
            }
            final String headerData = addExcelDataInputs.getHeaderData();
            if (!StringUtils.isBlank(headerData)) {
                hasHeaderData = true;
                setHeaderRow(worksheet, headerData, columnDelimiter);
            }

            final String rowData = addExcelDataInputs.getRowData();
            final String rowIndex = addExcelDataInputs.getRowIndex();
            final String columnIndex = addExcelDataInputs.getColumnIndex();
            final String overwriteString = addExcelDataInputs.getOverwriteData();
            final List<Integer> rowIndexList = processIndex(rowIndex, worksheet, rowData, rowDelimiter, columnDelimiter, true, hasHeaderData);
            final List<Integer> columnIndexList = processIndex(columnIndex, worksheet, rowData, rowDelimiter, columnDelimiter, false, hasHeaderData);
            final boolean overwrite = Boolean.valueOf(overwriteString.toLowerCase());

            if (!overwrite)
                shiftRows(worksheet, rowIndexList);

            if (StringUtils.isBlank(rowData)) {
                return getFailureResultsMap(ROW_DATA_REQD_MSG);
            } else {
                rowsAdded = setDataRows(worksheet, rowData, rowDelimiter, columnDelimiter, rowIndexList, columnIndexList);
            }

            updateWorkbook(excelDoc, excelFileName);
            return getSuccessResultsMap(String.valueOf(rowsAdded));

        } catch (Exception e) {
            return getFailureResultsMap(e.getMessage());
        }
    }

    @NotNull
    public static Map<String, String> modifyCell(@NotNull final ModifyCellInputs modifyCellInputs) {
        try {
            final String excelFileName = modifyCellInputs.getCommonInputs().getExcelFileName();
            final Workbook excelDoc = getExcelDoc(excelFileName);
            final Sheet worksheet = getWorksheet(excelDoc, modifyCellInputs.getCommonInputs().getWorksheetName());

            final int firstRowIndex = worksheet.getFirstRowNum();
            final int lastRowIndex = worksheet.getLastRowNum();
            final int firstColumnIndex = 0;
            final int lastColumnIndex = getLastColumnIndex(worksheet, firstRowIndex, lastRowIndex);
            final String columnDelimiter = modifyCellInputs.getColumnDelimiter();
            final String newValue = modifyCellInputs.getNewValue();

            final String rowIndexDefault = firstRowIndex + ":" + lastRowIndex;
            final String columnIndexDefault = firstColumnIndex + ":" + lastColumnIndex;
            final String rowIndex = defaultIfEmpty(modifyCellInputs.getRowIndex(), rowIndexDefault);
            final String columnIndex = defaultIfEmpty(modifyCellInputs.getColumnIndex(), columnIndexDefault);

            final List<Integer> rowIndexList = validateIndex(processIndex(rowIndex), firstRowIndex, lastRowIndex, true);
            final List<Integer> columnIndexList = validateIndex(processIndex(columnIndex), firstColumnIndex, lastColumnIndex, false);

            final List<String> dataList = getDataList(newValue, columnIndexList, columnDelimiter);

            incompleted = false;
            final int modifyCellDataResult = modifyCellData(worksheet, rowIndexList, columnIndexList, dataList);

            if (modifyCellDataResult != 0) {
                //update formula cells
                final FormulaEvaluator evaluator = excelDoc.getCreationHelper().createFormulaEvaluator();
                for (Row row : worksheet) {
                    for (Cell cell : row) {
                        if (cell.getCellType() == CellType.FORMULA) {
                            evaluator.evaluateFormulaCell(cell);
                        }
                    }
                }
                updateWorkbook(excelDoc, excelFileName);
            }

            if (modifyCellDataResult == rowIndexList.size() && !incompleted) {
                return getSuccessResultsMap(String.valueOf(modifyCellDataResult));
            } else {
                return getFailureResultsMap(String.valueOf(modifyCellDataResult));
            }
        } catch (Exception e) {
            return getFailureResultsMap(e.getMessage());
        }
    }

    public static Map<String, String> deleteCell(@NotNull final DeleteCellInputs deleteCellInputs) {
        try {
            final String excelFileName = deleteCellInputs.getCommonInputs().getExcelFileName();
            final Workbook excelDoc = getExcelDoc(excelFileName);
            final Sheet worksheet = getWorksheet(excelDoc, deleteCellInputs.getCommonInputs().getWorksheetName());

            final int firstRowIndex = worksheet.getFirstRowNum();
            final int firstColumnIndex = 0;
            final int lastRowIndex = worksheet.getLastRowNum();
            final int lastColumnIndex = getLastColumnIndex(worksheet, firstRowIndex, lastRowIndex);

            final String rowIndexDefault = firstRowIndex + ":" + lastRowIndex;
            final String columnIndexDefault = firstColumnIndex + ":" + lastColumnIndex;
            final String rowIndex = defaultIfEmpty(deleteCellInputs.getRowIndex(), rowIndexDefault);
            final String columnIndex = defaultIfEmpty(deleteCellInputs.getColumnIndex(), columnIndexDefault);

            final List<Integer> rowIndexList = validateIndex(processIndex(rowIndex), firstRowIndex, lastRowIndex, true);
            final List<Integer> columnIndexList = validateIndex(processIndex(columnIndex), firstColumnIndex, lastColumnIndex, false);

            if (rowIndexList.size() != 0 && columnIndexList.size() != 0) {
                final int deleteCellResult = deleteCell(worksheet, rowIndexList, columnIndexList);
                //update formula cells
                final FormulaEvaluator evaluator = excelDoc.getCreationHelper().createFormulaEvaluator();
                for (Row r : worksheet) {
                    for (Cell c : r) {
                        if (c.getCellType() == CellType.FORMULA) {
                            evaluator.evaluateFormulaCell(c);
                        }
                    }
                }
                updateWorkbook(excelDoc, excelFileName);
                return getSuccessResultsMap(String.valueOf(deleteCellResult));

            } else {
                return getSuccessResultsMap("0");
            }
        } catch (Exception e) {
            return getFailureResultsMap(e.getMessage());
        }
    }

    public static Map<String, String> newExcelDocument(@NotNull final NewExcelDocumentInputs newExcelDocumentInputs) {
        final FileOutputStream output;
        final String excelFileName = newExcelDocumentInputs.getExcelFileName();
        final Workbook excelDoc;
        try {
            excelDoc = getNewExcelDocument(excelFileName);
        } catch (Exception exception) {
            return OutputUtilities.getFailureResultsMap(BAD_CREATE_EXCEL_FILE_MSG);
        }

        final String sheetnameDelimiter = newExcelDocumentInputs.getDelimiter();
        String sheetNames = newExcelDocumentInputs.getWorksheetNames();

        if (sheetNames.isEmpty()) {
            sheetNames = "Sheet1" + sheetnameDelimiter + "Sheet2" + sheetnameDelimiter + "Sheet3";
        }

        final StringTokenizer tokenizer = new StringTokenizer(sheetNames, sheetnameDelimiter);
        if (tokenizer.countTokens() == 0) {
            return OutputUtilities.getFailureResultsMap(EXCEPTION_WORKSHEET_NAME_EMPTY);
        }
        try {
            while (tokenizer.hasMoreTokens()) {
                String sheetName = tokenizer.nextToken();
                excelDoc.createSheet(sheetName);
            }
        } catch (Exception exception) {
            return OutputUtilities.getFailureResultsMap(BAD_CREATE_EXCEL_FILE_MSG);
        }

        try {
            output = new FileOutputStream(excelFileName);
            excelDoc.write(output);
            output.close();
        } catch (Exception exception) {
            return getFailureResultsMap(exception.getMessage());
        }
        return OutputUtilities.getSuccessResultsMap(excelFileName + " created successfully");
    }

    @NotNull
    public static Map<String, String> getCell(@NotNull final GetCellInputs getCellInputs) {
        try {
            final Workbook excelDoc = getExcelDoc(getCellInputs.getCommonInputs().getExcelFileName());
            final Sheet worksheet = getWorksheet(excelDoc, getCellInputs.getCommonInputs().getWorksheetName());

            int firstRowIndex = Integer.parseInt(getCellInputs.getFirstRowIndex());
            final int lastRowIndex = worksheet.getLastRowNum();
            final int firstColumnIndex = 0;
            final int lastColumnIndex = getLastColumnIndex(worksheet, firstRowIndex, lastRowIndex);
            final String rowDelimiter = getCellInputs.getRowDelimiter();
            final String columnDelimiter = getCellInputs.getColumnDelimiter();
            final String hasHeader = getCellInputs.getHasHeader();

            if (hasHeader.equals(YES))
                firstRowIndex++;

            final String rowIndexDefault = firstRowIndex + ":" + lastRowIndex;
            final String columnIndexDefault = firstColumnIndex + ":" + lastColumnIndex;
            final String rowIndex = defaultIfEmpty(getCellInputs.getRowIndex(), rowIndexDefault);
            final String columnIndex = defaultIfEmpty(getCellInputs.getColumnIndex(), columnIndexDefault);

            final List<Integer> rowIndexList = validateIndex(processIndex(rowIndex), firstRowIndex, lastRowIndex, true);
            final List<Integer> columnIndexList = validateIndex(processIndex(columnIndex), firstColumnIndex, lastColumnIndex, false);

            final String resultString = getCellFromWorksheet(excelDoc, worksheet, columnIndexList, rowIndexList, rowDelimiter, columnDelimiter);
            final Map<String, String> results = getSuccessResultsMap(resultString);

            if (hasHeader.equals(YES)) {
                final String headerString = getHeader(worksheet, firstRowIndex, columnIndexList, columnDelimiter);
                results.put(HEADER, headerString);
            }

            results.put(ROWS_COUNT, String.valueOf(rowIndexList.size()));
            results.put(COLUMNS_COUNT, String.valueOf(columnIndexList.size()));

            return results;
        } catch (Exception e) {
            return getFailureResultsMap(e.getMessage());
        }
    }

    @NotNull
    public static Map<String, String> getRowIndexbyCondition(@NotNull final GetRowIndexByConditionInputs getRowIndexbyConditionInputs) {
        final Map<String, String> result;
        final Sheet worksheet;
        final Workbook excelDoc;

        try {
            excelDoc = getExcelDoc(getRowIndexbyConditionInputs.getCommonInputs().getExcelFileName());
            worksheet = getWorksheet(excelDoc, getRowIndexbyConditionInputs.getCommonInputs().getWorksheetName());
        } catch (Exception e) {
            return getFailureResultsMap(e.getMessage());
        }

        int firstRowIndex = Integer.parseInt(getRowIndexbyConditionInputs.getFirstRowIndex());

        if (getRowIndexbyConditionInputs.getHasHeader().equalsIgnoreCase("yes")) {
            firstRowIndex++;
        }
        int columnIndexInt = Integer.parseInt(getRowIndexbyConditionInputs.getColumnIndexToQuery());
        String value = getRowIndexbyConditionInputs.getValue();
        String operator = getRowIndexbyConditionInputs.getOperator();

        getMergedCell(worksheet, firstRowIndex, columnIndexInt);
        processFormulaColumn(excelDoc, worksheet, firstRowIndex, columnIndexInt);

        final String resultString;
        final int rowsCount;
        try {
            resultString = getRowIndex(worksheet, firstRowIndex, value, columnIndexInt, operator);
            rowsCount = resultString.split(",").length;
        } catch (Exception e) {
            return getFailureResultsMap(e.getMessage());
        }
        if (!StringUtils.isBlank(resultString)) {
            result = getSuccessResultsMap(resultString);
            result.put(ROWS_COUNT, String.valueOf(rowsCount));

        } else {
            result = getSuccessResultsMap("");
            result.put(ROWS_COUNT, String.valueOf(0));
        }
        return result;
    }

    private static String getCellFromWorksheet(final Workbook excelDoc,
                                               final Sheet worksheet,
                                               final List<Integer> columnIndex,
                                               final List<Integer> rowIndex,
                                               final String rowDelimiter,
                                               final String columnDelimiter) {
        StringBuilder result = new StringBuilder();
        final DataFormatter formatter = new DataFormatter();

        for (int rIndex : rowIndex) {
            Row row = worksheet.getRow(rIndex);
            if (row == null) {
                row = worksheet.createRow(rIndex);
            }
            if (row != null) {
                for (int cIndex : columnIndex) {
                    Cell cell = row.getCell(cIndex);

                    if (cell == null) {
                        cell = row.createCell(cIndex);
                    }

                    String cellString = formatter.formatCellValue(cell);
                    FormulaEvaluator evaluator = excelDoc.getCreationHelper().createFormulaEvaluator();
                    if (cell != null) {
                        //fraction
                        if (cellString.indexOf("?/?") > 1 && cell.getCellType() == CellType.NUMERIC) {
                            result.append(cell.getNumericCellValue());
                        }

                        //Formula
                        else if (cell.getCellType() == CellType.FORMULA) {
                            CellValue cellValue = evaluator.evaluate(cell);
                            switch (cellValue.getCellType()) {
                                case BOOLEAN:
                                    result.append(cellValue.getBooleanValue());
                                    break;
                                case NUMERIC:
                                    result.append(cellValue.getNumberValue());
                                    break;
                                case STRING:
                                    result.append(cellValue.getStringValue());
                                    break;
                                case BLANK:
                                    break;
                                case ERROR:
                                    break;

                                // CellType.FORMULA will never happen
                                case FORMULA:
                                    break;
                            }

                        }
                        //string
                        else {
                            //Fix for QCIM1D248808
                            if (!cell.toString().isEmpty() && isNumericCell(cell)) {
                                double aCellValue = cell.getNumericCellValue();
                                cellString = round(Double.toString(aCellValue));
                            }
                            result.append(cellString);
                        }

                    }
                    result.append(columnDelimiter);
                }
                //get rid of last column delimiter
                int index = result.lastIndexOf(columnDelimiter);
                if (index > -1)
                    result = new StringBuilder(result.substring(0, index));
            }

            result.append(rowDelimiter);
        }

        int index = result.lastIndexOf(rowDelimiter);
        if (index > -1)
            result = new StringBuilder(result.substring(0, index));

        return result.toString();
    }

    private static int modifyCellData(final Sheet worksheet,
                                      final List<Integer> rowIndexList,
                                      final List<Integer> columnIndexList,
                                      final List<String> dataList) {
        int rowCount = 0;

        for (Integer rowIndex : rowIndexList) {
            boolean isModified = false;
            int i = 0;
            Row row = worksheet.getRow(rowIndex);
            //if the specified row does not exist
            if (row == null) {
                row = worksheet.createRow(rowIndex);

            }
            for (Integer columnIndex : columnIndexList) {
                Cell cell = row.getCell(columnIndex);
                //if the specified cell does not exist
                if (cell == null) {
                    cell = row.createCell(columnIndex);
                }
                //the cell is a merged cell, cannot modify it
                if (isMergedCell(worksheet, rowIndex, columnIndex)) {
                    i++;
                    incompleted = true;
                } else {
                    //if the cell needs to be modified is in formula type,
                    if (cell.getCellType() == CellType.FORMULA) {
                        cell.setCellType(CellType.STRING);
                    }
                    try {
                        double valueNumeric = Double.parseDouble(dataList.get(i).trim());
                        cell.setCellValue(valueNumeric);
                    }
                    //for non-numeric value
                    catch (Exception e) {
                        try {
                            Date date = new Date(dataList.get(i).trim());
                            cell.setCellValue(date);
                        } catch (Exception e1) {
                            cell.setCellValue(dataList.get(i).trim());
                        }
                    }
                    i++;
                    isModified = true;
                }
            }
            if (isModified) rowCount++;
        }

        return rowCount;
    }

    public static boolean isMergedCell(final Sheet worksheet, final int rowIndex, final int columnIndex) {
        int countMRegion = worksheet.getNumMergedRegions();

        for (int i = 0; i < countMRegion; i++) {
            CellRangeAddress range = worksheet.getMergedRegion(i);
            int firstRow = range.getFirstRow();
            int firstColumn = range.getFirstColumn();

            boolean isInRange = range.isInRange(rowIndex, columnIndex);

            if (isInRange) {
                if (!(rowIndex == firstRow && columnIndex == firstColumn && isInRange)) {
                    return true;
                }
            }

        }
        return false;
    }

    private static List<String> getDataList(final String newValue, final List<Integer> columnIndexList, final String columnDelimiter) throws Exception {
        final List<String> dataList = Arrays.asList(newValue.split(columnDelimiter));

        if (dataList.size() != columnIndexList.size()) {
            throw new ExcelOperationException("The data input is not valid. " +
                    "The size of data input should be the same as size of columnIndex input, which is " + columnIndexList.size() + ".");
        }

        return dataList;
    }


    /**
     * retrieves data from header row
     *
     * @param worksheet    an Excel worksheet
     * @param columnIndex  a list of column indexes
     * @param colDelimiter a column delimiter
     * @return a string of delimited header data
     */
    private static String getHeader(final Sheet worksheet,
                                    final int firstRowIndex,
                                    final List<Integer> columnIndex,
                                    final String colDelimiter) {
        StringBuilder result = new StringBuilder();
        int headerIndex = firstRowIndex - 1;
        final Row headerRow = worksheet.getRow(headerIndex);

        for (int cIndex : columnIndex) {
            final Cell cell = headerRow.getCell(cIndex);
            if (cell != null) {
                String cellString = headerRow.getCell(cIndex).toString();
                result.append(cellString);
            }
            result.append(colDelimiter);
        }

        //get rid of last column index
        final int index = result.lastIndexOf(colDelimiter);
        if (index > -1)
            result = new StringBuilder(result.substring(0, index));

        return result.toString();
    }

    private static boolean isNumericCell(final Cell cell) {
        try {
            cell.getNumericCellValue();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private static String round(final String value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return bd.toString();
    }

    private static boolean isValidExcelFormat(final String format) throws Exception {
        if (StringUtils.isBlank(format)) {
            throw new ExcelOperationException(BAD_EXCEL_FILE_MSG);
        } else
            return (format.equalsIgnoreCase(FORMAT_XLSX)
                    || format.equalsIgnoreCase(FORMAT_XLS)
                    || format.equalsIgnoreCase(FORMAT_XLSM));

    }

    private static Workbook getWorkbook(final String fileName) throws Exception {
        final String format = getFileFormat(fileName);
        if (!isValidExcelFormat(format)) {
            throw new InvalidFormatException(BAD_EXCEL_FILE_MSG);
        }

        FileInputStream input = null;
        final Workbook excelDoc;

        try {
            input = new FileInputStream(fileName);
            excelDoc = WorkbookFactory.create(input);
            input.close();
        } catch (IOException e) {
            throw e;
        } finally {
            if (input != null) {
                input.close();
            }
        }

        return excelDoc;
    }

    private static String getRowIndex(final Sheet worksheet,
                                      final int firstRow,
                                      final String input,
                                      final int columnIndex,
                                      final String operator) {
        String result = "";
        double cellValueNumeric;
        String cellFormat;

        double inputNumeric = processValueInput(input);

        for (int i = firstRow; i <= worksheet.getLastRowNum(); i++) {
            Row row = worksheet.getRow(i);
            if (row == null) {
                row = worksheet.createRow(i);
            }
            if (row != null) {
                Cell cell = row.getCell(columnIndex);
                if (cell == null) {
                    cell = row.createCell(columnIndex);
                }
                if (cell != null) {
                    CellType cellType = cell.getCellType();
                    if (cellType != CellType.ERROR) {
                        cellFormat = getCellType(cell);
                        //string comparison
                        if (cellFormat.equalsIgnoreCase("string") && inputFormat.equalsIgnoreCase("string")) {
                            DataFormatter aFormatter = new DataFormatter();
                            String aCellString = aFormatter.formatCellValue(cell);
                            if (compareStringValue(aCellString, input, operator)) {
                                result += i + ",";
                            }
                        }
                        //value input is empty, and the cell in the worksheet is in numeric type
                        else if (!cellFormat.equalsIgnoreCase(inputFormat))
                        //((cellType != CellType.STRING && inputFormat.equalsIgnoreCase("string"))||
                        //(cellType != CellType.NUMERIC && !inputFormat.equalsIgnoreCase("string")))
                        {
                            if (operator.equals("!=")) {
                                result += i + ",";
                            }
                        }

                        //numeric comparison
                        else if (cellType == CellType.NUMERIC && !inputFormat.equalsIgnoreCase("string")) {
                            cellValueNumeric = cell.getNumericCellValue();
                            //both are date or time
                            if ((cellFormat.equalsIgnoreCase("date") && inputFormat.equalsIgnoreCase("date")) ||
                                    (cellFormat.equalsIgnoreCase("time") && inputFormat.equalsIgnoreCase("time")) ||
                                    (cellFormat.equalsIgnoreCase("num") && inputFormat.equalsIgnoreCase("num"))) {
                                if (compareNumericValue(cellValueNumeric, inputNumeric, operator)) {
                                    result += i + ",";
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!result.isEmpty()) {
            final int index = result.lastIndexOf(',');
            result = result.substring(0, index);
        }

        return result;
    }

    private static double processValueInput(final String input) {
        double result = 0;

        //check if the input is in number format
        try {
            result = Double.parseDouble(input);
            inputFormat = "num";
        } catch (Exception e) {
        }

        //check if the input is in a percentage format
        if (StringUtils.isBlank(inputFormat)) {
            try {
                int pIndex = input.indexOf("%");
                if (pIndex > -1) {
                    result = percentToDouble(input);
                    inputFormat = "num";
                }
            } catch (Exception e) {

            }
        }

        //check if the input is in a date format(YYYY/MM/DD)
        if (StringUtils.isBlank(inputFormat)) {
            try {
                final Date date = DateUtil.parseYYYYMMDDDate(input);
                result = DateUtil.getExcelDate(date);
                inputFormat = "date";
            } catch (Exception e) {
            }
        }

        //check if the input is in a time format (HH:MM:SS)
        if (StringUtils.isBlank(inputFormat)) {
            try {
                result = DateUtil.convertTime(input);
                inputFormat = "time";
            } catch (Exception e) {
            }
        }
        //check if the input is in a datetime format(YYYY/MM/DD HH:MM:SS)
        if (StringUtils.isBlank(inputFormat)) {
            String[] temp = input.split(" ");
            if (temp.length == 2) {
                try {
                    final String dateString = temp[0];
                    final String timeString = temp[1];
                    final Date date = DateUtil.parseYYYYMMDDDate(dateString);
                    final Double dateDouble = DateUtil.getExcelDate(date);
                    final Double time = DateUtil.convertTime(timeString);
                    result = dateDouble + time;
                    inputFormat = "date";
                } catch (Exception e) {
                }
            }
        }

        if (StringUtils.isBlank(inputFormat)) {
            inputFormat = "string";
        }

        return result;
    }

    public static void updateWorkbook(final Workbook workbook, final String fileName) throws IOException {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(fileName);
            workbook.write(output);
            output.close();
        } catch (IOException e) {
            throw e;
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private static Workbook getNewExcelDocument(final String excelFileName) throws Exception {
        final String format = getFileFormat(excelFileName);

        if (StringUtils.isBlank(format)) {
            throw new Exception(BAD_CREATE_EXCEL_FILE_MSG);
        }

        //QCCR139188 need to check if user inputs .xlsx, empty file name
        final int index = excelFileName.lastIndexOf(".");
        final int indexFSlash = excelFileName.lastIndexOf("/");
        final int indexBSlash = excelFileName.lastIndexOf("\\");
        final String fileName;

        //excelFileName contains folder path
        if (indexFSlash > -1) {
            fileName = excelFileName.substring(indexFSlash + 1, index);
        } else if (indexBSlash > -1) {
            fileName = excelFileName.substring(indexBSlash + 1, index);
        }
        //excelFileName doesn't contain folder path
        else {
            fileName = excelFileName.substring(0, index);
        }

        if (StringUtils.isBlank(fileName)) {
            throw new Exception("Excel file name cannot be empty.");
        }

        //if excelFileName doesn't contain '.'
        if (StringUtils.isBlank(format)) {
            throw new ExcelOperationException(BAD_EXCEL_FILE_MSG);
        } else {
            if (format.equalsIgnoreCase(FORMAT_XLSX))
                return new XSSFWorkbook();
            else if (format.equalsIgnoreCase(FORMAT_XLS))
                return new HSSFWorkbook();
        }
        return null;
    }

    private static String getFileFormat(final String excelFileName) {
        final int index = excelFileName.lastIndexOf(".");
        if (index > -1 && index < excelFileName.length()) {
            return excelFileName.substring(index + 1);
        }

        return null;
    }


    private static Workbook getExcelDoc(final String excelFileName) {
        Workbook excelDoc = null;
        try {
            excelDoc = getWorkbook(excelFileName);
            if (excelDoc == null) {
                throw new ExcelOperationException("Could not open " + excelFileName);
            }
        } catch (Exception e) {

        }
        return excelDoc;
    }

    private static Sheet getWorksheet(final Workbook excelDoc, final String sheetName) throws ExcelOperationException {
        final Sheet worksheet = excelDoc.getSheet(sheetName);
        if (worksheet == null) {
            throw new ExcelOperationException("Worksheet " + sheetName + " does not exist.");
        }
        return worksheet;
    }

    private static double percentToDouble(final String percent) throws Exception {
        final double result;
        final String[] number = percent.split("%");
        if (number.length == 1) {
            result = Double.parseDouble(number[0]) / 100;
        } else {
            throw new NumberFormatException();
        }
        return result;
    }

    public static void getMergedCell(final Sheet sheet, final int firstRowIndex, final int cIndex) {
        final int countMRegion = sheet.getNumMergedRegions();

        for (int i = 0; i < countMRegion; i++) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            final int firstRow = range.getFirstRow();
            final int firstColumn = range.getFirstColumn();

            for (int j = firstRowIndex; j < sheet.getLastRowNum(); j++) {
                final boolean isInRange = range.isInRange(j, cIndex);

                Row row = sheet.getRow(j);
                if (row == null) {
                    row = sheet.createRow(j);
                }
                Cell cell = row.getCell(cIndex);
                if (cell == null) {
                    cell = row.createCell(cIndex);
                }
                if (isInRange)
                    if (!(j == firstRow && cIndex == firstColumn)) {
                        cell.setCellType(CellType.ERROR);
                    }
            }
        }
    }

    private static void processFormulaColumn(final Workbook excelDoc,
                                             final Sheet worksheet,
                                             final int firstRow,
                                             final int columnIndex) {

        final FormulaEvaluator evaluator = excelDoc.getCreationHelper().createFormulaEvaluator();
        for (int i = firstRow; i <= worksheet.getLastRowNum(); i++) {
            final Row row = worksheet.getRow(i);
            if (row != null) {
                final Cell cell = row.getCell(columnIndex);
                if (cell != null && (cell.getCellType() != CellType.BLANK)) {
                    //formula type
                    if (cell.getCellType() == CellType.FORMULA) {
                        CellValue cellValue = evaluator.evaluate(cell);

                        switch (cellValue.getCellType()) {
                            case BOOLEAN:
                                cell.setCellType(CellType.STRING);
                                break;
                            case NUMERIC:
                                cell.setCellType(CellType.NUMERIC);
                                break;
                            case STRING:
                                if (StringUtils.isBlank(cell.getStringCellValue())) {
                                    cell.setCellType(CellType.BLANK);
                                } else {
                                    cell.setCellType(CellType.STRING);
                                }
                                break;
                            case BLANK:

                                break;
                            case ERROR:
                                break;

                            // CELL_TYPE_FORMULA will never happen
                            case FORMULA:
                                break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the type of a cell, it can be num, date, time or string
     *
     * @param cell
     * @return
     */
    private static String getCellType(final Cell cell) {
        final String result;
        final double cellValueNumeric;
        final CellType cellType = cell.getCellType();

        if (cellType == CellType.NUMERIC) {
            cellValueNumeric = cell.getNumericCellValue();

            //date cell, it can be date, time or datetime
            if (DateUtil.isCellDateFormatted(cell)) {
                //time cell
                if (cellValueNumeric < 1) {
                    result = "time";
                }
                //date cell
                else {
                    result = "date";
                }
            }
            //numeric cell
            else {
                result = "num";
            }
        }
        //String cell
        else {
            result = "string";
        }
        return result;
    }

    /**
     * compare the numeric values
     *
     * @param value1
     * @param value2
     * @param operator
     * @return true or false
     * @throws Exception
     */
    private static boolean compareNumericValue(double value1, double value2, final String operator) {
        boolean result = false;
        if (operator.equals("==")) {
            if (value1 == value2) result = true;
        } else if (operator.equals("!=")) {
            if (value1 != value2) result = true;
        } else if (operator.equals("<")) {
            if (value1 < value2) result = true;
        } else if (operator.equals(">")) {
            if (value1 > value2) result = true;
        } else if (operator.equals("<=")) {
            if (value1 <= value2) result = true;
        } else if (operator.equals(">=")) {
            if (value1 >= value2) result = true;
        }

        return result;
    }

    /**
     * compare the string values
     *
     * @param s1
     * @param s2
     * @param operator a math operator
     * @return true or false
     * @throws Exception
     */
    private static boolean compareStringValue(final String s1, final String s2, final String operator) {
        boolean result = false;
        if (operator.equals("==")) {
            if (s1.equals(s2))
                result = true;
        } else if (operator.equals("!=")) {
            if (!s1.equals(s2))
                result = true;
        }

        return result;
    }

    /**
     * get last column index
     */
    private static int getLastColumnIndex(final Sheet worksheet, final int firstRowIndex, final int lastRowIndex) {
        //get the last column index in a sheet
        int tempLastColumnIndex;
        int lastColumnIndex = 0;
        for (int i = firstRowIndex; i <= lastRowIndex; i++) {
            final Row row = worksheet.getRow(i);
            if (row != null) {
                tempLastColumnIndex = row.getLastCellNum() - 1;
                if (tempLastColumnIndex > lastColumnIndex) {
                    lastColumnIndex = tempLastColumnIndex;
                }
            }
        }

        return lastColumnIndex;
    }

    private static List<Integer> processIndex(final String index) {
        final List<Integer> result = new ArrayList<>();

        final String[] temp = index.split(",");
        String[] tempArray;

        for (int i = 0; i < temp.length; i++) {
            temp[i] = temp[i].trim();
            tempArray = temp[i].split(":");

            //not a range index
            if (tempArray.length == 1) {
                int tmp = Integer.parseInt(temp[i]);
                result.add(tmp);

            }
            // range index
            else if (tempArray.length == 2) {
                tempArray[0] = tempArray[0].trim();
                tempArray[1] = tempArray[1].trim();
                final int start = Integer.parseInt(tempArray[0]);
                final int end = Integer.parseInt(tempArray[1]);

                for (int j = start; j <= end; j++) {
                    if (!result.contains(j)) {
                        result.add(j);
                    }
                }

            }
        }

        return result;
    }

    private static List<Integer> validateIndex(final List<Integer> indexList,
                                               final int firstIndex,
                                               final int lastIndex,
                                               final boolean isRow) throws Exception {
        final List<Integer> resultList = new ArrayList<>();
        for (Integer index : indexList) {
            //trim the row or column index if it's above range
            if (index >= firstIndex && index <= lastIndex) {
                resultList.add(index);
            }
            if (index < 0) {
                if (isRow) {
                    throw new ExcelOperationException("The rowIndex input is not valid. " +
                            "The valid row index must be equal or greater than 0.");
                } else {
                    throw new ExcelOperationException("The columnIndex input is not valid. " +
                            "The valid column index must be equal or greater than 0.");
                }

            }

        }

        return resultList;
    }

    public static int deleteCell(final Sheet worksheet, final List<Integer> rowIndex, final List<Integer> columnIndex) {
        int rowsDeleted = 0;

        for (Integer rIndex : rowIndex) {
            Row row = worksheet.getRow(rIndex);

            if (row != null) {
                for (Integer cIndex : columnIndex) {
                    Cell cell = row.getCell(cIndex);
                    if (cell != null) {
                        row.removeCell(cell);
                    }
                }
                rowsDeleted++;
            }
        }

        return rowsDeleted;
    }

    public static void setHeaderRow(final Sheet worksheet, final String headerData, final String delimiter) {
		/*StringTokenizer headerTokens = new StringTokenizer(headerData, delimiter);
		Row headerRow = worksheet.createRow(0);
		int columnIndex = 0;
		while (headerTokens.hasMoreTokens())
		{
			Cell cell = headerRow.createCell(columnIndex);
			cell.setCellValue(headerTokens.nextToken());
			columnIndex++;
		}*/
        final Row headerRow = worksheet.createRow(0);

        final String[] tmp = headerData.split(delimiter);
        for (int i = 0; i < tmp.length; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) {
                cell = headerRow.createCell(i);
            }
            try {
                double valueNumeric = Double.parseDouble(tmp[i].trim());
                cell.setCellValue(valueNumeric);
            }
            //for non-numeric value
            catch (Exception e) {
                cell.setCellValue(tmp[i].trim());
            }
        }
    }

    public static int setDataRows(final Sheet worksheet,
                                  final String rowData,
                                  final String rowDelimiter,
                                  final String columnDelimiter,
                                  final int startRowIndex,
                                  final int startColumnIndex) {
		/*StringTokenizer dataTokens = new StringTokenizer(rowData, rowDelimiter);
		int rowIndex = startRowIndex;

		while (dataTokens.hasMoreTokens())
		{
			int columnIndex = startColumnIndex;
			Row dataRow = worksheet.getRow(rowIndex);
			if (dataRow == null)
				dataRow = worksheet.createRow(rowIndex);

			StringTokenizer rowToken = new StringTokenizer(dataTokens.nextToken(), columnDelimiter);
			while (rowToken.hasMoreTokens())
			{
				Cell cell = dataRow.getCell(columnIndex);
				if (cell == null)
					cell = dataRow.createCell(columnIndex);

				cell.setCellValue(rowToken.nextToken());
				columnIndex++;
			}

			rowIndex++;
		}

		return (rowIndex-startRowIndex);*/
        //QCCR 139182 allow user to enter an empty row in the middle

        String[] tmpRow = rowData.split(rowDelimiter);

        int rowIndex = startRowIndex;

        for (int i = 0; i < tmpRow.length; i++) {
            int columnIndex = startColumnIndex;
            Row row = worksheet.getRow(rowIndex);
            if (row == null) {
                row = worksheet.createRow(rowIndex);
            }

            String[] tmpCol = tmpRow[i].split(columnDelimiter);
            for (int j = 0; j < tmpCol.length; j++) {
                Cell cell = row.getCell(columnIndex);
                if (cell == null) {
                    cell = row.createCell(columnIndex);
                }
                try {
                    double value_num = Double.parseDouble(tmpCol[j].trim());
                    cell.setCellValue(value_num);
                }
                //for non-numeric value
                catch (Exception e) {
                    cell.setCellValue(tmpCol[j].trim());
                }
                columnIndex++;
            }
            rowIndex++;
        }
        return tmpRow.length;
    }


    /**
     * Adds (inserts/appends) specified data to the worksheet.
     *
     * @param worksheet       Worksheet where the rowData will be added
     * @param rowData         Data to be added to the worksheet
     * @param rowDelimiter    Delimiter for rows in rowData
     * @param columnDelimiter Delimiter for column in rowData
     * @param rowIndexList    List of row indexes where data will be added in the worksheet
     * @param columnIndexList List of column indexes where data will be added in the worksheet
     * @return Number of rows that were added to the worksheet
     * @throws Exception Input list sizes doesn't match
     */
    private static int setDataRows(final Sheet worksheet, final String rowData, final String rowDelimiter, final String columnDelimiter,
                                   final List<Integer> rowIndexList, final List<Integer> columnIndexList) {
        final String[] rows = rowData.split(rowDelimiter);
        String[] columns;

        if (rows.length != rowIndexList.size())
            throw new IllegalArgumentException("Row index list size doesn't match rowData row count.");

        for (int i = 0; i < rowIndexList.size(); i++) {
            Row row = worksheet.getRow(rowIndexList.get(i));
            if (row == null) {
                row = worksheet.createRow(rowIndexList.get(i));
            }
            columns = rows[i].split(columnDelimiter);
            if (columns.length != columnIndexList.size())
                throw new IllegalArgumentException("Column index list size doesn't match rowData column count.");
            for (int j = 0; j < columnIndexList.size(); j++) {
                Cell cell = row.getCell(columnIndexList.get(j));
                if (cell == null) {
                    cell = row.createCell(columnIndexList.get(j));
                }
                try {
                    double numberValue = Double.parseDouble(columns[j].trim());
                    cell.setCellValue(numberValue);
                }
                //for non-numeric value
                catch (NumberFormatException e) {
                    cell.setCellValue(columns[j].trim());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid row data");
                }
            }
        }
        return rowIndexList.size();
    }

    /**
     * Constructs a list of indexes where the data will be added in the worksheet
     *
     * @param index           A list of indexes
     * @param worksheet       The worksheet where the data will be added
     * @param rowData         Data that will be added to the worksheet
     * @param rowDelimiter    rowData row delimiter
     * @param columnDelimiter rowData column delimiter
     * @param isRow           true - if the index list (param) contains row indexes
     * @return List of indexes where data will be added in the worksheet
     */
    public static List<Integer> processIndex(final String index, final Sheet worksheet, final String rowData, final String rowDelimiter,
                                             final String columnDelimiter, final boolean isRow, final boolean hasHeader) {
        final String[] rows = rowData.split(rowDelimiter);
        String[] indexArray = null;
        if (!StringUtils.isBlank(index)) {
            indexArray = index.split(",");
        }
        int sheetLastRowIndex = worksheet.getLastRowNum();
        if (sheetLastRowIndex > 0) {
            sheetLastRowIndex++;
        }
        final int dataRows = rows.length;
        final int dataColumns = rows[0].split(columnDelimiter).length;
        int headerOffset = 0;
        if (hasHeader) {
            headerOffset = 1;
        }
        if (isRow) {
            return processIndexWithOffset(indexArray, headerOffset, sheetLastRowIndex, sheetLastRowIndex + dataRows);
        } else {
            return processIndexWithOffset(indexArray, 0, 0, dataColumns);
        }
    }

    /**
     * Processes the index list with an offset
     *
     * @param indexArray List of indexes to be processed
     * @param offset     Index offset (Apache poi works with 0 index based excel files while Microsoft Excel file starts with index 1)
     * @param startIndex Start index for the default case (ex. append for rows)
     * @param endIndex   End index for the default case (ex. append for rows)
     * @return List of indexes where data will be added in the worksheet
     */
    private static List<Integer> processIndexWithOffset(final String[] indexArray, final int offset, final int startIndex, final int endIndex) {
        final List<Integer> indexList = new ArrayList<>();
        String[] range;
        if (indexArray != null) {
            for (String index : indexArray) {
                range = index.split(":");
                // adding every row/column in the range
                if (range.length > 1) {
                    for (int ind = Integer.parseInt(range[0].trim()); ind <= Integer.parseInt(range[1].trim()); ind++) {
                        indexList.add(ind + offset);
                    }
                } else {
                    indexList.add(Integer.parseInt(range[0].trim()) + offset);
                }
            }
        } else {
            // default case
            for (int i = startIndex; i < endIndex; i++) {
                if (startIndex == 0) {
                    indexList.add(i + offset);
                } else {
                    indexList.add(i);
                }
            }
        }
        return indexList;
    }

    /**
     * Inserts rows at the specified indexes in the worksheet
     *
     * @param worksheet    Worksheet where rows will be inserted
     * @param rowIndexList List of row indexes where rows will be inserted
     */
    public static void shiftRows(final Sheet worksheet, final List<Integer> rowIndexList) {
        int insertPoint;
        int nRows;
        int i = 0;
        while (i < rowIndexList.size()) {
            insertPoint = rowIndexList.get(i);
            nRows = 1;
            while (i < rowIndexList.size() - 1 && (insertPoint + nRows == rowIndexList.get(i + 1))) {
                nRows++;
                i++;
            }
            if (insertPoint > worksheet.getLastRowNum()) {
                for (int j = insertPoint; j < insertPoint + nRows; j++) {
                    worksheet.createRow(j);
                }
            } else {
                worksheet.shiftRows(insertPoint, worksheet.getLastRowNum(), nRows, false, true);
            }
            i++;
        }
    }
}
