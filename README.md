# MestreScan

**Está com vírus de propaganda? O Mestre resolve.**

App Android da MestreCell que analisa os apps instalados, aponta os que causam
propaganda/travamento e ajuda o usuário a remover. É o Projeto 2 (ver o PDF
nesta pasta); o Projeto 1 é a ferramenta de PC via cabo (`../pc-tiravirus`).

## Princípios

- **O app aponta, o usuário decide.** Nada é removido sem o usuário tocar em
  *Remover* / *Limpar* e confirmar na tela do Android.
- O MestreScan **não** pede acessibilidade nem sobreposição, para não parecer vírus.
- A marca (nome, logo, cores, WhatsApp) fica separada do código, para o white label.

## O que já faz

| Tela | Função |
|---|---|
| Início | Logo MestreCell, botão *Escanear*, resumo do último scan, atalhos |
| Resultado | Suspeitos em vermelho/amarelo com o motivo; *Remover* por app; *Limpar todos os perigosos* (um por um, com confirmação do Android) |
| Detalhe | Motivos com pontos, origem, data, atalhos: tirar administrador, tirar sobreposição, tirar acessibilidade, *Confio neste app* |
| Apps escondidos | Apps sem ícone na tela |
| O que abriu ao ligar | Linha do tempo de cada reinício: quais apps abriram sozinhos, em quantos segundos, se era tela de propaganda |
| Ajuda | Guia do modo seguro, dicas de prevenção, WhatsApp da loja |

### Detecção ao ligar (diferencial)

Vírus de propaganda costuma começar ~30 s depois de ligar e travar o celular.

1. O Android guarda por alguns dias um histórico de uso com cada tela aberta
   (app + nome da tela + horário). Com o *acesso ao uso* liberado, o MestreScan
   lê esse histórico — funciona até para reinícios de antes de o app ser instalado.
2. Ao ligar, o `BootReceiver` agenda uma análise para 3 min depois
   (`BootAnalysisWorker`). Ela salva o reinício em `boot_history.json` e manda
   notificação se algum app abriu sozinho.
3. Um app é marcado como *abriu sozinho* se: não tem ícone, **ou** abriu uma tela
   de anúncio (AdMob, Facebook Ads, AppLovin…), **ou** abriu antes do desbloqueio,
   **ou** não veio da tela inicial.
4. Repetir em vários reinícios soma mais pontos (`boot_repeat`).

Limite: propaganda desenhada como *janela flutuante* (sobreposição) não aparece
no histórico — nesse caso pesam os outros sinais (sobreposição, sem ícone, etc.).

Limite 2: a notificação depende do Android liberar a tarefa em segundo plano. Se o
cliente quase não abre o MestreScan, o Android pode atrasar a análise (grupo
"raro" de economia de bateria). O diagnóstico em si não se perde: ao abrir o app,
ele relê o histórico e mostra o culpado do mesmo jeito.

## Regras de pontuação

Ficam em [`app/src/main/assets/rules.json`](app/src/main/assets/rules.json) —
a mesma tabela do Projeto 1, em formato de dados, para o backend poder servir
o mesmo arquivo aos dois projetos. Faixas: 0–29 normal, 30–59 suspeito, 60+ perigoso.

Regras novas do Projeto 2: `boot_popup` (40), `boot_ad_screen` (30), `boot_repeat` (30).

## White label: nova assistência

1. Copie `app/src/mestrecell/` para `app/src/<nova>/` e troque logo, `brand.xml`
   e `brand_colors.xml`.
2. Em `app/build.gradle.kts`, crie um sabor novo com `applicationId` e
   `WHATSAPP_NUMBER` próprios.
3. `./gradlew assemble<Nova>Debug`.

## Compilar

Ferramentas instaladas em `~/android-dev` (JDK 17, Android SDK, emulador).

```sh
export JAVA_HOME=~/android-dev/jdk/Contents/Home
./gradlew assembleMestrecellDebug
# APK: app/build/outputs/apk/mestrecell/debug/app-mestrecell-debug.apk
```

Instalar num celular com depuração USB:
```sh
~/android-dev/sdk/platform-tools/adb install -r app/build/outputs/apk/mestrecell/debug/app-mestrecell-debug.apk
```

## Publicar para download no site

Passo a passo completo (chave de assinatura, release no GitHub e botão no site):
**[docs/PUBLICAR-DOWNLOAD.md](docs/PUBLICAR-DOWNLOAD.md)**

Resumo: `./scripts/publicar-release.sh "o que mudou"` gera o APK assinado e publica
em `https://github.com/SandiehMoreira/MestreScan/releases/latest/download/MestreScan.apk`.

## Ofertas por notificação

A loja pode mandar ofertas para quem tem o app (Firebase, projeto `mestrecell-18beb`,
tópico `ofertas-mestrecell`). O app avisa disso na hora de pedir a permissão, e as
ofertas ficam num canal separado que a pessoa pode silenciar em **Ajuda**.
Passo a passo: **[docs/NOTIFICACOES-DO-MESTRESCAN.md](docs/NOTIFICACOES-DO-MESTRESCAN.md)**
(versão técnica: [docs/ENVIAR-OFERTAS.md](docs/ENVIAR-OFERTAS.md))

## Próximos passos

- Testar em 5–10 celulares reais com propaganda (ajustar pesos e o tempo de janela).
- Sincronizar `rules.json` / lista de maliciosos com o backend do Projeto 1.
- Monitor de novas instalações.
- Consumo de dados por app (bateria por app não é acessível a apps comuns).
- Política de privacidade e envio para a Play Store.
