
.PHONY: clean build release install-git-hooks

build:
	@./gradlew build --console=plain

clean:
	@./gradlew clean --console=plain

release:
	@./src/build-tools/release.sh

install-git-hooks:
	mkdir -p .git/hooks
	ln -s "$(PWD)/git/hooks/pre-commit" ".git/hooks/pre-commit"
