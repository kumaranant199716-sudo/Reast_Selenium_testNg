package com.automation.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class ExcelUtils {
    private static final String TEST_DATA_PATH = "src/test/resources/testdata/";
    
    public static Object[][] getTestData(String fileName, String sheetName) {
        List<Map<String, String>> dataList = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(TEST_DATA_PATH + fileName);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheet(sheetName);
            Row headerRow = sheet.getRow(0);
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row currentRow = sheet.getRow(i);
                Map<String, String> rowData = new HashMap<>();
                
                for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                    String key = headerRow.getCell(j).getStringCellValue();
                    String value = "";
                    
                    if (currentRow.getCell(j) != null) {
                        currentRow.getCell(j).setCellType(CellType.STRING);
                        value = currentRow.getCell(j).getStringCellValue();
                    }
                    
                    rowData.put(key, value);
                }
                dataList.add(rowData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Convert List<Map> to Object[][]
        Object[][] result = new Object[dataList.size()][1];
        for (int i = 0; i < dataList.size(); i++) {
            result[i][0] = dataList.get(i);
        }
        
        return result;
    }
    
    public static void writeToExcel(String fileName, String sheetName, String[] headers, List<Map<String, String>> data) {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(TEST_DATA_PATH + fileName)) {
            
            Sheet sheet = workbook.createSheet(sheetName);
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            // Create data rows
            int rowNum = 1;
            for (Map<String, String> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;
                for (String header : headers) {
                    row.createCell(colNum++).setCellValue(rowData.get(header));
                }
            }
            
            workbook.write(fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
