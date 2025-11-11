package domain.problem.product;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
@Getter
public class Transaction {
    private final String storeName;
    private final ProductCategory productCategory;
    private final int price;
}
