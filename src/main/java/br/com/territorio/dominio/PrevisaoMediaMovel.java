package br.com.territorio.dominio;

import java.util.List;
public class PrevisaoMediaMovel extends EstrategiaPrevisao {
    public PrevisaoMediaMovel() { super("Média móvel (5 leituras)", 5); }
    @Override public String explicarProjecao() {
        return "Estima a próxima observação mantendo a média das cinco leituras mais recentes.";
    }
    @Override public double prever(List<Medicao> historico) {
        super.validar(historico);
        return media(historico, historico.size() - 5, historico.size());
    }
}
