package com.whereishumanity.worldgen.structures;

/**
 * Énumération des différents types de structures pour les biomes urbains
 * Chaque type définit ses dimensions et sa catégorie
 */
public enum StructureType {
    // Bâtiments individuels
    SMALL_HOUSE(7, 7, 8, "residential"),
    MEDIUM_HOUSE(9, 9, 10, "residential"),
    LARGE_HOUSE(12, 12, 15, "residential"),
    APARTMENT_BUILDING(12, 12, 30, "residential"),
    
    // Bâtiments multi-blocks
    APARTMENT_COMPLEX(36, 12, 30, "residential"),      // Barre d'immeuble (3x1 blocs)
    SKYSCRAPER(24, 24, 100, "commercial"),             // Gratte-ciel (2x2 blocs)
    OFFICE_BUILDING(24, 24, 50, "commercial"),         // Immeuble de bureaux (2x2 blocs)
    MALL(48, 48, 20, "commercial"),                    // Centre commercial (4x4 blocs)
    
    // Services publics
    POLICE_STATION(16, 16, 15, "government"),
    FIRE_STATION(16, 20, 15, "government"),
    HOSPITAL(32, 24, 25, "government"),                // Hôpital (2x3 blocs)
    GOVERNMENT_BUILDING(24, 32, 25, "government"),     // Bâtiment administratif (2x2 blocs)
    SCHOOL(32, 32, 15, "government"),                  // École (2x2 blocs)
    
    // Militaire
    MILITARY_OUTPOST(20, 20, 12, "military"),
    MILITARY_BASE(48, 48, 20, "military"),             // Base (4x4 blocs)
    
    // Industriel
    FACTORY(24, 32, 20, "industrial"),                 // Usine (2x2 blocs)
    WAREHOUSE(16, 24, 12, "industrial"),
    
    // Infrastructures
    GAS_STATION(20, 24, 8, "infrastructure"),          // Station-service (ajustée)
    POWER_STATION(16, 16, 15, "infrastructure"),
    WATER_TOWER(10, 10, 25, "infrastructure"),
    
    // Routes et ponts
    ROAD_STRAIGHT(16, 8, 2, "road"),
    ROAD_TURN(16, 16, 2, "road"),
    ROAD_INTERSECTION(16, 16, 2, "road"),
    HIGHWAY_SECTION(16, 16, 10, "road"),
    BRIDGE_SECTION(16, 16, 15, "road"),
    
    // Divers
    PARK(16, 16, 5, "misc"),
    PARKING_LOT(16, 16, 2, "misc"),
    RUINS(10, 10, 6, "misc"),
    
    // Décoration urbaine
    STREET_PROPS(5, 5, 6, "props");
    
    private final int width;  // Largeur (X)
    private final int height; // Hauteur (Y) - Maximum, pas une limite stricte
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