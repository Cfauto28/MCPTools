package net.cfauto.mcptools.tasks;


import com.cloudbees.diff.PatchException;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/*
 Stolen from Installer Tools
 */
public abstract class Task {
    public abstract void process(String[] args) throws Exception;

    protected void error(String message) {
        log(message);
        throw new RuntimeException(message);
    }

    protected void log(String message) {
        System.out.println(message);
    }

    //Stolen from the first result on google
    protected static void recurseDirectory(Path path, List<Path> files) throws IOException
    {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path))
        {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    recurseDirectory(entry, files);
                } else {
                    files.add(entry);
                }
            }
        }
    }
}
