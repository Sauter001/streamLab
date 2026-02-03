package util;

public final class ErrorLogger {

    private ErrorLogger() {
    }

    /**
     * 사용자에게 보여지는 에러 메시지 (채점 관련)
     */
    public static void user(String message) {
        System.err.println("❌ " + message);
    }

    /**
     * 시스템 내부 에러 (파일 감시, 인프라)
     */
    public static void system(String message) {
        System.err.println("[SYSTEM] " + message);
    }

    /**
     * 시스템 내부 에러 (예외 포함)
     */
    public static void system(String message, Throwable cause) {
        System.err.println("[SYSTEM] " + message + " - " + cause.getMessage());
    }
}
