package org.linky.cli.validation;

import java.util.Collection;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class Constraints implements Constraint {

    private final Collection<Constraint> constraints;

    @Override
    public Optional<String> violation() {
        return constraints.stream()
                .map(Constraint::violation)
                .flatMap(Optional::stream)
                .findFirst();
    }
}
