package net.cfauto.mcptools.tasks;

import com.cloudbees.diff.ContextualPatch;
import com.cloudbees.diff.PatchException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ApplyPatchesetTask extends Task{
    @Override
    public void process(String[] args) throws IOException, PatchException {
        OptionParser parser = new OptionParser();
        OptionSpec<File> targetArg = parser.accepts("target", "Directory to apply the patchset to").withRequiredArg().ofType(File.class).required();
        OptionSpec<File> patchesArg = parser.accepts("patches", "Patchset directory").withRequiredArg().ofType(File.class).required();
        OptionSpec<File> rejectsArg = parser.accepts("rejects", "Directory in which to place the rejects").withRequiredArg().ofType(File.class).required();

        File target = null;
        File patchesDir = null;
        File rejects = null;
        List<Path> patchFiles = new ArrayList<>();

        try {
            OptionSet options = parser.parse(args);
            target = options.valueOf(targetArg);
            patchesDir = options.valueOf(patchesArg);
            rejects = options.valueOf(rejectsArg);
        } catch (Exception ex) {
            parser.printHelpOn(System.out);
            ex.printStackTrace();
        }

        if (!patchesDir.exists()) {
            patchesDir.mkdir();
        }
        if (!patchesDir.isDirectory()) {
            error("Patches directory is not a directory");
        }
        if (!target.exists()) {
            target.mkdir();
        }
        if (!target.isDirectory()) {
            error("Target directory is not a directory");
        }
        if (!rejects.exists()) {
            rejects.mkdir();
        }
        if (!rejects.isDirectory()) {
            error("Rejects directory is not a directory");
        }


        recurseDirectory(patchesDir.toPath(), patchFiles);
        log(patchFiles.toString());
        boolean failed = false;
        log("Started Patching");
        for (Path path : patchFiles) {
            if (path.toString().endsWith(".patch")) {
                log("Started patching: " + path);
                ContextualPatch patch = ContextualPatch.create(path.toFile(), target);
                ContextualPatch.PatchReport report = patch.patch(false).iterator().next();
                log("Patched: " + path);
                if (report.getStatus() == ContextualPatch.PatchStatus.Patched) {
                    report.getOriginalBackupFile().delete();
                } else {
                    failed = true;
                    File output = new File(rejects, patchesDir.getCanonicalPath());
                    output.getParentFile().mkdirs();
                    output.createNewFile();
                    FileInputStream fileInputStream = new FileInputStream(path.toFile());
                    FileOutputStream fileOutputStream = new FileOutputStream(output);
                    fileOutputStream.write(fileInputStream.read());
                    error("Failed to apply: " + path);
                    if (report.getFailure() instanceof ParseException) {
                        error(report.getFailure().getMessage());
                    } else {
                        report.getFailure().printStackTrace();
                    }
                }

                File NUL = new File("/dev/null");
                if (System.getProperty("os.name").toLowerCase().contains("win") && NUL.exists()) {
                    NUL.delete();
                }

                if (failed) {
                    error("One or more patches failed to apply, see log for details");
                }

            }
        }
        log("Finished patching!");
    }
}
