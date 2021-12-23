package org.symly.cli.validation;

import java.util.Collection;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class Constraints implements Constraint {

    private final Collection<Constraint> allConstraints;

    @Override
    public Optional<String> violation() {
        return allConstraints.stream()
            .map(Constraint::violation)
            .flatMap(Optional::stream)
            .findFirst();
    }
}
