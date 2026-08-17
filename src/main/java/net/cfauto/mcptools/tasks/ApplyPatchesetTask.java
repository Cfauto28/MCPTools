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
import java.text.ParseException;
import java.util.Objects;

public class ApplyPatchesetTask extends Task{
    @Override
    public void process(String[] args) throws IOException, PatchException {
        OptionParser parser = new OptionParser();
        OptionSpec<File> targetArg = parser.accepts("target", "Directory to apply the patchset to").withRequiredArg().ofType(File.class).required();
        OptionSpec<File> patchesArg = parser.accepts("patches", "Patchset directory").withRequiredArg().ofType(File.class).required();
        OptionSpec<File> rejectsArg = parser.accepts("rejects", "Directory in which to place the rejects").withRequiredArg().ofType(File.class).required();

        File target = null;
        File patches = null;
        File rejects = null;

        try {
            OptionSet options = parser.parse(args);
            target = options.valueOf(targetArg);
            patches = options.valueOf(patchesArg);
            rejects = options.valueOf(rejectsArg);
        } catch (Exception ex) {
            parser.printHelpOn(System.out);
            ex.printStackTrace();
        }

        if (!patches.exists()) {
            patches.mkdir();
        }
        if (!patches.isDirectory()) {
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


        log("Started Patching");
        boolean failed = false;
        //TODO: Find out how to make this work
        for (File file : patches.listFiles()) {
            if (file.getPath().endsWith(".patch")) {
                ContextualPatch patch = ContextualPatch.create(file, target);
                ContextualPatch.PatchReport status = patch.patch(false).iterator().next();
                log("Patched: " + file);
                if (status.getStatus() == ContextualPatch.PatchStatus.Patched) {
                    status.getOriginalBackupFile().delete();
                } else {
                    failed = true;
                    if (rejects != null) {
                        File output = new File(rejects, patches.getCanonicalPath());
                        output.getParentFile().mkdirs();
                        output.createNewFile();
                        FileInputStream fileInputStream = new FileInputStream(file);
                        FileOutputStream fileOutputStream = new FileOutputStream(output);
                        fileOutputStream.write(fileInputStream.read());
                    }
                    error("Failed to apply: " + file);
                    if (status.getFailure() instanceof ParseException) {
                        error(status.getFailure().getMessage());
                    } else {
                        status.getFailure().printStackTrace();
                    }
                }

                File NUL = new File("/dev/null");
                if (System.getProperty("os.name").toLowerCase().contains("win") && NUL.exists()) {
                    NUL.delete();
                }

                if (failed) {
                    error("One or more patches failed to apply, see log for details");
                }

                log("Finished patching!");

            }
        }
    }
}
