package ru.btc.utils.excel2csv;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XLSXReader;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Vova
 * Date: 28.01.14
 * Time: 0:48
 */
public class Main2 {

    private static DataBaseWorker dataBaseWorker;


    public static void main(String[] args) throws IOException, SQLException {
        int argNum = 0;
        String fileName = args[argNum++];
        String sheetName = args[argNum++];
        final String skipFistLine = args[argNum++];
        final String dbTableName = args[argNum++];
        final String dbFields = args[argNum++];
        final String dbValuesFrom = args[argNum++];
        String dbURI = args[argNum++];
        String dbUser = args[argNum++];
        String dbPass = args[argNum++];
        final int rowsToProcess = Integer.parseInt(args[argNum++]);
        System.out.println("��������� �������:");
        for (String arg : args) {
            System.out.print("\"" + arg + "\" ");
        }

        dataBaseWorker = new DataBaseWorker(dbURI, dbUser, dbPass, 1000);

        try {

            Date started = new Date();
            System.out.println("\n�������� �����: \"" + fileName + "\"..");


            XLSXReader.main(fileName, sheetName, new XLSXReader.RowHandlller() {
                private int lineNum = 0;
                Pattern pattern = Pattern.compile("#(\\w+)");

                public void processRow(HashMap<String, String> rowData) {
                    lineNum++;
                    if(rowsToProcess > 0 && lineNum > rowsToProcess) {
                        throw new RuntimeException("STOPPED");
                    }
                    if(lineNum==1 && (skipFistLine.equalsIgnoreCase("Y") || skipFistLine.equalsIgnoreCase("Yes"))) {
                        return;
                    }
                    String sqlValues = dbValuesFrom;
                    Matcher m = pattern.matcher(dbValuesFrom);
                    while (m.find()) {
                        String ref = m.group(1);
                        String celllData = rowData.get(ref);
                        if(celllData==null) {
                            celllData = "";
                        }
                        celllData = celllData.replaceAll("'", "''");
                        sqlValues = sqlValues.replaceFirst(m.group(), celllData);
                    }

                    String sql = "INSERT INTO " + dbTableName + "(" + dbFields + ") VALUES(" + sqlValues + ")";
                    //System.out.println(sqlValues);
                    try {
                        dataBaseWorker.execSqlInBatch(sql);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                }
            });
            dataBaseWorker.commitAllSkipErrors();
            Date finished = new Date();
            System.out.println("Started: " + started + "\nFinished: " + finished + "\nSeconds in processs: " + (finished.getTime()-started.getTime())/1000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataBaseWorker.close();
        }

    }
}
