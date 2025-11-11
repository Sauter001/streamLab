package controller;

import lombok.RequiredArgsConstructor;
import ui.view.ConsoleView;

@RequiredArgsConstructor
public class GameController {
    private final ConsoleView view;

    public void run() {
        view.showIntro();
    }
}
