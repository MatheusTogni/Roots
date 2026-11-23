# 🌱 Roots

App Android para acompanhar hábitos pela consistência, não pela cobrança. Cada hábito tem o seu próprio calendário de quadradinhos, no estilo do gráfico de contribuições do GitHub: dia feito ganha a cor do hábito, dia sem registro fica vazio — e tudo bem. Sem streaks, sem pontos, sem alertas de atraso. Tudo offline, salvo no próprio celular.

[![Baixar APK](https://img.shields.io/badge/baixar-APK-2f7d4f?logo=android&logoColor=white)](https://github.com/MatheusTogni/Roots/releases/latest)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3ddc84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7f52ff?logo=kotlin&logoColor=white)
![Sem dependências](https://img.shields.io/badge/depend%C3%AAncias-0-blue)

<!--
  Capturas de tela: salve as imagens em docs/ e descomente o bloco abaixo.
<p align="center">
  <img src="docs/inicio.png" width="230" alt="Tela inicial com os calendários">
  <img src="docs/widget.png" width="230" alt="Widget Meus hábitos">
</p>
-->

## Funcionalidades

- **Um calendário por hábito** — cada hábito tem nome, ícone (emoji) e cor próprios e o seu gráfico independente: semanas em colunas, dias da semana em linhas, nome dos meses no topo e a quantidade de dias feitos no período.
- **Navegação de meses** — as setas no topo deslocam a janela de 3 meses de todos os calendários de uma vez; o mês atual abre primeiro e não dá para avançar para datas futuras. Tocar num quadradinho mostra a data e se o hábito foi realizado.
- **Registro do dia** — a lista "Hoje" marca e desmarca cada hábito com um toque; salva na hora e o quadradinho muda de cor com uma animação discreta. Cada hábito tem um registro independente por data e não é preciso concluir todos.
- **Gerenciar hábitos** — o app começa vazio e você cria os seus. Novo/editar hábito abre uma folha que sobe da base da tela, com o ícone em destaque e uma prévia ao vivo de como o calendário fica na cor escolhida; dá para editar nome/ícone/cor e excluir (com confirmação avisando que os registros também serão excluídos). Mudar a cor recolore o calendário sem mexer no histórico.
- **Widget "Meus hábitos"** — lista os hábitos do dia na tela inicial, com a cor de cada um, e permite marcar/desmarcar sem abrir o app. Redimensionável, rolável e com várias instâncias; todas ficam sincronizadas com o app.
- **Widget "Meus gráficos"** — um card por hábito com o calendário dos últimos 3 meses (o mesmo do app, na cor do hábito) e a contagem de dias. Também redimensionável, rolável, com várias instâncias e sempre em sincronia com o banco; tocar abre o app.
- **Modo escuro** — um toque no botão 🌙/☀️ do topo alterna entre claro e escuro, com uma transição em que o tema novo se abre em círculo a partir do botão. Segurar o botão volta ao *Automático* (acompanha o aparelho, inclusive quando ele troca de tema). A escolha vale para o app, os diálogos e o widget. No escuro, o calendário mantém as cores dos hábitos sobre um fundo verde quase preto.
- **Novo dia, hábitos pendentes** — cada conclusão pertence a uma data, então à meia-noite os hábitos voltam a aparecer pendentes sem apagar nada do histórico.

## Destaques técnicos

- **100% Kotlin e SDK do Android, sem nenhuma biblioteca externa** — nem AndroidX. O APK fica pequeno e não há dependência para atualizar.
- **Interface montada em código**, sem XML de layout, com um pequeno conjunto de componentes próprios (cards, botões, campos, `CheckCircle`). O calendário é uma `View` própria desenhada em `Canvas` ([`ContributionGraph.kt`](app/src/main/java/com/roots/app/ui/ContributionGraph.kt)). A exceção são os layouts do widget, que o Android só aceita em XML (`RemoteViews`).
- **Tema claro/escuro sem biblioteca** — a escolha fica em `SharedPreferences` ([`ThemeSettings`](app/src/main/java/com/roots/app/ui/ThemeSettings.kt)) e a paleta do app ([`Palette`](app/src/main/java/com/roots/app/ui/Theme.kt)) é resolvida a cada tela. A troca é feita no lugar, sem recriar a tela: uma foto da tela antiga cobre a nova e um buraco circular de borda suave ([`ThemeTransition`](app/src/main/java/com/roots/app/ui/ThemeTransition.kt)) vai revelando o tema novo. No widget, o modo *Automático* usa cores de recurso (`values-night`), que o Android troca na hora junto com o aparelho; com *Claro* ou *Escuro* forçado, ele usa layouts sem cor fixa, pintados em código com a mesma paleta.
- **Calendário calculado com `java.time`** ([`MonthWindow.kt`](app/src/main/java/com/roots/app/ui/MonthWindow.kt)): primeiro dia da semana, tamanho de cada mês, anos bissextos e virada de ano sem tabelas manuais.
- **SQLite com `SQLiteOpenHelper`**, chave estrangeira com `ON DELETE CASCADE`, `UNIQUE (habit_id, date)`, índice por data e migração de schema versionada. Datas locais no formato `yyyy-MM-dd`.
- **Banco como fonte de verdade** — app e widget usam o mesmo `Repository`; toda gravação avisa os widgets, então os dois nunca divergem.
- **Widgets com `AppWidgetProvider` + `RemoteViewsService`** (listas roláveis), checkbox desenhado em `Canvas` e clique com `PendingIntent` explícito. No "Meus gráficos", o calendário de cada hábito é desenhado num bitmap ([`GraphDraw.kt`](app/src/main/java/com/roots/app/widget/GraphDraw.kt)) a partir da mesma `MonthWindow` do app, com cores neutras que servem aos temas claro e escuro.
- **Virada do dia sem timer em segundo plano** — a data é sempre consultada na hora; um alarme inexato de meia-noite (`AlarmManager`) e os broadcasts de mudança de data/hora/fuso, boot e atualização do app redesenham os widgets. Se o widget estiver mostrando um dia velho, o toque só atualiza em vez de gravar no dia errado.
- **CI/CD com GitHub Actions** — cada push na `main` compila e publica o APK em [Releases](https://github.com/MatheusTogni/Roots/releases/latest), numerando a versão pelo número da execução e publicando junto um `version.json`.
- **Atualização dentro do app** ([`Updater.kt`](app/src/main/java/com/roots/app/update/Updater.kt)) — ao abrir, o app compara o `version.json` da release com a versão instalada e, se houver uma nova, mostra um card no topo da tela. O APK só é baixado quando o usuário toca em *Atualizar*, e a instalação usa o `PackageInstaller` do Android, sempre com a confirmação do sistema.

## Estrutura

```
app/src/main/java/com/roots/app/
├── MainActivity.kt            início: navegação de meses, "Hoje" e calendários
├── HabitEditDialog.kt         formulário de hábito (criar, editar, excluir)
├── data/                      SQLite (Db, Repository) e modelos
├── ui/                        tema, componentes, formatação, calendário (Canvas) e matemática de datas
├── update/                    verificação, download e instalação de versões novas
└── widget/                    provider, lista do widget, atualização e virada do dia
```

## Instalação

Baixe o `Roots.apk` em **[Releases → latest](https://github.com/MatheusTogni/Roots/releases/latest)** pelo celular e instale (o Android pede para permitir apps de fontes desconhecidas). Depois disso não é preciso voltar ao GitHub: quando sai uma versão nova, um card no topo da tela inicial do app oferece a atualização, e basta tocar em **Atualizar** e confirmar a instalação. Na primeira vez, o Android pede para permitir que o Roots instale apps. Versões novas instalam por cima sem perder os dados. Para usar o widget, segure um espaço vazio da tela inicial, escolha **Widgets** e arraste o **Meus hábitos** (checklist do dia) e/ou o **Meus gráficos** (calendários).

## Compilando

Requer JDK 17, Gradle 8.9 e Android SDK 35.

```bash
gradle assembleDebug
# APK em app/build/outputs/apk/debug/app-debug.apk
```

Ou faça um fork e deixe o GitHub Actions compilar ([`build.yml`](.github/workflows/build.yml)).

---

Fonte [Nunito](https://github.com/googlefonts/nunito) sob a [SIL Open Font License](licenses/Nunito-OFL.txt).
