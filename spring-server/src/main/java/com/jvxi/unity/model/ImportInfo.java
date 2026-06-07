package com.jvxi.unity.model;

import java.util.List;

/**
 * 导入信息
 */
public class ImportInfo {
    private String dllName;
    private List<ImportFunction> functions;

    public String getDllName() { return dllName; }
    public void setDllName(String dllName) { this.dllName = dllName; }
    public List<ImportFunction> getFunctions() { return functions; }
    public void setFunctions(List<ImportFunction> functions) { this.functions = functions; }

    public static class ImportFunction {
        private String name;
        private int hint;
        private String thunkRva;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getHint() { return hint; }
        public void setHint(int hint) { this.hint = hint; }
        public String getThunkRva() { return thunkRva; }
        public void setThunkRva(String thunkRva) { this.thunkRva = thunkRva; }
    }
}
