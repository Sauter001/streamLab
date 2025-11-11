import controller.GameController;
import ui.view.ConsoleView;

public class Application {
    public static void main(String[] args) {
        ConsoleView view = new ConsoleView();
        GameController gameController = new GameController(view);
        gameController.run();
    }
}
