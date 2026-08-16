package net.cfauto.mcptools.tasks;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

import java.io.File;
import java.io.IOException;

public class ConvertTask extends Task{
    @Override
    public void process(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        OptionSpec<File> oldMapArg = parser.accepts("oldMap", "Original Mapping File").withRequiredArg().ofType(File.class).required();
        OptionSpec<File> newMapArg = parser.accepts("newMap", "New Mapping File").withRequiredArg().ofType(File.class).required();
        OptionSpec<MappingFormat> oldTypeArg = parser.accepts("oldType", "Old Mapping Format").withRequiredArg().ofType(MappingFormat.class).required();
        OptionSpec<MappingFormat> newTypeArg = parser.accepts("newType", "New Mapping Format").withRequiredArg().ofType(MappingFormat.class).required();

        //These are set to null because even if we exit if they aren't set idea complains
        File oldMap = null;
        File newMap = null;
        MappingFormat oldFormat = null;
        MappingFormat newFormat = null;
        
        try {
            OptionSet options = parser.parse(args);
            oldMap = options.valueOf(oldMapArg);
            newMap = options.valueOf(newMapArg);
            oldFormat = options.valueOf(oldTypeArg);
            newFormat = options.valueOf(newTypeArg);

        } catch (Exception ex) {
            parser.printHelpOn(System.out);
            ex.printStackTrace();
        }

        try {
            VisitableMappingTree tree = new MemoryMappingTree();
            log("Reading mapping file " + newMap + " of type " + oldFormat);
            MappingReader.read(oldMap.toPath(), oldFormat, tree);
            log("Writing mapping file " + oldMap + " of type " + newFormat);
            tree.accept(MappingWriter.create(newMap.toPath(), newFormat));
            log("Converted mappings successfully");
        } catch (Exception ex) {
            error("Failed to convert mappings:" + ex);
        }
    }
}
