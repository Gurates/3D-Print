package printer;

public class CostConfig {
    public double filamentPricePerKg = 600.0;
    public double electricityCostPerHour = 0.70;
    public double laborCostPerHour = 0.0;
    public double profitMarginPercent = 30.0;
    public double filamentDiameterMm = 1.75;
    public double filamentDensity    = 1.24;

    public double mmToGrams(double lengthMm) {
        double radiusCm = (filamentDiameterMm / 2.0) / 10.0; // mm → cm
        double volumeCm3 = Math.PI * radiusCm * radiusCm * (lengthMm / 10.0);
        return volumeCm3 * filamentDensity;
    }

    public double gramsToCost(double grams) {
        return (grams / 1000.0) * filamentPricePerKg;
    }

    public double secondsToTimeCost(int seconds) {
        double hours = seconds / 3600.0;
        return hours * (electricityCostPerHour + laborCostPerHour);
    }

    public double totalCost(double filamentMm, int durationSeconds) {
        double filamentGrams = mmToGrams(filamentMm);
        double filamentCost  = gramsToCost(filamentGrams);
        double timeCost      = secondsToTimeCost(durationSeconds);
        return filamentCost + timeCost;
    }

    public double sellingPrice(double costTL) {
        return costTL * (1.0 + profitMarginPercent / 100.0);
    }

    @Override
    public String toString() {
        return String.format(
            "CostConfig{filament=%.0f TL/kg, elektrik=%.2f TL/sa, işçilik=%.2f TL/sa, margin=%.0f%%}",
            filamentPricePerKg, electricityCostPerHour, laborCostPerHour, profitMarginPercent
        );
    }
}
