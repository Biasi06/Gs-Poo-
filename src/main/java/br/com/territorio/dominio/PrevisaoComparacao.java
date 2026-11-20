package br.com.territorio.dominio;

import java.util.List;
public class PrevisaoComparacao extends EstrategiaPrevisao {
    public PrevisaoComparacao() { super("Comparação (2 blocos de 5)", 10); }
    @Override public String explicarProjecao() {
        return "Projeta o próximo bloco pela média recente mais a diferença entre os dois blocos de cinco leituras, limitada à faixa física do sensor.";
    }
    @Override public double prever(List<Medicao> historico) {
        super.validar(historico);
        int n = historico.size();
        double atual = media(historico, n - 5, n);
        double anterior = media(historico, n - 10, n - 5);
        return Math.max(0, Math.min(historico.get(n - 1).getTipo().getMaximo(), atual + (atual - anterior)));
    }
}
