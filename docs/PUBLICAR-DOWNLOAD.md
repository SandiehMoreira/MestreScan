# Publicar o MestreScan para download no site

Guia completo para gerar o APK final e colocar o botão **"Baixar MestreScan"** no
site da MestreCell (`~/Documents/Site-MestreCell`).

## Como funciona (visão geral)

```
1. Criar a chave da MestreCell (uma vez só, guardar para sempre)
2. Gerar o APK final assinado  ─┐
3. Publicar como release no GitHub ─┴─ o script faz os dois: scripts/publicar-release.sh
4. Botão no site aponta para um link fixo que sempre baixa a versão mais nova
```

Link fixo de download (não muda entre versões):

```
https://github.com/SandiehMoreira/MestreScan/releases/latest/download/MestreScan.apk
```

O repositório é público, então esse link funciona para qualquer pessoa.

---

## O que precisa estar instalado

Tudo já está neste Mac:

| Ferramenta | Onde |
|---|---|
| Java 17 | `~/android-dev/jdk` |
| Android SDK | `~/android-dev/sdk` |
| GitHub CLI (`gh`) | já logado na conta **SandiehMoreira** |

Em todos os comandos abaixo, abra o Terminal na pasta do projeto:

```sh
cd ~/Documents/MestreScan
export JAVA_HOME=~/android-dev/jdk/Contents/Home
```

---

## Passo 1 — Criar a chave da MestreCell (só na primeira vez)

A chave "assina" o APK e prova que ele foi feito pela MestreCell.

> ⚠️ **MUITO IMPORTANTE**
> - Sem essa chave, **não dá para lançar atualizações**: o Android recusa instalar
>   uma versão nova assinada com outra chave por cima da antiga.
> - Faça **backup** do arquivo `.jks` e das senhas (pendrive + Google Drive, por exemplo).
> - **Nunca** coloque a chave no GitHub. O `.gitignore` já bloqueia `*.jks` e
>   `keystore.properties`.

**1.1 — Gerar a chave** (fica guardada fora da pasta do projeto):

```sh
mkdir -p ~/chaves-mestrecell
$JAVA_HOME/bin/keytool -genkeypair -v \
  -keystore ~/chaves-mestrecell/mestrescan.jks \
  -alias mestrescan \
  -keyalg RSA -keysize 2048 -validity 10000
```

Ele pede:
- uma **senha** (anote!);
- nome, empresa, cidade: pode preencher `MestreCell`, `Curitiba`, `PR`, `BR`;
- confirmação `sim`.

**1.2 — Criar o arquivo `keystore.properties`** na pasta do projeto
(troque `SUA_SENHA` pela senha que você criou):

```sh
cat > keystore.properties <<EOF
storeFile=$HOME/chaves-mestrecell/mestrescan.jks
storePassword=SUA_SENHA
keyAlias=mestrescan
keyPassword=SUA_SENHA
EOF
```

**1.3 — Conferir que o git está ignorando a chave:**

```sh
git status --short
```

`keystore.properties` **não** pode aparecer na lista.

---

## Passo 2 — Definir a versão

Abra `app/build.gradle.kts` e ajuste:

```kotlin
versionCode = 1        // número inteiro, SEMPRE aumentar a cada publicação (1, 2, 3...)
versionName = "0.1.0"  // o que o cliente vê (0.1.0, 0.2.0, 1.0.0...)
```

- **Primeira publicação:** pode deixar como está (`1` / `"0.1.0"`).
- **Cada atualização:** some 1 no `versionCode` e mude o `versionName`.

Depois faça o commit e envie para o GitHub:

```sh
git add -A
git commit -m "Versão 0.1.0"
git push
```

---

## Passo 3 — Gerar e publicar (um comando)

```sh
./scripts/publicar-release.sh "Primeira versão do MestreScan"
```

O script:
1. confere se a chave existe e se o código está igual ao do GitHub;
2. gera o APK final assinado (`assembleMestrecellRelease`);
3. confere a assinatura;
4. cria a release `v0.1.0` no GitHub com o arquivo **`MestreScan.apk`**;
5. mostra o link fixo de download.

Confira em: https://github.com/SandiehMoreira/MestreScan/releases

<details>
<summary>Fazer à mão, sem o script</summary>

```sh
./gradlew clean assembleMestrecellRelease
~/android-dev/sdk/build-tools/35.0.0/apksigner verify \
  app/build/outputs/apk/mestrecell/release/app-mestrecell-release.apk
mkdir -p release
cp app/build/outputs/apk/mestrecell/release/app-mestrecell-release.apk release/MestreScan.apk
gh release create v0.1.0 release/MestreScan.apk --title "MestreScan 0.1.0" --notes "Primeira versão"
```

O nome do arquivo precisa ser exatamente **`MestreScan.apk`**, senão o link fixo não funciona.
</details>

---

## Passo 4 — Colocar o botão no site

O site fica em `~/Documents/Site-MestreCell` (Next.js + Tailwind).

### 4.1 — Adicionar o link em `src/lib/site-config.ts`

Dentro de `siteConfig`, acrescente:

```ts
  mestrescanDownloadUrl:
    "https://github.com/SandiehMoreira/MestreScan/releases/latest/download/MestreScan.apk",
```

### 4.2 — Criar `src/components/sections/MestreScanSection.tsx`

Usa as mesmas cores e componentes do site (dourado, `ScrollReveal`, ícones lucide):

```tsx
"use client";

import { ShieldCheck, Download, MessageCircle } from "lucide-react";
import ScrollReveal from "@/components/ScrollReveal";
import { siteConfig, whatsappLink } from "@/lib/site-config";

const steps = [
  "Toque em Baixar MestreScan e abra o arquivo baixado.",
  "Se o celular pedir, permita instalar apps desta fonte.",
  "Abra o MestreScan e toque em Escanear.",
];

export default function MestreScanSection() {
  return (
    <section id="mestrescan" className="relative py-24 sm:py-32">
      <div className="mx-auto max-w-7xl px-5 sm:px-8">
        <div className="overflow-hidden rounded-3xl border border-border bg-gradient-to-br from-bg-card via-bg-card to-bg p-8 sm:p-12 lg:p-16">
          <ScrollReveal className="flex flex-col items-start gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-gold-100 via-gold-300 to-gold-700">
              <ShieldCheck size={22} className="text-ink" strokeWidth={2.2} />
            </div>
            <span className="text-sm font-semibold uppercase tracking-widest text-gold-500">
              App grátis para Android
            </span>
            <h2 className="max-w-2xl text-3xl font-semibold tracking-tight sm:text-4xl lg:text-5xl">
              Está com vírus de propaganda? O Mestre resolve.
            </h2>
            <p className="max-w-xl text-base leading-relaxed text-muted">
              O MestreScan encontra os apps que enchem o celular de propaganda, mostra
              onde estão e ajuda você a remover. Você decide o que apagar.
            </p>

            <ol className="mt-2 flex flex-col gap-2 text-sm text-muted">
              {steps.map((step, i) => (
                <li key={step}>
                  <span className="font-semibold text-gold-500">{i + 1}.</span> {step}
                </li>
              ))}
            </ol>

            <div className="mt-4 flex flex-wrap gap-3">
              <a
                href={siteConfig.mestrescanDownloadUrl}
                className="group inline-flex items-center gap-2 rounded-full bg-gradient-to-br from-gold-100 via-gold-300 to-gold-700 px-7 py-3.5 text-sm font-semibold text-ink transition-transform hover:scale-[1.02] sm:text-base"
              >
                <Download size={18} /> Baixar MestreScan
              </a>
              <a
                href={whatsappLink("Olá! Baixei o MestreScan e preciso de ajuda com propaganda no celular.")}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 rounded-full border border-gold-700/40 px-7 py-3.5 text-sm font-semibold transition-transform hover:scale-[1.02] sm:text-base"
              >
                <MessageCircle size={18} /> Não resolveu? Fale com o Mestre
              </a>
            </div>
            <p className="text-xs text-muted">
              Somente Android 8 ou mais novo. Não funciona em iPhone.
            </p>
          </ScrollReveal>
        </div>
      </div>
    </section>
  );
}
```

> Cores e estilo de botão iguais aos de `SoftwareSection.tsx` e `Hero.tsx`
> (conferidos em `src/app/globals.css`).

### 4.3 — Mostrar a seção na página: `src/app/page.tsx`

```tsx
import MestreScanSection from "@/components/sections/MestreScanSection";
// ...
        <SoftwareSection />
        <MestreScanSection />   {/* nova */}
        <GoogleReviews />
```

### 4.4 — (Opcional) Link no menu: `navLinks` em `src/lib/site-config.ts`

```ts
  { label: "App Anti-Vírus", href: "#mestrescan" },
```

### 4.5 — Testar e publicar o site

```sh
cd ~/Documents/Site-MestreCell
npm run dev        # abrir http://localhost:3000 e testar o botão
npm run build      # conferir que compila
git add -A && git commit -m "Seção de download do MestreScan" && git push
```

Depois publique o site do jeito de sempre (a hospedagem que você já usa).

---

## Passo 5 — (Opcional) QR code no balcão

Gere um QR code com o link fixo (qualquer gerador de QR serve) e imprima para o
balcão: *"Proteja seu celular — baixe o MestreScan"*. Como o link não muda, o QR
continua valendo em todas as versões.

---

## O que o cliente vai ver ao instalar

Por ser APK fora da Play Store, é normal aparecer:

| Aviso | O que fazer |
|---|---|
| "Por segurança, seu celular não permite instalar apps desta fonte" | Tocar em **Configurações** → ativar **Permitir desta fonte** → voltar |
| "Arquivo pode ser prejudicial" (Chrome) | **Baixar mesmo assim** |
| Play Protect: "App não reconhecido" | **Mais detalhes** → **Instalar mesmo assim** |

Esses avisos somem quando o app estiver na Play Store (etapa futura).

Depois de instalar, peça para o cliente:
1. abrir o MestreScan e tocar em **Escanear**;
2. tocar em **Ativar agora** (acesso ao uso), para a detecção de propaganda ao ligar;
3. permitir as **notificações**.

---

## Lançar uma atualização (resumo)

```sh
cd ~/Documents/MestreScan
export JAVA_HOME=~/android-dev/jdk/Contents/Home
# 1. aumentar versionCode e versionName em app/build.gradle.kts
git add -A && git commit -m "Versão 0.2.0" && git push
./scripts/publicar-release.sh "O que mudou nesta versão"
```

O site **não precisa mudar**: o link fixo passa a baixar a versão nova.
Quem já tem o app instala por cima, sem perder nada.

---

## Problemas comuns

| Problema | Solução |
|---|---|
| `falta keystore.properties` | Fazer o Passo 1.2 |
| `há alterações sem commit` / `código diferente do GitHub` | `git add -A && git commit -m "..." && git push` |
| `a versão vX já foi publicada` | Aumentar `versionCode` e `versionName` (Passo 2) |
| Cliente: "App não instalado" ao atualizar | O APK antigo foi assinado com outra chave. Desinstalar o antigo e instalar o novo. Use sempre a mesma chave. |
| `gh: not logged in` | `gh auth login` |
| Perdi a chave | Não tem como recuperar. Crie outra; os clientes vão precisar desinstalar e instalar de novo. Por isso o backup. |

---

## Checklist rápido

- [ ] Chave criada em `~/chaves-mestrecell/mestrescan.jks` **e com backup**
- [ ] `keystore.properties` criado (e **fora** do git)
- [ ] Versão ajustada, commit e push
- [ ] `./scripts/publicar-release.sh "..."` rodou sem erro
- [ ] Link fixo baixa o APK no celular
- [ ] Seção do MestreScan no site, testada com `npm run dev`
- [ ] Site publicado
