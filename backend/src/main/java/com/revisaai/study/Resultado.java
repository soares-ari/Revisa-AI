package com.revisaai.study;

import java.util.Map;

public class Resultado {

    private int total;
    private int acertos;
    private double percentual;
    private Map<String, Double> desempenhoPorArea;

    public Resultado() {
    }

    public Resultado(int total, int acertos, double percentual,
                     Map<String, Double> desempenhoPorArea) {
        this.total = total;
        this.acertos = acertos;
        this.percentual = percentual;
        this.desempenhoPorArea = desempenhoPorArea;
    }

    public int getTotal() { return total; }
    public int getAcertos() { return acertos; }
    public double getPercentual() { return percentual; }
    public Map<String, Double> getDesempenhoPorArea() { return desempenhoPorArea; }
}
