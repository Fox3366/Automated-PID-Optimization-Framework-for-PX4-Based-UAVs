package com.fly.uloganalyzer.infrastructure;

import com.fly.uloganalyzer.domain.AnalysisRequest;
import com.fly.uloganalyzer.domain.AnalysisResult;

public interface OpenAIClient {
    AnalysisResult analyzeData(AnalysisRequest request);
}