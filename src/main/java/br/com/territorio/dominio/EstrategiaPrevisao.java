package br.com.territorio.dominio;

import java.util.List;
import br.com.territorio.excecao.DadosInvalidosException;
public abstract class EstrategiaPrevisao {
    private final String nome;
    private final int minimoLeituras;
    protected EstrategiaPrevisao(String nome, int minimoLeituras) { this.nome = nome; this.minimoLeituras = minimoLeituras; }
    public abstract double prever(List<Medicao> historico);
    public abstract String explicarProjecao();
    protected void validar(List<Medicao> historico) {
        if (historico == null || historico.size() < minimoLeituras)
            throw new DadosInvalidosException(nome + ": histórico insuficiente; colete ao menos " + minimoLeituras + " leituras.");
        Medicao primeira = historico.get(0);
        if (primeira == null) throw new DadosInvalidosException("Histórico contém medição nula.");
        for (int i = 0; i < historico.size(); i++) {
            Medicao m = historico.get(i);
            if (m == null || !m.getSensorId().equals(primeira.getSensorId()) || m.getTipo() != primeira.getTipo())
                throw new DadosInvalidosException("Histórico deve conter um único sensor e tipo.");
            if (i > 0 && m.getInstante().isBefore(historico.get(i - 1).getInstante()))
                throw new DadosInvalidosException("Histórico deve estar em ordem cronológica.");
        }
    }
    protected double media(List<Medicao> historico, int inicio, int fim) {
        double soma = 0;
        for (int i = inicio; i < fim; i++) {
            soma += historico.get(i).getValor();
        }
        return soma / (fim - inicio);
    }
    public String getNome() { return nome; }
    public int getMinimoLeituras() { return minimoLeituras; }
}
