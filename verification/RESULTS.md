# Verificação em 14/10/2026

Versão: `0.2.0-dev`.

| Plataforma | Compilação final | Servidor com Easy Villagers |
| --- | --- | --- |
| Forge 1.20.1 / 47.4.10 | Passou | 163 verificações passaram |
| NeoForge 1.21.1 / 21.1.248 | Passou | 163 verificações passaram |
| NeoForge 26.1.2 / 26.1.2.98 | Passou | 163 verificações passaram |

Easy Villagers: 1.1.39 no Forge; 1.1.47 nas versões NeoForge.

As verificações executam dentro do servidor real, com registros e dados do
Minecraft carregados. Cobrem as quatro máquinas, catálogo de nível 1 com
Remendo, seleção inicial, ofertas sem esgotamento, pagamento de 32 esmeraldas
somente após 400 ticks, reputação +1 e pausa no limite da versão, negociação
do Leveler, saída cheia sem perda de insumos, reinício e parada ao encontrar
o alvo, salvamento/recarregamento de dono, reputação, ofertas, XP, inventário
e alvo. Exercitam também o menu registrado, slots tipados de villager/entrada/saída,
pagamento nos slots vanilla, retirada do resultado e XP exato da negociação.

## Cliente gráfico

As três versões foram abertas no cliente Minecraft em mundos isolados. O teste
passou com os marcadores `ADDON_CLIENT_UI_PASSED` e
`ADDON_CLIENT_REAL_TRADE_PASSED` em Forge 1.20.1, NeoForge 1.21.1 e NeoForge 26.1.2.
Foram verificadas a renderização das quatro telas, pesquisa por encantamento,
autocomplete com Tab, seleção de Remendo como oferta inicial, preenchimento
dos pagamentos, retirada real do livro pelo protocolo de inventário e XP do
villager. As telas do Leveler e Briber também foram capturadas durante execução.

As capturas foram inspecionadas visualmente. O autocomplete renderiza acima
das ofertas, os labels estão em português e o layout compacto cabe em uma
janela 720p com a escala padrão do Forge. Exemplos estão em `docs/ui`.

Os JARs finais foram inspecionados: autoria correta, dependências obrigatórias
sem limites máximos, metadados expandidos e ausência de classes de teste ou
de código embutido do Easy Villagers.

Os testes usaram Easy Villagers e o loader, sem os demais mods de um modpack.
O catálogo aplica as regras/fábricas de ofertas ativas, com os limites de
exploração de variantes documentados no README.
