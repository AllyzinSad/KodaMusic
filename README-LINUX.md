# Koda Music 3.11.0 para Linux x64

Base: versão Windows com a atualização aprovada da tela cheia, arte e letras. A interface, busca, biblioteca e player continuam no mesmo código Compose Desktop. O áudio usa o mpv do Linux e controles JSON IPC via socket Unix.

## Requisitos

- Distribuição Linux x64 com ambiente gráfico; Ubuntu 24.04 ou equivalente é a base de teste.
- `mpv` instalado no sistema: `sudo apt update && sudo apt install mpv`.
- Para compilar: JDK 21 com `jpackage`, conexão para dependências Gradle e bibliotecas gráficas da distribuição.
- Para executar o pacote gerado: Java está incluído; `mpv` continua sendo dependência do sistema.

## Testar do código

```bash
./gradlew :desktop:run
```

## Gerar o pacote

```bash
./BUILD-LINUX.sh
```

Extraia `release/KodaMusic-3.11.0-Linux-x64.tar.gz` e abra `KodaMusic-3.11.0-Linux/bin/KodaMusic`. Não mova apenas o executável; mantenha `app/` e `runtime/` ao lado. O pacote Linux não contém funções de download/offline. O login embutido usa JCEF e deve ser testado na distribuição de destino.
