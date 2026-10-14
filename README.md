# Gash's Easy villager Addon

Autor: **Gasharaujo**

Mod ID: `gashs_easy_villager_addon`

Dependência obrigatória: [Easy Villagers](https://github.com/henkelmax/easy-villagers), de henkelmax.

## Estado do projeto

Versão de desenvolvimento `0.2.0-dev`, com quatro blocos e interface própria de negociação:

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

A tela segue a negociação do Minecraft: ofertas à esquerda, pagamento e resultado
à direita, barra de XP, slots de villager/profissão e inventário do jogador abaixo.
Os slots de pagamento e resultado usam o contêiner de negociação vanilla,
incluindo consumo dos itens, XP, estatísticas e descontos. Shift-clique funciona
no inventário e no resultado. A seleção de uma oferta preenche o pagamento com
itens do inventário, como na tela normal de negociação.

Na aba **Trocas**, clique numa oferta e retire seu resultado para negociar.
No Advanced Trader, a aba **Inicial** lista somente ofertas de nível 1; selecione
a desejada e clique em **Definir troca inicial**. No Leveler, use **Alvo**, escolha
o nível, selecione a oferta e clique em **Definir troca alvo**. O botão
**Iniciar/Pausar** controla a automação. A aba **Estoque** mostra slots reais de
entrada e saída; no Briber, mostra as esmeraldas e o progresso do pagamento.

O campo de pesquisa fica na própria tela e filtra enquanto se digita. Inclui
autocomplete de nomes de itens e encantamentos, com sugestões clicáveis,
setas para navegar e **Tab/Enter** para completar. Acentos e maiúsculas são
ignorados. Os nomes seguem o idioma do cliente; encantamentos vanilla também
aceitam termos em português como `remendo` e `fortuna`. As prévias de ofertas
são elementos da interface e não ocupam slots de inventário.

![Busca com autocomplete no cliente Minecraft](docs/ui/autocomplete.png)

![Estoque do Leveler com inventário real](docs/ui/inventory.png)

O catálogo consulta as fábricas de trocas do pack no Forge 1.20.1 e NeoForge 1.21.1,
incluindo rebalanceamento experimental quando habilitado. No 26.1.2, consulta
os registros de conjuntos de trocas e suas condições. Não introduz ofertas
por uma lista fixa independente do pack. Explora variantes em lotes, até 4096
combinações por fábrica e 4096 entradas por catálogo. Geradores personalizados
podem ter variantes adicionais além desse limite. Reconstrói o catálogo ao abrir
o bloco ou alterar nível/estação, sem cache global de ofertas antigas.

Retire villager/estação pelo respectivo slot real da interface.
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
e o JAR correspondente deste addon na pasta `mods`. O addon é necessário
tanto no cliente quanto no servidor. Não instale JARs de versões diferentes
do addon ao mesmo tempo.

## Integração contínua

O GitHub Actions compila as três versões em cada push e pull request e
disponibiliza os JARs como artefatos da execução. Isso verifica a compilação;
o carregamento em jogo deve ser validado separadamente.

## Código compartilhado e testes

`common` contém o comportamento e os modelos, que usam texturas vanilla.
Cada versão adapta registro, persistência, rede e renderização. As telas usam
widgets nativos, slots de negociação vanilla e pacotes próprios para estado
e catálogo. O servidor valida a distância, o menu aberto e a revisão do catálogo
antes de aceitar uma seleção.

Com o JDK correspondente em `JAVA_HOME`, execute na raiz:

```powershell
.\verification\verify.ps1 -Target neoforge-1.21.1
```

O teste cria um servidor isolado em `build/verification-run`, limitado à rede
local do computador e com aceitação da EULA para esse ambiente de teste.
Verifica catálogo, encantamentos, seleção, estoque ilimitado, reputação,
consumo, reinício/parada do Leveler, persistência, tipos de slots, pagamento,
retirada do resultado e XP concedido pela negociação vanilla.
Habilitado apenas com `-Pverification`; classes/mods de teste são excluídos do
JAR normal. O perfil adicional `-PuiVerification` abre um mundo de teste em
`build/ui-client/saves/GashUiTest`, exercita seleção, autocomplete e uma compra
real pelo cliente e salva capturas em `build/ui-client/screenshots`. Esse mundo
deve existir antes do teste; pode ser copiado do mundo do servidor de verificação.
