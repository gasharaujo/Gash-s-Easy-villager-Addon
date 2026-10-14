# Gash's Easy villager Addon

Autor: **Gasharaujo**

Mod ID: `gashs_easy_villager_addon`

Dependência obrigatória: [Easy Villagers](https://github.com/henkelmax/easy-villagers), de henkelmax.

## Estado do projeto

Versão de desenvolvimento `0.1.0-dev`, com quatro blocos:

- **Advanced Trader:** seleciona a primeira troca de um villager de nível 1 sem
  XP de negociação. Mostra somente ofertas de nível 1. Todas as trocas ficam
  disponíveis sem esgotamento enquanto o villager estiver neste bloco.
- **Desneuralizer:** remove automaticamente profissão, nível, XP e ofertas,
  preservando identidade, tipo de bioma, inventário e reputação.
- **Briber:** consome 32 esmeraldas por 400 ticks (20 segundos a 20 TPS) e dá
  exatamente +1 de reputação positiva ao jogador que colocou o bloco. Pausa
  sem esmeraldas e deixa de cobrar no limite vanilla dessa reputação (200 no
  1.20.1; 25 no 1.21.1 e 26.1.2).
  A configuração de reputação universal do Easy Villagers continua aplicável.
- **Leveler:** escolhe oferta alvo e nível, negocia com os insumos fornecidos e
  guarda os produtos na saída. Se a oferta não aparecer ao atingir o nível,
  reinicia a profissão, nível e XP e repete. Para quando encontra o alvo.
  Os insumos consumidos em tentativas anteriores não são reembolsados.

Villagers bebês não são processados. Advanced Trader e Leveler atribuem a
profissão da estação a villagers sem XP; villagers já negociados mantêm sua
profissão até passar pelo Desneuralizer ou reinício do Leveler.

## Interface e pesquisa

Clique com o item de villager para inseri-lo. No Advanced Trader e no Leveler,
clique com uma estação de trabalho para inseri-la. Outro clique abre a interface.

A primeira linha contém villager, estação, pesquisa, páginas, nível alvo, ação
principal, alvo e estado. A segunda recebe insumos, a terceira guarda produtos,
e as três inferiores mostram ofertas. Clique numa oferta para selecioná-la.
No Leveler, escolha o nível antes da oferta e use a alavanca para iniciar/pausar.
No Advanced Trader, a alavanca abre a negociação normal do villager.

A bússola abre o campo de busca usando a tela vanilla de renomeação. Digite e
clique na bússola de resultado para aplicar, sem custo de itens ou XP. Também
pode usar Esc e reabrir o bloco. A busca aceita nomes/IDs de itens e encantamentos;
encantamentos vanilla incluem termos em português como `remendo` e `fortuna`.
Acentos e maiúsculas são ignorados. As prévias são botões e não podem ser coletadas.

O catálogo consulta as fábricas de trocas do pack no Forge 1.20.1 e NeoForge 1.21.1,
incluindo rebalanceamento experimental quando habilitado. No 26.1.2, consulta
os registros de conjuntos de trocas e suas condições. Não introduz ofertas
por uma lista fixa independente do pack. Explora variantes em lotes, até 4096
combinações por fábrica e 4096 entradas por catálogo. Geradores personalizados
podem ter variantes adicionais além desse limite. Reconstrói o catálogo ao abrir
o bloco ou alterar nível/estação, sem cache global de ofertas antigas.

Retire villager/estação clicando no respectivo slot com o cursor vazio.
Funis inserem insumos por cima/laterais e extraem produtos por baixo.
Quebrar o bloco solta villager, estação e inventário; use picareta para receber
também o bloco. O estado é salvo ao descarregar chunks ou fechar o mundo;
ao quebrar a máquina, suas configurações são reiniciadas.

## Versões

| Diretório | Minecraft | Loader de desenvolvimento | Java |
| --- | --- | --- | --- |
| `forge-1.20.1` | 1.20.1 | Forge 47.4.10 | 17 |
| `neoforge-1.21.1` | 1.21.1 | NeoForge 21.1.248 | 21 |
| `neoforge-26.1.2` | 26.1.2 | NeoForge 26.1.2.98 | 25 |

Todos os intervalos de dependências nos metadados usam apenas um mínimo,
no formato `[mínimo,)`, sem versão máxima. Isso não garante compatibilidade
com versões futuras: utilize o JAR correspondente à versão alvo da tabela.

As versões exatas de Easy Villagers em `gradle.properties` servem para obter
um ambiente de desenvolvimento reproduzível. A dependência de execução usa
`[1.1.39,)` no Forge e `[1.1.47,)` no NeoForge, sem limite superior.

## Compilar

Entre no diretório da versão desejada e execute:

```powershell
.\gradlew.bat build
```

Linux/macOS: `bash ./gradlew build`. Configure `JAVA_HOME` para o JDK indicado
na tabela. Os arquivos finais ficam em `build/libs` de cada diretório.
O Gradle baixa Easy Villagers do Maven público do Modrinth para o ambiente
de desenvolvimento; a dependência não é embutida no JAR do addon.

## Executar em desenvolvimento

No diretório da versão desejada, execute `gradlew.bat runClient` ou
`gradlew.bat runServer`. O primeiro início do servidor exige aceitar a EULA
do Minecraft no diretório de execução gerado.

## Instalar

Instale o loader adequado, Easy Villagers para a mesma versão do Minecraft
e o JAR correspondente deste addon na pasta `mods`. A base é carregada
tanto no cliente quanto no servidor. Não instale JARs de versões diferentes
do addon ao mesmo tempo.

## Integração contínua

O GitHub Actions compila as três versões em cada push e pull request e
disponibiliza os JARs como artefatos da execução. Isso verifica a compilação;
o carregamento em jogo deve ser validado separadamente.

## Código compartilhado e testes

`common` contém o comportamento e os modelos, que usam texturas vanilla.
Cada versão adapta registro, persistência e APIs. O Gradle do 26.1.2 adapta
dois nomes de API ao compilar o código comum.

Com o JDK correspondente em `JAVA_HOME`, execute na raiz:

```powershell
.\verification\verify.ps1 -Target neoforge-1.21.1
```

O teste cria um servidor isolado em `build/verification-run`, limitado à rede
local do computador e com aceitação da EULA para esse ambiente de teste.
Verifica catálogo, encantamentos, seleção, estoque ilimitado, reputação,
consumo, reinício/parada do Leveler, persistência e proteção das prévias.
Habilitado apenas com `-Pverification`; classes/mods de teste são excluídos do
JAR normal. A apresentação visual da interface deve ser conferida no cliente.
