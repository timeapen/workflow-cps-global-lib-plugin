package org.jenkinsci.plugins.workflow.cps.global;

import groovy.lang.Binding;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.CpsThread;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

/**
 * Global variable backed by user-supplied script.
 *
 * @author Kohsuke Kawaguchi
 */
// not @Extension because these are manually registered
public class UserDefinedGlobalVariable extends GlobalVariable {
    private final WorkflowLibRepository repo;
    private final String name;

    /*package*/ UserDefinedGlobalVariable(WorkflowLibRepository repo, String name) {
        this.repo = repo;
        this.name = name;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    @Nonnull
    @Override
    public Object getValue(@Nonnull CpsScript script) throws Exception {
        Binding binding = script.getBinding();
        Object instance;
        if (binding.hasVariable(getName())) {
            instance = binding.getVariable(getName());
        } else {
            CpsThread c = CpsThread.current();
            if (c==null)
                throw new IllegalStateException("Expected to be called from CpsThread");

            instance = c.getExecution().getShell().getClassLoader().parseClass(source(".groovy")).newInstance();
            binding.setVariable(getName(), instance);
        }
        return instance;
    }

    /**
     * Loads help from HTML, if available.
     */
    public String getHelpHtml() throws IOException {
        File help = source(".html");
        if (!help.exists())     return null;

        return FileUtils.readFileToString(help, Charsets.UTF_8);
    }

    private File source(String extension) {
        return new File(repo.workspace, PREFIX+"/"+ name + extension);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserDefinedGlobalVariable that = (UserDefinedGlobalVariable) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /*package*/ static final String PREFIX = "vars";
}
