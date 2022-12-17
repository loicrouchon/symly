
.PHONY: clean build release dirty-check

build:
	@./gradlew build --console=plain

clean:
	@./gradlew clean --console=plain

dirty-check:
	@./src/docs/resources/dirty-check.sh

release:
	@./src/build-tools/release.sh
