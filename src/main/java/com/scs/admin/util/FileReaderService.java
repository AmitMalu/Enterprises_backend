package com.scs.admin.util;

import com.scs.admin.dto.LocationImportRow;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author SHUBHAM KHOPADE
 */
@Component
public class FileReaderService {

    public List<LocationImportRow> read(
            MultipartFile file
    ) {

        String fileName =
                file.getOriginalFilename();

        if (
                fileName == null
        ) {
            throw new RuntimeException(
                    "Invalid file"
            );
        }

        if (
                fileName.endsWith(".csv")
        ) {

            return readCsv(
                    file
            );

        }

        if (
                fileName.endsWith(".xlsx") ||
                        fileName.endsWith(".xls")
        ) {

            return readExcel(
                    file
            );

        }

        throw new RuntimeException(
                "Only CSV/XLS/XLSX supported"
        );

    }

    private List<LocationImportRow>
    readCsv(
            MultipartFile file
    ) {

        List<LocationImportRow>
                rows =
                new ArrayList<>();

        try (

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        file.getInputStream()
                                )
                        );

                CSVParser parser =
                        CSVFormat.DEFAULT
                                .builder()
                                .setHeader()
                                .setSkipHeaderRecord(true)
                                .build()
                                .parse(reader)

        ) {

            for (
                    CSVRecord record
                    : parser
            ) {

                LocationImportRow row =
                        new LocationImportRow();

                row.setPinCode(
                        get(record,"Pincode")
                );

                row.setCity(
                        get(record,"City")
                );

                row.setDistrict(
                        get(record,"District")
                );

                row.setState(
                        get(record,"State")
                );

                row.setCountry(
                        get(record,"Country")
                );

                row.setCountryCode(
                        get(record,"CountryCode")
                );

                row.setStateCode(
                        get(record,"StateCode")
                );

                row.setRegionType(
                        get(record,"RegionType")
                );

                row.setDistrictCode(
                        get(record,"DistrictCode")
                );

                row.setCityCode(
                        get(record,"CityCode")
                );

                rows.add(
                        row
                );

            }

        }

        catch (Exception ex) {

            throw new RuntimeException(
                    "CSV parsing failed",
                    ex
            );

        }

        return rows;

    }


    private String get(
            CSVRecord record,
            String column
    ) {

        try {

            return record
                    .get(column)
                    .trim();

        }

        catch (Exception ex) {

            return "";

        }

    }


    private List<LocationImportRow>
    readExcel(
            MultipartFile file
    ) {

        List<LocationImportRow>
                rows =
                new ArrayList<>();

        try (

                Workbook wb =
                        WorkbookFactory.create(
                                file.getInputStream()
                        )

        ) {

            Sheet sheet =
                    wb.getSheetAt(
                            0
                    );

            for (
                    int i = 1;
                    i <= sheet.getLastRowNum();
                    i++
            ) {

                Row r =
                        sheet.getRow(i);

                if (
                        r == null
                ) {
                    continue;
                }

                LocationImportRow row =
                        new LocationImportRow();

                row.setPinCode(
                        getCell(r,0)
                );

                row.setCity(
                        getCell(r,1)
                );

                row.setDistrict(
                        getCell(r,2)
                );

                row.setState(
                        getCell(r,3)
                );

                row.setCountry(
                        getCell(r,4)
                );

                row.setCountryCode(
                        getCell(r,5)
                );

                row.setStateCode(
                        getCell(r,6)
                );

                row.setRegionType(
                        getCell(r,7)
                );

                row.setDistrictCode(
                        getCell(r,8)
                );

                row.setCityCode(
                        getCell(r,9)
                );

                rows.add(
                        row
                );

            }

        }

        catch (Exception ex) {

            throw new RuntimeException(
                    "Excel parsing failed",
                    ex
            );

        }

        return rows;

    }


    private String getCell(
            Row row,
            int index
    ) {

        Cell cell =
                row.getCell(index);

        if (
                cell == null
        ) {

            return "";

        }

        cell.setCellType(
                CellType.STRING
        );

        return cell
                .getStringCellValue()
                .trim();

    }

}
