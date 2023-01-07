
.PHONY: clean build version-check release $(RELEASER)

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

release: $(RELEASER_JAR)
	@$(RELEASER) releaser.Releaser release

release: $(RELEASER)
	@$(RELEASER) release

$(RELEASER_JAR):
	@$(RELEASER_GW) --quiet build

clean-releaser:
	@$(RELEASER_GW) --quiet clean
