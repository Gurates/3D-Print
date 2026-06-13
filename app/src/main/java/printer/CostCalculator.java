package printer;

public class CostCalculator {
    public static class CostBreakdown {
        public double filamentGrams;
        public double filamentCostTL;
        public double electricityCostTL;
        public double laborCostTL;
        public double totalCostTL;
        public double sellingPriceTL;
        public boolean isEstimate;

        public String filamentLabel() {
            return String.format("%.1f g", filamentGrams);
        }

        public String summary() {
            return String.format(
                "%s maliyet: %.2f TL (filament %.2f + elektrik %.2f%s)",
                isEstimate ? "Tahmini" : "Gerçek",
                totalCostTL,
                filamentCostTL,
                electricityCostTL,
                laborCostTL > 0 ? String.format(" + işçilik %.2f", laborCostTL) : ""
            );
        }
    }

    private final CostConfig config;

    public CostCalculator(CostConfig config) {
        this.config = config;
    }

    public CostBreakdown calculate(double filamentMm, int durationSeconds) {
        CostBreakdown b = new CostBreakdown();
        b.isEstimate       = false;
        b.filamentGrams    = config.mmToGrams(filamentMm);
        b.filamentCostTL   = config.gramsToCost(b.filamentGrams);
        b.electricityCostTL = config.secondsToTimeCost(durationSeconds)
                              * (config.electricityCostPerHour /
                                 Math.max(config.electricityCostPerHour + config.laborCostPerHour, 0.0001));
        b.laborCostTL      = config.secondsToTimeCost(durationSeconds) - b.electricityCostTL;
        b.totalCostTL      = b.filamentCostTL + b.electricityCostTL + b.laborCostTL;
        b.sellingPriceTL   = config.sellingPrice(b.totalCostTL);
        return b;
    }

    public CostBreakdown estimateFromSlicer(double estimatedFilamentM, int estimatedSeconds) {
        CostBreakdown b = calculate(estimatedFilamentM * 1000.0, estimatedSeconds);
        b.isEstimate = true;
        return b;
    }

    public CostBreakdown projectFromProgress(int progressPercent,
                                              double usedFilamentMm,
                                              int elapsedSeconds) {
        if (progressPercent <= 0) return calculate(0, 0);
        double factor = 100.0 / progressPercent;
        double projectedFilamentMm = usedFilamentMm * factor;
        int    projectedSeconds    = (int) (elapsedSeconds * factor);
        CostBreakdown b = calculate(projectedFilamentMm, projectedSeconds);
        b.isEstimate = true;
        return b;
    }
}
