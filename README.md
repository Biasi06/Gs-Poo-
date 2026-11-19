# Territory Predictive Insights

Sistema de console em Java para monitorar condições relacionadas a alagamentos em uma bacia comunitária fictícia. O programa coleta leituras de sensores simulados, armazena medições, consulta históricos, calcula previsões e registra alertas.

## Público beneficiado e decisão apoiada

Associações de bairro e equipes de manutenção urbana podem usar os resultados para priorizar vistorias em canais e pontos sujeitos a alagamentos. O sistema apresenta sinais de atenção para apoiar a decisão de uma pessoa responsável.

A região, a população e as leituras utilizadas na simulação são fictícias.

## Executar e testar

### Requisitos

- JDK 17 ou superior.
- Maven 3.9 ou superior para compilar e executar os testes pelo terminal.
- Acesso à internet na primeira compilação para baixar as dependências e os plugins do Maven.

A versão foi validada com JDK 21.

### Pelo terminal

Abra o terminal na pasta que contém o arquivo `pom.xml` e execute um comando por vez:

```sh
mvn clean verify
java "-Dfile.encoding=UTF-8" -jar target/territory-predictive-insights-1.0.0.jar
```

O primeiro comando compila o projeto, executa os testes e gera o JAR. O segundo inicia o sistema.

Para executar somente os testes:

```sh
mvn test
```

Se o comando `mvn` não for reconhecido, é necessário instalar o Maven e configurar seu diretório `bin` no PATH, ou utilizar o Maven integrado ao Eclipse.

### Pelo Eclipse

1. Acesse **File → Import → Maven → Existing Maven Projects**.
2. Selecione a pasta que contém o `pom.xml`.
3. Conclua a importação e aguarde o carregamento das dependências.
4. Configure o projeto para utilizar JDK 17 ou superior.
5. Para iniciar o sistema, clique com o botão direito em `SistemaMonitoramento.java` e escolha **Run As → Java Application**.
6. Para executar os testes, clique com o botão direito em `src/test/java` e escolha **Run As → JUnit Test**.

Para verificar a compilação completa pelo Eclipse, clique com o botão direito no projeto, selecione **Run As → Maven build…**, informe `clean verify` no campo **Goals**, deixe **Skip Tests** desmarcado e execute.

O Maven utiliza o compilador Eclipse configurado no `pom.xml`. Essa configuração faz parte da construção do projeto; não é necessário instalar a IDE Eclipse para executar os comandos pelo terminal.

## Banco de dados

O projeto utiliza JDBC com H2 em arquivo. O banco é criado automaticamente em `dados/territorio.mv.db`, com usuário local `sa` e senha vazia.

A criação das tabelas está implementada em `BancoDados.inicializar()`. Não é necessário instalar um servidor de banco de dados nem executar um script SQL separado.

Execute o programa sempre na mesma pasta para reutilizar o mesmo histórico. Medições e alertas permanecem salvos após o encerramento. População e limiares são configurações da sessão e retornam aos valores padrão ao reiniciar.

Não execute duas instâncias simultaneamente utilizando o mesmo arquivo de banco.

As consultas e gravações com dados variáveis utilizam `PreparedStatement`. Conexões, comandos e resultados são fechados com `try-with-resources`. Falhas de banco chegam ao menu como `SQLException` e são apresentadas ao usuário.

Se uma coleta falhar depois de registrar algumas medições, os registros já gravados permanecem no histórico.

## Menu e demonstração

| Opção | Ação |
|---|---|
| 1 | Verificar sensores |
| 2 | Coletar leituras, informando entre 1 e 100 rodadas |
| 3 | Executar previsões: média móvel ou comparação |
| 4 | Consultar o histórico pelo ID do sensor |
| 5 | Consultar os alertas emitidos |
| 6 | Alterar a população ou os limiares do território |
| 7 | Mostrar o status completo |
| 8 | Exportar o histórico de um sensor em CSV |
| 0 | Encerrar o sistema |

### Roteiro de demonstração

1. Execute a opção `1` para verificar os sensores.
2. Execute a opção `2` e informe `10` rodadas. Com os três sensores disponíveis, serão coletadas 30 medições.
3. Execute a opção `3` e selecione `1` para calcular a média móvel.
4. Execute novamente a opção `3` e selecione `2` para calcular a comparação.
5. Execute a opção `4` e informe o ID de um sensor para consultar seu histórico.
6. Execute a opção `5` para consultar os alertas registrados.
7. Execute a opção `8` e informe o ID de um sensor para exportar seu histórico.
8. Encerre com a opção `0`, reabra o programa na mesma pasta e consulte o histórico e os alertas para verificar a persistência.

Como as leituras são simuladas, os valores e os níveis de alerta podem variar entre as execuções.

Os IDs dos sensores são `CH-01`, `NA-01` e `US-01`. O código de acesso para alterar os limiares é `territorio2026`.

A população não pode ser negativa. Os limiares devem respeitar a ordem `0 < atenção < alerta < crítico` e permanecer dentro da faixa válida do sensor.

## Bônus: exportação CSV

A opção `8` exporta o histórico de um sensor para:

```text
exportacoes/historico-ID.csv
```

O arquivo utiliza UTF-8, vírgula como separador e ponto como separador decimal. Os campos são:

- ID do sensor;
- tipo;
- valor;
- unidade;
- instante da medição.

Uma nova exportação do mesmo sensor substitui o arquivo anterior. Para abrir no Excel, utilize a importação de texto/CSV e selecione a vírgula como separador.

## Sensores e limiares demonstrativos

| ID | Classe | Faixa válida | Atenção | Alerta | Crítico |
|---|---|---|---|---|---|
| `CH-01` | `SensorChuva` | 0 a 200 mm/h | 20 mm/h | 40 mm/h | 60 mm/h |
| `NA-01` | `SensorNivelAgua` | 0 a 500 cm | 150 cm | 250 cm | 350 cm |
| `US-01` | `SensorUmidadeSolo` | 0 a 100% | 70% | 85% | 95% |

As leituras simuladas utilizam `Random`, com limites de geração de 90 mm/h para chuva, 450 cm para nível da água e 100% para umidade do solo.

Leituras negativas, fora da faixa válida, infinitas ou `NaN` são rejeitadas antes da gravação.

Os limiares são demonstrativos e não representam valores técnicos validados para uso em situações reais.

## Previsões e alertas

### Média móvel

Utiliza as cinco últimas leituras do sensor, soma seus valores e divide o resultado por cinco.

Exemplo:

```text
Leituras: 10, 20, 30, 40 e 50
Previsão: (10 + 20 + 30 + 40 + 50) / 5 = 30
```

A estratégia estima a próxima observação mantendo o patamar médio recente.

### Comparação

Utiliza as dez últimas leituras, divididas em dois blocos de cinco. Calcula a média de cada bloco e projeta:

```text
previsão = média recente + (média recente - média anterior)
```

Se a média anterior for 10 e a recente for 20, a previsão será 30. O resultado é limitado à faixa física aceita pelo sensor.

### Condições de cálculo

As estratégias utilizam o histórico em ordem cronológica. A média móvel exige pelo menos cinco leituras, e a comparação exige pelo menos dez.

O programa não aplica prazo de expiração ao histórico. As rodadas representam amostras coletadas durante a execução, não horas ou dias simulados.

### Classificação dos alertas

Ao executar a opção `3`, o sistema calcula a previsão, compara o resultado com os limiares e registra o alerta quando necessário.

Igualar um limiar já caracteriza seu nível. Quando mais de um limiar é atingido, prevalece o nível mais alto: atenção, alerta ou crítico.

A mensagem informa o sensor, a previsão, a unidade, a estratégia utilizada e o limiar atingido. Um resultado abaixo do limiar de atenção não gera alerta, mas também não garante que o local esteja seguro.

Executar novamente uma previsão pode registrar outro alerta com os mesmos valores, pois não há eliminação de duplicados.

## POO e decisões técnicas

- **Abstração e herança:** `ComponenteTerritorial` é uma classe abstrata com ID, nome, região, status e o método abstrato `verificarStatus()`. Os sensores concretos herdam suas características por meio de `SensorSimulado`.
- **Interface:** `Sensor` define as operações de leitura, funcionamento e identificação do tipo. Existem três implementações concretas: chuva, nível da água e umidade do solo.
- **Encapsulamento:** `DadosTerritorio` mantém seus atributos privados e controla a alteração dos limiares por código de acesso.
- **Polimorfismo:** `EstrategiaPrevisao` concentra comportamentos compartilhados. `PrevisaoMediaMovel` e `PrevisaoComparacao` chamam `super()` em seus construtores e sobrescrevem `prever()`.
- **Padrão de criação:** `FabricaPrevisao` centraliza a criação da estratégia conforme a opção escolhida no menu. Seu `switch` seleciona qual objeto criar, enquanto o serviço trabalha com a classe abstrata `EstrategiaPrevisao`. Essa organização evita espalhar a criação das estratégias pela aplicação.
- **Classes de dados:** `Medicao`, `Alerta`, `Limiares` e `ResultadoPrevisao` utilizam construtores, atributos privados e getters. O retorno `null` de `ResultadoPrevisao.getAlerta()` indica que não houve alerta.
- **Estruturas básicas:** os cálculos e buscas utilizam laços `for`, arrays e listas.
- **Exceções:** `DadosInvalidosException` e `FonteIndisponivelException` representam falhas de validação e indisponibilidade da fonte. Falhas SQL são tratadas como `SQLException`.
- **Persistência:** JDBC com H2 em arquivo permite armazenar o histórico sem instalar um servidor externo.
- **Bônus:** a exportação CSV permite consultar o histórico em planilhas e outras ferramentas.

## Testes

A suíte contém 22 testes:

| Grupo | Quantidade |
|---|---|
| Domínio | 8 |
| Previsões | 6 |
| Serviço de monitoramento com Mockito | 5 |
| Persistência | 2 |
| Exportação CSV | 1 |
| **Total** | **22** |

Os testes seguem a organização Arrange, Act e Assert: preparação dos dados, execução da operação e verificação do resultado.

Os testes de previsão verificam os cálculos e suas condições de execução. `assertThrows` verifica as exceções customizadas.

Mockito simula os DAOs nos testes do serviço, permitindo verificar o nível, a previsão e o limiar do alerta sem utilizar um banco real.

Os testes de persistência utilizam arquivos temporários de banco, e o teste de CSV verifica o arquivo exportado em um diretório temporário. Os testes não exigem interação pelo console.

## Reflexão crítica

O sistema deve deixar de emitir uma previsão quando não dispõe de leituras suficientes para a estratégia escolhida ou quando a fonte está indisponível. A aplicação exige cinco leituras para a média móvel e dez para a comparação, mas essas quantidades não garantem confiabilidade. Cinco medições coletadas em segundos podem representar apenas um instante, sem revelar o comportamento do território ao longo do dia. Além disso, esta versão não verifica a idade do histórico: quem consulta precisa observar os horários antes de utilizar o resultado. Um valor abaixo dos limiares significa somente que a previsão não atingiu o limite configurado, e não que o local esteja seguro.

Um sensor não representa todas as ruas de uma região. Diferenças de relevo, obstruções de drenagem, condições das moradias e dificuldades de acesso não aparecem nas leituras simuladas. A geração aleatória também não reproduz a relação física entre chuva, saturação do solo e nível da água. A população agregada apenas descreve o território fictício; ela não identifica pessoas que precisam de assistência. Mesmo um histórico numeroso pode oferecer uma visão incompleta se os sensores estiverem concentrados em pontos de fácil acesso e deixarem outras áreas sem observação.

O alerta deve apoiar a decisão de uma pessoa, apresentando de onde veio o dado, qual cálculo foi realizado e qual limiar foi atingido. Cabe ao responsável verificar o local, consultar outras fontes e avaliar uma resposta proporcional. Um falso positivo pode desviar equipes, enquanto um falso negativo pode atrasar uma ação necessária. Um uso real exigiria limiares validados, manutenção dos equipamentos, frequência de coleta definida e participação da comunidade. O programa organiza informações para discussão e demonstração de conceitos de POO; não substitui a experiência de quem conhece o território nem a responsabilidade pela decisão.

## Integrantes e divisão de tarefas

| Integrante | RM | Contribuição |
|---|---|---|
| Gabriel De Biasi Couto | 563247 | Estrutura inicial e configuração Maven; classes de domínio, sensores simulados, validações, exceções personalizadas e testes de domínio. Commit: `adiciona dominio, sensores e validacoes`. |
| Rodrigo Campos Cordeiro | 566386 | Estratégias de previsão por média móvel e comparação, fábrica de previsões, integração do serviço de monitoramento com o menu, testes de previsão e serviço e documentação README. Commit: `integra previsoes, menu e testes do sistema`. |
| João Pedro da Silva Costa | 565031 | Persistência JDBC com H2, inicialização do banco, DAOs de medições e alertas, exportação CSV como bônus e testes de persistência e CSV. Commit: `adiciona persistencia JDBC e exportacao CSV`. |
