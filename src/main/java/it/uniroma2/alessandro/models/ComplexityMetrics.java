package it.uniroma2.alessandro.models;

public class ComplexityMetrics {
    private final int cbo;
    private final int fanIn;
    private final int fanOut;
    private final int publicMethodsQty;

    public ComplexityMetrics(int cbo, int fanIn, int fanOut, int publicMethodsQty) {
        this.cbo = cbo;
        this.fanIn = fanIn;
        this.fanOut = fanOut;
        this.publicMethodsQty = publicMethodsQty;
    }

    public int getCbo() {
        return cbo;
    }

    public int getFanIn() {
        return fanIn;
    }

    public int getFanOut() {
        return fanOut;
    }

    public int getPublicMethodsQty() {
        return publicMethodsQty;
    }
}
