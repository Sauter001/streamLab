package config;

import handler.IntroHandler;
import handler.LevelHandler;
import handler.MainHandler;
import handler.ProfileHandler;
import handler.SecretHandler;
import handler.TutorialHandler;
import repository.LevelDataRepository;
import repository.ProfileRepository;
import tools.SecretPhaseUnlocker;
import ui.view.common.CommonConsoleView;
import ui.view.level.LevelConsoleView;
import ui.view.main.MainConsoleView;
import ui.view.profile.ProfileConsoleView;
import ui.view.secret.SecretConsoleView;
import ui.view.tutorial.TutorialConsoleView;

public class HandlerConfig {
    public static IntroHandler createIntroHandler() {
        return new IntroHandler(new CommonConsoleView());
    }

    public static ProfileHandler createProfileHandler() {
        return new ProfileHandler(new ProfileConsoleView(), new ProfileRepository());
    }

    public static TutorialHandler createTutorialHandler() {
        return new TutorialHandler(new TutorialConsoleView());
    }

    public static MainHandler createMainHandler() {
        ProfileRepository profileRepository = new ProfileRepository();
        return new MainHandler(new MainConsoleView(), profileRepository, new SecretPhaseUnlocker(profileRepository));
    }

    public static LevelHandler createLevelHandler() {
        return new LevelHandler(new LevelConsoleView(), new LevelDataRepository(), new ProfileRepository());
    }

    public static SecretHandler createSecretHandler() {
        return new SecretHandler(new SecretConsoleView(), new LevelDataRepository(), new ProfileRepository());
    }
}
