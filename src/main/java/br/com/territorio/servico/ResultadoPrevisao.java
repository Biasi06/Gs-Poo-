package br.com.territorio.servico;
import br.com.territorio.dominio.Alerta;

public class ResultadoPrevisao {
    private final double valor;
    private final String explicacao;
    private final Alerta alerta;

    public ResultadoPrevisao(double valor, String explicacao, Alerta alerta) {
        this.valor = valor;
        this.explicacao = explicacao;
        this.alerta = alerta;
    }
    public double getValor() { return valor; }
    public String getExplicacao() { return explicacao; }
    public Alerta getAlerta() { return alerta; }
}
