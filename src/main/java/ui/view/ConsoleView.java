package ui.view;

public class ConsoleView {
    private static final String TITLE_HEADER = """
        ╔══════════════════════════════════════════════════════╗
        ║                                                      ║
        ║            🧪 Welcome to Stream Lab 🧪              ║
        ║                                                      ║
        ║   Mastering Java Streams, One Challenge at a Time.   ║
        ║   v1.0.0                                             ║
        ╚══════════════════════════════════════════════════════╝
        """;

    public void showIntro() {
        System.out.println(TITLE_HEADER);
    }
}
