package com.fly.uloganalyzer.infrastructure;

import com.fly.uloganalyzer.domain.CSVData;
import java.util.List;

public interface CSVFilter {
    List<CSVData> filterEssentialCSV(List<CSVData> allCsvData, String nameOfULG);
}