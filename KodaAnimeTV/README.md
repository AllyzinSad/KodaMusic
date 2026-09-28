# Koda Anime TV — versão de teste 0.2

Projeto Android TV em Java. Visual preto/branco/vermelho inspirado em `design/referencia-aprovada.png` (a imagem é apenas referência; a interface é nativa e interativa). Navegação com D-pad, catálogo, pesquisa com filtros, favoritos, player e vinculação Google por código.

## Gerar APK

Abra esta pasta no Android Studio com JDK 17, Android SDK 35 e conexão para baixar Gradle/Media3. Execute `:app:assembleDebug` ou **Build > Build APK(s)**. Arquivo: `app/build/outputs/apk/debug/app-debug.apk`.

Também há `.github/workflows/android.yml`. Envie o conteúdo desta pasta para um repositório GitHub, execute **Actions > Android TV APK > Run workflow** e baixe o artefato `KodaAnimeTV-debug-apk`. Este ZIP é o código-fonte, não um APK compilado. Neste ambiente não havia Android SDK/Gradle para testar a compilação.

Instale em Android TV/Google TV/Mi Stick (Android TV OS). Telas Samsung Tizen e LG webOS não executam APK. O ícone é 512 px; o banner da TV está em `res/drawable/tv_banner.png`.

## Como testar

1. Início carrega recomendações (`Jikan /v4/top/anime`) e episódios recentes diretamente do catálogo público de `animestvs.org` (dublados e recentes). Abra um card recente e toque **Assistir agora** para testar um episódio no player. Se as duas listas falharem, os lançamentos voltam ao Jikan sem vídeo. Pesquisa e categorias usam `/v4/anime`.
2. Abra **Configurações > Verificar player**. Big Buck Bunny é um vídeo demonstrativo; confirme play/pause, voltar/avançar 10 segundos, qualidade e pular abertura (+90 segundos quando não houver marcação). Você também pode informar uma URL HTTPS autorizada de MP4/HLS. A lista de resoluções aparece apenas quando o vídeo tem múltiplas faixas; MP4 único mantém a qualidade original.
3. Em **Configurações**, informe URL HTTPS de instâncias próprias/permitidas dos projetos `api-animesonline-cc` e/ou `SugoiAPI`. Sem URLs, o app não tenta obter episódios. Abra um anime e selecione **Buscar episódios nas fontes**. A fonte 1 tenta listar episódios e resolve o selecionado; a fonte 2 tenta o episódio 1. URLs MP4/HLS reconhecidas são passadas ao Media3. Os repositórios não fornecem um serviço hospedado estável; se retornarem outros campos, ajuste `Sources.java`. Não inclua credenciais/segredos das fontes no app.
4. Favoritos são locais ao aparelho e separados pelo ID da conta Google vinculada. Vincular Google **não transfere a conta do Super Animes nem sincroniza favoritos entre TVs**.

## Vincular Google na TV

Crie em seu projeto Google Cloud um OAuth client do tipo **TVs e dispositivos com entrada limitada**, configure a tela de consentimento e copie o `client_id` e `client_secret`. No app, **Configurações > Vincular conta por código**: informe os dados, abra no celular o endereço mostrado na TV e digite o código. O app solicita apenas `email profile`, respeita intervalo/expiração e lê o perfil do Google. O `client_secret` é usado só em memória durante a vinculação e não é salvo em preferências. O ID do cliente e nome/ID de usuário ficam salvos localmente; tokens não são persistidos. Para produção, use um backend apropriado para validar a identidade, persistir usuários e sincronizar favoritos com segurança.

**Não use o endereço de ativação de outro aplicativo.** O código mostrado é emitido para o seu próprio projeto OAuth.

## Limitações desta versão

- Os episódios recentes do serviço público podem ser reproduzidos diretamente quando os links estiverem ativos; recomendações e pesquisa do Jikan fornecem metadados/capas, mas exigem uma fonte de episódios. A disponibilidade depende das fontes e das permissões de uso do conteúdo.
- Nenhum servidor externo de episódios pôde ser testado a partir deste ambiente. As integrações são adaptadores iniciais que podem precisar de ajuste ao JSON real. Login Google real depende das credenciais do projeto do usuário e também não foi testado aqui.
- A tela inicial é fiel à estrutura aprovada, mas usa capas dinâmicas dos animes retornados pelo catálogo. A imagem de referência contém títulos fictícios.
- O vídeo de teste Big Buck Bunny é do Blender Foundation, publicado em licença Creative Commons Attribution 3.0.
