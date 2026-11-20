package br.com.territorio.dominio;
import br.com.territorio.excecao.DadosInvalidosException;
import br.com.territorio.servico.FabricaPrevisao;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PrevisaoTest {
    private List<Medicao> serie(double[] valores) {
        List<Medicao> lista = new ArrayList<>();
        LocalDateTime inicio = LocalDateTime.of(2026,1,1,0,0);
        for (int i = 0; i < valores.length; i++) {
            lista.add(new Medicao("CH-01", TipoSensor.CHUVA, valores[i], inicio.plusHours(i)));
        }
        return lista;
    }
    @Test void mediaUsaSomenteUltimasCincoLeituras() {
        // Arrange
        List<Medicao> historico = serie(new double[]{200,10,20,30,40,50});
        EstrategiaPrevisao estrategia = FabricaPrevisao.criar("1");
        // Act
        double resultado = estrategia.prever(historico);
        // Assert
        assertEquals(30, resultado, 0.001);
    }
    @Test void comparacaoProjetaVariacaoEntreBlocos() {
        // Arrange
        List<Medicao> historico = serie(new double[]{10,10,10,10,10,20,20,20,20,20});
        EstrategiaPrevisao estrategia = FabricaPrevisao.criar("2");
        // Act
        double resultado = estrategia.prever(historico);
        // Assert
        assertEquals(30, resultado, 0.001);
    }
    @Test void estrategiasProduzemResultadosDiferentes() {
        // Arrange
        List<Medicao> historico = serie(new double[]{10,10,10,10,10,20,20,20,20,20});
        // Act
        double media = FabricaPrevisao.criar("1").prever(historico);
        double comparacao = FabricaPrevisao.criar("2").prever(historico);
        // Assert
        assertEquals(20, media, 0.001);
        assertEquals(30, comparacao, 0.001);
    }
    @Test void historicoInsuficienteImpedePrevisao() {
        // Arrange
        List<Medicao> historico = serie(new double[]{10,20});
        EstrategiaPrevisao estrategia = FabricaPrevisao.criar("1");
        // Act / Assert
        assertThrows(DadosInvalidosException.class, () -> estrategia.prever(historico));
    }
    @Test void comparacaoNaoRetornaValorNegativo() {
        // Arrange
        List<Medicao> historico = serie(new double[]{100,100,100,100,100,10,10,10,10,10});
        // Act
        double resultado = FabricaPrevisao.criar("2").prever(historico);
        // Assert
        assertEquals(0, resultado, 0.001);
    }
    @Test void fabricaRecusaOpcaoInvalida() {
        // Arrange
        String opcao = "9";
        // Act / Assert
        assertThrows(DadosInvalidosException.class, () -> FabricaPrevisao.criar(opcao));
    }
}
