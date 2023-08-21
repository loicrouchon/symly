MAVEN_WRAPPER=./mvnw
MAVEN_RELEASER_WRAPPER=cd tools/releaser && ../../mvnw --batch-mode --quiet
RELEASER_JAR=tools/releaser/target/releaser-*.jar
RELEASER=java -cp $(RELEASER_JAR)

.PHONY: build
build: build-local

.PHONY: clean
clean:
	@$(MAVEN_WRAPPER) clean

.PHONY: version
version: $(RELEASER_JAR)
	@$(RELEASER) releaser.Releaser version

.PHONY: version-check
version-check: $(RELEASER_JAR)
	@$(RELEASER) releaser.Releaser check

.PHONY: jreleaser-dry-run
jreleaser-dry-run: build
	@echo "fake token" | ./releaser/src/main/resources/jreleaser-dry-run.sh "$(shell make version)"

.PHONY: release
release: $(RELEASER_JAR)
	@$(RELEASER) releaser.Releaser release

.PHONY: publish
publish: $(RELEASER_JAR)
	@$(RELEASER) releaser.Publisher

$(RELEASER_JAR):
	@$(MAVEN_RELEASER_WRAPPER) verify

.PHONY: clean-releaser
clean-releaser:
	@$(MAVEN_RELEASER_WRAPPER) clean

.PHONY: build-local
build-local:
	$(MAVEN_WRAPPER) spotless:apply clean verify

.PHONY: codegen
codegen:
	$(MAVEN_WRAPPER) spotless:apply clean verify -Pstandard,codegen

.PHONY: build-ci
build-ci:
	$(MAVEN_WRAPPER) clean verify -Pstandard,ci

.PHONY: build-debian
build-debian:
	$(MAVEN_WRAPPER) --settings settings-debian.xml clean verify

.PHONY: debian-build-env
debian-build-env:
	@podman run -ti \
		-v "$(shell pwd):/workspace" \
		-w /workspace \
		debian:bookworm \
		bash -c "apt update \
			&& apt install openjdk-17-jdk-headless maven maven-debian-helper libpicocli-java -y \
			&& bash"
