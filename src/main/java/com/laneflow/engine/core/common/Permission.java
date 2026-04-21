package com.laneflow.engine.core.common;

public final class Permission {

    public static final String DEPT_READ      = "DEPT_READ";
    public static final String DEPT_WRITE     = "DEPT_WRITE";

    public static final String STAFF_READ     = "STAFF_READ";
    public static final String STAFF_WRITE    = "STAFF_WRITE";

    public static final String ROLE_READ      = "ROLE_READ";
    public static final String ROLE_WRITE     = "ROLE_WRITE";

    public static final String USER_READ      = "USER_READ";
    public static final String USER_WRITE     = "USER_WRITE";

    public static final String WORKFLOW_READ  = "WORKFLOW_READ";
    public static final String WORKFLOW_WRITE = "WORKFLOW_WRITE";

    public static final String TRAMITE_READ   = "TRAMITE_READ";
    public static final String TRAMITE_WRITE  = "TRAMITE_WRITE";

    public static final String REPORT_READ    = "REPORT_READ";

    public static final java.util.List<String> ALL = java.util.List.of(
            DEPT_READ, DEPT_WRITE,
            STAFF_READ, STAFF_WRITE,
            ROLE_READ, ROLE_WRITE,
            USER_READ, USER_WRITE,
            WORKFLOW_READ, WORKFLOW_WRITE,
            TRAMITE_READ, TRAMITE_WRITE,
            REPORT_READ
    );

    private Permission() {}
}
