package net.cfauto.mcptools.tasks;

import com.cloudbees.diff.PatchException;
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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SourceRemapTask extends Task{
    @Override
    public void process(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        OptionSpec<File> oldDirArg = parser.accepts("oldDir", "Directory to remap").withRequiredArg().ofType(File.class).required();
        OptionSpec<File> newDirArg = parser.accepts("newDir", "Directory to remap to").withRequiredArg().ofType(File.class).required();
        OptionSpec<File> mapFileArg = parser.accepts("map", "Mappings file to use").withRequiredArg().ofType(File.class).required();
        OptionSpec<String> fromNamespaceArg = parser.accepts("fromNamespace", "Original namespace").withRequiredArg().ofType(String.class).required();
        OptionSpec<String> toNamespaceArg = parser.accepts("toNamespace", "Namespace to remap to").withRequiredArg().ofType(String.class).required();
        OptionSpec<File> classpathDirArg = parser.accepts("lib", "Directory containing classpath libraries").withRequiredArg().ofType(File.class).required();

        //These are set to null because even if we exit, if they aren't set ,idea complains
        File oldDir = null;
        File newDir = null;
        File mapFile = null;
        String fromNamespace = null;
        String toNamespace = null;
        List<Path> classpath = new ArrayList<>();
        File classpathDir = null;

        try {
            OptionSet options = parser.parse(args);
            oldDir = options.valueOf(oldDirArg);
            newDir = options.valueOf(newDirArg);
            mapFile = options.valueOf(mapFileArg);
            fromNamespace = options.valueOf(fromNamespaceArg);
            toNamespace = options.valueOf(toNamespaceArg);
            classpathDir = options.valueOf(classpathDirArg);

        } catch (Exception ex) {
            parser.printHelpOn(System.out);
            ex.printStackTrace();
        }

        VisitableMappingTree tree = new MemoryMappingTree();
        log("Reading mapping file " + mapFile + " of type " + MappingReader.detectFormat(mapFile.toPath()));
        MappingReader.read(mapFile.toPath(), MappingReader.detectFormat(mapFile.toPath()), tree);
        IMappingProvider mappingProvider = TinyUtils.createMappingProvider(tree, fromNamespace, toNamespace);

        if (!oldDir.exists() || !oldDir.isDirectory()) {
            error("Input must be a directory!");
        }
        if (!classpathDir.exists() || !classpathDir.isDirectory()) {
            error("Libraries must be a directory!");
        }
        if (!newDir.exists()) {
            if (!newDir.mkdir()) {
                error("Creating output directory failed!");
            }
        } else if (!newDir.isDirectory()) {
            error("Output must be a directory!");
        }

        recurseDirectory(classpathDir.toPath(), classpath);
        try {
            log("Setting up Mercury");
            Mercury mercury = new Mercury();
            mercury.getClassPath().addAll(classpath);
            mercury.getProcessors().add(MercuryRemapper.create(TinyRemapper.newRemapper()
                    .withMappings(mappingProvider)
                    .fixPackageAccess(true)
                    .ignoreFieldDesc(true)
                    .inferNameFromSameLvIndex(true)
                    .ignoreConflicts(true)
                    .build().getEnvironment()));
            log("Remapping...");
            mercury.rewrite(newDir.toPath(), oldDir.toPath());
        } catch (Exception ex) {
            error("Failed to remap source: " + ex);
        }
    }

}
