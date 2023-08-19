
.PHONY: clean build version version-check jreleaser-dry-run release publish clean-releaser

MAIN_GW=./gradlew --console=plain
RELEASER_GW=./gradlew --console=plain --build-file=releaser/build.gradle.kts
RELEASER_JAR=releaser/build/libs/releaser.jar
RELEASER=java -cp $(RELEASER_JAR)

build:
	@$(MAIN_GW) build

clean:
	@$(MAIN_GW) --quiet clean

version: $(RELEASER_JAR)
	@$(RELEASER) releaser.Releaser version

version-check: $(RELEASER_JAR)
	@$(RELEASER) releaser.Releaser check

jreleaser-dry-run: build
	@echo "fake token" | ./releaser/src/main/resources/jreleaser-dry-run.sh "$(shell make version)"

release: $(RELEASER_JAR)
	@$(RELEASER) releaser.Releaser release

publish: $(RELEASER_JAR)
	@$(RELEASER) releaser.Publisher

$(RELEASER_JAR):
	@$(RELEASER_GW) --quiet build

clean-releaser:
	@$(RELEASER_GW) --quiet clean

.PHONY: build-local
build-local:
	mvn spotless:apply clean verify

.PHONY: build-ci
build-ci:
	mvn clean verify -Pstandard,ci

.PHONY: build-debian
build-debian:
	mvn --settings settings-debian.xml clean verify

.PHONY: debian-build-env
debian-build-env:
	@podman run -ti \
		-v "$(shell pwd):/workspace" \
		-w /workspace \
		debian:bookworm \
		bash -c "apt update \
			&& apt install openjdk-17-jdk-headless maven maven-debian-helper libpicocli-java -y \
			&& bash"
