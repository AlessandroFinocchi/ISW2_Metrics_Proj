package it.uniroma2.alessandro.models;

public class LOCMetrics {
    private int maxVal;
    private double avgVal;
    private int val;

    public LOCMetrics(){
        maxVal = 0;
        avgVal = 0;
        val = 0;
    }

    public int getMaxVal() {
        return maxVal;
    }

    public void setMaxVal(int maxVal) {
        this.maxVal = maxVal;
    }

    public double getAvgVal() {
        return avgVal;
    }

    public void setAvgVal(double avgVal) {
        this.avgVal = avgVal;
    }

    public int getVal() {
        return val;
    }

    public void setVal(int val) {
        this.val = val;
    }

    public void addToVal(int val) {
        this.val += val;
    }
}
