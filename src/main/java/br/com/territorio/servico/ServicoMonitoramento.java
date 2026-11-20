package br.com.territorio.servico;
import br.com.territorio.dominio.*;
import br.com.territorio.excecao.*;
import br.com.territorio.persistencia.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class ServicoMonitoramento {
    private final MedicaoDAO medicoes;
    private final AlertaDAO alertas;
    private final DadosTerritorio territorio;

    public ServicoMonitoramento(MedicaoDAO medicoes, AlertaDAO alertas, DadosTerritorio territorio) {
        this.medicoes = medicoes;
        this.alertas = alertas;
        this.territorio = territorio;
    }
    public Medicao coletar(Sensor sensor) throws SQLException {
        if (!sensor.verificarFuncionamento()) {
            throw new FonteIndisponivelException("Sensor " + sensor.getId() + " indisponível.");
        }
        Medicao medicao = new Medicao(sensor.getId(), sensor.retornarTipo(), sensor.lerValor(), LocalDateTime.now());
        medicoes.salvar(medicao);
        return medicao;
    }
    public ResultadoPrevisao prever(Sensor sensor, EstrategiaPrevisao estrategia) throws SQLException {
        if (!sensor.verificarFuncionamento()) {
            throw new FonteIndisponivelException("Sensor " + sensor.getId() + " indisponível.");
        }
        List<Medicao> historico = medicoes.buscarHistoricoPorSensor(sensor.getId());
        double valor = estrategia.prever(historico);
        sensor.retornarTipo().validar(valor);
        Limiares limites = territorio.getLimiares(sensor.retornarTipo());
        NivelAlerta nivel = limites.classificar(valor);
        Alerta alerta = null;
        String mensagem;
        if (nivel != null) {
            double limite = limites.limite(nivel);
            mensagem = String.format(Locale.ROOT,
                    "%s: %s prevê %.2f %s; atingiu o limite %s de %.2f %s. Verificar o ponto monitorado e avaliar medidas preventivas.",
                    sensor.getId(), estrategia.getNome(), valor, sensor.retornarTipo().getUnidade(), nivel, limite,
                    sensor.retornarTipo().getUnidade());
            alerta = new Alerta(sensor.getId(), sensor.retornarTipo(), nivel, valor, limite,
                    estrategia.getNome(), mensagem, LocalDateTime.now());
            alertas.salvar(alerta);
        } else {
            mensagem = String.format(Locale.ROOT,
                    "%s: %s prevê %.2f %s, abaixo do limite de atenção %.2f. Ausência de alerta não garante segurança.",
                    sensor.getId(), estrategia.getNome(), valor, sensor.retornarTipo().getUnidade(), limites.getAtencao());
        }
        return new ResultadoPrevisao(valor, mensagem + " " + estrategia.explicarProjecao(), alerta);
    }
}
