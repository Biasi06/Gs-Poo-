package br.com.territorio;
import br.com.territorio.dominio.*;
import br.com.territorio.excecao.*;
import br.com.territorio.persistencia.*;
import br.com.territorio.servico.*;
import java.sql.SQLException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class SistemaMonitoramento {
    private final Scanner entrada;
    private final DadosTerritorio territorio;
    private final Sensor[] sensores;
    private final MedicaoDAO medicoes;
    private final AlertaDAO alertas;
    private final ServicoMonitoramento servico;

    public SistemaMonitoramento(Scanner entrada, BancoDados banco) {
        this.entrada = entrada;
        territorio = new DadosTerritorio("Bacia comunitária demonstrativa", 1200, "territorio2026");
        sensores = new Sensor[] {
            new SensorChuva("CH-01", territorio.getRegiao(), new Random()),
            new SensorNivelAgua("NA-01", territorio.getRegiao(), new Random()),
            new SensorUmidadeSolo("US-01", territorio.getRegiao(), new Random())
        };
        medicoes = new MedicaoDAO(banco);
        alertas = new AlertaDAO(banco);
        servico = new ServicoMonitoramento(medicoes, alertas, territorio);
    }
    public static void main(String[] args) {
        try (Scanner entrada = new Scanner(System.in)) {
            String caminhoBanco = new java.io.File("dados/territorio").getAbsolutePath();
            BancoDados banco = new BancoDados("jdbc:h2:file:" + caminhoBanco);
            banco.inicializar();
            new SistemaMonitoramento(entrada, banco).executar();
        } catch (SQLException e) {
            System.err.println("Não foi possível abrir o banco: " + e.getMessage());
        }
    }
    public void executar() {
        System.out.println("TERRITORY PREDICTIVE INSIGHTS | Alagamentos | Dados simulados");
        boolean continuar = true;
        while (continuar) {
            System.out.println("\n1. Verificar sensores");
            System.out.println("2. Coletar leituras");
            System.out.println("3. Rodar previsões e emitir alertas");
            System.out.println("4. Ver histórico por sensor");
            System.out.println("5. Ver alertas emitidos");
            System.out.println("6. Gerenciar dados do território");
            System.out.println("7. Status completo");
            System.out.println("8. Exportar histórico em CSV");
            System.out.println("0. Sair");
            if (!entrada.hasNextLine()) break;
            try {
                String opcao = ler("Opção: ");
                switch (opcao) {
                    case "1": verificarSensores(); break;
                    case "2": coletar(); break;
                    case "3": prever(); break;
                    case "4": historico(); break;
                    case "5": listarAlertas(); break;
                    case "6": gerenciar(); break;
                    case "7": status(); break;
                    case "8": exportar(); break;
                    case "0": continuar = false; break;
                    default: System.out.println("Opção inválida. Escolha de 0 a 8.");
                }
            } catch (DadosInvalidosException | FonteIndisponivelException e) {
                System.out.println("Operação não concluída: " + e.getMessage());
            } catch (SQLException e) {
                System.out.println("Falha na operação com o banco: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Falha ao exportar CSV: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("Número inválido. Digite um valor numérico.");
            } catch (NoSuchElementException e) {
                System.out.println("Entrada encerrada.");
                continuar = false;
            }
        }
        System.out.println("Encerrado. Medições e alertas continuam salvos no banco.");
    }
    private String ler(String mensagem) {
        System.out.print(mensagem);
        return entrada.nextLine().trim();
    }
    private double decimal(String mensagem) {
        return Double.parseDouble(ler(mensagem).replace(',', '.'));
    }
    private Sensor escolherSensor() {
        verificarSensores();
        String id = ler("ID do sensor: ");
        for (Sensor sensor : sensores) {
            if (sensor.getId().equalsIgnoreCase(id)) return sensor;
        }
        throw new DadosInvalidosException("Sensor não encontrado: " + id);
    }
    private void verificarSensores() {
        for (Sensor sensor : sensores) {
            String status = "INDISPONÍVEL";
            if (sensor.verificarFuncionamento()) status = "OPERACIONAL";
            System.out.println(sensor.getId() + " | " + sensor.getNome() + " | " + status);
        }
    }
    private void coletar() throws SQLException {
        int rodadas = Integer.parseInt(ler("Rodadas de coleta (1 a 100; 10 habilitam ambas as estratégias): "));
        if (rodadas < 1 || rodadas > 100) throw new DadosInvalidosException("Escolha de 1 a 100 rodadas.");
        int gravadas = 0;
        for (int i = 0; i < rodadas; i++) {
            for (Sensor sensor : sensores) {
                try {
                    Medicao medicao = servico.coletar(sensor);
                    gravadas++;
                    mostrarMedicao(medicao);
                } catch (DadosInvalidosException | FonteIndisponivelException e) {
                    System.out.println("Falha na coleta de " + sensor.getId() + ": " + e.getMessage());
                }
            }
        }
        System.out.println(gravadas + " medições gravadas. Use a opção 3 para gerar previsões e alertas.");
    }
    private void prever() throws SQLException {
        EstrategiaPrevisao estrategia = FabricaPrevisao.criar(ler("Estratégia: 1 - Média móvel | 2 - Comparação: "));
        for (Sensor sensor : sensores) {
            try {
                ResultadoPrevisao resultado = servico.prever(sensor, estrategia);
                System.out.println(resultado.getExplicacao());
            } catch (DadosInvalidosException | FonteIndisponivelException e) {
                System.out.println(sensor.getId() + ": " + e.getMessage());
            }
        }
    }
    private void mostrarMedicao(Medicao medicao) {
        System.out.printf(Locale.ROOT, "%s | %.2f %s | %s%n", medicao.getSensorId(), medicao.getValor(),
                medicao.getTipo().getUnidade(), medicao.getInstante());
    }
    private void historico() throws SQLException {
        Sensor sensor = escolherSensor();
        List<Medicao> lista = medicoes.buscarHistoricoPorSensor(sensor.getId());
        if (lista.isEmpty()) System.out.println("Nenhuma medição encontrada.");
        for (Medicao medicao : lista) mostrarMedicao(medicao);
    }
    private void listarAlertas() throws SQLException {
        List<Alerta> lista = alertas.listarTodos();
        if (lista.isEmpty()) System.out.println("Nenhum alerta emitido.");
        for (Alerta alerta : lista) {
            System.out.println(alerta.getInstante() + " | " + alerta.getNivel() + " | " + alerta.getMensagem());
        }
    }
    private void gerenciar() {
        String opcao = ler("1 - Alterar população | 2 - Alterar limiares: ");
        switch (opcao) {
            case "1":
                territorio.setPopulacao(Integer.parseInt(ler("População estimada: ")));
                break;
            case "2":
                Sensor sensor = escolherSensor();
                String codigo = ler("Código de acesso: ");
                double atencao = decimal("Atenção: ");
                double alerta = decimal("Alerta: ");
                double critico = decimal("Crítico: ");
                territorio.alterarLimiares(sensor.retornarTipo(), new Limiares(atencao, alerta, critico), codigo);
                break;
            default: throw new DadosInvalidosException("Escolha 1 ou 2.");
        }
        System.out.println("Dados atualizados para esta sessão. Limiares e população retornam aos padrões ao reiniciar.");
    }
    private void status() throws SQLException {
        System.out.println("Região: " + territorio.getRegiao() + " | População estimada: " + territorio.getPopulacao());
        verificarSensores();
        for (Sensor sensor : sensores) {
            Limiares limites = territorio.getLimiares(sensor.retornarTipo());
            System.out.println(sensor.getId() + " | leituras: " + medicoes.buscarHistoricoPorSensor(sensor.getId()).size()
                    + " | Atenção: " + limites.getAtencao() + " | Alerta: " + limites.getAlerta() + " | Crítico: " + limites.getCritico());
        }
        System.out.println("Alertas persistidos: " + alertas.listarTodos().size());
    }
    private void exportar() throws SQLException, IOException {
        Sensor sensor = escolherSensor();
        List<Medicao> lista = medicoes.buscarHistoricoPorSensor(sensor.getId());
        Path destino = Path.of("exportacoes", "historico-" + sensor.getId() + ".csv");
        new ExportadorCsv().exportar(lista, destino);
        System.out.println(lista.size() + " medições exportadas em " + destino.toAbsolutePath());
    }
}
