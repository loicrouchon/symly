
.PHONY: clean build release

build:
	@./gradlew build --console=plain

clean:
	@./gradlew clean --console=plain

release:
	@./src/build-tools/release.sh
