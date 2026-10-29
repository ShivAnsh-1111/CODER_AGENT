package com.coderagent.api;

import com.coderagent.llm.OllamaModel;
import java.util.List;

public record ModelsResponse(String activeModel, List<OllamaModel> models) {
}
