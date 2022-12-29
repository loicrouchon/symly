
.PHONY: clean build version-check release $(RELEASER)

MAIN_GW=./gradlew --console=plain
RELEASER_GW=./gradlew --console=plain --build-file=releaser/build.gradle.kts
RELEASER=releaser/build/install/releaser/bin/releaser

build:
	@$(MAIN_GW) build

clean:
	@$(MAIN_GW) --quiet clean

version-check: $(RELEASER)
	@$(RELEASER) check --dry-run

release: $(RELEASER)
	@$(RELEASER) release --dry-run

$(RELEASER): clean
	@$(RELEASER_GW) --quiet clean installDist
