package com.whereishumanity.worldgen.structures;

/**
 * Énumération des différents types de structures pour les biomes urbains
 * Chaque type définit ses dimensions et sa catégorie
 */
public enum StructureType {
    // Bâtiments individuels
    SMALL_HOUSE(7, 6, 7, "residential"),
    MEDIUM_HOUSE(9, 7, 9, "residential"),
    LARGE_HOUSE(12, 8, 12, "residential"),
    APARTMENT_BUILDING(12, 20, 12, "residential"),
    
    // Bâtiments multi-blocks
    APARTMENT_COMPLEX(36, 20, 12, "residential"),      // Barre d'immeuble (3x1 blocs)
    SKYSCRAPER(24, 80, 24, "commercial"),              // Gratte-ciel (2x2 blocs)
    OFFICE_BUILDING(24, 40, 24, "commercial"),         // Immeuble de bureaux (2x2 blocs)
    MALL(48, 15, 48, "commercial"),                    // Centre commercial (4x4 blocs)
    
    // Services publics
    POLICE_STATION(16, 12, 16, "government"),
    FIRE_STATION(16, 12, 16, "government"),
    HOSPITAL(32, 20, 24, "government"),                // Hôpital (2x3 blocs)
    GOVERNMENT_BUILDING(24, 20, 32, "government"),     // Bâtiment administratif (2x2 blocs)
    SCHOOL(32, 12, 32, "government"),                  // École (2x2 blocs)
    
    // Militaire
    MILITARY_OUTPOST(20, 10, 20, "military"),
    MILITARY_BASE(48, 15, 48, "military"),             // Base (4x4 blocs)
    
    // Industriel
    FACTORY(24, 15, 32, "industrial"),                 // Usine (2x2 blocs)
    WAREHOUSE(16, 10, 24, "industrial"),
    
    // Infrastructures
    GAS_STATION(10, 6, 12, "infrastructure"),
    POWER_STATION(16, 12, 16, "infrastructure"),
    WATER_TOWER(10, 20, 10, "infrastructure"),
    
    // Routes et ponts
    ROAD_STRAIGHT(16, 1, 8, "road"),
    ROAD_TURN(16, 1, 16, "road"),
    ROAD_INTERSECTION(16, 1, 16, "road"),
    HIGHWAY_SECTION(16, 8, 16, "road"),
    BRIDGE_SECTION(16, 12, 16, "road"),
    
    // Divers
    PARK(16, 4, 16, "misc"),
    PARKING_LOT(16, 1, 16, "misc"),
    RUINS(10, 5, 10, "misc"),
    
    // Décoration urbaine
    STREET_PROPS(5, 5, 5, "props");
    
    private final int width;  // Largeur (X)
    private final int height; // Hauteur (Y)
    private final int length; // Longueur (Z)
    private final String category; // Catégorie pour l'organisation des fichiers
    
    /**
     * Constructeur pour les types de structures
     * @param width Largeur en blocs
     * @param height Hauteur en blocs
     * @param length Longueur en blocs
     * @param category Catégorie de la structure
     */
    StructureType(int width, int height, int length, String category) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.category = category;
    }
    
    /**
     * Obtient la largeur de la structure
     * @return Largeur en blocs
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Obtient la hauteur de la structure
     * @return Hauteur en blocs
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Obtient la longueur de la structure
     * @return Longueur en blocs
     */
    public int getLength() {
        return length;
    }
    
    /**
     * Obtient la catégorie de la structure
     * @return Nom de la catégorie
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * Vérifie si cette structure est multi-blocks (s'étend sur plusieurs blocks de la grille urbaine)
     * @param gridBlockSize Taille d'un bloc de la grille urbaine
     * @return true si la structure est multi-blocks
     */
    public boolean isMultiBlock(int gridBlockSize) {
        return width > gridBlockSize || length > gridBlockSize;
    }
    
    /**
     * Calcule combien de blocs de la grille urbaine cette structure occupe en largeur
     * @param gridBlockSize Taille d'un bloc de la grille urbaine
     * @return Nombre de blocs de grille en largeur
     */
    public int getGridWidth(int gridBlockSize) {
        return (int) Math.ceil((double) width / gridBlockSize);
    }
    
    /**
     * Calcule combien de blocs de la grille urbaine cette structure occupe en longueur
     * @param gridBlockSize Taille d'un bloc de la grille urbaine
     * @return Nombre de blocs de grille en longueur
     */
    public int getGridLength(int gridBlockSize) {
        return (int) Math.ceil((double) length / gridBlockSize);
    }
}