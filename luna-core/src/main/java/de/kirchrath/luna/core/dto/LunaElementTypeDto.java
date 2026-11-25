package de.kirchrath.luna.core.dto;

public enum LunaElementTypeDto {
    TASK,
    ALERT,
    APPOINTMENT,
    INVOICE,
    CONTRACT,
    NOTE;

    /**
     * Annotation values have to be compile-time constants, so the names of the enum values are repeated here.
     */
    public static final class Constants {

        public static final String TASK = "TASK";
        public static final String ALERT = "ALERT";
        public static final String APPOINTMENT = "APPOINTMENT";
        public static final String INVOICE = "INVOICE";
        public static final String CONTRACT = "CONTRACT";
        public static final String NOTE = "NOTE";

        private Constants() {
        }
    }
}
