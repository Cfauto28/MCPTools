package net.cfauto.mcptools.tasks;

import java.util.function.Supplier;

/*
 Stolen from Installer Tools
 */
@SuppressWarnings("unused")
public enum Tasks {
    SOURCE_REMAP(SourceRemapTask::new),
    CONVERT_MAPPINGS(ConvertTask::new);
// TODO: MCPConfig apply patches, check here https://github.com/MinecraftForge/ProPatcher


    private Supplier<? extends Task> supplier;

    Tasks(Supplier<? extends Task> supplier) {
        this.supplier = supplier;
    }

    @SuppressWarnings("unchecked")
    public <T extends Task> T get() {
        return (T)supplier.get();
    }
}
