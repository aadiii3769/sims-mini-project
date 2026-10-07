package com.sims.model;

/**
 * POJO mapping to the {@code DEPARTMENT} database table.
 */
public class Department {

    private long   deptId;
    private String deptName;
    private String deptCode;

    public Department() {}

    public Department(long deptId, String deptName, String deptCode) {
        this.deptId   = deptId;
        this.deptName = deptName;
        this.deptCode = deptCode;
    }

    public long getDeptId()                     { return deptId; }
    public void setDeptId(long deptId)          { this.deptId = deptId; }

    public String getDeptName()                 { return deptName; }
    public void setDeptName(String deptName)    { this.deptName = deptName; }

    public String getDeptCode()                 { return deptCode; }
    public void setDeptCode(String deptCode)    { this.deptCode = deptCode; }

    @Override
    public String toString() {
        return deptCode + " - " + deptName;
    }
}
