MAVEN_WRAPPER=./mvnw
MAVEN_RELEASER_WRAPPER=cd tools/releaser && ../../mvnw --batch-mode --quiet
RELEASER_JAR=tools/releaser/target/releaser-*.jar
RELEASER=java -cp $(RELEASER_JAR)

.PHONY: build
build: build-local

.PHONY: clean
clean:
	@$(MAVEN_WRAPPER) clean

.PHONY: format
format:
	@$(MAVEN_WRAPPER) spotless:apply

.PHONY: version
version: $(RELEASER_JAR)
	@$(RELEASER) releaser.Releaser version

.PHONY: version-check
version-check: $(RELEASER_JAR)
	@$(RELEASER) releaser.Releaser check

.PHONY: check-for-versions-updates
check-for-versions-updates:
	@$(MAVEN_WRAPPER) versions:display-plugin-updates versions:display-dependency-updates

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
	@$(MAVEN_WRAPPER) spotless:apply clean verify

.PHONY: codegen
codegen:
	@$(MAVEN_WRAPPER) spotless:apply clean verify -Pcodegen

.PHONY: build-all-assemblies
build-all-assemblies:
	@$(MAVEN_WRAPPER) clean verify -Pall-assemblies
