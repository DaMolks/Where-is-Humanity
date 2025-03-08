# Système de génération urbaine

Ce document explique le fonctionnement du système de génération des villes abandonnées dans le mod "Where is Humanity".

## Principe de la grille urbaine

Le système est basé sur une grille orthogonale (ou damier) similaire aux plans urbains américains. Cette approche offre plusieurs avantages :

1. **Organisation claire** - Les villes ont une structure facilement compréhensible
2. **Performance optimisée** - La génération par blocs est plus rapide qu'une génération organique complexe
3. **Flexibilité de taille** - Fonctionne aussi bien pour des métropoles que pour des villages
4. **Facilité d'extension** - Nouveaux bâtiments s'intègrent facilement dans la grille

## Structure de la grille

La grille urbaine est composée de :

- **Cellules** : Carrés de taille égale (généralement 16×16 blocs) qui contiennent un bâtiment ou une autre structure
- **Routes** : Situées entre les cellules, formant un réseau orthogonal
- **Intersections** : Points de jonction des routes où se trouvent souvent des feux de signalisation, panneaux, etc.

## Hiérarchie des routes

Le système utilise plusieurs types de routes pour créer une ville réaliste :

1. **Autoroutes** (`HIGHWAY`) : Routes les plus larges, entourant généralement la ville ou la traversant
2. **Boulevards** (`BOULEVARD`) : Routes principales à l'intérieur de la ville, plus larges que les rues
3. **Rues** (`STREET`) : Routes standard qui forment le gros du réseau routier
4. **Ruelles** (`ALLEY`) : Petits passages, généralement en arrière des bâtiments

## États de dégradation

Pour créer l'ambiance post-apocalyptique, les routes et structures peuvent être dans différents états :

- **Intact** (`INTACT`) : État d'origine, peu commun
- **Légèrement endommagé** (`SLIGHTLY_DAMAGED`) : Fissures, débris mineurs
- **Gravement endommagé** (`HEAVILY_DAMAGED`) : Trous, effondrement partiel
- **Détruit** (`DESTROYED`) : Impraticable, totalement effondré

La fréquence de chaque état dépend du type de biome (plus de dégâts dans les villages, moins dans les métropoles plus robustes).

## Zonage urbain

La ville est divisée en zones selon la distance au centre :

1. **Centre-ville** (`DOWNTOWN`) : Gratte-ciels, bureaux, bâtiments gouvernementaux
2. **Zone commerciale** (`COMMERCIAL`) : Magasins, centres commerciaux, bureaux
3. **Zone résidentielle** (`RESIDENTIAL`) : Appartements, maisons
4. **Zone rurale** (`RURAL`) : Maisons clairsemées en périphérie
5. **Parcs** (`PARK`) : Espaces verts, plus fréquents en périphérie
6. **Espaces vides** (`EMPTY`) : Terrains vagues, parkings
7. **Zones spéciales** (`SPECIAL`) : Bâtiments uniques (commissariats, hôpitaux, etc.)

## Processus de génération

Le générateur suit ces étapes pour créer une ville :

1. **Initialisation de la grille** : Création d'une grille vide de taille définie
2. **Génération du réseau routier principal** : Placement des boulevards et autoroutes
3. **Génération des routes secondaires** : Ajout des rues locales
4. **Application des dommages** : Dégradation aléatoire des routes pour créer l'aspect abandonné
5. **Zonage** : Attribution des types de zones selon la distance au centre
6. **Placement des bâtiments** : Sélection des structures appropriées pour chaque cellule
7. **Placement des bâtiments spéciaux** : Ajout de structures uniques comme les commissariats et hôpitaux
8. **Détails finaux** : Ajout de détails comme la végétation envahissante, débris, etc.

## Types de biomes urbains

Le système prend en charge trois types de biomes urbains, chacun avec ses particularités :

### Métropole (`METROPOLIS`)
- Taille typique : 16×16 à 32×32 cellules
- Densité de gratte-ciels : Élevée
- Fréquence des boulevards : Tous les 3 blocs
- Présence d'autoroutes : Oui (périphérique et traversantes)
- Dégradation moyenne : Légère (infrastructures plus robustes)
- Bâtiments spéciaux : Nombreux (3 commissariats, 2-3 hôpitaux, bases militaires)

### Ville moyenne (`CITY`)
- Taille typique : 12×12 à 20×20 cellules
- Densité de gratte-ciels : Moyenne (centre-ville uniquement)
- Fréquence des boulevards : Tous les 4 blocs
- Présence d'autoroutes : Occasionnelle (périphérique uniquement)
- Dégradation moyenne : Modérée
- Bâtiments spéciaux : Standard (1-2 commissariats, 1 hôpital)

### Village (`VILLAGE`)
- Taille typique : 8×8 à 16×16 cellules
- Densité de gratte-ciels : Nulle
- Fréquence des boulevards : Tous les 6 blocs
- Présence d'autoroutes : Non
- Dégradation moyenne : Élevée (infrastructures moins solides)
- Bâtiments spéciaux : Limités (1 commissariat, parfois 1 hôpital)

## Structures multi-blocs

Certains bâtiments s'étendent sur plusieurs cellules de la grille, comme :

- **Barres d'immeubles** (`APARTMENT_COMPLEX`) : 3×1 cellules
- **Centres commerciaux** (`MALL`) : 4×4 cellules
- **Hôpitaux** (`HOSPITAL`) : 2×3 cellules
- **Bases militaires** (`MILITARY_BASE`) : 4×4 cellules

Ces structures sont placées en vérifiant la disponibilité de l'espace requis dans la grille, et toutes les cellules occupées par une même structure sont liées entre elles.

## Orientation des structures

Les structures sont orientées par rapport aux routes adjacentes :

- L'entrée principale d'un bâtiment est généralement placée face à la route la plus importante
- Pour les structures de coin, l'entrée est placée à l'angle
- Les barres d'immeubles sont orientées parallèlement aux routes principales
- Les bâtiments sans accès direct à une route sont orientés vers la route la plus proche

## Système de templates

Les bâtiments sont stockés sous forme de templates dans le dossier `config/whereishumanity/structures/`, organisés par catégorie :

- `residential/` : Structures résidentielles
- `commercial/` : Structures commerciales
- `government/` : Bâtiments administratifs et services publics
- `military/` : Structures militaires
- `infrastructure/` : Infrastructures (routes, ponts, etc.)
- `misc/` : Divers (parcs, parkings, etc.)

Chaque template est composé d'un fichier `.nbt` (structure) et d'un fichier `.json` (métadonnées) qui contient notamment la position de l'entrée pour l'orientation.

## Création de templates personnalisés

De nouveaux templates peuvent être créés à l'aide de la commande `/wih structure` :

1. `/wih structure record <type> <name>` - Définit la zone de construction
2. `/wih structure setentrance` - Marque la position de l'entrée
3. `/wih structure save` - Sauvegarde la structure

Le système de templates étant flexible, il est facile d'ajouter de nouveaux bâtiments sans modifier le code du générateur.

## Optimisation et performance

Le système de génération est optimisé pour minimiser l'impact sur les performances :

- Les structures sont générées uniquement lorsqu'un chunk est chargé
- Le plan global de la ville est déterminé une seule fois et mis en cache
- Des structures pré-calculées sont utilisées pour les éléments répétitifs
- Les détails visuels (débris, fissures) sont ajoutés à la volée lors de la génération finale

## Interaction avec les autres systèmes

Le système de génération urbaine interagit avec d'autres systèmes du mod :

- **Système de loot** : Distribution des ressources dans les bâtiments selon leur type
- **Système de spawn des zombies** : Densité variable selon le type de zone
- **Système de détection sonore** : Propagation du son différente selon la densité urbaine