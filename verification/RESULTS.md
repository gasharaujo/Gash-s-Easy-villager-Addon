# Verificação em 14/10/2026

Versão: `0.4.1-dev`.

| Plataforma | Compilação final | Servidor com Easy Villagers |
| --- | --- | --- |
| Forge 1.20.1 / 47.4.10 | Passou | 329 verificações passaram |
| NeoForge 1.21.1 / 21.1.248 | Passou | 329 verificações passaram |
| NeoForge 26.1.2 / 26.1.2.98 | Passou | 329 verificações passaram |

Easy Villagers: 1.1.39 no Forge; 1.1.47 nas versões NeoForge.

As verificações executam dentro do servidor real, com registros e dados do
Minecraft carregados. Cobrem as quatro máquinas, catálogo de nível 1 com
Remendo, seleção inicial, ofertas sem esgotamento, pagamento de 32 esmeraldas
somente após 400 ticks, reputação +1 e pausa no limite da versão, negociação
do Leveler, saída cheia sem perda de insumos, reinício e parada ao encontrar
o alvo, salvamento/recarregamento de dono, reputação, ofertas, XP, inventário
e alvo. Exercitam também o menu registrado, slots tipados de villager/entrada/saída,
pagamento nos slots vanilla, retirada do resultado e XP exato da negociação.

A seleção inicial é assíncrona e não altera a coleção de ofertas ao escolher o
alvo. O teste compara um sorteio nativo com o sorteio do Advanced Trader usando
a mesma sequência aleatória: preço, quantidade, XP, ordem e ofertas acompanhantes
coincidem. No 26.1.2, a sequência é a do registro de trocas/loot nativo.
O Desneuralizer é testado com fila de villagers, saída cheia sem consumir a
entrada e transferência do villager processado à saída. Os menus dos três
processadores rejeitam resultado de negociação manual e não oferecem trocas.

Na versão 0.4, a retirada é exercitada pelo `ServerPlayerGameMode.useItemOn`
nativo, com Shift e item na outra mão: villager primeiro, bancada depois,
sem abrir GUI e sem perder XP/nome. O Leveler usa o catálogo real do ferreiro
de ferramentas para verificar picareta de diamante genérica e variantes de
encantamento, seleção entre níveis, OU antecipado, E com todos no mesmo villager,
reinício quando só parte dos alvos aparece, remoção/limpeza e migração do alvo
único. Os alvos, componentes, níveis e modo E/OU sobrevivem ao salvamento.

Na versão 0.4.1, os testes verificam tentativas consecutivas a cada tick,
dez negociações pagas com subida de nível e reinício no mesmo tick, parada
imediata quando o último espaço da saída enche e preservação de todos os
pagamentos/produtos. Uma tentativa de 250 negociações confirma o limite de
64 por tick, continuidade no tick seguinte e somente um reinício por tick.
O Advanced Trader mantém 32 sorteios por tick. Os testes do Briber e do
Desneuralizer confirmam os intervalos anteriores.

## Cliente gráfico

Na versão 0.4.0, as três versões foram abertas no cliente Minecraft em mundos
isolados. A versão 0.4.1 altera somente o processamento no servidor. O teste
passou com os marcadores `ADDON_CLIENT_UI_PASSED` e
`ADDON_CLIENT_REAL_TRADE_PASSED` em Forge 1.20.1, NeoForge 1.21.1 e NeoForge 26.1.2.
Foram verificadas a renderização das quatro telas, pesquisa por encantamento,
autocomplete com Tab, seleção de Remendo como oferta inicial, preenchimento
dos pagamentos, retirada real do livro pelo protocolo de inventário e XP do
villager. As telas do Leveler e Briber também foram capturadas durante execução.
O marcador adicional `ADDON_CLIENT_TARGETS_PASSED count=2 and=true` confirma
o clique nos widgets do seletor: item genérico, segundo alvo em outro nível,
troca de OU para E, remoção individual e combinação de alvos genéricos/exatos.

As capturas foram inspecionadas visualmente em janela 720p com escala 3.
Advanced Trader usa o fundo de negociação vanilla (villager2.png no Forge
1.20.1; villager.png nas versões NeoForge), com busca inicial em janela separada.
Desneuralizer usa o fundo de entrada/saída do Incubator. Briber e Leveler têm
telas compactas próprias sem pagamentos/resultados de negociação manual.
O autocomplete renderiza acima da lista e os labels estão em português.
Exemplos estão em `docs/ui`.

Os JARs finais foram inspecionados: autoria correta, dependências obrigatórias
sem limites máximos, metadados expandidos e ausência de classes de teste ou
de código embutido do Easy Villagers.

Os testes usaram Easy Villagers e o loader, sem os demais mods de um modpack.
O catálogo aplica as regras/fábricas de ofertas ativas, com os limites de
exploração de variantes documentados no README.
