package net.cfauto.mcptools.tasks;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import org.cadixdev.mercury.Mercury;
import org.cadixdev.mercury.remapper.MercuryRemapper;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class SourceRemapTask extends Task{
    @Override
    public void process(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        OptionSpec<File> oldDirArg = parser.accepts("oldDir", "Directory to remap").withRequiredArg().ofType(File.class).required();
        OptionSpec<File> newDirArg = parser.accepts("newDir", "Directory to remap to").withRequiredArg().ofType(File.class).required();
        OptionSpec<File> mapFileArg = parser.accepts("map", "Mappings file to use").withRequiredArg().ofType(File.class).required();
        OptionSpec<String> fromNamespaceArg = parser.accepts("fromNamespace", "Original namespace").withRequiredArg().ofType(String.class).required();
        OptionSpec<String> toNamespaceArg = parser.accepts("toNamespace", "Namespace to remap to").withRequiredArg().ofType(String.class).required();
//      TODO: Classpath support
//      parser.accepts("lib", "Classpath libraries, separated by :").withRequiredArg().withValuesSeparatedBy(":");

        //These are set to null because even if we exit, if they aren't set ,idea complains
        File oldDir = null;
        File newDir = null;
        File mapFile = null;
        String fromNamespace = null;
        String toNamespace = null;
//      List<File> classpath = null;

        try {
            OptionSet options = parser.parse(args);
            oldDir = options.valueOf(oldDirArg);
            newDir = options.valueOf(newDirArg);
            mapFile = options.valueOf(mapFileArg);
            fromNamespace = options.valueOf(fromNamespaceArg);
            toNamespace = options.valueOf(toNamespaceArg);
//          classpath = (List<File>) options.valueOf("lib");

        } catch (Exception ex) {
            parser.printHelpOn(System.out);
            ex.printStackTrace();
        }

        VisitableMappingTree tree = new MemoryMappingTree();
        log("Reading mapping file " + mapFile + " of type " + MappingReader.detectFormat(mapFile.toPath()));
        MappingReader.read(mapFile.toPath(), MappingReader.detectFormat(mapFile.toPath()), tree);
        IMappingProvider mappingProvider = TinyUtils.createMappingProvider(tree, fromNamespace, toNamespace);

        if (!newDir.exists() || !newDir.isDirectory()) {
            error("Input must be a directory!");
        }

        if (!oldDir.exists()) {
            if (!oldDir.mkdir()) {
                error("Creating output directory failed!");
            }
        } else if (!oldDir.isDirectory()) {
            error("Output must be a directory!");
        }

        log("Remapping...");
        try {
            Mercury mercury = new Mercury();
/*          if (classpath != null) {
                for (File file : classpath)
                    mercury.getClassPath().add(file.toPath());
            }*/
            mercury.getProcessors().add(MercuryRemapper.create(TinyRemapper.newRemapper().withMappings(mappingProvider).build().getEnvironment()));
            mercury.rewrite(newDir.toPath(), oldDir.toPath());
        } catch (Exception ex) {
            error("Failed to remap source: " + ex);
        }
    }
}
