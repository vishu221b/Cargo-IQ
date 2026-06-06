package io.cargoiq.application.port.in;

import io.cargoiq.domain.model.Answer;
import io.cargoiq.domain.model.Query;

/** Inbound port: the RAG entry point. */
public interface AnswerQueryUseCase {
    Answer answer(Query query);
}
