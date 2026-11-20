package br.com.territorio.servico;
import br.com.territorio.dominio.*;
import br.com.territorio.persistencia.*;
import br.com.territorio.excecao.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicoMonitoramentoTest {
    private final MedicaoDAO medicoes = mock(MedicaoDAO.class);
    private final AlertaDAO alertas = mock(AlertaDAO.class);
    private final ServicoMonitoramento servico = new ServicoMonitoramento(medicoes, alertas, new DadosTerritorio("Bacia",100,"senha"));
    private final Sensor sensor = new SensorChuva("CH-01","Bacia",new Random(1));

    private List<Medicao> serie(double valor) {
        List<Medicao> lista = new ArrayList<>();
        LocalDateTime inicio = LocalDateTime.of(2026,1,1,0,0);
        for (int i = 0; i < 5; i++) {
            lista.add(new Medicao("CH-01",TipoSensor.CHUVA,valor,inicio.plusHours(i)));
        }
        return lista;
    }
    @Test void alertaCriticoEhGravadoSemBancoReal() throws SQLException {
        // Arrange
        when(medicoes.buscarHistoricoPorSensor("CH-01")).thenReturn(serie(70));
        // Act
        ResultadoPrevisao resultado = servico.prever(sensor,FabricaPrevisao.criar("1"));
        // Assert
        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(alertas).salvar(captor.capture());
        Alerta gravado = captor.getValue();
        assertEquals(NivelAlerta.CRITICO, gravado.getNivel());
        assertEquals(70, gravado.getValorPrevisto(), 0.001);
        assertEquals(60, gravado.getLimite(), 0.001);
        assertTrue(gravado.getMensagem().contains("CH-01"));
        assertSame(gravado, resultado.getAlerta());
    }
    @Test void abaixoDoLimiteNaoGeraAlerta() throws SQLException {
        // Arrange
        when(medicoes.buscarHistoricoPorSensor("CH-01")).thenReturn(serie(10));
        // Act
        ResultadoPrevisao resultado = servico.prever(sensor,FabricaPrevisao.criar("1"));
        // Assert
        assertNull(resultado.getAlerta());
        assertEquals(10,resultado.getValor(),0.001);
        verifyNoInteractions(alertas);
    }
    @Test void leituraValidaEhGravada() throws SQLException {
        // Arrange
        Sensor fonte = mock(Sensor.class);
        when(fonte.getId()).thenReturn("CH-01");
        when(fonte.retornarTipo()).thenReturn(TipoSensor.CHUVA);
        when(fonte.verificarFuncionamento()).thenReturn(true);
        when(fonte.lerValor()).thenReturn(25.0);
        // Act
        Medicao medicao = servico.coletar(fonte);
        // Assert
        verify(medicoes).salvar(medicao);
        assertEquals(25, medicao.getValor(), 0.001);
    }
    @Test void leituraInvalidaNaoEhGravada() {
        // Arrange
        Sensor fonte = mock(Sensor.class);
        when(fonte.getId()).thenReturn("CH-01");
        when(fonte.retornarTipo()).thenReturn(TipoSensor.CHUVA);
        when(fonte.verificarFuncionamento()).thenReturn(true);
        when(fonte.lerValor()).thenReturn(-1.0);
        // Act / Assert
        assertThrows(DadosInvalidosException.class, () -> servico.coletar(fonte));
        verifyNoInteractions(medicoes);
    }
    @Test void bancoIndisponivelNaoRetornaAlertaComoGravado() throws SQLException {
        // Arrange
        when(medicoes.buscarHistoricoPorSensor("CH-01")).thenReturn(serie(70));
        doThrow(new SQLException("Banco indisponível")).when(alertas).salvar(any(Alerta.class));
        // Act / Assert
        assertThrows(SQLException.class, () -> servico.prever(sensor,FabricaPrevisao.criar("1")));
    }
}
