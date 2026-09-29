# Interface de streaming — versão 0.11

A tela inicial agora usa XML nativo, mantendo catálogo e player do projeto.

## Arquivos

- `app/src/main/res/layout/activity_main.xml`: ConstraintLayout preto; banner centerCrop com altura calculada em 65% da área útil; gradientes; título 36sp; sinopse 14sp de três linhas; Assistir e Mais Informações; RecyclerViews horizontais. A página permite rolagem vertical para que cards altos sejam acessíveis em TVs HD.
- `app/src/main/res/layout/item_anime.xml`: largura de 160dp; CardView com cantos 12dp, proporção `H,2:3`, pôster centerCrop, pílula DUB/LEG e título abaixo, negrito, 14sp e uma linha.
- `app/src/main/res/drawable/bg_gradient_hero.xml`: transparente → preto, de cima para baixo (270 graus).
- `bg_hero_scrim.xml`: sombra lateral para leitura do texto.
- `bg_button_primary.xml`: branco; laranja #FF6600 quando focado/pressionado; raio 4dp.
- `bg_button_secondary.xml`: cinza escuro translúcido; laranja no foco; raio 4dp.
- `bg_audio_pill.xml` e `bg_card_focus.xml`: badge de áudio e foco laranja.
- `app/src/main/res/values/themes.xml`: Theme.KodaAnime herda de Theme.AppCompat.NoActionBar; fundo #0A0A0D.
- `MainActivity.java`: AppCompatActivity; onCreate chama showHome, que infla activity_main e configura LinearLayoutManager.HORIZONTAL; mantém busca, favoritos, histórico e detalhes.
- `AnimeRowAdapter.java`: reciclagem dos cards, eventos de foco e seleção, atualização do banner sem iniciar vídeo automaticamente.

## Configuração já aplicada

Dependências em app/build.gradle:

```gradle
implementation 'androidx.appcompat:appcompat:1.7.0'
implementation 'androidx.constraintlayout:constraintlayout:2.2.1'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
implementation 'androidx.cardview:cardview:1.0.0'
```

MainActivity no Manifest usa `android:theme="@style/Theme.KodaAnime"`.
PlayerActivity mantém seu tema e código anteriores. Não remova as bibliotecas
Media3 nem as permissões/entradas de launcher existentes.

Extraia o ZIP, abra a pasta KodaAnimeTV. Use JDK 17, SDK 35 e Gradle 8.9:

```sh
gradle :app:testDebugUnitTest :app:assembleDebug
```

O APK é gerado em app/build/outputs/apk/debug/app-debug.apk.
O workflow GitHub incluído compila a pasta KodaAnimeTV e executa os testes.

## Foco e comportamento

- Setas navegam nos botões superiores, botões do banner e cards.
- Foco de card atualiza o anime do banner. OK abre a página de detalhes.
- Assistir consulta a lista atual de episódios e abre o episódio em andamento,
  quando encontrado no histórico; caso contrário, abre o primeiro disponível.
- Mais Informações abre a página de temporadas, áudio e episódios.
- O player não foi reescrito; resolução e controles mantêm a implementação 0.10.
- A correção de filas e cache persistente do catálogo permanece.

## Limites visuais e teste

Este layout implementa as especificações fornecidas; não foi validado como
cópia pixel a pixel de Netflix/Crunchyroll. A fonte atual disponibiliza pôsteres,
não um backdrop horizontal separado. Por isso o banner usa o pôster recortado
com centerCrop: não distorce a imagem, mas pode cortar partes da ilustração.
Para maior fidelidade, forneça uma URL de backdrop horizontal por anime.

O banner ocupa 65% da área útil inicial. Cards 160x240dp mais título não cabem
no restante de uma TV com UI 540dp; a página rola verticalmente, mantendo a
proporção. Os títulos e os cards não são comprimidos para tentar caber.

Valide no controle: botão Assistir, Mais Informações, todas as fileiras,
retorno dos detalhes, navegação nas extremidades e reprodução. Compilação não
substitui validação em Android TV físico.
