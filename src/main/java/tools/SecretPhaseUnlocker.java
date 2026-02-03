package tools;

import domain.Profile;
import repository.ProfileRepository;

import java.lang.reflect.Method;
import java.util.List;

public class SecretPhaseUnlocker {
    private static final List<Integer> FIBONACCI_KEY = List.of(1, 1, 2, 3, 5, 8, 13);

    private final ProfileRepository profileRepository;

    public SecretPhaseUnlocker(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public void tryUnlock(Class<?> solutionClass, Profile profile) {
        if (profile.isSecretUnlocked()) {
            return;
        }

        try {
            Method doNotTouchMethod = solutionClass.getMethod("doNotTouch");
            Object result = doNotTouchMethod.invoke(null);

            if (result instanceof List<?> list && list.equals(FIBONACCI_KEY)) {
                profile.unlockSecret();
                profileRepository.save(profile);
                displayUnlockBanner();
            }
        } catch (Exception e) {
            // doNotTouch 메서드 호출 실패 시 무시
        }
    }

    private void displayUnlockBanner() {
        System.out.println();
        System.out.println("██████████████████████████████████████");
        System.out.println("█                                    █");
        System.out.println("█   ░░░ HIDDEN SEQUENCE DETECTED ░░░ █");
        System.out.println("█                                    █");
        System.out.println("██████████████████████████████████████");
        System.out.println();
        System.out.println("🔓 Secret Phase 해금!");
        System.out.println();
    }
}
