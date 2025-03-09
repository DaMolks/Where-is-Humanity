package com.whereishumanity.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.whereishumanity.WhereIsHumanity;
import com.whereishumanity.worldgen.structures.StructureType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Commande pour générer des villages/villes
 */
public class GenerateCommand {

    // Définition des types de zones urbaines
    public enum UrbanType {
        VILLAGE(8, 8, "residential", "commercial", "infrastructure"),
        CITY(16, 16, "residential", "commercial", "government", "infrastructure"),
        METROPOLIS(24, 24, "residential", "commercial", "government", "infrastructure", "military");
        
        private final int width;
        private final int length;
        private final String[] allowedTypes;
        
        UrbanType(int width, int length, String... allowedTypes) {
            this.width = width;
            this.length = length;
            this.allowedTypes = allowedTypes;
        }
        
        public int getWidth() {
            return width;
        }
        
        public int getLength() {
            return length;
        }
        
        public String[] getAllowedTypes() {
            return allowedTypes;
        }
    }
    
    /**
     * Enregistre la commande dans le dispatcher
     * @param dispatcher Le dispatcher de commandes
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("wih")
                .requires(source -> source.hasPermission(2)) // Niveau op 2 minimum
                .then(Commands.literal("generate")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (UrbanType type : UrbanType.values()) {
                                builder.suggest(type.name().toLowerCase());
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> generateUrban(context, 0, 0)) // Utilise la position du joueur
                        .then(Commands.argument("size", IntegerArgumentType.integer(1, 5))
                            .executes(context -> generateUrban(context, IntegerArgumentType.getInteger(context, "size"), 0))
                            .then(Commands.argument("rotation", IntegerArgumentType.integer(0, 3))
                                .executes(context -> generateUrban(context, 
                                    IntegerArgumentType.getInteger(context, "size"),
                                    IntegerArgumentType.getInteger(context, "rotation") * 90))
                            )
                        )
                    )
                )
        );
    }

    /**
     * Génère une zone urbaine autour de la position donnée
     * @param context Contexte de la commande
     * @param sizeMultiplier Multiplicateur de taille (1-5)
     * @param rotation Rotation globale en degrés (0, 90, 180, 270)
     * @return Code de résultat
     */
    private static int generateUrban(CommandContext<CommandSourceStack> context, int sizeMultiplier, int rotation) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        String typeArg = StringArgumentType.getString(context, "type").toUpperCase();
        
        // Valider le type de zone urbaine
        UrbanType urbanType;
        try {
            urbanType = UrbanType.valueOf(typeArg);
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("Type de zone urbaine invalide: " + typeArg));
            return 0;
        }
        
        // Obtenir la position du joueur comme point de départ
        BlockPos playerPos = player.blockPosition();
        
        // Utiliser un multiplicateur de taille par défaut si non spécifié
        if (sizeMultiplier <= 0) {
            sizeMultiplier = 1;
        }
        
        // Calculer la taille finale de la zone urbaine
        int urbanWidth = urbanType.getWidth() * sizeMultiplier;
        int urbanLength = urbanType.getLength() * sizeMultiplier;
        
        // Convertir la rotation en degrés à un type de rotation Minecraft
        Rotation globalRotation;
        switch (rotation) {
            case 90:
                globalRotation = Rotation.CLOCKWISE_90;
                break;
            case 180:
                globalRotation = Rotation.CLOCKWISE_180;
                break;
            case 270:
                globalRotation = Rotation.COUNTERCLOCKWISE_90;
                break;
            default:
                globalRotation = Rotation.NONE;
                break;
        }
        
        try {
            // Créer une grille pour la zone urbaine
            // La grille est indexée par [x][z] et contient true si la cellule est occupée
            boolean[][] grid = new boolean[urbanWidth][urbanLength];
            
            // Calculer la position de départ (coin nord-ouest de la zone urbaine)
            BlockPos startPos = playerPos.offset(-urbanWidth / 2, 0, -urbanLength / 2);
            
            // Générer les routes principales
            generateRoads(level, startPos, urbanWidth, urbanLength, globalRotation);
            
            // Charger les structures disponibles pour ce type de zone urbaine
            Map<String, List<Path>> availableStructures = new HashMap<>();
            for (String structureType : urbanType.getAllowedTypes()) {
                List<Path> structures = loadAvailableStructures(structureType);
                if (!structures.isEmpty()) {
                    availableStructures.put(structureType, structures);
                }
            }
            
            // Remplir la grille avec des bâtiments
            Random random = new Random();
            for (int x = 0; x < urbanWidth; x++) {
                for (int z = 0; z < urbanLength; z++) {
                    // Sauter si cette cellule est déjà occupée
                    if (grid[x][z]) continue;
                    
                    // Déterminer le type de structure à placer
                    String structureType = determineStructureType(x, z, urbanWidth, urbanLength, urbanType);
                    
                    // Vérifier si des structures de ce type sont disponibles
                    if (!availableStructures.containsKey(structureType) || availableStructures.get(structureType).isEmpty()) {
                        continue;
                    }
                    
                    // Choisir une structure aléatoire du type approprié
                    List<Path> structures = availableStructures.get(structureType);
                    Path structurePath = structures.get(random.nextInt(structures.size()));
                    
                    // Déterminer une rotation aléatoire pour cette structure (0, 90, 180, 270)
                    Rotation structureRotation = Rotation.values()[random.nextInt(4)];
                    
                    // Calculer la position de la structure
                    BlockPos structurePos = startPos.offset(x * 16, 0, z * 16);
                    
                    // Placer la structure
                    boolean placed = placeStructure(level, structurePath, structurePos, 
                            combineRotations(globalRotation, structureRotation));
                    
                    if (placed) {
                        // Marquer cette cellule comme occupée
                        grid[x][z] = true;
                        
                        // TODO: Pour les structures plus grandes, marquer plusieurs cellules
                    }
                }
            }
            
            context.getSource().sendSuccess(() -> Component.literal("Zone urbaine générée avec succès! Type: " + urbanType.name() + 
                    ", Taille: " + urbanWidth + "x" + urbanLength + 
                    (rotation > 0 ? ", Rotation: " + rotation + "°" : "")), true);
            
            return 1;
        } catch (Exception e) {
            WhereIsHumanity.LOGGER.error("Erreur lors de la génération de la zone urbaine", e);
            context.getSource().sendFailure(Component.literal("Erreur lors de la génération de la zone urbaine: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Détermine le type de structure à placer à une position donnée dans la grille
     * @param x Position X dans la grille
     * @param z Position Z dans la grille
     * @param width Largeur totale de la grille
     * @param length Longueur totale de la grille
     * @param urbanType Type de zone urbaine
     * @return Type de structure à placer
     */
    private static String determineStructureType(int x, int z, int width, int length, UrbanType urbanType) {
        String[] allowedTypes = urbanType.getAllowedTypes();
        
        // Distance par rapport au centre (entre 0.0 et 1.0)
        double centerX = width / 2.0;
        double centerZ = length / 2.0;
        double distanceFromCenter = Math.sqrt(Math.pow((x - centerX) / centerX, 2) + Math.pow((z - centerZ) / centerZ, 2));
        
        // Probabilités en fonction de la distance du centre
        Random random = new Random();
        double randomValue = random.nextDouble();
        
        if (urbanType == UrbanType.METROPOLIS) {
            // Centre = commercial/gouvernemental, périphérie = résidentiel
            if (distanceFromCenter < 0.3) {
                // Centre-ville - principalement commercial et gouvernemental
                if (randomValue < 0.6) return "commercial";
                else if (randomValue < 0.9) return "government";
                else return "infrastructure";
            } else if (distanceFromCenter < 0.7) {
                // Zone intermédiaire - mixte
                if (randomValue < 0.4) return "commercial";
                else if (randomValue < 0.7) return "residential";
                else if (randomValue < 0.9) return "government";
                else return "infrastructure";
            } else {
                // Périphérie - principalement résidentiel
                if (randomValue < 0.7) return "residential";
                else if (randomValue < 0.8) return "commercial";
                else if (randomValue < 0.9) return "military";
                else return "infrastructure";
            }
        } else if (urbanType == UrbanType.CITY) {
            // Distribution plus équilibrée pour une ville moyenne
            if (distanceFromCenter < 0.4) {
                // Centre-ville
                if (randomValue < 0.5) return "commercial";
                else if (randomValue < 0.8) return "government";
                else return "infrastructure";
            } else {
                // Périphérie
                if (randomValue < 0.7) return "residential";
                else if (randomValue < 0.9) return "commercial";
                else return "infrastructure";
            }
        } else {
            // Village - principalement résidentiel
            if (randomValue < 0.7) return "residential";
            else if (randomValue < 0.9) return "commercial";
            else return "infrastructure";
        }
    }
    
    /**
     * Génère les routes principales de la zone urbaine
     * @param level Le niveau du serveur
     * @param startPos Position de départ
     * @param width Largeur de la zone urbaine
     * @param length Longueur de la zone urbaine
     * @param rotation Rotation globale
     */
    private static void generateRoads(ServerLevel level, BlockPos startPos, int width, int length, Rotation rotation) {
        // Générer les routes principales en forme de grille
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                // Si c'est le bord d'un block de cellule (tous les 16 blocs), placer une route
                if (x % 16 == 0 || z % 16 == 0) {
                    BlockPos roadPos = startPos.offset(x, 0, z);
                    
                    // Appliquer la rotation globale à la position
                    roadPos = applyRotation(roadPos, startPos.offset(width / 2, 0, length / 2), rotation);
                    
                    // Placer un bloc de route (ici, nous utilisons la dalle d'asphalte/béton)
                    level.setBlock(roadPos, Blocks.GRAY_CONCRETE.defaultBlockState(), 3);
                    
                    // Ajouter des variations pour les intersections
                    if (x % 16 == 0 && z % 16 == 0) {
                        // Intersection
                        level.setBlock(roadPos, Blocks.BLACK_CONCRETE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
    
    /**
     * Charge toutes les structures disponibles d'un type donné
     * @param structureType Type de structure (dossier)
     * @return Liste des chemins vers les fichiers de structure
     */
    private static List<Path> loadAvailableStructures(String structureType) throws IOException {
        Path structuresDir = Paths.get("config", WhereIsHumanity.MOD_ID, "structures", structureType.toLowerCase());
        if (!Files.exists(structuresDir)) {
            return Collections.emptyList();
        }
        
        List<Path> structures = new ArrayList<>();
        try (Stream<Path> paths = Files.list(structuresDir)) {
            paths.filter(path -> path.toString().endsWith(".nbt"))
                 .forEach(structures::add);
        }
        
        return structures;
    }
    
    /**
     * Place une structure à partir d'un fichier NBT
     * @param level Le niveau du serveur
     * @param structurePath Chemin vers le fichier NBT
     * @param pos Position où placer la structure
     * @param rotation Rotation à appliquer
     * @return true si la structure a été placée avec succès
     */
    private static boolean placeStructure(ServerLevel level, Path structurePath, BlockPos pos, Rotation rotation) {
        try {
            // Paramètres de placement
            StructurePlaceSettings placeSettings = new StructurePlaceSettings()
                    .setRotation(rotation)
                    .setMirror(Mirror.NONE)
                    .setIgnoreEntities(false);
            
            // Charger la structure depuis le fichier NBT
            CompoundTag nbt = NbtIo.readCompressed(structurePath.toFile());
            StructureTemplateManager templateManager = level.getStructureManager();
            StructureTemplate template = templateManager.readStructure(nbt);
            
            if (template == null) {
                WhereIsHumanity.LOGGER.error("Échec du chargement de la structure: {}", structurePath);
                return false;
            }
            
            // Placer la structure
            template.placeInWorld(level, pos, pos, placeSettings, level.random, 2);
            
            return true;
        } catch (Exception e) {
            WhereIsHumanity.LOGGER.error("Erreur lors du placement de la structure: {}", structurePath, e);
            return false;
        }
    }
    
    /**
     * Applique une rotation à une position par rapport à un point central
     * @param pos Position à transformer
     * @param center Centre de rotation
     * @param rotation Rotation à appliquer
     * @return Position transformée
     */
    private static BlockPos applyRotation(BlockPos pos, BlockPos center, Rotation rotation) {
        // Si pas de rotation, retourner la position d'origine
        if (rotation == Rotation.NONE) {
            return pos;
        }
        
        // Calculer les coordonnées relatives au centre
        int relX = pos.getX() - center.getX();
        int relZ = pos.getZ() - center.getZ();
        
        // Appliquer la rotation
        int newRelX, newRelZ;
        switch (rotation) {
            case CLOCKWISE_90:
                newRelX = -relZ;
                newRelZ = relX;
                break;
            case CLOCKWISE_180:
                newRelX = -relX;
                newRelZ = -relZ;
                break;
            case COUNTERCLOCKWISE_90:
                newRelX = relZ;
                newRelZ = -relX;
                break;
            default:
                return pos;
        }
        
        // Recalculer les coordonnées absolues
        return new BlockPos(center.getX() + newRelX, pos.getY(), center.getZ() + newRelZ);
    }
    
    /**
     * Combine deux rotations
     * @param rotation1 Première rotation
     * @param rotation2 Deuxième rotation
     * @return Rotation combinée
     */
    private static Rotation combineRotations(Rotation rotation1, Rotation rotation2) {
        // Convertir les rotations en degrés
        int degrees1 = rotationToDegrees(rotation1);
        int degrees2 = rotationToDegrees(rotation2);
        
        // Additionner les degrés et normaliser
        int combinedDegrees = (degrees1 + degrees2) % 360;
        
        // Convertir les degrés en rotation
        return degreesToRotation(combinedDegrees);
    }
    
    /**
     * Convertit une rotation en degrés
     * @param rotation Rotation à convertir
     * @return Degrés équivalents
     */
    private static int rotationToDegrees(Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_90:
                return 90;
            case CLOCKWISE_180:
                return 180;
            case COUNTERCLOCKWISE_90:
                return 270;
            default:
                return 0;
        }
    }
    
    /**
     * Convertit des degrés en rotation
     * @param degrees Degrés à convertir
     * @return Rotation équivalente
     */
    private static Rotation degreesToRotation(int degrees) {
        switch (degrees) {
            case 90:
                return Rotation.CLOCKWISE_90;
            case 180:
                return Rotation.CLOCKWISE_180;
            case 270:
                return Rotation.COUNTERCLOCKWISE_90;
            default:
                return Rotation.NONE;
        }
    }
}