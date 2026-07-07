package com.nokia.powehi.rapp.mloptimizer.service;

import com.nokia.powehi.rapp.mloptimizer.model.CellPerformanceMetrics;
import java.util.Optional;

/**
 * Extension point for ONNX Runtime inference. Implementations load a trained model
 * and return a predicted offload percentage; the default mock defers to heuristics.
 */
public interface OnnxModelHook {

    Optional<Double> predictOffloadPercentage(CellPerformanceMetrics metrics);
}
