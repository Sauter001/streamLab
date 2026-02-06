package tools.grader.level;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import constants.LevelConstants;
import domain.AnalysisContext;
import domain.validation.ValidationResult;
import domain.validation.factory.ViolationFactory;
import tools.validator.CompositeValidator;
import tools.validator.MaxVariableDeclarationValidator;
import tools.validator.Validator;

import java.io.File;

/**
 * 코드 구조 검증 클래스
 * - 레벨별 적절한 Validator 생성
 * - 소스 파일 파싱 및 검증 실행
 */
public class CodeValidator {
    private final int level;

    public CodeValidator(int level) {
        this.level = level;
    }

    public Validator createValidator(LevelTestData.ValidationConfig config) {
        CompositeValidator validator = getValidatorByLevel();
        validator.add(new MaxVariableDeclarationValidator(config.getMaxVariables()));
        return validator;
    }

    public ValidationResult validateMethod(File sourceFile, String methodName, Validator validator) {
        try {
            JavaParser parser = new JavaParser();
            CompilationUnit cu = parser.parse(sourceFile).getResult().orElseThrow();
            AnalysisContext context = AnalysisContext.forMethod(cu, methodName);
            return validator.validate(context);
        } catch (Exception e) {
            return ValidationResult.error("파일 파싱 실패: " + e.getMessage());
        }
    }

    private CompositeValidator getValidatorByLevel() {
        if (level <= LevelConstants.STRICT_VALIDATOR_MAX_LEVEL) {
            return ViolationFactory.createStrictestValidator();
        }
        if (level <= LevelConstants.MAX_LEVEL) {
            return ViolationFactory.createBlockLambdaAllowedValidator();
        }
        return ViolationFactory.createSecretPhaseValidator();
    }
}
