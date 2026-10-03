# Notificações do MestreScan

Atualizado em 3 de outubro de 2026

## Visão geral

A MestreCell pode mandar ofertas por notificação para todo celular com o MestreScan 0.2.0 ou mais novo. O envio é feito pelo console do Firebase, no mesmo projeto do mestrecell-app (`mestrecell-18beb`).

- **Aviso à pessoa:** na primeira vez que ela toca em Escanear, o app explica que vai mandar alertas de segurança e ofertas da MestreCell. Só depois pede a permissão de notificações do Android.
- **Tópico:** todo celular com o app se inscreve em `ofertas-mestrecell`. É para esse tópico que você envia.
- **Canal separado:** as ofertas chegam em "Ofertas da MestreCell" e os alertas em "Alertas de segurança". A pessoa pode silenciar só as ofertas em Ajuda › Silenciar ofertas.
- **Ao tocar:** abre o WhatsApp da loja com uma mensagem pronta ou um link que você escolher.

O código já está pronto. Falta registrar o app no Firebase (Parte 1) e publicar a versão nova (Parte 2).

## Parte 1 — Registrar o app no Firebase (uma vez só)

Use a conta Google **dona** do projeto `mestrecell-18beb`. A conta jpcellacessorios59@gmail.com, logada no Mac, não tem acesso a ele.

1. Entre em [console.firebase.google.com](https://console.firebase.google.com) e abra o projeto `mestrecell-18beb`.
2. Clique na engrenagem › **Configurações do projeto**.
3. Em **Seus apps**, clique em **Adicionar app** e escolha o ícone do **Android**.
4. Em **Nome do pacote Android**, digite exatamente `br.com.mestrecell.mestrescan`.
5. Em **Apelido**, digite `MestreScan`. O campo SHA-1 pode ficar vazio.
6. Clique em **Registrar app** e **baixe o arquivo `google-services.json`**.
7. Pule os outros passos do assistente: o código já está pronto.

## Parte 2 — Colocar o arquivo e publicar a versão 0.2.0

1. Copie o `google-services.json` baixado para `~/Documents/MestreScan/app/google-services.json`. Esse arquivo não é senha e pode ir para o GitHub.
2. Em `app/build.gradle.kts`, mude a versão para `versionCode = 3` e `versionName = "0.2.0"`.
3. No Terminal, rode:

```sh
cd ~/Documents/MestreScan
export JAVA_HOME=~/android-dev/jdk/Contents/Home
git add -A && git commit -m "Versão 0.2.0: ofertas por notificação" && git push
./scripts/publicar-release.sh "Ofertas da MestreCell por notificação"
```

O link de download do site passa a entregar a 0.2.0 sozinho. Só recebe ofertas quem instalar essa versão ou uma mais nova. Se preferir, é só colocar o arquivo na pasta e pedir para o Claude fazer os passos 2 e 3.

## Parte 3 — Enviar uma oferta (sempre que quiser)

1. No [console do Firebase](https://console.firebase.google.com), abra o projeto `mestrecell-18beb`.
2. No menu, vá em **Executar › Messaging** › **Nova campanha** › **Mensagens do Firebase Notifications**.
3. Preencha a notificação:
   - **Título:** por exemplo, `iPhone 13 128GB com 10% off`
   - **Texto:** por exemplo, `Só até sábado na MestreCell. Toque e garanta o seu.`
   - **Imagem (opcional):** link de uma foto do produto.
4. Em **Destino**, escolha **Tópico** e digite `ofertas-mestrecell`.
5. Em **Programação**, escolha **Agora** ou agende data e hora.
6. Em **Opções adicionais**, coloque `offers` em **Canal de notificação do Android** e, se quiser, um dos dados personalizados da tabela abaixo.
7. Clique em **Revisar** e depois em **Publicar**.

| Chave | Valor (exemplo) | O que acontece ao tocar |
| --- | --- | --- |
| `whatsapp` | Vi a oferta do iPhone 13 no app e quero saber mais | Abre o WhatsApp da loja com essa mensagem |
| `link` | https://... (página do produto) | Abre o link no navegador |

Sem nenhum dos dois, o toque só abre o MestreScan. Antes de mandar para todos, teste no seu celular com **Enviar mensagem de teste**.

## Boas práticas

- Mande **no máximo 1 ou 2 ofertas por semana**. Mais que isso faz a pessoa desinstalar.
- Envie só ofertas de verdade: promoção, produto chegando, aviso da loja.
- Envie em horário comercial, entre 9h e 20h.
- Use texto curto e claro, sempre com o nome da loja.
- Não tire o aviso da primeira vez nem o botão Silenciar ofertas. Eles existem para cumprir a LGPD e as regras da Play Store.

## Outra assistência (white label)

Cada marca tem o seu tópico em `app/src/<marca>/res/values/brand.xml`, na linha `brand_offers_topic`. Troque para `ofertas-<marca>` e cada loja envia só para os clientes dela.

## Problemas comuns

| Problema | Solução |
| --- | --- |
| Ninguém recebe | Confira se a versão publicada é a 0.2.0 ou mais nova e se foi gerada com o `google-services.json` na pasta |
| O tópico não aparece no console | Normal no começo. Digite `ofertas-mestrecell` mesmo assim |
| Uma pessoa não recebe | Ela negou as notificações, silenciou as ofertas ou o celular não tem os serviços do Google |
| A oferta chega atrasada | Economia de bateria do Android. O Firebase entrega quando o celular libera |

O mesmo passo a passo também está no projeto, em `docs/ENVIAR-OFERTAS.md`.
