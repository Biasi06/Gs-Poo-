package br.com.territorio.servico;
import br.com.territorio.dominio.*;
import br.com.territorio.excecao.DadosInvalidosException;

public class FabricaPrevisao {
    private FabricaPrevisao() {}

    public static EstrategiaPrevisao criar(String opcao) {
        if (opcao == null) throw new DadosInvalidosException("Informe a estratégia.");
        // A criação fica apenas na Factory. O serviço usa o polimorfismo.
        switch (opcao) {
            case "1": return new PrevisaoMediaMovel();
            case "2": return new PrevisaoComparacao();
            default: throw new DadosInvalidosException("Escolha 1 (média móvel) ou 2 (comparação).");
        }
    }
}
