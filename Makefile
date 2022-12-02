
.PHONY: clean build release

build:
	@./mvnw verify

clean:
	@./mvnw clean

release:
	@./src/build-tools/release.sh
