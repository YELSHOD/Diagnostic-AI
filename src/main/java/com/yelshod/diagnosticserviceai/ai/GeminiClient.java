package com.yelshod.diagnosticserviceai.ai;

public interface GeminiClient {

    String generateDiagnosisJson(String model, String prompt);

    String generateChatJson(String model, String prompt);
}
