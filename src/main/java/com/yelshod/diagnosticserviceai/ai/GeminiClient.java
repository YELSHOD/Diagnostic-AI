package com.yelshod.diagnosticserviceai.ai;

public interface GeminiClient {

    String generateDiagnosisJson(String model, String prompt);
}
