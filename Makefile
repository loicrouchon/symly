RELEASER_JAR=tools/releaser/target/releaser-*.jar
RELEASER=java -cp $(RELEASER_JAR)

.PHONY: build
build: build-local

.PHONY: clean
clean:
	@./mvnw clean

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
	@cd tools/releaser && ../../mvnw -q verify

.PHONY: clean-releaser
clean-releaser:
	@cd tools/releaser && ../../mvnw -q clean

.PHONY: build-local
build-local:
	./mvnw spotless:apply clean verify

.PHONY: codegen
codegen:
	./mvnw spotless:apply clean verify -Pstandard,codegen

.PHONY: build-ci
build-ci:
	./mvnw clean verify -Pstandard,ci

.PHONY: build-debian
build-debian:
	./mvnw --settings settings-debian.xml clean verify

.PHONY: debian-build-env
debian-build-env:
	@podman run -ti \
		-v "$(shell pwd):/workspace" \
		-w /workspace \
		debian:bookworm \
		bash -c "apt update \
			&& apt install openjdk-17-jdk-headless maven maven-debian-helper libpicocli-java -y \
			&& bash"
