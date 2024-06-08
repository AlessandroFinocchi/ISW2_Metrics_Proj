package it.uniroma2.alessandro.models;

public class ACUMEInstance {
    private int id;
    private int size;
    private double predicted;
    private String actual;

    public ACUMEInstance(int id, int size, double predicted, String actual) {
        if(predicted < 0 || predicted > 1)
            throw new IllegalArgumentException("predicted must be between 0 and 1");

        this.id = id;
        this.size = size;
        this.predicted = predicted;
        this.actual = actual;
    }

    public int getId() {
        return id;
    }
    public int getSize() {
        return size;
    }
    public double getPredicted() {
        return predicted;
    }

    public String getActual() {
        return actual;
    }
}
