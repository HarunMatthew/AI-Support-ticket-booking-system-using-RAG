package com.example.support.service;

import ai.djl.MalformedModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Real sentence embeddings for "all-MiniLM-L6-v2", replacing the Python:
 *
 *   SentenceTransformer("all-MiniLM-L6-v2").encode(texts)
 *
 * How the model is obtained:
 * ---------------------------------------------------------------------------
 * We use Deep Java Library (DJL) on the PyTorch engine. On first use, DJL's
 * Criteria/ModelZoo resolves the model URN
 *   "djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2"
 * which downloads the traced TorchScript weights + HuggingFace tokenizer
 * files for that exact model from DJL's model zoo (which mirrors the
 * HuggingFace Hub artifact) and caches them under the user's home directory
 * (~/.djl.ai/cache). No network access is required on subsequent runs.
 * The native PyTorch engine binaries are pulled the same way, via the
 * pytorch-engine / pytorch-model-zoo Maven dependencies declared in pom.xml.
 *
 * TextEmbeddingTranslatorFactory takes care of tokenization, mean-pooling
 * over token embeddings and L2 normalization - the same post-processing
 * SentenceTransformer applies internally - so the resulting 384-dim vectors
 * are directly comparable to ones produced by the original Python model.
 * ---------------------------------------------------------------------------
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final String MODEL_URN =
            "djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2";

    private ZooModel<String, float[]> model;

    @PostConstruct
    public void init() {
        try {
            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optModelUrls(MODEL_URN)
                    .optEngine("PyTorch")
                    .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                    .optProgress(new ai.djl.training.util.ProgressBar())
                    .build();

            this.model = ModelZoo.loadModel(criteria);
            log.info("all-MiniLM-L6-v2 embedding model loaded successfully via DJL/PyTorch.");
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            // If the model cannot be downloaded/loaded (e.g. no network access on first
            // run), fail loudly rather than silently falling back to fake vectors -
            // the assignment explicitly forbids random/zero embeddings.
            throw new IllegalStateException(
                    "Failed to load all-MiniLM-L6-v2 via DJL. Ensure the application has network "
                            + "access on first startup to download the model, or pre-populate the "
                            + "DJL cache (~/.djl.ai).", e);
        }
    }

    /**
     * Generates a real 384-dimensional embedding for a single piece of text.
     */
    public float[] generateEmbedding(String text) {
        try (Predictor<String, float[]> predictor = model.newPredictor()) {
            return predictor.predict(text);
        } catch (TranslateException e) {
            throw new IllegalStateException("Failed to generate embedding for query text", e);
        }
    }

    /**
     * Batch variant, used at startup to embed all historical tickets.
     */
    public List<float[]> generateEmbeddings(List<String> texts) {
        List<float[]> embeddings = new ArrayList<>(texts.size());
        try (Predictor<String, float[]> predictor = model.newPredictor()) {
            for (String text : texts) {
                embeddings.add(predictor.predict(text));
            }
        } catch (TranslateException e) {
            throw new IllegalStateException("Failed to generate embeddings for ticket batch", e);
        }
        return embeddings;
    }

    @PreDestroy
    public void close() {
        if (model != null) {
            model.close();
        }
    }
}
