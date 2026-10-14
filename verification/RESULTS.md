# Verificação em 14/10/2026

Versão: `0.1.0-dev`.

| Plataforma | Compilação final | Servidor com Easy Villagers |
| --- | --- | --- |
| Forge 1.20.1 / 47.4.10 | Passou | 156 verificações passaram |
| NeoForge 1.21.1 / 21.1.248 | Passou | 156 verificações passaram |
| NeoForge 26.1.2 / 26.1.2.98 | Passou | 156 verificações passaram |

Easy Villagers: 1.1.39 no Forge; 1.1.47 nas versões NeoForge.

As verificações executam dentro do servidor real, com registros e dados do
Minecraft carregados. Cobrem as quatro máquinas, catálogo de nível 1 com
Remendo, seleção inicial, ofertas sem esgotamento, pagamento de 32 esmeraldas
somente após 400 ticks, reputação +1 e pausa no limite da versão, negociação
do Leveler, saída cheia sem perda de insumos, reinício e parada ao encontrar
o alvo, salvamento/recarregamento de dono, reputação, ofertas, XP, inventário
e alvo. Exercitam também os menus no servidor para impedir a retirada de
prévias e aceitar a entrada de texto da pesquisa.

Os JARs finais foram inspecionados: autoria correta, dependências obrigatórias
sem limites máximos, metadados expandidos e ausência de classes de teste ou
de código embutido do Easy Villagers.

Limites: a aparência e os cliques em um cliente gráfico não foram verificados.
Os testes usaram Easy Villagers e o loader, sem os demais mods de um modpack.
O catálogo aplica as regras/fábricas de ofertas ativas, com os limites de
exploração de variantes documentados no README.
