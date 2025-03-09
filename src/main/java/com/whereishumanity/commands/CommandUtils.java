package com.whereishumanity.commands;

import com.whereishumanity.WhereIsHumanity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Classe utilitaire pour les commandes
 */
public class CommandUtils {

    /**
     * Liste les dossiers de types de structures disponibles
     * @return Liste des noms de dossiers (types)
     * @throws IOException Si une erreur survient lors de la lecture des dossiers
     */
    public static List<String> listStructureDirectories() throws IOException {
        Path structuresDir = Paths.get("config", WhereIsHumanity.MOD_ID, "structures");
        if (!Files.exists(structuresDir)) {
            return Collections.emptyList();
        }

        List<String> directories = new ArrayList<>();
        try (Stream<Path> paths = Files.list(structuresDir)) {
            paths.filter(Files::isDirectory)
                 .map(path -> path.getFileName().toString())
                 .forEach(directories::add);
        }
        return directories;
    }

    /**
     * Liste les fichiers de structures pour un type donné
     * @param type Le type de structure
     * @return Liste des noms de structures (sans extension)
     * @throws IOException Si une erreur survient lors de la lecture des fichiers
     */
    public static List<String> listStructureFiles(String type) throws IOException {
        Path typeDir = Paths.get("config", WhereIsHumanity.MOD_ID, "structures", type.toLowerCase());
        if (!Files.exists(typeDir)) {
            return Collections.emptyList();
        }

        List<String> structures = new ArrayList<>();
        try (Stream<Path> paths = Files.list(typeDir)) {
            paths.filter(path -> path.toString().endsWith(".nbt"))
                 .map(path -> path.getFileName().toString().replace(".nbt", ""))
                 .forEach(structures::add);
        }
        return structures;
    }
}