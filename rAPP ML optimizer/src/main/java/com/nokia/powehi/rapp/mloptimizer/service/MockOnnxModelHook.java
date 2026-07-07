package com.nokia.powehi.rapp.mloptimizer.service;

import com.nokia.powehi.rapp.mloptimizer.model.CellPerformanceMetrics;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MockOnnxModelHook implements OnnxModelHook {

    @Override
    public Optional<Double> predictOffloadPercentage(CellPerformanceMetrics metrics) {
        return Optional.empty();
    }
}
