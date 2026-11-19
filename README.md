mvn clean verify
java -Dfile.encoding=UTF-8 -jar target/territory-predictive-insights-1.0.0.jar
```

Para executar somente os testes:

```sh
mvn test
```

Para executar o JAR pronto sem Maven:
# Territory Predictive Insights

Sistema de console em Java para monitorar condições relacionadas a alagamentos em uma bacia comunitária fictícia. Lê sensores simulados, grava as medições, consulta o histórico para calcular previsões e registra alertas.

## Público beneficiado e decisão apoiada

Associações de bairro e equipes de manutenção urbana podem usar os resultados para priorizar vistorias de canais e pontos de drenagem. O sistema informa sinais de atenção para apoiar a decisão de uma pessoa responsável. Região, população e leituras são fictícias; não são usados dados pessoais reais.

## Executar e testar

Requisitos: JDK 17 ou superior e Maven 3.9 ou superior. A validação foi realizada com JDK 21. A primeira compilação precisa de internet para baixar H2, JUnit, Mockito e os plugins do Maven.

Na pasta que contém `pom.xml`:

```sh
```sh
java -Dfile.encoding=UTF-8 -jar dist/territory-predictive-insights-1.0.0.jar
```

No Eclipse, importe por **File > Import > Maven > Existing Maven Projects**, selecionando a pasta do `pom.xml`. Configure o projeto para usar JDK 17 ou superior, nunca Java 8. Execute `SistemaMonitoramento.java` em **Run As > Java Application**. Para os testes, use **src/test/java > Run As > JUnit Test**.

O Maven utiliza o compilador Eclipse configurado no `pom.xml`. Isso é infraestrutura de compilação; não exige instalar o Eclipse para rodar pelo terminal.

## Banco de dados

Foi escolhido o caminho **JDBC com H2 em arquivo**. O banco é criado automaticamente em `dados/territorio.mv.db`, usando usuário local `sa` e senha vazia. O DDL está em `BancoDados.inicializar()`, portanto não é necessário executar script SQL separado nem instalar servidor.

Execute sempre na mesma pasta para reutilizar o mesmo histórico. Medições e alertas sobrevivem ao encerramento. População e limiares são configurações da sessão e retornam aos padrões ao reiniciar. Não execute duas instâncias simultaneamente sobre o mesmo arquivo.

As consultas e gravações com dados variáveis usam `PreparedStatement`. Conexões, comandos e resultados são fechados por `try-with-resources`. Uma falha de banco chega ao menu como `SQLException`, com mensagem visível. Se houver falha no meio de uma coleta, as medições já gravadas permanecem no histórico.

## Menu e demonstração

| Opção | Ação |
|---|---|
| 1 | Verificar sensores |
| 2 | Coletar leituras; informar entre 1 e 100 rodadas |
| 3 | Rodar previsões: 1 para média móvel, 2 para comparação |
| 4 | Consultar histórico por ID do sensor |
| 5 | Consultar alertas emitidos |
| 6 | Alterar população ou limiares |
| 7 | Mostrar status completo |
| 8 | Exportar histórico por sensor em CSV, bônus do projeto |
| 0 | Sair |

Comece com **2 > 10** para coletar dez rodadas, totalizando 30 medições. Em seguida, execute **3 > 1** e **3 > 2**. Consulte **5** para os alertas. Saia com **0**, reabra na mesma pasta e confira o histórico e os alertas.

Os sensores são `CH-01`, `NA-01` e `US-01`. O código para alterar limiares é `territorio2026`. A população é validada para não aceitar números negativos. Os limiares precisam obedecer a `0 < atenção < alerta < crítico` e à faixa válida do sensor.

O CSV é salvo em `exportacoes/historico-ID.csv`, com UTF-8, vírgula como separador e ponto decimal. Nova exportação do mesmo sensor substitui o arquivo anterior. O arquivo contém ID, tipo, valor, unidade e instante. No Excel, importe indicando vírgula como separador.

## Sensores e limites demonstrativos

| ID | Classe | Faixa válida | Atenção | Alerta | Crítico |
|---|---|---|---:|---:|---:|
| CH-01 | SensorChuva | 0 a 200 mm/h | 20 | 40 | 60 |
| NA-01 | SensorNivelAgua | 0 a 500 cm | 150 | 250 | 350 |
| US-01 | SensorUmidadeSolo | 0 a 100% | 70 | 85 | 95 |

As leituras simuladas usam `Random`, com tetos de 90 mm/h, 450 cm e 100%, respectivamente. Leituras negativas, fora da faixa, infinitas ou `NaN` são rejeitadas antes da gravação.

## Previsões e alertas

**Média móvel:** soma as cinco últimas leituras e divide por cinco. A sequência 10, 20, 30, 40, 50 produz 30. Representa a manutenção do patamar recente na próxima observação.

**Comparação:** usa dois blocos de cinco leituras. Calcula `média recente + (média recente - média anterior)`. Se a média anterior for 10 e a recente 20, prevê 30. O resultado é limitado à faixa física do sensor.

As estratégias recebem o histórico do banco em ordem cronológica. Sem cinco leituras, a média é suspensa; sem dez, a comparação é suspensa. O programa não aplica prazo de expiração ao histórico. As rodadas são amostras imediatas, não horas ou dias simulados.

Ao executar a opção 3, a classificação e a gravação dos alertas são automáticas. Igualar um limite já caracteriza o nível. Se vários limites forem atingidos, vale o maior. A mensagem registra o sensor, a previsão, a unidade, a estratégia e o limite utilizado. Uma nova execução pode gravar outro alerta igual; não há eliminação de duplicados.

## POO e decisões técnicas

- `ComponenteTerritorial` é abstrata, com ID, nome, região, status e `verificarStatus()` abstrato. Os sensores concretos herdam por meio de `SensorSimulado`.
- `Sensor` define leitura, funcionamento e tipo. Existem três implementações concretas coerentes com o recorte.
- `DadosTerritorio` possui atributos privados e controla a alteração de limiares por código de acesso.
- `EstrategiaPrevisao` concentra a validação do histórico e a soma por laço. As subclasses chamam `super()` e sobrescrevem `prever()`.
- **Factory:** `FabricaPrevisao` centraliza a criação a partir da opção do menu. Seu `switch` escolhe qual objeto criar; o serviço chama `prever()` pela classe abstrata, sem `instanceof` nem condicionais por tipo. Essa escolha evita espalhar a criação e a seleção das estratégias pela aplicação.
- `Medicao`, `Alerta`, `Limiares` e `ResultadoPrevisao` são classes comuns, com construtores, atributos privados e getters. `null` em `ResultadoPrevisao.getAlerta()` significa que não houve alerta.
- Os cálculos e buscas usam laços `for`, arrays e listas.
- `DadosInvalidosException` e `FonteIndisponivelException` são as duas exceções customizadas. Falhas SQL são tratadas diretamente como `SQLException`.
- O bônus implementado é a exportação do histórico em CSV.

## Testes

A suíte possui **22 testes**: 8 de domínio, 6 de previsões, 5 de serviço com Mockito, 2 de persistência e 1 de CSV. Os testes identificam Arrange, Act e Assert. Os testes unitários não dependem de banco nem de digitação; os de persistência usam arquivos temporários.

`assertThrows` verifica as exceções customizadas. As lambdas ficam apenas nessas chamadas de teste, para passar ao JUnit a operação que deve falhar. Mockito simula os DAOs e captura o alerta para conferir nível, previsão e limite sem banco real.

## Reflexão crítica

O sistema deve se calar quando não dispõe de leituras suficientes para o cálculo escolhido ou quando a fonte está indisponível. A aplicação exige cinco leituras para média móvel e dez para comparação, mas esses números não garantem confiabilidade. Cinco medições coletadas em segundos podem representar apenas um instante, sem revelar o comportamento do território ao longo do dia. Além disso, esta versão não verifica a idade do histórico: quem consulta precisa observar os horários antes de usar o resultado. Um valor abaixo do limiar significa somente que o cálculo não atingiu o limite configurado, e não que o local esteja seguro.

Um sensor não representa todas as ruas de uma região. Diferenças de relevo, obstruções de drenagem, qualidade das moradias e dificuldades de deslocamento não aparecem nas medições simuladas. A geração aleatória também não reproduz a relação física entre chuva, saturação do solo e nível da água. A população agregada apenas descreve o território fictício; ela não identifica pessoas que precisem de assistência. Mesmo um histórico numeroso pode conter uma visão incompleta se os sensores estiverem concentrados em pontos de fácil acesso e deixarem outras áreas sem observação.

O alerta deve apoiar a decisão de uma pessoa, apresentando de onde veio o dado, qual cálculo foi feito e qual limite foi atingido. Cabe ao responsável verificar o local, consultar outras fontes e avaliar uma resposta proporcional. Um falso positivo pode desviar equipes, enquanto um falso negativo pode atrasar uma ação necessária. Um uso real exigiria calibração dos limites, manutenção dos equipamentos, frequência de coleta definida e participação da comunidade. O programa organiza informações para discussão e demonstra conceitos de POO; não substitui a experiência de quem conhece o território nem a responsabilidade pela decisão.

## Integrantes e divisão de tarefas

| Integrante | RM | Contribuição |
|---|---|---|
| Gabriel De Biasi Couto | 563247 | Estrutura inicial e configuração Maven; classes de domínio, sensores simulados, validações, exceções personalizadas e testes de domínio. Commit: `adiciona dominio, sensores e validacoes`. |
| Rodrigo Campos Cordeiro | 566386 |  Estratégias de previsão por média móvel e comparação, fábrica de previsões, integração do serviço de monitoramento com o menu, testes de previsão e serviço e documentação README. Commit: `integra previsoes, menu e testes do sistema`. |
| João Pedro da Silva Costa | 565031 | Persistência JDBC com H2, inicialização do banco, DAOs de medições e alertas, exportação CSV como bônus e testes de persistência e CSV. Commit: `adiciona persistencia JDBC e exportacao CSV`. |
