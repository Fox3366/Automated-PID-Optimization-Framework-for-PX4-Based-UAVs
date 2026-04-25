package com.fly.uloganalyzer.infrastructure;

import com.fly.uloganalyzer.domain.ULogFile;
import com.fly.uloganalyzer.domain.CSVData;
import java.util.List;

public interface ULogParser {
    List<CSVData> parseULog(ULogFile ulogFile);
}