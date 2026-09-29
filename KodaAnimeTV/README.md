# Koda Anime TV — versão de teste 0.12

Projeto Android TV e celular em Java. Visual preto/branco/vermelho inspirado em `design/referencia-aprovada.png` (a imagem é apenas referência; a interface é nativa e interativa). Navegação com D-pad, catálogo, pesquisa com filtros, favoritos, histórico local, retomada, player e vinculação Google por código.

## Gerar APK

Abra esta pasta no Android Studio com JDK 17, Android SDK 35 e conexão para baixar Gradle/Media3. Execute `:app:assembleDebug` ou **Build > Build APK(s)**. Arquivo: `app/build/outputs/apk/debug/app-debug.apk`.

Requisitos: JDK 17, Android SDK 35 e Gradle 8.9. O projeto não inclui Gradle Wrapper: instale Gradle 8.9 e execute `gradle :app:testDebugUnitTest :app:assembleDebug` nesta pasta, ou configure essa instalação no Android Studio. No Windows, `BUILD-WINDOWS.bat` usa o Gradle instalado no PATH.

O ZIP também inclui `.github/workflows/koda-anime-tv.yml` na raiz externa, junto da pasta `KodaAnimeTV`. Mantenha essa estrutura no repositório para compilar pelo GitHub Actions; execute manualmente em Actions > Koda Anime TV APK. A compilação automática usa a branch `koda-anime-tv-test`.

Instale em Android TV/Google TV/Mi Stick (Android TV OS) ou em um celular Android para testar. No celular a interface abre em paisagem e aparece no launcher normal. Telas Samsung Tizen e LG webOS não executam APK. O ícone é 512 px; o banner da TV está em `res/drawable/tv_banner.png`.

## Como testar

1. Início carrega animes conhecidos e episódios recentes do catálogo de `animestvs.org`. Pesquisa e categorias percorrem as 8 mil+ entradas do catálogo em páginas de 40 e mostram títulos adicionais do Kitsu como metadados. Abra um card para a página própria com capa e sinopse; as temporadas, idiomas e episódios ficam agrupados em um painel único. A fonte de vídeo usa `/animes-dublados/{titulo}/episodios` ou `/animes-legendados/{titulo}/episodios`. Temporada é inferida do título quando a fonte não informa campo próprio. Especiais e filmes têm identificação distinta.
2. Abra **Configurações > Verificar player**. Big Buck Bunny é um vídeo demonstrativo; confirme play/pause, voltar/avançar 10 segundos, qualidade e pular abertura (+90 segundos quando não houver marcação). Os controles somem após alguns segundos. Você também pode informar uma URL HTTPS autorizada de MP4/HLS. A lista de resoluções aparece apenas quando o vídeo tem múltiplas faixas; MP4 único mantém a qualidade original.
3. Em **Configurações**, informe URL HTTPS de instâncias próprias/permitidas dos projetos `api-animesonline-cc` e/ou `SugoiAPI`. Sem URLs, o app não tenta obter episódios. Abra um anime e selecione **Buscar episódios nas fontes**. A fonte 1 tenta listar episódios e resolve o selecionado; a fonte 2 tenta o episódio 1. URLs MP4/HLS reconhecidas são passadas ao Media3. Os repositórios não fornecem um serviço hospedado estável; se retornarem outros campos, ajuste `Sources.java`. Não inclua credenciais/segredos das fontes no app.
4. Favoritos são locais ao aparelho e separados pelo ID da conta Google vinculada. Vincular Google **não transfere a conta do Super Animes nem sincroniza favoritos entre TVs**.

## Vincular Google na TV

Crie em seu projeto Google Cloud um OAuth client do tipo **TVs e dispositivos com entrada limitada**, configure a tela de consentimento e copie o `client_id` e `client_secret`. No app, **Configurações > Vincular conta por código**: informe os dados, abra no celular o endereço mostrado na TV e digite o código. O app solicita apenas `email profile`, respeita intervalo/expiração e lê o perfil do Google. O `client_secret` é usado só em memória durante a vinculação e não é salvo em preferências. O ID do cliente e nome/ID de usuário ficam salvos localmente; tokens não são persistidos. Para produção, use um backend apropriado para validar a identidade, persistir usuários e sincronizar favoritos com segurança.

**Não use o endereço de ativação de outro aplicativo.** O código mostrado é emitido para o seu próprio projeto OAuth.

## Limitações desta versão

- A lista de episódios diretos é consultada pelo título do anime. Algumas versões especiais têm apenas 1–2 episódios; as temporadas relacionadas são listadas separadamente. O Kitsu fornece metadados, não vídeos. Quando a fonte não disponibilizar uma lista para um título, uma fonte configurada pode oferecer episódios adicionais. A disponibilidade depende das fontes e das permissões de uso do conteúdo.
- Nenhum servidor externo de episódios pôde ser testado a partir deste ambiente. As integrações são adaptadores iniciais que podem precisar de ajuste ao JSON real. Login Google real depende das credenciais do projeto do usuário e também não foi testado aqui.
- A tela inicial é fiel à estrutura aprovada, mas usa capas dinâmicas dos animes retornados pelo catálogo. A imagem de referência contém títulos fictícios.
- O vídeo de teste Big Buck Bunny é do Blender Foundation, publicado em licença Creative Commons Attribution 3.0.

## Atualização 0.7

Home com proporções baseadas na referência aprovada, ícones desenhados e foco vermelho. Catálogo mundial Kitsu navegável por popularidade e busca, com paginação de 20 em 20 sem limite fixo no app; fornece informações, não vídeos. Recomendações, recentes e temporadas sem os limites artificiais anteriores. Mais categorias e quatro cards por linha na pesquisa. Compilação validada pelo workflow Koda Anime TV APK; aparência e controle remoto ainda precisam ser validados no aparelho.

## Atualização 0.8

Ajuste automático pela largura e altura disponíveis, mantendo proporção 16:9. Margens de segurança de 3% por lado em TV/TV Box sem touchscreen e 1% em celulares. Interface centralizada; textos e controles do player usam a mesma escala. Vídeo continua preenchendo a área do player, respeitando a proporção original. Não identifica polegadas físicas. Recalcula ao recriar a tela por alteração de configuração.

## Atualização 0.10 — catálogo

Cache persistente de catálogo e recomendações por uma hora, leitura e escrita fora da interface. Em falha da fonte, permite reaproveitar metadados de até sete dias (podem estar desatualizados). Episódios, URLs de vídeos, recentes e dados de conta não são gravados nesse cache. Mantidas as filas separadas de imagens/API, o limite de espera e a rejeição de respostas de telas antigas da 0.9. PlayerActivity.java permanece byte a byte igual à versão 0.9. A primeira abertura ainda depende da velocidade da API.

Testes automatizados cobrem persistência após recriação do cache, expiração, substituição de dados e exclusão de URLs de reprodução/conta. Validação de reprodução e navegação em aparelho físico ainda necessária.

## Interface 0.11

Tela inicial migrada para XML com hero banner e RecyclerView horizontal. Consulte INTERFACE-TV.md para mapa de arquivos, dependências, navegação e limites. Player mantido sem alteração.

## Identidade visual 0.12

Home azul da meia-noite, hero 60%, botões em degradê laranja/vermelho, cards 150dp e proporção 2:3, raio 8dp e título externo 13sp. Player preservado. Instruções completas em INTERFACE-TV.md.
