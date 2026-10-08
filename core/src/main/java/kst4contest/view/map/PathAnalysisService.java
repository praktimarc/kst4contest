package kst4contest.view.map;

/**
 * Abstraction for path analysis.
 *
 * First implementation can be geometry-only.
 * Later this can call terrain APIs, caching layers, LOS checks, etc.
 */
public interface PathAnalysisService {

    PathAnalysisResult analyze(PathAnalysisRequest request);
}