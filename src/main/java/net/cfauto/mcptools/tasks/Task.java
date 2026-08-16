package net.cfauto.mcptools.tasks;


import java.io.IOException;

/*
 Stolen from Installer Tools
 */
public abstract class Task {
    public abstract void process(String[] args) throws IOException;

    protected void error(String message) {
        log(message);
        throw new RuntimeException(message);
    }

    protected void log(String message) {
        System.out.println(message);
    }
}
