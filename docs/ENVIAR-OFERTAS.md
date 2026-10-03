# Enviar ofertas por notificação (Firebase)

Com isso a MestreCell manda uma notificação para todos que têm o MestreScan
instalado, por exemplo: *"iPhone 13 128GB com 10% off — só esta semana"*. Ao tocar,
abre o WhatsApp da loja com a mensagem pronta ou um link.

Usa o **mesmo projeto Firebase do `mestrecell-app`**: `mestrecell-18beb`.

## Como funciona

- Na primeira vez que a pessoa toca em **Escanear**, o app avisa:
  *"O MestreScan avisa quando um app suspeito aparecer e também manda ofertas da
  MestreCell."* Depois pede a permissão de notificações.
- Todo celular com o app se inscreve no tópico **`ofertas-mestrecell`**.
- As ofertas chegam no canal **"Ofertas da MestreCell"**, separado dos
  **"Alertas de segurança"**. A pessoa pode silenciar só as ofertas em
  **Ajuda › Silenciar ofertas**, sem perder os alertas.
- Só recebe quem instalou a versão **0.2.0 ou mais nova**.

---

## Parte 1 — Ligar o Firebase no app (uma vez só)

### 1.1 — Registrar o MestreScan no projeto Firebase

1. Entre em https://console.firebase.google.com com a conta **dona** do projeto
   `mestrecell-18beb`.
2. Abra o projeto → ícone de engrenagem → **Configurações do projeto**.
3. Em **Seus apps**, clique em **Adicionar app** → ícone do **Android**.
4. **Nome do pacote Android:** `br.com.mestrecell.mestrescan` (exatamente assim).
5. **Apelido:** `MestreScan`. O campo SHA-1 pode ficar vazio.
6. Clique em **Registrar app** e **baixe o `google-services.json`**.
7. Pule os outros passos do assistente: o código já está pronto.

### 1.2 — Colocar o arquivo no projeto

Copie o arquivo baixado para:

```
~/Documents/MestreScan/app/google-services.json
```

Esse arquivo não é senha. É normal ele ficar no GitHub junto com o código.

### 1.3 — Publicar a versão nova

Em `app/build.gradle.kts`, suba a versão (`versionCode = 3`, `versionName = "0.2.0"`) e:

```sh
cd ~/Documents/MestreScan
export JAVA_HOME=~/android-dev/jdk/Contents/Home
git add -A && git commit -m "Versão 0.2.0: ofertas por notificação" && git push
./scripts/publicar-release.sh "Ofertas da MestreCell por notificação"
```

O link de download do site passa a entregar a 0.2.0 automaticamente.

---

## Parte 2 — Enviar uma oferta (sempre que quiser)

1. https://console.firebase.google.com → projeto `mestrecell-18beb`.
2. Menu **Executar › Messaging** → **Criar a primeira campanha** (ou **Nova campanha**)
   → **Mensagens do Firebase Notifications**.
3. **Notificação:**
   - **Título:** `iPhone 13 128GB com 10% off 🔥`
   - **Texto:** `Só até sábado na MestreCell. Toque e garanta o seu.`
   - **Imagem (opcional):** link de uma foto do produto.
4. **Destino:** escolha **Tópico** → digite `ofertas-mestrecell`.
5. **Programação:** **Agora** (ou agende data e hora).
6. **Opções adicionais:**
   - **Canal de notificação do Android:** `offers`
   - **Dados personalizados**: escolha **um** dos dois:

     | Chave | Valor | O que acontece ao tocar |
     |---|---|---|
     | `whatsapp` | `Vi a oferta do iPhone 13 no app e quero saber mais` | Abre o WhatsApp da loja com essa mensagem |
     | `link` | `https://...` (página do produto) | Abre o link no navegador |

     Sem nenhum dos dois, o toque só abre o MestreScan.
7. **Revisar** → **Publicar**.

> Antes de mandar para todos, teste no seu celular: em **Destino**, use
> **Enviar mensagem de teste** com o token do aparelho, ou crie um tópico de teste.

---

## Boas práticas (para não virar "propaganda chata")

- **No máximo 1 ou 2 por semana.** Muitas ofertas fazem a pessoa desinstalar.
- **Ofertas de verdade:** promoção, produto chegando, aviso da loja.
- **Horário comercial:** entre 9h e 20h.
- **Texto curto e claro**, sempre com o nome da loja.
- O aviso da primeira vez e o botão **Silenciar ofertas** existem para cumprir a
  LGPD e as regras da Play Store. Não tire.

---

## Outra assistência (white label)

Cada marca tem o seu tópico em `app/src/<marca>/res/values/brand.xml`:

```xml
<string name="brand_offers_topic">ofertas-mestrecell</string>
```

Troque para `ofertas-<marca>` e cada loja envia só para os clientes dela.

---

## Problemas comuns

| Problema | Solução |
|---|---|
| Ninguém recebe | Conferir se a versão publicada é a 0.2.0+ **com** o `google-services.json` no projeto |
| Tópico não aparece no console | Normal no começo. Digite `ofertas-mestrecell` mesmo assim |
| Uma pessoa não recebe | Ela negou as notificações, silenciou as ofertas ou o celular não tem os serviços do Google |
| Chega atrasada | Economia de bateria do Android. O Firebase entrega quando o celular libera |
