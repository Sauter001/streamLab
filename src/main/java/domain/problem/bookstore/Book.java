package domain.problem.bookstore;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@EqualsAndHashCode
@Getter
class Book {
    String title;
    List<Author> authors; // N:M (공저자)
    int publishYear;
    double price;
    Genre genre;
}
